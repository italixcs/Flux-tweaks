# Spotify repeat icon — 0.2.2

The repeat glyph still used integer rectangle strokes and filled triangle heads after the 0.2.1 HUD redesign. Replace its entire loop and arrowheads with the existing analytic, antialiased `Ui.line` strokes, rounded caps and curved corner paths. Keep its button bounds and current off/one/all state mapping: white when off, theme secondary when enabled, small vector 1 for repeat-one. Both native adapters use identical geometry. No media-service or configuration change.

Use the existing isolated Spotify frame fixture to capture all three repeat states and check their rendered color. Inspect real captures in both Minecraft targets with Fabric and Sodium. No playback commands or server setup are needed.

## Verification

Both Java builds pass 43 logic cases. All four packaged clients pass: Minecraft 1.21.11 on Java 21 with Fabric alone/Sodium 0.8.14; Minecraft 26.2 on Java 25 with Fabric alone/Sodium 0.9.2. New test-only media frames cover off/one/all repeat states, assert actual rendered white/secondary pixels, and capture the icon. Visual inspection confirms rounded loop corners, open arrowheads and a legible vector 1 in both native renderers. Production geometry matches exactly across the two adapters.

Commands: root `gradlew.bat build runProductionClientGameTest --console plain` and its `-PsodiumAcceptance` run; `versions\26.2\gradlew.bat -p versions\26.2 build` and sequential `runProductionClientGameTest` runs with/without `-PsodiumAcceptance`. Local logs: `work/repeat022-{fabric,sodium}.log`, `work/repeat022-26-{build,fabric,sodium}.log`.

Captures: [off](acceptance/022-repeat-0-mc12111.png), [one](acceptance/022-repeat-1-mc12111.png), [all](acceptance/022-repeat-2-mc12111.png); [26.2 repeat-one](acceptance/022-repeat-1-mc262.png).

Release metadata/bytecode, native DLL, licenses, absence of test classes and source archive byte equality are checked during packaging. The installed toolkit is unchanged (`changes: []`). No real playback commands, multiplayer tests, shader-pack tests, normal-profile installation, commit or public upload were performed for this visual patch.
