# Flux Tweaks 0.2.1 GUI polish

## Design contract

Keep the 0.2.0 two-panel layout, Lato typography, charcoal surfaces, rounded analytic shapes and configurable purple/cyan accents. Fix alignment at the shared renderer: measure the actual capital glyph from the active font, align labels to a consistent cap-height grid, and vertically center controls within their real bounds. Input text, hints and cursors use the same font and transform. Recompute font measurements after resource replacement.

Theme gains a saved GUI Scale slider and typed percentage, 50–150%, default 100%. Scale both panels, popups, clipping and pointer coordinates together. Preserve existing automatic fit at 100%; at larger sizes shorten scrollable panels as needed and constrain width to keep both panels reachable. Reset Layout restores positions and 100%. Spotify keeps its independent HUD scale.

Spotify card: shared radius/border/background, 14-unit margins, a rounded cover, title/artist hierarchy, explicit Playing/Paused/Preview status, accent progress track, and evenly spaced rounded playback buttons. Use real media capabilities and state; hide unsupported actions. Hover/focus/pressed feedback is confined to interactive editors, with toolkit timings and Reduced Motion. Missing artwork uses a restrained music-note placeholder; missing Spotify is only previewed in HUD Editor. Truncate long metadata without crossing padding. Preserve artwork caching and background decode.

HUD Editor retains drag, scroll scale, arrow nudging, Reset and Done. It uses the same aligned labels and components. Checks use real game captures at multiple window sizes, theme scales and both supported versions, Fabric alone and Sodium. No dedicated-server setup or live playback changes are needed for visual checks.

## Evidence

Fresh pre-change Minecraft 1.21.11 packaged client run passed on 2026-10-23 (`work/gui-before.log`). Screenshots reproduce high text in search, labels, dropdowns and value pills. Final checks and captures are recorded below after implementation.


### Completed matrix

| Minecraft | Java | Fabric alone | Fabric + Sodium | Logic cases |
| --- | --- | --- | --- | --- |
| 1.21.11 | 21 | Passed | Passed, Sodium 0.8.14 | 43 passed |
| 26.2 | 25 | Passed | Passed, Sodium 0.9.2 | 43 passed |

Commands: root `gradlew.bat build runProductionClientGameTest --console plain` and `gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain` on Java 21; `versions\26.2\gradlew.bat -p versions\26.2 build runProductionClientGameTest --console plain` and its `-PsodiumAcceptance` variant on Java 25. Clients run sequentially. Local logs: `work/gui-polish-final-{fabric,sodium}.log`, `work/gui-polish26-final-{fabric,sodium}.log`. The actual game windows were launched and their PNG captures inspected.

New acceptance covers pixel alignment of headings/search/feature labels/status/keybind/value fields; typed 125%, slider limits 50/150%, save/reload, popups at changed scale, feature toggle/search after scaling, smaller window recovery and Home resetting 100%. Existing navigation, dragging, scrolling, slider/range/hex/choice/modal checks and all gameplay checks still pass. The typography check compares capitals against a stable baseline; descenders do not shift a label's baseline.

Spotify checks use an isolated stopped-worker service fixture in the test mod only, with playing/paused/unavailable frames, long metadata, Latin/Japanese fallback, real uploaded PNG pixels, all/restricted playback capabilities, hit areas, drag and independent HUD scaling. Fresh live desktop Spotify metadata/art also rendered during the standard world suite. No real playback requests were sent in this patch. The card's cover shader shares the rounded-shape coverage and is checked in both render stacks.

The installed toolkit remains unchanged: installer dry-run returns `changes: []`. No new runtime dependency or configuration schema change was introduced.

### Screenshots

- [Before: high labels and search](acceptance/021-before-alignment.png)
- [Aligned interface, 1.21.11](acceptance/021-alignment-mc12111.png)
- [Aligned interface in a world, 26.2](acceptance/021-world-mc262.png)
- [Theme at 50%](acceptance/021-scale-50.png), [125%](acceptance/021-theme-125.png), [150%](acceptance/021-scale-150.png)
- [Large scale in a small window](acceptance/021-small-window.png)
- [Spotify playing, 1.21.11](acceptance/021-spotify-mc12111.png), [26.2](acceptance/021-spotify-mc262.png)
- [Spotify paused / missing artwork](acceptance/021-spotify-paused-mc12111.png), [no-session editor preview](acceptance/021-spotify-preview-mc12111.png)
- [Sodium interface, 1.21.11](acceptance/021-world-sodium12111.png), [Sodium Spotify, 26.2](acceptance/021-spotify-sodium262.png)

### Release and limits

0.2.1 is distributed as one matching JAR per Minecraft target plus a complete source archive containing both native adapters, wrappers, shared source/assets/tests/docs and the intact toolkit. Archive/JAR metadata, Java bytecode level, Windows media DLL, bundled licenses, absence of test classes in release JARs and source byte equality are checked during packaging. Keep previous releases available.

Still pending: owner review in normal multiplayer profiles, subjective font/motion/size approval, sustained GPU/frame-time measurements, live resource/font-pack reload permutations, and the existing full Spotify close/reopen/playback/remote-shield matrix. Minecraft's bitmap fallback remains for glyphs outside Lato; comprehensive translated/font-pack quality is not established. Iris/shader packs and 26.2 Vulkan are unverified. Dedicated-server EULA acceptance remains disabled. No commit, push, normal-profile mod replacement or public release upload was performed.
