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
| **`1.20.1-update`** | **1.11** | **Mantle 1.11 的 Fabric 移植 WIP —— 主战场** |
| `1.21.1` | — | Alpha 的 1.21.1 尝试 |
| `1.11` / `1.12` / … | — | 继承自上游 SlimeKnights 的 Forge 分支，不是 Fabric 适配 |

## `1.20.1-update` 的现状（已复现）

分支 tip：`eb1e9a5a`，提交信息 **`6 errors left (I'm lazy ok)`**，2026-01-12。
提交历史本身记录了收敛过程：`87 errors left` → `43 errors left` → `6 errors left`。

本机复现（JDK 21，`./gradlew compileJava`）：

```
5 errors，全部在同一个文件：
src/main/java/slimeknights/mantle/client/model/util/MantleItemLayerModel.java
  :81  cannot find symbol: IGeometryBakingContext (x2, 返回类型与参数)
  :505 cannot find symbol: IGeometryBakingContext / RenderTypeGroup (x3)
```

（`:86` 的 `new RenderTypeGroup(...)` 与 `ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT` 因类型已报错而被 javac 抑制，
**实际待修点比 5 处更多**，见下。）

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

## 未完成

1. 修完 `1.20.1-update` 的编译错误
2. `./gradlew build` 出包；决定发布方式（`publishToMavenLocal` / 自有 maven / 本地 jar）
3. 与 Tinkers 侧联调：让 `TinkersConstruct` 的端口改用 Mantle 1.11

## 环境

- 必须 **JDK 21**：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
- 首次构建含依赖下载约 **9 分钟**，之后 1–3 分钟
- 构建命令：`./gradlew compileJava`（快速验证）/ `./gradlew build`
