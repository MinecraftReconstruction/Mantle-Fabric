# AGENTS.md — 给接手的 AI 智能体

本仓库是 `MinecraftReconstruction` 组织下的**非官方、AI 生成（largely vibed）** Mantle Fabric 移植工程，
是 Tinkers' Construct 同步的前置卡点。

## 动手之前

1. 读 [ATTRIBUTION.md](ATTRIBUTION.md)（归属与风险声明，**不得**删改其中"非官方 / AI 生成 / 未经原作者审核"的表述）
2. 读 [docs/STATUS.md](docs/STATUS.md)（进度、已定位的根因、待修清单）
3. 姊妹仓库 `MinecraftReconstruction/TinkersConstruct` 的 `docs/PLAN.md` 有整体路线
4. 本地化相关工作读 [docs/I18N.md](docs/I18N.md)（含 AI 翻译范围的声明）
5. 行为差异读 [docs/BEHAVIOUR-DIFFERENCES.md](docs/BEHAVIOUR-DIFFERENCES.md)（**只要不是语义等价替换就必须登记一行**）

## 主战场

分支 **`mcr/mantle-1.11`**（现在是本仓库的默认分支）。它承载 Mantle **1.11** 的 Fabric 移植工作。

⚠️ 本仓库是 **non-fork 的 canonical 仓库**，**不携带上游分支**。上游分支
（`1.20.1`、`1.20.1-update`、`1.21.1`、`1.11`、`1.12` …）在
`MinecraftReconstruction/Mantle-Fabric-upstream`（旧 fork）与
`Alpha-s-Stuff/Mantle` / `SlimeKnights/Mantle`，需要对照时去那边拉。

## 分支约定（重要）

| 分支 | 归属 | 规则 |
|---|---|---|
| **`mcr/*`**（`mcr` = MinecraftReconstruction） | **本组织** | 我们的全部改动都在这里。当前工作分支 **`mcr/mantle-1.11`（默认分支）**，基点 = 上游 `1.20.1-update` 的 tip `eb1e9a5a`。在迁移到 canonical 仓库之前，这就是唯一一条分支；未来若开新分支（例如 1.21.1），沿用 `mcr/` 前缀 |
| `1.20.1`、`1.20.1-update`、`1.21.1`、`1.20-dev`、`1.20.1-port` | **原作者 AlphaMode** | 不在本仓库。需要时从 `Mantle-Fabric-upstream` 或 `Alpha-s-Stuff/Mantle` 拉，**不要在它们上面直接提交** |
| `1.11`、`1.12`、`1.15`… | **上游 SlimeKnights** | **Forge** 分支，不是 Fabric 适配，仅作参考 |

这样做的好处：`git diff eb1e9a5a..mcr/mantle-1.11` 就是"我们到底做了什么"的完整答案。
提交信息里也要写清是 AI 生成的改动。

### 已完成的清理

- **2026-09-28**：我们 fork 的 `1.20.1-update` 曾被误叠 7 个提交，已 force push 还原到 Alpha 的原始 tip
  `eb1e9a5a`（`6a26c223 → eb1e9a5a`）。我们的全部内容都在 `mcr/mantle-1.11` 上，没有丢失。
- **`1.20.1`（默认分支）刻意保留我们的文档提交**：只新增了 `ATTRIBUTION.md` / `AGENTS.md` / `docs/` 与 README 说明，
  **没有改动任何代码**。理由：仓库首页必须能看到"非官方 / AI 生成 / 未获上游背书"的声明，否则访客会误判这是官方仓库。
  代码改动一律只进 `mcr/*` 分支。

## 硬性规则

- `LICENSE`（MIT，SlimeKnights 版权）不得修改或删除；第三方依赖（Porting Lib 为 LGPL）条款必须遵守
- 署名要求同 `ATTRIBUTION.md`：SlimeKnights 与 AlphaMode 是原作者，我们只是 AI 生成的衍生工作
- 不得暗示原作者认可或背书本仓库
- 没跑过验证就不算做完

## 环境与验证

- **JDK 21**：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew compileJava`
- 首次约 9 分钟（依赖下载），之后 1–3 分钟
- 改完必须重跑 `compileJava`；能出包后跑 `build`

### 验证阶梯（别停在第 1 级）

| 级别 | 命令 | 现在到哪了 |
|---|---|---|
| 1 编译 | `./gradlew compileJava` | ✅ 0 错误 |
| 2 出包 + 校验 | `./gradlew check`（含 datagen、access widener、语言文件校验） | ✅ 通过 |
| 3 专用服务器 | `./gradlew runServer`（需要 `run/eula.txt`，改完可以顺手 `stop`） | ✅ `Done (28.575s)`、0 ERROR/FATAL |
| 4 客户端 | `./gradlew runClient` 进世界看模型 / 书本 / 着色器 | ❌ **一次都没跑过** |
| 5 行为差异实测 | 逐条核对 [BEHAVIOUR-DIFFERENCES.md](docs/BEHAVIOUR-DIFFERENCES.md) 的"验证状态" | ❌ 大部分还没做 |

命令行也能测服务端逻辑（不需要玩家在线）：`runServer` 起来后在控制台直接敲
`mantle tags for id minecraft:block minecraft:stone`、`mantle sources data recipes minecraft:stick`
这类命令，能覆盖大部分移植过的代码路径。

## 文档纪律

- 每修完一项就更新 `docs/STATUS.md`（修了什么、怎么验证、还剩什么）
- 失败尝试也写进去，注明"已试过、无效、原因"
- 新发现的 API 变更要写清**证据来源**（类名、文件路径、命令行），下一位接手者才能复现你的判断
