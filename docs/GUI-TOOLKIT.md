# Minecraft GUI toolkit in Flux Tweaks

Installed on 2026-10-22 from [italixcs/Gui-animations](https://github.com/italixcs/Gui-animations/tree/1b8346d97a15b665d3a4b773d439a5115181c04f), commit `1b8346d97a15b665d3a4b773d439a5115181c04f`, package **minecraft-gui 0.1.0**.

## Installation

The complete self-contained package is in [`.agents/skills/minecraft-gui`](../.agents/skills/minecraft-gui/SKILL.md): 63 manifest-listed payload files plus the manifest. It includes the four workflow guides, compatibility/source map, installer, Java motion core and tests, ModernUI adapter, showcase sources, standalone Gradle starter, design tokens/specifications, Inter font and licenses/notices.

[AGENTS.md](../AGENTS.md) requires this skill for all GUI work. Its managed installer section is followed by project-owned rules. [gui-toolkit.lock.json](../gui-toolkit.lock.json) records ownership and file hashes. The upstream manifest has no Git revision field value; the exact acquisition revision is recorded above without modifying the upstream payload.

The installation used the upstream existing-project installer, without `--starter`. All 176 pre-existing tracked files retain their original SHA-256 hashes, including source, build files, project specification, checklist and existing instructions. No renderer dependency was added. Narrow [Git attributes](../.gitattributes) protect the vendored hashes and managed instructions against Windows line-ending conversion.

## Project compatibility and GUI workflow

Flux's authoritative stack remains **Minecraft 1.21.11 / Fabric / Java 21**, official Mojang mappings, Loader 0.19.3 (owner-requested compatibility correction), Fabric API 0.141.6+1.21.11, Loom 1.15.5 and Gradle 9.2.1. Sodium coverage uses 0.8.14.

For GUI work, read the installed skill and relevant design, implementation, motion and review guides. Reuse Flux's `Ui`, `SmoothGui`, `FluxScreen`, `HudEditorScreen`, Lato font and configurable theme. Use Ponytail and version-specific Context7 documentation as already requested. Keep adaptations in project-owned source; leave the installed package intact.

The bundled ModernUI adapter/showcase targets **Minecraft 26.1.2 / Java 25**. Its presence is a reference installation, not a runtime port to 1.21.11. Do not copy its build pins over Flux's build or introduce the adapter without proving compatibility. The independent Java motion core is verified below on Java 21; this does not establish ModernUI compatibility.

## Checks

Commands run with UTF-8 Python and Java 21 where applicable. Toolkit repository checks ran in an isolated upstream checkout under ignored `work/`; they did not change Flux source.

| Check | Result |
| --- | --- |
| Installed payload and lock hashes | All 63 payload files and 64 lock-owned files match; exact payload file set verified |
| Installed Markdown links and skill entry point | All 49 package-local links resolve; skill front matter verified |
| Actual repeat installation, including project rules | Zero changes; project-owned rules preserved |
| Original tracked files | All 176 unchanged |
| Toolkit separate-consumer verification | Passed Codex/Claude installation, links, hashes, rule/code preservation, repeat install, coexistence, local-edit conflict and corrupt-source rejection |
| Toolkit design validation | Passed token/schema, 13 contrast pairs, font and license checksums; all 14 design tests passed |
| Bundled motion core on Java 21 | All 20 JUnit cases passed in an isolated harness against the installed source; no changes to its build snapshot |
| Flux build and logic checks | `build` passed; all 18 existing logic tests passed |
| Flux packaged gameplay checks | `runProductionClientGameTest` passed with Fabric alone and with Sodium 0.8.14 on Minecraft 1.21.11 |

Upstream Python suite: **23 of 29 tests passed**. The remaining six rely on Unix executable-mode semantics (three failures) or symlink creation privileges unavailable to this Windows process (three errors). They were run unchanged and are not claimed as passing. The independent consumer verifier explicitly handles Windows and passed.

Upstream `scripts/package_skill.py --check` reports a manifest mismatch on Windows: Windows marks `gradlew.bat` executable while the upstream Unix-generated manifest declares mode `0644`. Its file hash and every other payload byte match canonical sources. The installed manifest and vendor files remain the exact upstream versions; no metadata was regenerated to conceal this platform difference. The upstream repository validator passed 44 Markdown files and 118 local links.

The standalone Minecraft 26.1.2 showcase was not built or launched. No visual redesign is included in this installation, so these checks do not claim new animation smoothness or font quality. Dedicated-server testing and EULA acceptance remain disabled; earlier user-confirmed multiplayer results are separate evidence.

Local run evidence is in ignored `work/gui-toolkit-{integrity.json,reinstall.json,consumer.log,python-tests.log,core.log,flux-logic.log,fabric.log,sodium.log}`. The installed package's source revision and the completed checks are recorded here so provenance remains available without those temporary files.

### Rechecking

From the project root, with Java 21 selected:

```powershell
python -X utf8 .agents/skills/minecraft-gui/scripts/install.py --project . --agent codex --source .agents/skills/minecraft-gui --dry-run --json
.\gradlew.bat build --console plain
.\gradlew.bat runProductionClientGameTest --console plain
.\gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain
```

The first command must report an empty `changes` list. Do not use `--starter` on this existing project. For updates, inspect the upstream revision and compatibility metadata, use the upstream installer in dry-run mode, preserve project-owned rules, and resolve any local payload conflict explicitly before applying the update.
