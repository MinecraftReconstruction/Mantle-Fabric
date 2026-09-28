> [!IMPORTANT]
> **Unofficial, largely AI-assisted ("vibed") port. Not affiliated with, reviewed by, or endorsed by the original authors.**
> 非官方、由 AI 大幅辅助完成（"largely vibed"）的移植工程，与原作者没有任何隶属或背书关系。
> Credits → [ATTRIBUTION.md](ATTRIBUTION.md) · Status → [docs/STATUS.md](docs/STATUS.md) · Behaviour differences → [docs/BEHAVIOUR-DIFFERENCES.md](docs/BEHAVIOUR-DIFFERENCES.md) · Localisation → [docs/I18N.md](docs/I18N.md)
> For a stable Mantle, use [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) (Forge) or [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) (Fabric).

![Mantle logo](src/main/resources/Mantle.png)
# Mantle (Fabric) — Mantle 1.11 for Minecraft 1.20.1

**Shared code for Fabric mods.**

This is the canonical repository for the **MinecraftReconstruction** port of Mantle to Fabric. It exists because
[Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) 3.12.1 requires **Mantle `[1.11.113,)`**
while the published Fabric build of Mantle only reaches **`1.20.1-1.9.296`** — so bringing Tinkers' Construct up to
date on Fabric has to start here. Downstream work: [MinecraftReconstruction/TinkersConstruct](https://github.com/MinecraftReconstruction/TinkersConstruct).

It is based on [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) (AlphaMode's Fabric port), itself
based on [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) (the original Forge library).

## This is not a GitHub fork — but the history is complete

The repository was created fresh rather than as a fork so that it can be maintained, versioned and released as its
own project. It still carries the **complete commit history** of the port, including every commit by the upstream
authors, and `mcr/mantle-1.11` is the exact branch that was developed in the old fork, preserved commit for commit.

```
git log --oneline mcr/mantle-1.11 | wc -l      # every upstream commit is still here
git diff eb1e9a5a..mcr/mantle-1.11             # exactly what this port changed, and nothing else
```

`eb1e9a5a` is AlphaMode's last upstream commit on the 1.11 branch (the one titled `6 errors left (I'm lazy ok)`).
Everything reachable from `mcr/mantle-1.11` that is not in that diff is the original authors' work.

## Status

| | |
|---|---|
| Compiles | ✅ `./gradlew build` — BUILD SUCCESSFUL |
| Dedicated server | ✅ `./gradlew runServer` — `Done (28.575s)!`, 78 mods, 0 ERROR/FATAL |
| Client startup | ✅ `./gradlew runClient` — reaches the main menu, 92 mods, 0 ERROR/FATAL (all models baked, shaders registered) |
| Client rendering *in world* | ⚠️ **never looked at** — the game has not been driven past the main menu, so nothing visual is confirmed |
| Behaviour differences | 📋 23 entries — see [docs/BEHAVIOUR-DIFFERENCES.md](docs/BEHAVIOUR-DIFFERENCES.md) |
| Localisation | ✅ 6 locales, key-for-key aligned, enforced by `./gradlew validateLangFiles` |

This began as a "6 errors left" branch that had never actually been run. Recompiling with `-Xmaxerrs` revealed
**158** real errors, and the first real server start revealed **two more bugs that never showed up at compile time**
(an invalid `en_us.json` and a missing Cardinal Components entrypoint constructor). Both are fixed. Full story,
including which behaviour differences still need in-game verification: **[docs/STATUS.md](docs/STATUS.md)**.

## Using it

Grab the jar from [Releases](../../releases) and drop it in `mods/` together with its dependencies:

| Dependency | Version used here |
|---|---|
| Fabric Loader | `>=0.17.2` |
| [Fabric API](https://modrinth.com/mod/fabric-api) | `0.92.6+1.20.1` |
| [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib) | `2.3.16-beta.81+1.20.1` |
| [Cardinal Components API](https://modrinth.com/mod/cardinal-components-api) | `5.2.3` |
| [Architectury API](https://modrinth.com/mod/architectury-api) | `9.1.12` |
| [Reach Entity Attributes](https://github.com/JamiesWhiteShirt/reach-entity-attributes) | `2.4.0` |

## Branch map

| Branch | Version | State |
|---|---|---|
| **`mcr/mantle-1.11`** *(default)* | **1.11** | the working branch — all of this project's changes live here |

`mcr` = MinecraftReconstruction. The prefix is deliberate: it makes `git diff eb1e9a5a..mcr/mantle-1.11` a complete
answer to "what did we change?". Upstream's own branches (`1.20.1`, `1.20.1-update`, `1.21.1`, `1.11`, `1.12`, …)
are **not** carried here; they are tracked in the
[upstream mirror](https://github.com/MinecraftReconstruction/Mantle-Fabric-upstream) and at
[Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) / [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle).

## Compile from source

Requires **JDK 21** (`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` on macOS).
First build downloads dependencies (≈9 min); later builds take 1–3 minutes.

```bash
./gradlew compileJava        # fast feedback
./gradlew build              # jar + datagen + validation
./gradlew check              # build + validateLangFiles
./gradlew validateLangFiles  # lang files only
./gradlew runServer          # dedicated server smoke test (needs run/eula.txt)
```

* setup: import as a Gradle project into your IDE. Run `gradlew[.bat] [genIntellijRuns|genEclipseRuns]` to launch the game
* if obscure gradle issues are found try `gradlew clean` or `gradlew cleanCache`

## Issue reporting

* **Issues caused by this port** → open them in *this* repository.
* **Issues with Mantle itself** → [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) (Forge) or
  [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) (Fabric). The original authors are **not**
  responsible for anything in this port.

When reporting an issue, please include: Minecraft version, Mantle version, Fabric Loader/API version, the versions
of any mods involved, steps to reproduce, and the full log/crash report.

## Credits and licence

All credit for Mantle goes to **SlimeKnights** (original Forge library) and **AlphaMode** (the Fabric port this is
based on). The upstream code is theirs; what the MinecraftReconstruction organisation changed is the migration
itself — **the MinecraftReconstruction migration work is largely AI-assisted and was carried out under human
direction, review, and validation**, and is labelled as such throughout the repository. See
[ATTRIBUTION.md](ATTRIBUTION.md). Nothing here implies endorsement by the original authors.

The logo above is the upstream project's asset, redistributed from the MIT-licensed upstream repository purely to
identify what this port is a port *of*. It remains SlimeKnights' work and mark; it is not this project's own
branding, and we will replace it on request.

## Licenses
The MIT License (MIT)
Copyright (c) 2013-2022 Slime Knights (mDiyo, fuj1n, Sunstrike, progwml6, pillbox, alexbegt, KnightMiner)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


Any alternate licenses are noted where appropriate.
