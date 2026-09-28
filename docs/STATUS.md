# 进度状态 — Mantle (Fabric)

> 最后更新：2026-09-28　|　性质见 [ATTRIBUTION.md](../ATTRIBUTION.md)

## 为什么这个仓库重要

上游 Tinkers' Construct **3.12.1 要求 Mantle `[1.11.113,)`**，
而公开发布的 Fabric 版 Mantle（`https://mvn.devos.one/snapshots/`，group `slimeknights.mantle`）**只到 `1.20.1-1.9.296`**。

→ **Mantle 1.11 的 Fabric 移植是 Tinkers 同步的前置卡点。**

## 分支地图

| 分支 | 版本 | 状态 |
|---|---|---|
| `1.20.1`（默认） | 1.9.x | 当前对外发布所对应的分支 |
| `1.20.1-update` | 1.11 | **原作者 AlphaMode 的 WIP 分支**（tip `eb1e9a5a`，2026-01-12）。不要在其上提交 |
| **`mcr/mantle-1.11`** | **1.11** | **我们的工作分支**（`mcr` = MinecraftReconstruction），基点即上述 `eb1e9a5a` —— 主战场 |
| `1.21.1` | — | Alpha 的 1.21.1 尝试 |
| `1.11` / `1.12` / … | — | 继承自上游 SlimeKnights 的 Forge 分支，不是 Fabric 适配 |

### 归属与分支纪律

`Alpha-s-Stuff/Mantle` 里的 `1.20.1-update` 是 **AlphaMode 的工作分支**，本仓库 fork 时一并带入。
我们的所有改动都应落在 `mcr/*` 分支上，这样 `git diff eb1e9a5a..mcr/mantle-1.11` 就能一眼看出
"哪些是原作者的、哪些是 AI agent 改的"。当前该 diff 为：**5 个文件，+292/−29**，真正的代码改动只有
`MantleItemLayerModel.java`。

✅ **已于 2026-09-28 清理**：我们 fork 的 `1.20.1-update` 已 force push 还原到 Alpha 的原始 tip `eb1e9a5a`，
与上游完全一致；我们的全部改动都在 `mcr/mantle-1.11`。
默认分支 `1.20.1` **刻意保留**文档提交（仅新增文档、未改代码），以保证仓库首页能看到归属与 "largely vibed" 声明。

## `1.20.1-update` 的现状（已复现）

分支 tip：`eb1e9a5a`，提交信息 **`6 errors left (I'm lazy ok)`**，2026-01-12。
提交历史本身记录了收敛过程：`87 errors left` → `43 errors left` → `6 errors left`。

本机复现（JDK 21，`./gradlew compileJava`）首轮只报：

```
5 errors，全部在同一个文件：
src/main/java/slimeknights/mantle/client/model/util/MantleItemLayerModel.java
  :81  cannot find symbol: IGeometryBakingContext (x2, 返回类型与参数)
  :505 cannot find symbol: IGeometryBakingContext / RenderTypeGroup (x3)
```

### ⚠️ `6 errors left` 是个假象 —— 真实剩余错误是 **158 个**

那 5 个错误已在提交 `756dad64` 修复（详见下节）。修完之后重新编译，**错误数不降反升**：

```
158 errors / 55 files
```

原因：javac 默认 `-Xmaxerrs 100` 截断输出，且当某个符号无法解析时，javac 会**抑制依赖它的后续错误**。
所以 `6 errors left` 只是"被截断后还能看见的 6 个"，真实工作量从未暴露。
用 `-Xmaxerrs 100000` 重跑即可看到全貌（本仓库不落盘该参数，可用 `gradle -I <init script>` 注入）。

错误画像（这是判断剩余工作性质的关键）：

| 错误类型 | 数量 |
|---|---|
| `cannot find symbol` | 93 |
| `incompatible types`（如 `PackOutput` → `FabricDataOutput`） | 25 |
| `package ... does not exist`（如 `ForgeRegistries`） | 5 |
| 其余（方法签名不匹配、override 失效、私有访问等） | 35 |

找不到的符号几乎全是 Forge API，尚未迁移到 Fabric：

```
ICondition(8)  IFluidHandlerItem(7)  ForgeCapabilities(6)  LazyOptional(6)
IFluidHandler(5)  FluidAction(4)  ItemHandlerHelper(3)  EmptyFluidHandler(3)
IForgeRegistry(2)  ForgeHooks  ForgeEventFactory  FMLEnvironment
ToolActions  PacketDistributor  Registries ...
```

错误最集中的文件：

```
33  slimeknights/mantle/fluid/FluidTransferHelper.java
16  slimeknights/mantle/datagen/MantleFluidTransferProvider.java
 9  slimeknights/mantle/command/tags/ModifyTagCommand.java
 7  slimeknights/mantle/command/TagsForCommand.java
 6  slimeknights/mantle/data/loadable/common/DisplayContextLoadable.java
 6  slimeknights/mantle/client/screen/book/BookScreen.java
 ...
```

→ **结论：Fluid API 迁移仍是最大的一块**（占 49/158），与 Tinkers 侧的历史教训一致。
这也意味着 Mantle 1.11 Fabric 不是"差 6 个错误"，而是**差一轮中等规模的 API 迁移**。

### 根因（已定位）

Porting Lib 升到 `2.3.16-beta.81` 后，**Forge 的 geometry API 被移除或替换**：

| Forge（上游 Mantle 1.11 用） | Porting Lib 2.3.16-beta.81 | 
|---|---|
| `net.minecraftforge.client.model.geometry.IGeometryBakingContext` | 概念消失；`IUnbakedGeometry.bake()` 的上下文直接是 vanilla `BlockModel`（`class_793`） |
| `net.minecraftforge.client.RenderTypeGroup` | 类不存在 |
| `net.minecraftforge.client.ForgeRenderTypes` | 类不存在；替代为 `RenderTypeUtil.get(ResourceLocation) → RenderType` |
| `CompositeModel.Baked.Builder.addQuads(RenderTypeGroup, Collection)` | 签名变为 `addQuads(Collection<BakedQuad>)`，**不再接受 render type** |

相关证据（Porting Lib 源码，已解包核对）：

```java
// porting_lib/models/geometry/IUnbakedGeometry.java
class_1087 bake(class_793 context, class_7775 baker, Function<class_4730, class_1058> spriteGetter,
                class_3665 modelState, class_806 overrides, class_2960 modelLocation, boolean isGui3d);

// porting_lib/models/util/RenderTypeUtil.java
public static class_1921 get(class_2960 name);   // solid/cutout/cutout_mipped/translucent/tripwire
```

### 待修清单（建议顺序）

1. `getDefaultRenderType(IGeometryBakingContext)` → 参数类型改 `BlockModel`，返回值改 `RenderType`；
   默认值原为 `RenderTypeGroup(RenderType.translucent(), ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT)`
   —— Fabric 侧已无 `ForgeRenderTypes`，**需要决定用什么替代（这是本任务里最需要人工判断的一处）**。
2. `LayerData.getRenderType(IGeometryBakingContext, RenderTypeGroup)` → 同上去掉 `RenderTypeGroup`，
   改用 `RenderTypeUtil.get(...)` 解析 `render_type` 字段，解析不到再回落到默认值。
3. `record QuadGroup(RenderTypeGroup renderType, ...)` 与 `modelBuilder.addQuads(quadGroup.renderType, quadGroup.quads)`
   → 适配 Porting Lib 新签名；**注意：渲染类型语义可能因此静默改变，必须验证半透明/裁剪表现**。
4. 参考实现：`GeometryContextWrapper`（Fabric 版已改为 `extends BlockModel`，而不是 Forge 的 `implements IGeometryBakingContext`），
   以及本仓库 `client/model/util/fabric/` 下已有的兼容层（`QuadBakingVertexConsumer`、`TransformingVertexPipeline`、`VertexConsumerWrapper`）。
5. 上游 Forge 版对照文件：`SlimeKnights/Mantle` 分支 `1.20` 的同名文件。

### 参考数据

上游 Mantle 自身的演进规模（`v1.9.54 → v1.11.117`）：**513 个文件，+26,684 / −7,785 行**，
其中 161 个文件的 1.11 版本含 Forge 引用（即需要 Fabric 化）。

## 已完成

- [x] fork 到组织，配置 remote
- [x] 定位公开发布坐标与 maven 来源（`mvn.devos.one/snapshots`，最新 `1.20.1-1.9.296`，**尚无 1.11**）
- [x] 找到 Fabric 版 Mantle 的**源码仓库**（`Alpha-s-Stuff/Mantle`，公开，MIT）
- [x] 复现 `1.20.1-update` 的编译错误并定位根因（Porting Lib API 变更）
- [x] 修复首批 5 个编译阻断（提交 `756dad64`，Porting Lib geometry API 移除）
- [x] 揭穿 `6 errors left` 的假象：真实剩余 **158 errors / 55 files**，并完成归类
- [x] 归属声明、状态与交接文档
- [x] **修复 8 个 checkpoint：158 → 51 个编译错误**（详见下节）

## ⏩ 恢复点（给下一个 agent 的第一屏）

**当前：编译错误 158 → 0，`./gradlew compileJava` BUILD SUCCESSFUL**（全部已 push 到 `mcr/mantle-1.11`）。

**下一条命令**（编译已通过，接下来是出包 + 热测试）：

```bash
cd ~/Desktop/repo/mr-mantle-fabric
git checkout mcr/mantle-1.11 && git pull
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew build      # 出 jar
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew runServer  # 注册表/数据加载（run/eula.txt 已建）
```

需要**重新枚举错误数**时记得带 `-Xmaxerrs`：javac 默认截断到 100 条，而且符号解析失败会抑制后续错误，
会把 158 个错误显示成 5 个（原作者提交信息里的 `6 errors left` 就是这么来的）。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew compileJava \
  -I /Users/huangwenqin/Documents/Codex/2026-09-28/ni/work/maxerrs.gradle
```

**还剩什么**（编译过了 ≠ 完成）：
1. `./gradlew build` 出包，确认 jar 内容与 `fabric.mod.json`
2. `./gradlew runServer` 确认注册表 / 数据加载；`runClient` 进世界看模型与书本渲染
3. 逐条实测 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md) 里标"未验证"的条目，重点 **#3 流体浸没判定**、**#6/#13/#14 渲染类型**、**#16 燃料值**
4. 全部验证完，才谈"建新仓库发布 / 把 jar-in-jar 的 Mantle 拆成独立前置"

**做每条改动时的规矩**：如果替换**不是语义等价**，必须在 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md) 登记一行（类别 / 影响面 / 验证状态），并在提交信息里点名"行为差异"。

## 热测试记录（2026-09-29，JDK 21，macOS）

第一次把 1.11 分支真的跑起来，**抓到 2 个编译期看不出来的运行时 bug**，修完复测通过。

| 项目 | 结果 |
|---|---|
| `./gradlew build` | **BUILD SUCCESSFUL**（含 `runDatagen`、`validateAccessWidener`）；产出 `Mantle-1.20.1-1.11.DEV.<sha>.jar`（1.8 MB） |
| `./gradlew runServer` | **`Done (28.575s)!`**，加载 78 个模组；启动、区块生成、`/reload` 全部无异常 |
| `./gradlew runClient` | **启动到主菜单成功**，加载 92 个模组；资源重载、全部模型烘焙、着色器注册、地图集创建全部无异常 |
| 日志里的 ERROR/FATAL | **0 条**（服务器和客户端都是 0；修复前：语言文件解析失败 + CCA 初始化失败 + 每次区块生成都崩） |

### 客户端加载验证（2026-09-29 02:13，第一次真正启动客户端）

客户端**没有被操作过**（没有 GUI 自动化手段），但"启动到主菜单"这一趟已经能验证相当多东西：

```
Setting user: Player751                      -> 客户端入口点构造成功
Reloading ResourceManager: ... mantle ...    -> 资源重载包含 Mantle
(Mantle) Loaded 0 fluid textures             -> 流体贴图加载器
(FluidTooltipHandler) Loaded 6 fluid unit lists   -> 流体单位/提示（移植过的 loadable）
(Mantle) Finished loading 0 Block entity items    -> RenderItem.STATE_REGISTRY（我补的 reload listener id）
(Mantle) Finished loading 0 Block entity fluids   -> FluidCuboid
Sound engine started / Created: ...atlas...  -> 贴图图集与全部内建模型烘焙完成，无模型报错
```

即：**模型烘焙、资源重载、着色器注册、各数据加载器都在客户端真实跑过一遍且无报错**。
唯一剩下的空白是"进世界以后看起来对不对"（渲染观感），这需要人眼或用 GUI 自动化。
顺带确认：日志里那条 `Shader rendertype_entity_translucent_emissive could not find sampler` 与 Mantle 无关
（是原版/Porting Lib 的既有告警），`star:` 的 blockstate 告警来自 star 模组。

### 这一次修掉的运行时 bug

1. **`assets/mantle/lang/en_us.json` 是坏 JSON** —— 第 86 行漏了一个逗号（手写文件，datagen 不覆盖它）。
   后果：所有本地化字符串加载失败（`Language.loadDefault` 抛 `MalformedJsonException`）。
   这个 bug 在 1.20.1 分支上也存在，属于"一直没人真的启动过"。
2. **CCA entrypoint 构造器丢失** —— 见 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md) 第 22 条。
   后果：整个模组初始化失败，服务器一生成区块就 `ReportedException` 崩掉。
   顺带修了同一文件里 NBT 读写方向写反的老 bug（第 23 条）。
3. **`src/generated/resources` 是用 Forge 单位生成的陈年产物** —— 重跑 datagen 后 23 个流体 JSON 的数值
   从 mB 换算到 Fabric droplet（例：`wet_sponge` 250 → 27000）。详见第 21 条。

### 用控制台命令验证过的代码路径（不需要玩家在线）

```
mantle tags for id minecraft:block minecraft:stone          -> 列出 15 个 tag ✓
mantle tags view minecraft:block minecraft:mineable/pickaxe -> 列出 ~400 个值 ✓
mantle sources data recipes minecraft:stick                 -> "Sources for minecraft:recipes/stick.json: vanilla" ✓
mantle sources data path minecraft:recipes/stick.json       -> 同上（两条支路都通）✓
mantle tags add minecraft:block minecraft:mcr_test minecraft:stone    -> 写出数据包 ✓
mantle tags remove minecraft:block minecraft:mcr_test minecraft:stone -> 写出数据包 ✓
reload                                                       -> 生成的 tag 可见（空值，与 remove 一致）✓
```

生成的 `datapacks/SlimeKnightsGenerated/data/minecraft/tags/blocks/mcr_test.json` 内容为 `{"values": []}`，
JSON 结构正常（**没有** Forge 的 `remove` 列表 —— 正是第 1 条差异）。

### 还没验证的

- **渲染观感**：客户端能启动、模型能烘焙，但**没有人看过画面**。第 6、13、14 条（渲染类型）仍是"烘焙无报错、
  观感未知"。需要人眼，或给客户端加 GUI 自动化 / 截图权限。
- **需要下游模组才能测**：燃料值（第 16 条）依赖 `BurnableHangingSignItem` 的实际实例，而 Mantle 自己
  不注册这样一件物品 —— 只能等 Tinkers' Construct 接上来之后测。流体浸没判定（第 3 条）同理（`BurningLiquidBlock`
  的用法在下游）。
- **需要专门写测试**：参数类型网络同步（第 18 条，需要客户端↔服务端握手）、NBT 持久化（第 23 条，
  需要存档/重载）、流体浸没（第 3 条）。

## 修复进度（分支 `mcr/mantle-1.11`）

**158 → 0 个编译错误**，每个 checkpoint 一个提交，逐个 push（`—` 表示当时没有单独记录错误数）：

| # | 提交 | 内容 | 错误数 |
|---|---|---|---|
| 1 | `756dad64` | Porting Lib 2.3.16 移除了 Forge geometry API（`IGeometryBakingContext`/`RenderTypeGroup`/`ForgeRenderTypes`） | 158 → 122 |
| 2 | `fccc790a` | **`fluid` 整包**迁移到 Fabric Transfer API（capability → `FluidStorage`/`ContainerItemContext`/`Transaction`） | 122 → 109 |
| 3 | `790662ab` | 数据生成条件：`ICondition`/`NotCondition`/`TagFilledCondition` → Fabric `ConditionJsonProvider` | 109 → 88 |
| 4 | `02b64911` | tag 命令：Forge 给原版 `TagFile`/`TagEntry` 打的 `remove` 补丁在 Fabric 不存在，去掉了该支路 | 88 → 77 |
| 5 | `e909cdc0` | registry 查询、`RecipeManagerAccessor`、`ItemDisplayContext`、`ForgeRegistries.DISPLAY_CONTEXTS` | 77 → 75 |
| 6 | `775a058c` | `Holder` 适配（vanilla 的 `Holder` 不是 `Supplier`）、`PortingLibFluids.FLUID_TYPES` | 75 → 75 |
| 7 | `ec24e9e3` | **access widener 补齐** Forge 用 AT 打开的私有成员；能用公开 getter 的就用 getter | 75 → 62 |
| 8 | `9e6d0f03` | reload listener 注册（Fabric 要求 id）、`FMLEnvironment`、`BlockTags.create`、`getRecipeWidth` | 62 → 51 |
| 9 | `c422b6f6` | `CombatHelper` 的 Forge 钩子迁到 Porting Lib（暴击事件、工具损毁） | — |
| 10 | `06549d40` | 流体方块改用 vanilla 的 `LiquidBlock`/`BucketItem` 签名 | — |
| 11 | `c5db9246` | 剩余的 vanilla-vs-Forge 访问器差异 | — |
| 12 | `30e42d4f` | 剩余的访问器/查表差异，并登记行为差异 | 51 → 16 |
| 13 | 本批（本轮） | 收尾 16 个：geometry helper、`addQuads` 签名、`bakedBuilder`、默认精灵、`ArgumentTypeInfos`、Fabric 燃料表、客户端命令参数、Forge 网络 → Mantle 自带 `PacketDistributor`、shader 注册回调 | 16 → **0** |
| 14 | `28645f3a` | **第一次真机跑服务器**抓到的两个运行时 bug（坏掉的 `en_us.json`、CCA entrypoint 构造器丢失），外加按 Fabric 单位重跑 datagen | 0 → 0（编译本来就过，但服务器从「必崩」变成 `Done (28.575s)`） |
| 15 | 本批（i18n） | 补齐并统一 6 个语言文件（含从上游**还原** `es_cl`/`ru_ru`）、修 zh_cn 的位置参数 bug、新增 `validateLangFiles` 校验任务 —— 见 [I18N.md](I18N.md) | 0 → 0 |

### 已确认的 API 映射（可直接复用）

| Forge | Fabric / Porting Lib |
|---|---|
| `getCapability(ForgeCapabilities.FLUID_HANDLER, side)` | `FluidStorage.SIDED.find(level, pos, side)` |
| `getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)` | `FluidStorage.ITEM.find(stack, ContainerItemContext)` |
| `handler.fill(stack, FluidAction.SIMULATE/EXECUTE)` | `StorageUtil.simulateInsert(...)` + 提交 `Transaction` |
| `handler.drain(max, SIMULATE)` | `TransferUtil.firstCopyOrEmpty(storage)` |
| `IFluidHandlerItem#getContainer()` | `ContainerItemContext#getItemVariant().toStack()` |
| `ICondition` / `NotCondition` / `TagFilledCondition` | `DefaultResourceConditions.tagsPopulated/not` |
| `ForgeRegistries.FLUID_TYPES` | `PortingLibFluids.FLUID_TYPES` |
| `ForgeRegistries.DISPLAY_CONTEXTS` | 无（vanilla 就是 enum，按 `getSerializedName()` 查） |
| `FMLEnvironment.dist == Dist.CLIENT` | `FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT` |
| `BlockTags/ItemTags.create(id)` | `TagKey.create(Registries.BLOCK/ITEM, id)` |
| `RecipeManager#byType`（Forge 放宽） | `RecipeManagerAccessor#port_lib$byType` |
| `Holder#getTagKeys()` | `Holder#tags()`（返回 `Stream`） |
| Forge AT 打开的私有成员 | `mantle.accesswidener` 里的 `transitive-accessible` |

## 未完成

1. **编译已过，但一行都还没在游戏里跑过** —— 见本文件顶部"还剩什么"
2. `./gradlew build` 出包；决定发布方式（`publishToMavenLocal` / 自有 maven / 本地 jar）
3. 逐条实测 [BEHAVIOUR-DIFFERENCES.md](BEHAVIOUR-DIFFERENCES.md)，特别是 **#3 / #6 / #13 / #14 / #16**
4. 与 Tinkers 侧联调：让 `TinkersConstruct` 的端口改用 Mantle 1.11，并评估把 jar-in-jar 的 Mantle 拆成独立前置
5. **每完成一批立刻 commit + push 作为 checkpoint**（分工要求）

## 已修复内容（`756dad64`）

| 位置 | 原（Forge / 旧 Porting Lib） | 现（Porting Lib 2.3.16） |
|---|---|---|
| `getDefaultRenderType` | `RenderTypeGroup getDefaultRenderType(IGeometryBakingContext)` | `RenderType getDefaultRenderType(BlockModel)` |
| `LayerData#getRenderType` | `context.getRenderType(id)` → `RenderTypeGroup` | `RenderTypeUtil.get(id)` → `RenderType`，失败回落默认值 |
| `QuadGroup` + `addQuads` | `addQuads(RenderTypeGroup, Collection)` | `addQuads(Collection)`（**已知渲染保真缺口，代码内标了 TODO**） |

⚠️ 第二项与第三项改变了渲染类型语义（半透明/裁剪），**不能只当作编译修补**，发布前必须做视觉验证。

## 环境

- 必须 **JDK 21**：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
- 首次构建含依赖下载约 **9 分钟**，之后 1–3 分钟
- 构建命令：`./gradlew compileJava`（快速验证）/ `./gradlew build`
