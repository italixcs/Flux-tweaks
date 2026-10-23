# Flux GUI redesign and dual-version release

User request: rebuild the full GUI using the installed minecraft-gui toolkit and produce Fabric builds for Minecraft **1.21.11** and **26.2**. The original build remains the 1.21.11 build; version-specific 26.2 adapters belong under `versions/26.2`. All existing modules and configuration must carry forward.

## Screen contract

The primary task is choosing a feature and adjusting it while viewing the live world. Retain two independently draggable panels, search, Theme and HUD Editor. Use a consistent restrained charcoal surface with the existing configurable purple/cyan accents and licensed Lato typography. The toolkit's layout, interaction and motion rules guide the native adapter; bundled ModernUI examples are not version-compatible runtime imports.

- Feature panel: clear Flux identity, live enabled count, inset search, 32-unit feature rows with selected indicator and explicit on/off state, persistent Theme/HUD actions.
- Settings panel: feature title and concise purpose, status switch and bind field, compact checked choice popups, color/hex controls, numeric value fields and 4-unit slider tracks. Headings separate transform groups. Long content scrolls with a visible thumb.
- Modal editors: readable title, clear Close/Apply actions (colors retain their live preview; closing a numeric edit discards uncommitted text), inline validation, modal input capture and focus restoration. Escape closes the top layer before the screen. Keyboard navigation reaches settings; arrows adjust sliders and choices.
- HUD Editor: shared rounded toolbar, visible selected bounds, drag/arrow movement, scroll scaling and explicit layout recovery/return controls.
- Responsive layout: 190-unit navigation and 320-unit settings panels, safe insets and bounded positions. Scale the combined layout to fit small logical viewports; keep all dismissal/actions reachable. Layout recovery repositions panels together without overlap.
- Shared states: immediate setting changes, hover treatment, visible focus, selected/check indicators, readable disabled/empty/error states. Keep existing theme values and settings; add a saved Reduced Motion preference.
- Motion: monotonic elapsed-time cubic transitions from toolkit core; 100 ms hover, 220/140 ms screen arrival/departure, 160 ms popup opening. Retarget from the displayed value; reduced motion settles immediately. Render and input use the same transform.

## Acceptance

Use actual version-matched source and APIs, Context7 and Ponytail. Check compilation and focused logic, then packaged clients with Fabric and Sodium where supported. Exercise search, toggles, rebinding, sliders/typed values, checked choices, color picker/hex, keyboard/modal focus, dragging, layout recovery, persistence, reduced motion, resize and GUI scale. Inspect actual screenshots for type, clipping and rounded edges. Keep unobserved FPS/performance or live cases explicit.

The toolkit installation remains byte-identical. A pre-redesign source snapshot is saved locally in ignored `work/before-gui-redesign.zip`.


## Implementation record

- Release **0.2.0** retains all fourteen modules and uses the toolkit core's `Tween`/`Easing`, adapted into Flux's namespace with MIT attribution. The installed skill package remains unmodified.
- Existing configurable charcoal/purple/cyan theme and licensed Lato font remain authoritative. Body/title sizes are now 12/16 logical units with the existing filtered glyph rendering and Unicode fallback.
- The root native GUI uses 1.21.11 `GuiGraphics`; the second adapter uses 26.2 `GuiGraphicsExtractor`. Shared configuration, tokens expressed in the screen contract, font resources, animation code and logic checks stay in the root tree.
- The 26.2 build ports the render-state packages, reverse-depth pipeline and bind-group definitions, submitted geometry, camera/FOV, lighting, weather and clock hooks. It uses Loader 0.19.5, Fabric API 0.161.0+26.2, Loom 1.18.4, Gradle 9.7.1 and Java 25. The root pins remain unchanged apart from the mod version.
- Visual review found an opaque offscreen clear in the initial 26.2 Hand Chams port. Its clear now explicitly uses RGBA (0,0,0,0). A new comparison checks both a visible outline and preservation of the world background. Shield-state surfaces and Chams outlines still compose.
- A focused range-control check caught a stale render-frame cursor coordinate. Shared click dispatch now uses the input event's coordinate; continuous drags retain the selected handle. Settings callbacks read current values so multiple activations in one frame remain correct.

### Evidence and limits

All thirty-eight JUnit cases pass on both Java 21 and Java 25. Packaged client runs cover the GUI/search/binds, typed numbers and hex colors, hue strip pixels, checked choices with keyboard navigation, modal input isolation/Escape, rapid toggles, range handles, theme persistence, layout recovery, three window/GUI-scale combinations and 30/60/144 FPS caps. The caps are exercised for opening/closing and captures; this is not a measured frame-time benchmark or subjective smoothness approval.

The full client suite also exercises both hands/use animations, cameras and restoration, particles/circles/import rejection, fullbright/fire pixels, zoom/scroll/sensitivity/cleanup/spyglass, actual block-selection shapes/thickness/fill/depth, and body/armor/trim/dye/glint/filtering damage overlays. Client runs use isolated single-player worlds and the packaged mod; no dedicated server or EULA change is involved.

Run logs are kept locally under ignored `work/gui-redesign-release-{fabric,sodium}.log` and `work/port26-release-{fabric,sodium}.log`. JUnit XML results are under each build's `build/test-results/test`. Selected real captures are saved under `docs/acceptance/020-*` for review.

Remaining acceptance: owner approval in normal multiplayer profiles, subjective animation/type quality, sustained sessions and GPU/frame-time measurements, comprehensive non-Latin/translated-label and resource-reload combinations, and the pre-existing Spotify/remote-shield edge cases in the checklist. Iris/shader packs and Minecraft 26.2's Vulkan backend are not checked; observed rendering uses OpenGL on this PC's NVIDIA GeForce RTX 4070 Ti. No live Spotify playback commands were issued for this GUI work.


### Completed 0.2.0 matrix (2026-10-22)

| Target | Runtime | Fabric alone | Fabric + Sodium | JUnit |
| --- | --- | --- | --- | --- |
| Minecraft 1.21.11 | Java 21 | Passed | Passed, Sodium 0.8.14 | 38 passed |
| Minecraft 26.2 | Java 25 | Passed | Passed, Sodium 0.9.2 | 38 passed |

Both final JARs were inspected for the correct Minecraft/Java/version metadata, Windows x64 media DLL, bundled font and toolkit licenses, and absence of acceptance-test classes. The complete source archive contains both builds and the intact installed toolkit. No commits, pushes or uploads to a public release service were made.


### Follow-up: 0.2.1

The owner reported baseline/alignment problems in the initial design. The shared font-metric correction, Spotify card redesign and approved 50–150% GUI scale are documented in [GUI-POLISH-021.md](GUI-POLISH-021.md). The original 0.2.0 evidence above remains historical.
