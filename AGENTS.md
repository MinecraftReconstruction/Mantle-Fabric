# AGENTS.md — 给接手的 AI 智能体

本仓库是 `MinecraftReconstruction` 组织下的**非官方、AI 生成（largely vibed）** Mantle Fabric 移植工程，
是 Tinkers' Construct 同步的前置卡点。

## 动手之前

1. 读 [ATTRIBUTION.md](ATTRIBUTION.md)（归属与风险声明，**不得**删改其中"非官方 / AI 生成 / 未经原作者审核"的表述）
2. 读 [docs/STATUS.md](docs/STATUS.md)（进度、已定位的根因、待修清单）
3. 姊妹仓库 `MinecraftReconstruction/TinkersConstruct` 的 `docs/PLAN.md` 有整体路线

## 主战场

分支 **`ai/mantle-1.11`**（不是默认分支 `1.20.1`）。它承载 Mantle **1.11** 的 Fabric 移植工作。

## 分支约定（重要）

| 分支 | 归属 | 规则 |
|---|---|---|
| `1.20.1`（默认）、`1.20.1-update`、`1.21.1`、`1.20-dev`、`1.20.1-port` | **原作者 AlphaMode** | **不要在它们上面直接提交**。它们代表上游的工作，保持原样才能干净地对照与拉取更新 |
| `1.11`、`1.12`、`1.15`… | **上游 SlimeKnights** | 这些是 **Forge** 分支，不是 Fabric 适配，仅作参考 |
| **`ai/*`** | **本组织（AI 生成）** | 我们的全部改动都在这里。当前工作分支为 `ai/mantle-1.11`，基点 = `1.20.1-update` 的 tip `eb1e9a5a` |

这样做的好处：`git diff eb1e9a5a..ai/mantle-1.11` 就是"我们到底做了什么"的完整答案
（当前净值：5 个文件，+292/−29，其中真正的代码改动只有 `MantleItemLayerModel.java` 一个文件）。
提交信息里也要写清是 AI 生成的改动。

### 已知遗留

`MinecraftReconstruction/Mantle-Fabric` 上的 **`1.20.1-update` 分支目前被我们叠了提交**（早期误提交所致）。
理想状态是把它还原到 Alpha 的原始 tip `eb1e9a5a`，让上游分支保持纯净——这需要一次 force push，
**待人工确认后执行**。在此之前，请以 `ai/mantle-1.11` 为工作分支，不要继续往 `1.20.1-update` 上提交。

## 硬性规则

- `LICENSE`（MIT，SlimeKnights 版权）不得修改或删除；第三方依赖（Porting Lib 为 LGPL）条款必须遵守
- 署名要求同 `ATTRIBUTION.md`：SlimeKnights 与 AlphaMode 是原作者，我们只是 AI 生成的衍生工作
- 不得暗示原作者认可或背书本仓库
- 没跑过验证就不算做完

## 环境与验证

- **JDK 21**：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew compileJava`
- 首次约 9 分钟（依赖下载），之后 1–3 分钟
- 改完必须重跑 `compileJava`；能出包后跑 `build`

## 文档纪律

- 每修完一项就更新 `docs/STATUS.md`（修了什么、怎么验证、还剩什么）
- 失败尝试也写进去，注明"已试过、无效、原因"
- 新发现的 API 变更要写清**证据来源**（类名、文件路径、命令行），下一位接手者才能复现你的判断
