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
| 6 | `MantleItemLayerModel`（模型渲染类型） | 每层的 render type 通过 `RenderTypeGroup` 传给 `CompositeModel` | Porting Lib 2.3.16 的 `addQuads` **不接受 render type**，逐层渲染类型不再传递 | 功能缺失 | 模型可能以默认渲染层绘制，**半透明/裁剪表现可能退化**。代码内已标 TODO | 模型烘焙无报错（2026-09-29 客户端启动）；**渲染观感仍未验证** |
| 7 | `StructureElement`（书本结构预览） | `BlockEntity#getModelData()` 提供方块实体的模型数据 | 用空 `ModelData`（Fabric 的 `BlockEntity#getModelData` 不存在） | 功能缺失 | 书本里的结构预览可能丢失 retextured 方块的模型数据 | 未验证 |
| 8 | `DisplayContextLoadable`（网络格式） | `ForgeRegistries.DISPLAY_CONTEXTS` 的注册表 id | Buf 里读写 vanilla enum（`writeEnum`/`readEnum`） | 表示法变更 | **只影响跨版本/跨加载器的兼容性**：Fabric 端内部一致，但与 Forge 端或旧版 Mantle 通信会不兼容。若有存档/NBT 依赖需迁移 | — |
| 9 | `EdibleItem`（食物属性） | Forge 的 `Item#getFoodProperties(stack, entity)`（stack/实体感知） | vanilla 的 `Item#getFoodProperties()`（物品级） | **近似** | 依赖实体状态（如口渴、状态效果）动态改变食物属性的物品会丢失该行为 | 未验证 |
| 10 | `ShapedRetexturedRecipe`（配方原料解析） | Forge `CraftingHelper.getIngredient(json, flag)` | vanilla `Ingredient.fromJson(json, flag)` | 待核对 | 两者都接受一个 boolean，但语义（禁止空气 / 允许空）可能相反。**需要对照源码确认** | 待核对 |
| 11 | `Advancement.Builder#criteria` | Forge 放宽为可直接访问的字段 | 使用 vanilla 的 `getCriteria()` | 语义等价 | 无 | — |
| 12 | `Holder` | Forge 让 `Holder` 实现 `Supplier` | vanilla 的 `Holder` 不是 `Supplier`，改用 `holder::value` | 语义等价 | 无 | — |
| 13 | `StructureElement`（结构预览的渲染层） | `BakedModel#getRenderTypes(state, random, modelData)` 逐层取 render type，再用 Forge 扩展签名的 `tesselateBlock(..., modelData, renderType)` 绘制 | vanilla 的 `ModelBlockRenderer#tesselateBlock` 既不收 `ModelData` 也不收 `RenderType`，改成一次性绘制进 `TRANSLUCENT_FULLBRIGHT` 缓冲 | 功能缺失（同 #6） | 书本结构预览里逐模型指定的渲染层不再被尊重，与 #6 是同一类退化 | 模型烘焙无报错；书本预览的观感仍未验证 |
| 14 | `NBTKeyModel`（模型渲染类型） | `CompositeModel.Baked.Builder#addQuads(renderType, quads)` 传入模型级 render type | Porting Lib 2.3.16 的 `addQuads` 不接受 render type | 功能缺失（同 #6） | 与 #6 相同：可能以默认渲染层绘制 | 模型烘焙无报错；渲染观感仍未验证 |
| 15 | `QuadBakingVertexConsumer`（默认精灵） | 默认精灵 = Forge 的 `UnitTextureAtlasSprite.INSTANCE`（1×1 全透明） | Fabric 没有该精灵；默认值改为惰性解析 vanilla 的 `missingno` 精灵 | **近似** | 只有当调用方**忘记** `setSprite` 时才会用到默认值。仓库内所有调用点（`MantleItemLayerModel`）都会先设置精灵，因此该分支实际不可达 | 客户端启动无报错且该回退分支不可达（无实际可验证行为） |
| 16 | `BurnableHangingSignItem`（燃料值） | 覆写 `Item#getBurnTime(ItemStack, RecipeType)`，燃料值可按物品栈 / recipe type 动态变化 | 改为 Fabric `FuelRegistry`：每物品一个静态值，忽略 recipe type 与物品栈 | **近似** | vanilla 熔炉的燃料值一致；Forge 上可以按 recipe type 区分的行为（若有）丢失 | 未验证 |
| 17 | `MantleShaders`（着色器注册） | Forge `RegisterShadersEvent#registerShader` | Fabric `CoreShaderRegistrationCallback#register`，注册名与 vertex format 完全一致 | 语义等价 | 无（仅注册时机/宿主不同） | **已实测**（2026-09-29 客户端启动：`block_fullbright` 与 `fluid` 两个着色器均经 Fabric 回调注册，无编译/加载报错） |
| 18 | `ArgumentTypeDeferredRegister`（参数类型同步） | Forge 给原版打了 `ArgumentTypeInfos#registerByClass`，用于把参数类型注册进类表 | 原版 1.20.1 无此方法；用 access widener 打开私有 `BY_CLASS` 直接写入，语义与 `ArgumentTypeInfos#register` 中那一步完全相同 | 语义等价 | 无 | 未验证（建议用 `/mantle` 子命令实测一次客户端↔服务端参数同步） |
| 19 | `ClientSourcesCommand`（客户端命令参数） | vanilla `ResourceLocationArgument#getId(context, name)`（被写死为 `CommandSourceStack`） | Fabric 的客户端命令源不是 `CommandSourceStack`，改用通用的 `context.getArgument(name, ResourceLocation.class)` | 语义等价 | 无 | — |
| 20 | `RenderItem.REGISTRY` / `ColoredBlockModel` / `JsonHelper` / `DefaultRetexturedBlockEntity` | 各自的 Forge 形态（见 STATUS.md 的映射表） | 补上 Fabric reload listener 必需的 id、`bakedBuilder` 显式传 `isGui3d=false`、改用 Mantle 自带的 `PacketDistributor`、`getRenderData()` 返回 `ModelData` | 语义等价 | 无（`DefaultRetexturedBlockEntity` 实为**修复**：还原了上游 `getModelData()` 的语义） | — |
| 21 | **流体单位（全局）** | Forge 的流体单位是 mB，**1 桶 = 1000**；上游 Forge Mantle 的 `MantleValues.BOTTLE = 250` | Fabric Transfer API 用 droplet，**1 桶 = 81000**，`FluidConstants.BOTTLE = 27000`。1.11 分支的 `MantleValues` 已经改用 `FluidConstants`，所以 datagen 产出的数值整体放大 81 倍（例：`fluid_transfer/wet_sponge.json` 的 `amount` 从 `250` 变成 `27000`） | **表示法变更（跨加载器不可通约）** | 任何**为 Forge 版 Mantle 手写**的流体数据包数值在 Fabric 侧都差 81 倍；TCon 等下游的流体数据必须用 Fabric 单位重新 datagen。仓库里 `src/generated/resources` 下 23 个流体 JSON 已按 Fabric 单位重新生成 | **已实测**（datagen 重跑 + 服务器加载 5 个动态修饰符正常） |
| 22 | `OffhandCooldownTracker`（CCA entrypoint 构造器） | 上游 1.11 分支把 1.20.1 分支的无参构造器弄丢了；CCA 用反射实例化 entrypoint 类，于是**整个 mod 初始化失败**（每个实体构造都抛异常，服务器一生成区块就崩） | 恢复 1.20.1 分支的 `public OffhandCooldownTracker() { this.player = null; }` | **修复**（移植回归，非设计差异） | 无（恢复到了 1.20.1 的既有行为） | **已实测**（修复前 `runServer` 崩溃；修复后服务器 `Done (28.575s)`、客户端 92 mods 启动无报错） |
| 23 | `OffhandCooldownTracker` 的 NBT 读写 | `readFromNbt` 里写 tag、`writeToNbt` 里读 tag —— **方向写反了**。CCA 的契约是 `readFromNbt` 从 tag 读入组件，因此冷却状态存不下来（存档时把字段重置为 0） | 把两个方法体调换回正确方向 | **修复**（两个分支都存在的上游 bug，非本移植引入） | 冷却状态现在能跨存档保留（这是本来就应该有的行为） | 未实测（需实测：收起/切换武器后重进游戏看冷却是否保留） |

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
