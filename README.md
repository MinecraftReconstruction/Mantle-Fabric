> [!IMPORTANT]
> **Unofficial, largely vibed (AI-generated) fork. Not affiliated with, reviewed by, or endorsed by the original authors.**
> 非官方、由 AI 大幅生成（largely vibed）的试验性 fork，与原作者无任何隶属或背书关系。
> Credits → [ATTRIBUTION.md](ATTRIBUTION.md)　·　Status & next steps → [docs/STATUS.md](docs/STATUS.md)　·　Agent handoff → [AGENTS.md](AGENTS.md)
> For a stable Mantle, use [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) (Forge) or [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) (Fabric).

![Mantle logo](https://raw.github.com/SlimeKnights/Mantle/master/src/main/resources/Mantle.png)
# Mantle (Fabric) — MinecraftReconstruction fork

**Shared code for Fabric mods.**

This is a fork of [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) — itself the Fabric port of
[SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) ("shared code for Forge mods").

## Why this fork exists

[Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) 3.12.1 requires **Mantle `[1.11.113,)`**,
but the published Fabric build of Mantle only reaches **`1.20.1-1.9.296`**. Bringing Tinkers' Construct up to date
on Fabric therefore starts here — see [MinecraftReconstruction/TinkersConstruct](https://github.com/MinecraftReconstruction/TinkersConstruct).

## Branch map

| Branch | Version | State |
|---|---|---|
| `1.20.1` *(default)* | 1.9.x | matches the currently published Fabric Mantle |
| `1.20.1-update` | **1.11** | **the WIP 1.11 Fabric port — main work happens here** |
| `1.21.1` | — | earlier 1.21.1 attempt |
| `1.11`, `1.12`, … | — | inherited from upstream SlimeKnights; these are **Forge** branches, not Fabric |

## Current status

**Not compiling yet.** The upstream author's last commit on `1.20.1-update` is titled `6 errors left (I'm lazy ok)` —
but that number was an artifact of javac's default `-Xmaxerrs 100` cap. After fixing the first 5 blockers
(commit `756dad64`), a full recompile reports **158 errors across 55 files**, dominated by Forge APIs that have
no Fabric equivalent yet (`IFluidHandler`, `ForgeCapabilities`, `LazyOptional`, `IForgeRegistry`, `ICondition`, …).

Full breakdown, fix order and verification steps: **[docs/STATUS.md](docs/STATUS.md)**.

## Compile from Source

Requires **JDK 21** (`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` on macOS).
First build downloads dependencies (≈9 min); later builds take 1–3 minutes.

```bash
./gradlew compileJava   # fast feedback
./gradlew build         # full jar
```

* setup: Import as a gradle project into your desired IDE. Run `gradlew[.bat] [genIntellijRuns|genEclipseRuns]` to be able to launch the game
* if obscure gradle issues are found try running `gradlew clean` or/and `gradlew cleanCache`

## Issue reporting

* **Issues caused by this fork** → open them in *this* repository.
* **Issues with Mantle itself** → [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) (Forge) or
  [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) (Fabric). The original authors are **not**
  responsible for anything in this fork.

When reporting a fork issue, please include: Minecraft version, Mantle version, Fabric Loader/API version,
the versions of any mods involved, steps to reproduce, and the full log/crash report.

## Licenses  
The MIT License (MIT)
Copyright (c) 2013-2022 Slime Knights (mDiyo, fuj1n, Sunstrike, progwml6, pillbox, alexbegt, KnightMiner)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


Any alternate licenses are noted where appropriate.
