# Implementation and acceptance checklist

Implementation complete; acceptance evidence recorded below. Updated 2026-10-23.

## Icons, module actions and notification HUD — 0.2.4 (2026-10-23)

- [x] Shared smooth icons for all fourteen modules; names remain visible.
- [x] Separate Enable/Action binds for cameras/Zoom; existing binds migrate as Action, new Enable binds unbound; continuous module binds retained.
- [x] Saved availability and transient action state; Ready/Active display, camera mutual exclusion, hold/toggle and fresh-key cancellation.
- [x] Silent themed notification cards for actual Enabled/Disabled/Activated/Deactivated transitions; bounded/expiring queue, cubic easing and Reduced Motion.
- [x] HUD Editor selects, drags, scales, nudges and resets Notifications independently from Spotify; preview and schema-1 persistence; Theme notification toggle.
- [x] 52 logic cases pass on Java 21 and Java 25; packaged Fabric runs on both versions and 1.21.11 Sodium pass.
- [x] Final 26.2 Sodium/Iris production runs pass; both 0.2.4 JARs, complete source and checksum manifest verified.
- [ ] Owner visual/multiplayer/Lunar acceptance of these additions; shader packs and sustained performance remain unverified.

See [design, migration, screenshots and evidence](docs/MODULE-CONTROLS-024.md). Dedicated-server testing remains disabled.

## Minecraft 26.2 / Lunar-Iris crash — 0.2.3 (2026-10-23)

- [x] Context7 and official Lunar custom-mod guides researched; exact local crash reports identify conflicting Flux/Iris hand-render redirects.
- [x] Same crash reproduced outside Lunar using Flux 0.2.2 + Iris 1.11.4 + Sodium 0.9.2.
- [x] 26.2 hand hook now wraps/preserves the original operation through normal, Chams and shield-mask paths.
- [x] Patched Iris/Sodium packaged client suite passes without a shader pack; 43 Java 25 logic cases pass.
- [x] Final 26.2 Fabric/Sodium regression runs pass; patch JAR/source metadata, licenses and byte equality verified.
- [ ] Owner acceptance in real Lunar and shader-pack-specific rendering combinations.

See [Lunar compatibility report and relevant official docs](docs/LUNAR-COMPATIBILITY.md). The 1.21.11 adapter remains 0.2.2; Lunar's profile/settings are untouched.

## Spotify repeat icon — 0.2.2 (2026-10-23)

- [x] Replaced repeat loop/arrowheads with rounded, antialiased vector strokes in both adapters, retaining repeat-one and state colors.
- [x] Minecraft 1.21.11 Fabric packaged run and 43 logic cases pass; real captures cover off/one/all with rendered-color assertions.
- [x] All four final packaged clients pass (1.21.11/26.2, Fabric/Sodium); 43 logic cases pass on each JDK. JARs, source archive and checksums verified.

See [repeat icon notes](docs/SPOTIFY-REPEAT-022.md). No media commands are needed for these rendering checks.

## GUI alignment, Spotify HUD and scale — 0.2.1 (2026-10-23)

- [x] Fresh before captures reproduce the owner's text alignment report; actual font metrics now drive shared label/title/input alignment in both adapters.
- [x] Search, switches, value/range fields, checked choices and modal actions share consistent alignment; title fitting measures the actual title font.
- [x] Spotify theme/rounded-art/button redesign, progress/status, capability filtering and independent scale; HUD Editor toolbar avoids the default card position.
- [x] Saved Theme GUI Scale 50–150%, typed percentage/slider, transformed pointer/clipping, resizing and Home/layout recovery.
- [x] Forty-three configuration/layout/motion cases pass on Java 21 and Java 25, including older-config migration and scale limits.
- [x] Minecraft 1.21.11 packaged clients: Fabric and Sodium 0.8.14; pixel alignment checks, real scale/search/toggle input and deterministic Spotify metadata/art/capability/drag/scale cases.
- [x] Minecraft 26.2 final packaged Fabric/Sodium 0.9.2 checks and visual review.
- [x] Final 0.2.1 distribution/source verification (both matching JARs, licenses/native bridge, source bytes and SHA-256 manifest).
- [ ] Owner approval in normal multiplayer profiles and subjective sizing/typography; sustained GPU/performance checks remain pending.

See [design, evidence and screenshots](docs/GUI-POLISH-021.md). Deterministic media frames are test-only. No playback commands were sent and dedicated-server testing remains disabled.

## GUI redesign and dual Minecraft targets — 0.2.0 (2026-10-22)

- [x] Toolkit-guided full settings/HUD styling, larger Lato typography, rounded controls, compact choice dialogs, color/typed-value editors and keyboard focus.
- [x] Shared monotonic cubic motion, interruption-safe transitions and saved Reduced Motion; existing settings and schema 1 preserved.
- [x] Click/drag coordinates match rendering; rapid toggles read current settings and range clicks choose the nearest handle from the actual event.
- [x] Separate native 1.21.11 / Java 21 and 26.2 / Java 25 builds with all fourteen modules.
- [x] Thirty-eight logic/configuration/motion cases pass on each JDK.
- [x] Final 0.2.0 packaged client matrix: 1.21.11 Fabric and Sodium 0.8.14; 26.2 Fabric and Sodium 0.9.2.
- [x] Final source/JAR/license/version inspection and distribution copies.
- [ ] Owner approval in their usual worlds/servers, subjective smoothness and extended performance/session checks.

See [GUI redesign contract and evidence](docs/GUI-REDESIGN.md). Prior multiplayer confirmation below applies to earlier releases; no dedicated server was started.

## Zoom, Block Overlay and Hit Color — 0.1.4 (2026-10-21)

- [x] Three new registry/settings/GUI/search/Controls modules, initially disabled/unbound; version-1 configuration migration and saved preferences.
- [x] Smooth hold Zoom with 1.2x wheel steps, default reset, scaled sensitivity and native world-FOV composition; no saved-option changes.
- [x] Screen/focus/death/world cleanup and fresh-press gate; camera mode and spyglass composition.
- [x] Real target selection shapes with independent outline/fill controls, native line width and depth-tested external voxel faces.
- [x] Hit Color targets Players/Mobs/Self, separate overlay row, original body/armor/trim textures and dye, preserved glint and held items.
- [x] Cached armor variants and resource reload cleanup; live color/opacity updates without per-frame texture uploads.
- [x] Eighteen focused logic/configuration checks pass.
- [x] Final packaged Fabric/Sodium checks and JAR/source inspection passed; see docs/MODULES-0.1.4.md.
- [ ] New modules' gameplay acceptance in the owner's multiplayer profile.

## Low Fire — 0.1.3 (2026-10-21)

- [x] Eleventh module in the GUI/search and Minecraft Controls, initially disabled/unbound; saved enabled state and binding.
- [x] Lowers only first-person fire overlay geometry; leaves world/entity flames, damage, sounds and burning state alone. Disabling restores vanilla overlay height.
- [x] Older-config migration and enabled-state persistence covered by focused logic checks; thirteen focused JUnit checks pass.
- [x] Packaged Fabric/Sodium fire captures, normal-height restoration and release archive inspection. See docs/LOW-FIRE-0.1.3.md.
- [ ] Owner confirmation of the new Low Fire module in their multiplayer profile.

## Fullbright — 0.1.2 (2026-10-21)

- [x] Added Fullbright to the shared registry, GUI, search, Controls bindings and persistent configuration; disabled and unbound by default.
- [x] Client lightmap override with immediate normal-light restoration; no gamma or potion changes.
- [x] Existing-config migration preserves other features and adds Fullbright disabled. Enabled state survives config reload.
- [x] Twelve focused JUnit checks and production JAR build on Java 21.
- [x] Enclosed dark-room captures with Fabric and Sodium: keybind toggles brighten the scene, then restore the original sampled luminance.
- [x] Reviewed the Fullbright settings screenshot and packaged the 0.1.2 JAR/source archive.
- [x] Owner confirmed everything works, including multiplayer, on 2026-10-21. Automated dedicated-server testing remains disabled.

See docs/FULLBRIGHT-0.1.2.md for measured evidence.

## GUI refinement — 0.1.1 (2026-10-21)

The user reported that all nine features work in their installed first build, and requested smoother rendering plus refined layout/spacing.

- [x] Smooth filtering and preserved alpha coverage for Flux font styles, with regular/bold Lato faces.
- [x] Analytic rounded panels, switches, circles, slider tracks/thumbs, icons and focus outlines.
- [x] Compact responsive scale, all-nine navigation, adaptive settings height and consistent control spacing.
- [x] Smooth hover/toggle/slider-value/scroll feedback; input metrics match rendered fonts.
- [x] Final colour-picker visual review, hue-strip image assertions and packaged Fabric/Sodium checks on Java 21.
- [ ] User visual approval of the updated GUI and performance on their normal profile.

## Implemented

- [x] Approved specification and all 17 references saved before coding.
- [x] Java 21/Fabric foundation, feature registry, versioned JSON, validation, atomic persistence, Controls keybindings.
- [x] Native two-panel GUI, theme, search, scrolling, panel dragging, reset, typed numbers/ranges, picker/hex colors, HUD editor.
- [x] Independent main/off items and arms; handedness-aware transforms alongside use animations.
- [x] Freelook/Freecam input isolation, mutual exclusion and camera lifecycle cleanup.
- [x] Eight particle shapes and four physics modes; bounded particles and fading jump circles; validated custom PNG imports.
- [x] Ambience particles, render-time weather/time/fog and sky; server environment data/sounds preserved by hook placement.
- [x] Exact local shield cooldown and estimated remote shield states, priority/transitions/colors.
- [x] Animated Chams gradient/fill/glow, original textures and shield surface/outline integration.
- [x] Bundled Windows x64 media bridge, background polling, cached art and interactive Spotify HUD.
- [x] Packaged JAR and complete source archive with installation/build notes.

## Passed evidence

- [x] Eighteen focused JUnit checks (including Zoom timing/FOV/scroll/cancellation and module migration/persistence): defaults, config roundtrip, camera disarming, malformed/missing settings recovery, corrupt-file preservation, range/clamp validation, clock conversion, shield priority and color interpolation.
- [x] Production JAR build/remap on Java 21, including native DLL and font license.
- [x] Fabric single-player packaged-game smoke checks on Java 21.
- [x] Fabric + Sodium 0.8.14 single-player packaged-game smoke checks on Java 21.
- [x] GUI enable/key assignment/clearing and feature-key toggling, typed slider, hex color, search, navigation drag/reset and resize recovery (854Ã—480/scale 1, 1280Ã—800/scale 2, 1920Ã—1080/scale 3).
- [x] Both handedness settings, bow/use, apple/use, shields, maps and empty-hand rendering with View Models + Chams + Shield Status enabled; no render failure. Neutral/offset transforms exercised.
- [x] Hold activation/release and camera facing preservation, freecam/body movement separation, perspective restoration, mutual exclusion and world-exit reset.
- [x] All eight hit sprite types and four physics modes exercised in game; Default/Glowing/Custom jump styles exercised and jump event/effects screenshots.
- [x] Valid PNG accepted; malformed PNG rejected while preserving the previous working file.
- [x] Own shield cooldown selects disabled color from exact game data; pure priority/transition checks.
- [x] Live Spotify metadata/art/duration/progress and HUD/editor rendering.
- [x] Live previous/next and pause/resume requests; changed metadata/playback states observed and initial pause state restored.
- [x] Repeat failure handling: this Spotify session acknowledged requests without reporting a change; Flux hides its unconfirmed repeat control.

- [x] **Owner confirmed existing features work in multiplayer on 2026-10-21.** This is user-reported gameplay evidence; the automated dedicated-server matrix remains pending with no EULA acceptance set.

## Still pending

- [ ] Detailed remote-shield/nearby-player jump-circle matrix across server-specific behavior, beyond the owner's general multiplayer confirmation.
- [ ] Full live matrix of all circle appearances, particle settings/extremes, sustained event/resource stress and GPU/performance measurements.
- [ ] Independent listening check of server weather sounds and full dimension/death transition matrix; weather hooks were source-audited to affect rendering only.
- [ ] Spotify close/reopen, missing-art/long-title cases, and repeat on a Spotify session that actually exposes working repeat state/commands.
- [ ] Manual visual approval of every control and combination; automated item tests establish render survival, not pixel-perfect calibration of each animation.

See docs/ACCEPTANCE.md for commands and evidence boundaries. Unchecked checks are not implied by a successful build.
