# Flux Tweaks: first-release specification

Approved 2026-10-20. This file records the agreed scope; CHECKLIST.md records evidence.

## Foundation
- Name: Flux Tweaks; mod ID: fluxtweaks.
- Project: C:\Users\Leif\Documents\GitHub\Flux tweaks.
- Minecraft Java 1.21.11, Fabric, Windows x64; support vanilla Fabric rendering and Sodium.
- Package all nine features together after implementing and checking them in stages.
- Use Ponytail for coding and Context7 for version-specific documentation. Ask before making choices that depend on missing information.
- Java 21 bytecode, official Mojang mappings, Loader 0.19.5, Fabric API 0.141.6+1.21.11, Loom 1.15.5, Gradle 9.2.1.

## Interface
- Right Shift opens the rebindable GUI. Independently draggable rounded navigation/settings panels; save positions and provide Reset Layout.
- Flat feature list with search; Theme and HUD Editor at the bottom.
- Every feature has Enabled and an assignable keybind. Feature binds start unbound.
- Live world preview; unlocked cursor; suspend movement and attacks while editing.
- Charcoal panels, purple/cyan accents, smooth sans-serif text, sliders, switches, swatches and checked choices.
- Customize primary/secondary/background colors and background transparency. Scroll long panels; sliders accept typed values; color picker and hex entry.
- Persist settings, placement and enabled states. Camera modes always start inactive after launch/world changes.

## Features
### Hand Chams
First-person hands and held items, original textures, flowing animated two-color gradient outline and adjustable colored fill. Controls: both colors, animation speed, glow quality, radius, glow opacity, fill opacity. Zero fill means outline only. No other effect or color modes.
### View Models
Independent enabled, scale, position XYZ and rotation XYZ for main-hand item, off-hand item, main arm and off arm; preserve normal animations.
### Spotify HUD
Read local Spotify Windows desktop session. Cover, title, artist, progress and elapsed/total times; previous/play-pause/next/repeat, hiding unsupported buttons. Interact with GUI/editor open. Customize position/scale. Keep paused track; hide when Spotify closes.
### Hit Particles
Your melee contacts with players/living mobs, including blocked hits; none for air swings. Types: Stars, Snowflakes, Ghosts, Hearts, Diamonds, Thunders, Moons, Clouds. Color; Fall/Rise/Burst/Fly; scale, speed, amount, lifetime, rotation-angle range, rotation-speed range and glow.
### Jump Circle
Ground circle at takeoff, fixed chosen size, fade over duration. Size, duration, color, Default/Glowing/Custom local PNG. Only Self switch; off includes nearby observed player jumps.
### Ambience
Separate particles/time/weather/fog settings. Particles: Triangles/Cubes, color, amount, size, speed, spawn distance. Time: Dawn/Morning/Day/Evening/Sunset/Night/Real Time/custom; fixed presets/custom; Real Time follows PC clock. Weather visuals only: Clear/Rain/Thunder/Snow, preserving server weather sounds. Fog: Vanilla/Removed/Custom, color, start/end distances, Sync with Sky makes sky follow custom fog color. Restore current server environment on disable.
### Shield Status
Tint shields Ready/Using/Disabled/Low Durability. Configurable colors, threshold, smooth transitions, Only Self. Exact local cooldown; identifiable observed events estimate remote stuns. Destroyed shield disappears. Combined with Chams: status-colored surface, animated Chams outline.
### Freelook
Independent camera rotation; preserve player facing/movement. Third-person back, restoring previous perspective on exit. Toggle/Hold activation modes.
### Freecam
Detached toggle camera, speed/sprint multiplier, WASD/jump up/sneak down. Pass through blocks. Render body at actual position. Block player movement input/interactions, use only client-loaded terrain.

## Engineering
- Native Minecraft GUI/shared controls and small feature registry; Fabric HUD/world APIs and targeted mixins.
- Versioned local JSON configuration; keybinds synchronized with Minecraft Controls.
- Bundled Windows media bridge; media calls off rendering thread; update artwork only on changes.
- Mutually exclusive camera modes; restore on disable/death/disconnect/dimension change.
- Bounded effects; dispose GPU resources; reject invalid PNGs without losing working texture.
- Order: foundation/config -> GUI/theme -> viewmodels/cameras -> effects/ambience -> shields/chams -> Spotify -> combined checks/package.

## Defaults
All modules off initially, neutral viewmodels, purple/cyan on charcoal at 92% opacity, white/cyan Chams with zero fill, Only Self on for circles/shields, low-durability threshold 20%.

## Acceptance
- Controls/rebinding/search/scroll/drag/HUD scale and placement/persistence/layout recovery at different resolutions/GUI scales.
- Both hands/left-handed/maps/bows/eating/blocking/shields and combined first-person effects.
- All particles/physics/circle styles; invalid images; repeated events; bounded lifetimes.
- Ambience restoration and preserved server weather sounds.
- Camera/player separation and transition cleanup.
- Shield precedence/transitions and explicitly estimated remote states.
- Live Spotify transitions/closing/reopening/missing art/long titles/available controls.
- Focused logic checks, Fabric and Fabric+Sodium, single-player and multiplayer; builds alone do not prove gameplay acceptance.

Reference images are preserved under docs/references. No source or assets from the example client are included.

## Approved addition — Fullbright (2026-10-21)

The user requested a Fullbright feature after the GUI refinement. Added as the tenth module with the standard Enabled switch and assignable keybind, disabled and unbound by default. Brightens client lighting and restores normal lighting on disable without changing gamma or potion effects. Configuration and existing modules remain compatible. See docs/FULLBRIGHT-0.1.2.md for implementation and acceptance evidence.

## Approved addition — Low Fire (2026-10-21)

The owner confirmed the current features also work in multiplayer and requested a Low Fire module to reduce screen obstruction while burning. Added as the eleventh module with the standard Enabled switch and assignable keybind, disabled and unbound by default. Lowers only first-person fire overlay geometry, retains a burning indicator and restores vanilla height when disabled. Damage, world fire and third-person flames are unchanged. The prior decision to leave automated dedicated-server testing/EULA acceptance disabled remains in place.

## Approved additions — 0.1.4 (2026-10-21)

Implement Zoom, Block Overlay and Hit Color as approved in docs/MODULES-0.1.4.md. Zoom uses a separate hold binding while its Enabled setting arms the module. Block Overlay follows the target selection shape with independent outline/fill controls and depth testing. Hit Color filters Players/Mobs/Self and tints native hurt/death flashes on bodies and worn armor/trims while preserving held items and glint. All three start disabled/unbound; configuration schema remains version 1. The target/toolchain and no-new-dependency requirement remain unchanged. Package the JAR and complete source with Fabric/Sodium checks and explicit remaining multiplayer acceptance.


## Approved GUI redesign and second target — 0.2.0 (2026-10-22)

The user requested a full toolkit-guided GUI redesign and separate Fabric builds for **1.21.11** and **26.2** (second target explicitly clarified). See [GUI redesign contract and verification](docs/GUI-REDESIGN.md). Preserve the fourteen modules, configuration schema 1, theme customization, two draggable panels, search, keybindings and HUD placement. Add persistent Reduced Motion. The root remains Java 21 / 1.21.11; `versions/26.2` uses Java 25, Loom 1.18.4 and Fabric API 0.161.0+26.2 with native version-specific rendering hooks. The toolkit's 26.1.2 showcase is not a runtime dependency.


## GUI polish 0.2.1 (2026-10-23)

Requested after reviewing the owner's alignment screenshots. Keep the 0.2.0 design/toolkit and both native Minecraft adapters. Use real game captures before/after. Normalize capital-height metrics in shared text rendering; center real text fields and align row labels/value boxes/modal actions. Redesign Spotify using the same charcoal/theme/rounded components, rounded art, status, progress and capability-driven controls. Keep background media/art work and independent HUD scale.

The owner approved **50–150%** whole-GUI scaling, default 100%, in Theme. Save it in schema 1; scale both panels, popups, clipping and input together. Preserve automatic viewport fit, scroll on large sizes, and reset scale with layout recovery. Implementation and evidence: [GUI-POLISH-021.md](docs/GUI-POLISH-021.md).


## Spotify repeat glyph 0.2.2 (2026-10-23)

Owner requested that the remaining pixelated Spotify repeat symbol match the smoothed HUD. Both adapters now draw its loop, rounded corners, arrowheads and repeat-one mark with the existing antialiased vector primitives. Keep the current off/one/all capability and secondary-color mapping. See [repeat icon notes](docs/SPOTIFY-REPEAT-022.md).


## Lunar / Iris 26.2 startup compatibility — 0.2.3 (2026-10-23)

Research the official custom-Fabric-mod loading workflow using Context7 plus official Lunar guides where the index lacks launcher docs. The owner's exact crash is an exclusive first-person hand-call redirect conflict between Flux and Iris, reproducible outside Lunar. Patch only the 26.2 adapter to preserve the operation chain and add a pinned Iris acceptance profile. Keep the real Lunar retest and shader-pack support separate from ordinary Fabric + Iris evidence. The 1.21.11 build remains 0.2.2. Findings and sources: docs/LUNAR-COMPATIBILITY.md.

## Module icons, Enable/Action controls and notification HUD — 0.2.4 (2026-10-23)

Owner approved module icons and separate Enable/Action bindings, then requested a positionable notification HUD for Enabled, Disabled, Activated and Deactivated events. Preserve both adapters, all fourteen modules and the 26.2 Iris fix. Use shared vector icons and theme components; keep existing camera/Zoom binds as Action and add unbound Enable bindings. Save availability but never active camera state. Continuous modules retain their existing Enable binds. HUD Editor selects/moves/scales Spotify and Notifications independently, with previews and selected-element reset. Notification defaults: silent, three-card limit, 2.5-second lifetime, reduced-motion-aware cubic arrival/departure. See docs/MODULE-CONTROLS-024.md for the design and evidence.
