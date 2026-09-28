# Attribution / 归属声明

## ⚠️ 先读这段

本仓库是 **非官方、由 AI 大幅生成（largely vibed）** 的试验性工程，隶属 `MinecraftReconstruction` 组织。

- 代码几乎全部来自上游作者，我们的改动是移植与同步性质。
- **与任何原作者都没有隶属或背书关系**；原作者未参与、未审核，也不为本仓库的任何问题负责。
- 内容由 AI 智能体在人类指导下生成，**"largely vibed" 是准确描述**，请当作草稿而非参考实现。
- 想要稳定可用的版本，请使用上游项目。

## 上游与原作者

| 项目 | 作者 / 维护者 | 许可 | 说明 |
|---|---|---|---|
| [Mantle](https://github.com/SlimeKnights/Mantle) | **SlimeKnights** | MIT | 原始库，Forge 版 |
| [Mantle (Fabric)](https://github.com/Alpha-s-Stuff/Mantle) | **AlphaMode (Alpha)** | MIT | Mantle 的 Fabric 移植版，**本仓库直接 fork 自它**；`1.20.1-update` 分支上的 1.11 移植工作全部出自 Alpha |

第三方依赖（各自版权归其作者）：

- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib) — The Fabricators of Create，LGPL（自由软件，详见其 LICENSE）
- [Fabric API](https://github.com/FabricMC/fabric) — FabricMC，Apache-2.0
- [Architectury API](https://github.com/architectury/architectury-api)、[Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API)、[Reach Entity Attributes](https://github.com/JamiesWhiteShirt/reach-entity-attributes) 等，详见 `build.gradle`

`LICENSE` 保留上游 MIT 许可证与版权声明，未做修改。

## 仓库拓扑（2026-09-29 起）

| 仓库 | 角色 |
|---|---|
| **`MinecraftReconstruction/Mantle-Fabric`（本仓库，非 fork）** | **canonical 仓库**：面向用户与未来发布的正式仓库。为便于独立维护/发版而**新建**（不是 GitHub fork），但携带移植过程的**全部提交历史**，`mcr/mantle-1.11` 就是旧 fork 里那条工作分支，逐 commit 原样保留 |
| `MinecraftReconstruction/Mantle-Fabric-upstream` | 旧 fork（改名保留）：继续跟踪 AlphaMode 的上游分支（`1.20.1`、`1.20.1-update`、`1.21.1` …），也用于对照拉取上游更新 |
| `MinecraftReconstruction/Mantle` | SlimeKnights/Mantle 的 fork，仅作 Forge 版 diff 基准 |

**历史归属不受影响**：本仓库里只要有据可查的提交，作者仍是原作者；用
`git diff eb1e9a5a..mcr/mantle-1.11` 就能精确切出"我们改了什么"。`eb1e9a5a` 是 AlphaMode 在 1.11 分支上的最后一个提交。

---

*一句话：功劳归 SlimeKnights 和 AlphaMode，锅归我们。*
