# Acceptance record — Flux Tweaks 0.1.0

Date: 2026-10-20. This is the first implementation build, with multiplayer acceptance deliberately pending.

## Build environment and artifacts

- Windows x64; Eclipse Temurin Java 21.0.12.1 runtime/compiler for final production checks.
- Minecraft 1.21.11; Fabric Loader 0.19.5; Fabric API 0.141.6+1.21.11.
- Loom 1.15.5, Gradle 9.2.1, official Mojang mappings; Java class target 21.
- Optional Sodium checked: **0.8.14+mc1.21.11 Fabric**. It was loaded as a production mod, so its newer Loom metadata did not require changing this project's approved build tools.
- Windows C++/WinRT bridge compiled x64 with static C++ runtime; embedded in the mod JAR.
- Eleven JUnit checks passed with zero failures/errors. Test-only classes are excluded from the installable JAR.

## Commands used

```powershell
.\gradlew.bat build --console plain
.\gradlew.bat runProductionClientGameTest --console plain
.\gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain
```

The production runs use the actual remapped mod JAR plus a separately remapped test mod. They create disposable integrated single-player worlds, drive real input/screens, inspect relevant game state, and capture game-rendered screenshots. Neither command starts a dedicated multiplayer server. The test configuration retains `eula = false`; no `eula.txt` acceptance file was created.

Local evidence logs are in ignored `work/fabric-final.log`, `work/sodium-final.log` and `work/spotify-acceptance.log`; screenshots remain in `build/run/productionFabric/screenshots` and `build/run/productionSodium/screenshots`. Selected GUI and combined-rendering captures are copied to `docs/acceptance/`.

## Confirmed behavior

- Initial disabled defaults and neutral transforms; JSON roundtrip, malformed-file preservation/recovery, sanitized settings/ranges and camera disarming on reload.
- Right Shift ingame GUI, actual GUI key assignment/clearing and feature toggling, typed numeric values, hex colors, search selection, panel drag/reset, recovery across three resolution/GUI-scale combinations.
- First-person combined Chams gradient/outline and shield surface tint. The screenshots retain original item textures.
- Left/right handedness with bow, apple, shield, map and empty-hand use paths while View Models, Shield Status and Chams are enabled. These smoke checks detect render failure; they do not replace visual approval of each transform/animation.
- Freelook hold activation/release, camera-only rotation and restored perspective; Freecam moves the camera while the body/facing stay put; mutual exclusion and world-exit cleanup.
- All eight hit particle types and four physics modes exercised against a living mob; observed takeoff and Default/Glowing/Custom circle paths; valid PNG accepted and malformed PNG rejected without overwriting the working image.
- Ambience custom sky/fog and snow/particles rendered in game. Weather hooks only replace rendering inputs and the client attribute sampler; server environment values and sound routines are not rewritten. A full live listening/dimension matrix remains open.
- Own shield cooldown selects the configured disabled tint using actual cooldown data; state precedence and interpolation have focused checks.
- Spotify desktop metadata, cover art, duration and moving progress shown by the real in-game HUD/editor.

## Live media controls

The user explicitly authorized all playback-button checks. The native bridge's play/pause, next and previous commands were accepted and their changed playback/metadata states observed. The initial pause/repeat states were restored where observable.

**Repeat limitation observed:** this Spotify Windows session reports repeat enabled and acknowledges requests, but reports no changed repeat state for either next mode. Flux now verifies that response on its media worker and hides the unconfirmed repeat button for that session. It retries on reconnection. Repeat is available on sessions that expose and confirm it; a confirmed working repeat session was not available for acceptance here. No Spotify web API, account credentials or extra authorization flow was added.

Native check output:

```text
Play/pause and restoration: PASS
Repeat: Windows session did not confirm a change; unavailable control hidden: PASS
Next track and metadata change: PASS
Previous track and metadata change: PASS
Metadata/duration/artwork bridge: PASS
Live progress advances: PASS
```

## Remaining acceptance

**Multiplayer was left pending at the user's request.** Remote shield cooldowns remain explicitly estimated from identifiable local events, and server-specific timing has not been validated. Real second-player jump circles, the full death/dimension transition matrix, exhaustive settings/stress/performance coverage, and Spotify close/reopen/missing-art/long-title cases remain unchecked. See CHECKLIST.md for the maintained list. A successful build or single-player smoke run does not establish those results.

## Documentation used

- [Fabric 1.21.11 rendering](https://docs.fabricmc.net/1.21.11/develop/rendering/world)
- [Fabric GUI documentation](https://docs.fabricmc.net/1.21.11/develop/rendering/gui)
- [Fabric automatic testing](https://docs.fabricmc.net/1.21.11/develop/automatic-testing)
- [Loom production run tasks](https://docs.fabricmc.net/1.21.11/develop/loom/production-run-tasks)
- [Windows media sessions](https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssession)
- [Windows timeline update timestamps](https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssessiontimelineproperties.lastupdatedtime)

Version-specific Fabric documentation was retrieved through Context7. Ponytail guided the implementation. Supplied screenshots informed the interface; their embedded text was treated as reference content rather than instructions.
