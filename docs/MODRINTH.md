# Modrinth 发布材料（Mantle — Unofficial Fabric Port）

> 用途：把这一页的内容原样贴到 Modrinth 项目页 + 版本页，避免审核因为信息缺失被打回。
> 项目页：<https://modrinth.com/project/mantle-fabric-unofficial>

## 0. 提交审核前的检查清单

- [x] 项目 slug 不叫 `mantle`（官方 `mantle` 是 Forge/NeoForge 项目，见下）
- [x] `fabric.mod.json` 的 name 明确写了 "Unofficial Fabric Port"，authors 含原作者 + 我们组织
- [x] license = MIT，与上游 SlimeKnights/Mantle 一致（MIT 允许 fork 与再分发）
- [x] description 里写了非官方、署名、以及"这是库、要配合 TCon 移植用"
- [x] **依赖自足**：`reach-entity-attributes` 已改为 jar-in-jar（它不在 Modrinth 上，
      只靠 maven 分发，不打包用户就装不上）；Porting Lib 在 Modrinth 上有官方项目可直接依赖
- [ ] 构建产物：`ARTIFACT_VERSION=<版本> ./gradlew build`（见第 3 节）
- [ ] 上传时把 `docs/BEHAVIOUR-DIFFERENCES.md` 里"未验证"的条目写进 changelog（审核和用户都需要知道）

## 1. 项目页文案（可直接粘贴）

**Title**

```
Mantle (Unofficial Fabric Port)
```

**Summary**（Modrinth 的 256 字符简介）

```
Unofficial, largely AI-assisted ("vibed") Fabric port of SlimeKnights' Mantle 1.11 for 1.20.1 — a library, install it alongside the Fabric Tinkers' Construct port.
```

**Description**

```markdown
# Mantle (Unofficial Fabric Port)

**Unofficial, largely AI-assisted ("vibed") port.**

This is an unofficial port of [SlimeKnights' Mantle](https://github.com/SlimeKnights/Mantle) (the 1.11 line,
maintained for Fabric by [AlphaMode](https://github.com/Alpha-s-Stuff/Mantle)) to Fabric for Minecraft 1.20.1.

The upstream code is SlimeKnights' and AlphaMode's. The MinecraftReconstruction migration work
(Forge APIs → Fabric / Porting Lib) is **largely AI-assisted and was carried out under human direction, review
and validation**. Bugs found by running it are tracked in this repository's `docs/`.

## What this is (and is not)

* This is a **library**. On its own it does nothing — install it only if a mod asks for it.
  It exists so that the Fabric Tinkers' Construct port (Hephaestus-derived, synced to Tinkers' Construct 3.12.1)
  has the Mantle 1.11 API it needs.
* This is **not** the official Mantle. Please do not report bugs of this build to SlimeKnights.
* The mod id is `mantle`, the same id the 1.9 Fabric Mantle uses. **Do not install this together with mods
  that need Mantle 1.9** — they expect a different API (and vice versa).

## Requirements

* Minecraft 1.20.1, Fabric Loader, Fabric API
* [Porting Lib](https://modrinth.com/mod/porting_lib) (the bundle provides every `porting_lib_*` module this needs)
* Reach Entity Attributes is **bundled** (jar-in-jar), you do not need to install it
* Cardinal Components API and Star are **bundled** too

## Status

Alpha. `./gradlew build`, `runServer` and `runClient` (to the main menu) have been run with zero errors in the
logs; the visual behaviour differences that have not been verified yet are listed in
[docs/BEHAVIOUR-DIFFERENCES.md](https://github.com/MinecraftReconstruction/Mantle-Fabric/blob/mcr/mantle-1.11/docs/BEHAVIOUR-DIFFERENCES.md)
— most notably model render types (#6/#13/#14), the structure preview model data (#7) and fluid submersion (#3).

## Credits

* SlimeKnights — Mantle (MIT)
* AlphaMode — the Fabric 1.20.1 port this is based on
* MinecraftReconstruction — the 1.11 migration, AI-assisted
```

## 2. 版本页要填的字段

| 字段 | 值 |
|---|---|
| Name / version number | `1.11.0-alpha.1` |
| Release channel | **Alpha** |
| Game versions | `1.20.1` |
| Loaders | `Fabric` |
| Dependencies | `Fabric API` = **Required**；`Porting Lib` = **Required**；`Reach Entity Attributes` = **不列**（已 jar-in-jar） |
| Changelog | 见第 4 节 |

## 3. 构建发布用的 jar

```bash
cd ~/Desktop/repo/mr-mantle-fabric
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ARTIFACT_VERSION=1.11.0-alpha.1 ./gradlew build
# 产物：build/libs/Mantle-1.20.1-1.11.0-alpha.1.jar
shasum -a 512 build/libs/Mantle-1.20.1-1.11.0-alpha.1.jar
```

`ARTIFACT_VERSION` 是 build.gradle 里现成的开关（不设时才会拼上 `DEV.<git sha>`），
所以发布构建不会带 `DEV` 后缀。

## 4. changelog 模板

```markdown
First alpha of the unofficial Mantle 1.11 Fabric port for 1.20.1.

* Ported from SlimeKnights' Mantle 1.11 (via AlphaMode's Fabric branch) — Forge APIs replaced with Fabric /
  Porting Lib, under human direction and review.
* `./gradlew build` succeeds; `runServer` and `runClient` start with **0 ERROR/FATAL** in the logs.
* Runtime bugs found and fixed during the first in-game run: a malformed `en_us.json`, a missing Cardinal
  Components entrypoint constructor, and datagen output that still used Forge fluid units.
* Known unverified behaviour (visual): model render types (#6/#13/#14), structure preview model data (#7),
  fluid submersion (#3), food properties (#9). See docs/BEHAVIOUR-DIFFERENCES.md.
* `reach-entity-attributes` is bundled (jar-in-jar) because it is not distributed on Modrinth.
```

## 5. 审核可能问到的问题（提前准备答案）

1. **"这算不算重复上传官方 Mantle？"**
   不算：官方 Modrinth 项目 `mantle` 只有 Forge/NeoForge 文件，**没有任何 Fabric 版本**；本项目是
   Fabric 1.20.1 + Mantle 1.11 API，且署名与 MIT 许可都保留。
2. **"为什么 mod id 和别的 Mantle 一样？"**
   因为下游（TCon 移植）就是按 `mantle` 这个 id 依赖它的；id 变了下游加载不了。
   已在描述里明确警告与 1.9 分支不兼容。
3. **"AI 生成的吗？"**
   上游代码不是：Mantle 源码来自 SlimeKnights / AlphaMode。**迁移改动**由 AI agent 在人类设定目标、
   审查与验证下完成，描述里已经按这个口径写明。
