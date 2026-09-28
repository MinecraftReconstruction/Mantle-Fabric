# 行为差异披露（Fabric 版 vs Forge 原版）

> 性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)。本文件记录**所有已知的、Fabric 版 Mantle 与 Forge 原版行为不一致的地方**。
> 任何改动只要不是"语义等价替换"，都必须在这里登记一行。

## 为什么要有这份文件

Forge 给原版打了很多补丁（额外的字段、事件、注册表、访问权限）。Fabric 上没有对应物时，只有三种选择：

1. 用 Fabric/Porting Lib 的等价 API 替换（语义等价 → 不必登记）
2. 自己写垫片（**近似** → 必须登记，并注明近似程度）
3. 直接去掉该能力（**功能缺失** → 必须登记，并注明影响面）

下面每一条都属于 2 或 3。

## 差异清单

| # | 位置 | Forge 原版行为 | Fabric 版行为 | 类别 | 影响面 | 验证状态 |
|---|---|---|---|---|---|---|
| 1 | tag 命令（`ModifyTagCommand` / `DumpTagCommand`） | Forge 给原版 `TagFile`/`TagEntry`/`TagLoader.EntryWithSource` 打了补丁，支持 `remove` 列表，可从**其他数据包**的 tag 中删除单个条目 | 原版 `TagFile` 只有 `(entries, replace)`。我们改成：只能追加条目，或整体替换（`replace=true`） | 功能缺失 | `/mantle modify_tag remove` 无法删除他人拥有的条目；`dump_tag` 不再显示 removals。**这是 Forge 独有的能力，Fabric 生态没有等价物** | 未验证 |
| 2 | `CombatHelper`（暴击） | `ForgeHooks.getCriticalHit` 返回 `ICriticalHitEvent`，监听器可用 `Result.DENY` **取消暴击** | 改用 Porting Lib 的 `CriticalHitEvent`。该事件**只暴露 `damageModifier`**，没有 result/cancel | 功能缺失 | 无法阻止暴击，只能调整伤害倍率。TCon 的工具/修饰符若依赖"取消暴击"会失效 | 未验证 |
| 3 | `BurningLiquidBlock` / `MobEffectLiquidBlock` | `Entity#getFluidTypeHeight(fluidType) > 0` —— 精确计算实体在该流体类型中的浸没高度 | 近似：`实体脚部 Y < 方块 Y + fluidState.getOwnHeight()` | **近似** | 半浸、流动流体、非整格液面等边缘情况的行为可能与 Forge 不同。**建议实测** | 未验证（需实测） |
| 4 | `ItemStackLoadable`（网络同步） | 只发送 `getShareTag()`（部分 NBT，Forge 的网络优化） | 发送 `stack.getTag()`（完整 NBT） | **近似** | 网络包体可能变大；语义上更完整（不会丢数据）。如果某些物品的 NBT 很大，需要关注带宽 | 未验证 |
| 5 | `CombatHelper`（工具损毁） | 触发 `PlayerDestroyItemEvent` | **移除**：Fabric 无对应事件，且 Fabric 上不存在能监听 Forge 事件的监听器 | 功能缺失（无实际影响） | 理论上无影响——Fabric 上没有任何代码能订阅那个 Forge 事件 | — |
| 6 | `MantleItemLayerModel`（模型渲染类型） | 每层的 render type 通过 `RenderTypeGroup` 传给 `CompositeModel` | Porting Lib 2.3.16 的 `addQuads` **不接受 render type**，逐层渲染类型不再传递 | 功能缺失 | 模型可能以默认渲染层绘制，**半透明/裁剪表现可能退化**。代码内已标 TODO | 已知缺口，待视觉验证 |
| 7 | `StructureElement`（书本结构预览） | `BlockEntity#getModelData()` 提供方块实体的模型数据 | 使用空 `ModelData` | 功能缺失 | 书本里的结构预览可能丢失 retextured 方块的模型数据 | 未验证 |
| 8 | `DisplayContextLoadable`（网络格式） | `ForgeRegistries.DISPLAY_CONTEXTS` 的注册表 id | Buf 里读写 vanilla enum（`writeEnum`/`readEnum`） | 表示法变更 | **只影响跨版本/跨加载器的兼容性**：Fabric 端内部一致，但与 Forge 端或旧版 Mantle 通信会不兼容。若有存档/NBT 依赖需迁移 | — |
| 9 | `EdibleItem`（食物属性） | Forge 的 `Item#getFoodProperties(stack, entity)`（stack/实体感知） | vanilla 的 `Item#getFoodProperties()`（物品级） | **近似** | 依赖实体状态（如口渴、状态效果）动态改变食物属性的物品会丢失该行为 | 未验证 |
| 10 | `ShapedRetexturedRecipe`（配方原料解析） | Forge `CraftingHelper.getIngredient(json, flag)` | vanilla `Ingredient.fromJson(json, flag)` | 待核对 | 两者都接受一个 boolean，但语义（禁止空气 / 允许空）可能相反。**需要对照源码确认** | 待核对 |
| 11 | `Advancement.Builder#criteria` | Forge 放宽为可直接访问的字段 | 使用 vanilla 的 `getCriteria()` | 语义等价 | 无 | — |
| 12 | `Holder` | Forge 让 `Holder` 实现 `Supplier` | vanilla 的 `Holder` 不是 `Supplier`，改用 `holder::value` | 语义等价 | 无 | — |

> 表中标注"未验证"的条目，**在专用服务器/客户端实际跑通对应玩法之前，都不能视为已完成**。
> 验证方式见 [STATUS.md](STATUS.md) 的验证阶梯。

## 需要特别当心的三条

1. **#3 流体浸没判定** —— 这是唯一一处我们明知是近似、且直接决定伤害/效果是否触发的逻辑，必须实测（把实体放在液面边缘、流动流体里各试一次）。
2. **#6 渲染类型** —— 编译能过但视觉可能悄悄退化，只能靠截图比对发现。
3. **#1 tag remove** —— 这是整个移植里唯一一处**功能性能力丢失**（不是性能或表示法差异），依赖该功能的 addon 需要单独评估。

## 如何更新本文件

- 新增一条差异 → 在表格末尾追加，并在提交信息里点名"行为差异"
- 验证通过某条 → 把"验证状态"改成"已验证（日期 + 验证方式）"，**不要删除该行**
- 发现等价替换被误判成差异 → 可以合并/删除，但要在提交信息里说明理由
