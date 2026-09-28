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
"哪些是原作者的、哪些是 AI 生成的"。当前该 diff 为：**5 个文件，+292/−29**，真正的代码改动只有
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

## 修复进度（分支 `mcr/mantle-1.11`）

**158 → 51 个编译错误**，每个 checkpoint 一个提交，逐个 push：

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

1. **剩余 51 个错误**，集中在：
   - `CombatHelper`(4)：`ForgeHooks`/`ForgeEventFactory`/`ToolActions`/`ItemStack#getSweepHitBox` —— 需要自己写垫片
   - `client/book/StructureInfo`(4)、`element/StructureElement`(3)：`pos()`/`ModelData`（Forge 扩展）
   - `util/JsonHelper`(3)：`PacketDistributor`/`PacketTarget`（Forge 网络）
   - `loot/function/SetFluidLootFunction`(3)：`FluidStackLoadable` 位置变更
   - `client/render/MantleShaders`(2)：Forge 的 shader 注册事件
   - `block/fluid/BurningLiquidBlock`/`MobEffectLiquidBlock`/`FluidDeferredRegister`(共 5)：
     `LiquidBlock` 在 vanilla 收 `FlowingFluid` 而非 `Supplier`
   - `ItemStackLoadable`(2)：`readShareTag`/`getShareTag`（Forge 的 NBT 分享机制）
   - datagen 的 `PackOutput` → `FabricDataOutput`(2)
2. `./gradlew build` 出包；决定发布方式（`publishToMavenLocal` / 自有 maven / 本地 jar）
3. 与 Tinkers 侧联调：让 `TinkersConstruct` 的端口改用 Mantle 1.11
4. **每完成一批立刻 commit + push 作为 checkpoint**（分工要求）

**修复建议顺序**：流体 → Capability → 注册表 → datagen → 零散项。前两类占了 60% 的错误，
且与 Tinkers 侧共用同一套映射经验，先啃能复用。

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
