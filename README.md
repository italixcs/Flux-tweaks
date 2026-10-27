# Flux Tweaks

A client-only Minecraft Java mod for Windows x64, with separate **1.21.11** and **26.2** builds. Includes 15 modules: the original nine features, Fullbright, Low Fire, Zoom, Block Overlay, Hit Color and an animated 3D Butterfly Knife. The owner confirmed earlier features work in multiplayer on 2026-10-21; that confirmation does not cover the new knife. See [CHECKLIST.md](CHECKLIST.md) and [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md).

## Install

Choose the JAR that matches your Minecraft profile. Install only one Flux Tweaks JAR per profile.

| Minecraft | Java | Fabric Loader | Fabric API | Optional Sodium |
| --- | --- | --- | --- | --- |
| 1.21.11 | 21 | 0.19.3 or newer | 0.141.6+1.21.11 | 0.8.14 for 1.21.11 |
| 26.2 | 25 | 0.19.5 or newer | 0.161.0+26.2 | 0.9.2 for 26.2 |

1. Create the matching Fabric profile and install its Fabric API.
2. Add `fluxtweaks-0.3.4-mc1.21.11.jar` or `fluxtweaks-0.3.3-mc26.2.jar` to its `mods` folder. The Windows Spotify bridge is bundled.
3. Launch a world and press **Right Shift**. Existing settings migrate; new installations start with all modules disabled and feature keys unbound.

## Fabric Loader 0.19.3 compatibility - 0.3.4 for 1.21.11

The 1.21.11 JAR now accepts **Fabric Loader 0.19.3 or newer**, matching the owner's launcher profile. It includes all 0.3.3 features, knife-only animation, speed and Press/Hold/Toggle controls. Minecraft, Java, Fabric API and Sodium versions stay the same. The separate 26.2 build stays on 0.3.3 / Loader 0.19.5+. See [the dependency correction and checks](docs/LOADER-034.md).

## Knife-only spin and playback modes - 0.3.3

Removes the extra player hand/sleeve added in 0.3.2. Only the knife animates: the continuous pen-style spin, handle flips, catch, Animation Speed slider, size, glint and existing View Models / Hand Chams controls remain. **Inspect Mode** offers **Press** (play once, the default), **Hold** (repeat while held), and **Toggle** (repeat until the next press). The hand-spin comparison described the knife's movement; it did not require an attached animated arm. See [the correction and checks](docs/BUTTERFLY-KNIFE-033.md).

## Butterfly hand spin and speed - 0.3.2 (hand removed in 0.3.3)

Butterfly inspections now pivot around the held handle, with a visible player hand and sleeve, a continuous pen-style double roll, independent handle flips, wrist movement and a catch. **Animation Speed** runs from **0.25x to 3x**, default 1x, and changes both opening and inspection timing. Knife Scale keeps the grip anchored; View Models retains separate item/arm controls and Hand Chams captures both. The 0.3.1 Sodium geometry correction remains included. See [motion, controls and verification](docs/BUTTERFLY-SPIN-032.md).

## Sodium knife rendering fix — 0.3.1

Fixes the large white box that replaced the Butterfly Knife with Sodium. Custom mesh surfaces now bypass Sodium's cached cuboid compiler while retaining native lighting, glint, Hand Chams and View Models. No Sodium settings need changing. Both Minecraft builds include the fix. See [root cause, corrected evidence and geometry regression](docs/BUTTERFLY-SODIUM-031.md).

## Butterfly Knife — 0.3.0

- **Right Shift → Butterfly Knife:** enable an original articulated knife for all seven vanilla sword materials in your local first-person view.
- Curved beveled blade, rounded channelled handles, pivot hardware and latch; opening animation plus Flip, Spin, Flourish and Random inspections.
- Six finishes: Steel, Flux, Sunset, Violet Marble, Emerald Marble and Obsidian Red. Add moving glint with **Shimmer / Glint**; enchanted swords retain their normal glint when this switch is off.
- **Knife Scale: 50–200%**, composing with View Models' scale/position/rotation. Hand Chams outlines and fills the moving parts alongside Shield Status.
- Separate **Enable Key** and **Inspect Key**, initially unbound. Attack, use, item swaps and screens interrupt inspection. Settings persist; active animations reset. Reduced Motion uses a restrained presentation turn.

See [controls, rendering and verification](docs/BUTTERFLY-KNIFE.md). The 1.21.11 build uses 0.3.4 and the 26.2 build uses 0.3.3 and include the previous GUI, notifications and 26.2 Iris fixes. This is cosmetic; sword gameplay, other clients, inventory and third-person appearance remain normal.

## Module controls and notifications — 0.2.4

- Smooth module icons in the feature list, settings heading and notifications.
- **Freecam, Freelook and Zoom:** separate **Enable Key** and **Action Key**. Enable saves availability; Action starts/stops the operation. Both cameras offer Hold/Toggle; Zoom stays Hold. The GUI shows Disabled, Ready or Active. Existing keys for these three become Action keys; new Enable keys start unbound.
- **Notifications HUD:** themed, silent Enabled/Disabled/Activated/Deactivated cards, up to three at once for 2.5 seconds, with Reduced Motion support. Theme → Module Notifications turns them off.
- **HUD Editor:** choose Spotify or Notifications, then drag, scroll to scale, or use arrows to nudge. Tab changes selection; Reset/Home resets only the selected HUD. Placement and scale save independently.
- Saved camera availability survives launch and world changes; actual camera/zoom actions always start inactive. Disabling an active module stops it immediately.

See [controls, migration and acceptance](docs/MODULE-CONTROLS-024.md). These controls remain included in 0.3.0.

## Minecraft 26.2 Iris compatibility patch — 0.2.3

The 26.2 JAR fixes a startup conflict between Flux Hand Chams and Iris's hand renderer, also present in Lunar's bundled Iris stack. The hand hook preserves the original operation chain, including in 0.3.0. See [Lunar documentation, exact crash diagnosis and verification limits](docs/LUNAR-COMPATIBILITY.md).

## Spotify repeat icon in 0.2.2

The repeat control now uses smooth, rounded vector strokes for its loop, arrowheads and repeat-one mark. Off stays white; repeat-all and repeat-one use the secondary theme color. See [repeat icon notes](docs/SPOTIFY-REPEAT-022.md).

## GUI polish in 0.2.1

- Text now uses measured font metrics: aligned headings, centered search/input text, labels, toggles, value boxes and checked choices. Range fields and dialog actions share the same alignment rules.
- Spotify HUD uses the GUI theme, rounded artwork and buttons, clear title/artist/status hierarchy, accent progress and interaction feedback. Unsupported actions remain hidden; its HUD scale remains independent.
- **Theme → GUI Scale:** 50–150%, default 100%, with slider or typed percentage. Scales both panels and modal editors together, including input and clipping; large settings remain scrollable. The window may constrain the effective size.
- **Reset Layout / Home** also restores GUI scale to 100%. Existing configuration and all fourteen features are preserved.
- HUD Editor moves its toolbar to the edge opposite the card to reduce overlap.

See [GUI polish design and checks](docs/GUI-POLISH-021.md) for before/after captures and the version-specific acceptance record.

## GUI redesign in 0.2.0

The full settings interface follows the installed minecraft-gui toolkit: larger proportional type, consistent rounded surfaces and spacing, compact checked choice menus, clearer color/value fields, smooth sliders, explicit modal actions, keyboard focus, and a saved **Reduced Motion** preference. Two draggable panels, search, custom colors/transparency and every existing feature remain available. The HUD editor has Reset and Done controls. Press **Home** in the GUI to recover panel layout, or use **Theme → Reset Layout**.

See [design and verification notes](docs/GUI-REDESIGN.md) for the native adapters, dual-version commands and evidence limits. The upstream ModernUI showcase is reference material; it is not a dependency of these builds.

The server does not need this mod. Fabric alone and Fabric + Sodium are checked on both targets. The 26.2 compatibility patch also checks Iris 1.11.4 with no shader pack selected. Real Lunar acceptance and shader-pack rendering remain pending; see the compatibility report.

## New modules in 0.1.4

- **Zoom:** enable the module, assign a key, then hold it. Scroll while held to adjust magnification without changing hotbar slots. Each activation starts at the saved default (4x); transition duration defaults to 150 ms. Sensitivity scales automatically; FOV and sensitivity options are never overwritten. Opening screens, focus loss, death and world changes cancel zoom and require a fresh press.
- **Block Overlay:** independent outline/fill switches, colors and opacity; adjustable 1–6 px thickness. Follows the actual targeted selection shape and respects terrain depth. Defaults to a purple outline and cyan fill at 20% opacity. Both parts off hides the selection; disabling the module restores vanilla.
- **Hit Color:** replaces normal hurt/death flashes on textured bodies and armor, including trims. Default purple at 35% opacity; Players/Mobs on, Self off. Held items and enchantment glint keep their existing appearance.

All three start disabled and unbound. Existing version-1 settings remain compatible. See [the approved specification and acceptance record](docs/MODULES-0.1.4.md).

## Low Fire added in 0.1.3

Open **Right Shift → Low Fire** to enable it or assign a keybind. It lowers first-person burning flames toward the bottom of the screen, leaving a visible burning indicator. It starts disabled and unbound; enabled state and bindings are saved. Switching it off restores the normal overlay immediately. Fire damage and world fire visuals keep their vanilla behavior. See [Low Fire notes](docs/LOW-FIRE-0.1.3.md).

## Fullbright added in 0.1.2

Open **Right Shift → Fullbright** to enable it or assign a keybind. It starts disabled and unbound; enabled state and bindings are saved. It brightens client lighting without changing Minecraft brightness settings or adding potion effects. Disabling it immediately restores normal lighting. Existing configurations remain compatible. See [Fullbright checks](docs/FULLBRIGHT-0.1.2.md).

## GUI update in 0.1.1

- Smoothly filtered Lato text and real bold headings, including search and numeric/hex inputs.
- Anti-aliased shader-drawn panels, round switches/swatches/slider knobs, slim pill-shaped tracks and clean vector icons.
- More compact proportions and consistent spacing; all nine features fit in the navigation list at the checked resolutions.
- Settings panels fit short feature lists and scroll long ones; hover, switch, slider-value and scroll changes ease smoothly.
- A continuous saturation/value picker and hue strip, with hex entry.

Replace the old mod JAR with the new one. Your existing version-1 configuration remains compatible. See [the GUI update notes](docs/GUI-0.1.1.md) for screenshots and checks, and [the original acceptance record](docs/ACCEPTANCE.md) for first-release evidence and pending multiplayer checks.

## Controls and settings

- **Right Shift:** open/close Flux Tweaks. Rebind under Minecraft Controls → Flux Tweaks.
- Each feature has an Enabled switch and **Enable Key**. Freecam/Freelook/Zoom also have an **Action Key**: enable them first, then use Action to operate them. Click a binding row and press a keyboard or mouse button; **Delete/Backspace** clears it and **Esc** cancels. Bindings also appear in Minecraft Controls. Continuous modules use their Enable key to toggle the effect; cameras use their selected Hold/Toggle mode, and Zoom uses Hold.
- Drag a panel's heading to move it. Select a feature to open its settings; scroll the feature list or long settings. Search filters the flat list.
- Drag sliders, or click their displayed value to type. Range values accept `min, max`. Colors offer a picker and six-digit hex entry.
- **Theme:** primary/secondary/background colors, transparency, Reduced Motion and **GUI Scale (50–150%)**. **Reset Layout / Home** restores both panels, HUD placement and 100% GUI scale. Spotify and Notifications retain separate HUD scales. Theme also has a **Module Notifications** switch.
- **HUD Editor:** select Spotify or Notifications using its chip, preview, or Tab. Drag to move, scroll to scale, arrows to nudge, Reset/Home to reset the selected element, Esc/Done to return. Spotify playback buttons work while the editor or Flux GUI is open.
- **Ctrl+F:** focus search. **Tab/Shift+Tab:** move control focus, with narrator labels. **Enter/Space:** activate a focused control. **Page Up/Down:** scroll settings.

The world keeps running in Flux screens. Gameplay movement and attack input are suspended while editing. Settings are saved to `config/fluxtweaks.json`; imported circles are copied to `config/fluxtweaks/jump-circle.png`. Invalid settings files are preserved as `.invalid-<timestamp>` before defaults are recovered. Camera and Zoom actions reset on launch and world changes; saved Enabled preferences remain available as Ready.

## Features

| Feature | Included controls and behavior |
| --- | --- |
| Butterfly Knife | Original first-person articulated sword cosmetic; six finishes, three inspect styles/Random, Enable/Inspect keys, 0.5–2x knife scale and optional forced glint. Composes with View Models and Hand Chams; original sword mechanics and enchantment glint preserved. |
| Hand Chams | Original first-person hand/item textures, animated white/cyan gradient outline by default, optional fill; two colors, animation speed, glow quality/radius/opacity, fill opacity. Fill starts at zero. |
| View Models | Independent enable, scale, position XYZ and rotation XYZ for main/off-hand items and main/off arms. Neutral defaults; normal use animations remain. Logical main/off hands follow the player's handedness. |
| Spotify HUD | Local Spotify desktop session metadata, art, progress and times; available previous/play-pause/next/repeat controls; position/scale. Paused cards stay visible; absent sessions hide the card. |
| Hit Particles | Your melee contacts on living targets, including blocked hits; Stars/Snowflakes/Ghosts/Hearts/Diamonds/Thunders/Moons/Clouds; color, Fall/Rise/Burst/Fly, scale/speed/count/lifetime, angle and rotation-speed ranges, glow. |
| Jump Circle | Fixed-size circle at observed takeoff, fading over its duration; size/duration/color, Default/Glowing/Custom PNG, Only Self. Other observed players are included within 48 blocks when Only Self is off. |
| Ambience | Triangles/Cubes with color/count/size/speed/distance; fixed time presets/custom ticks or PC-clock Real Time; visual Clear/Rain/Thunder/Snow; Vanilla/Removed/Custom fog, color/range and Sync with Sky. Server time/weather data and weather sounds are preserved. |
| Shield Status | Ready/Using/Disabled/Low Durability surface colors, threshold, smooth transitions and Only Self. Priority: Disabled → Low Durability → Using → Ready. Default low threshold 20%. |
| Freelook | Independent third-person camera, Toggle/Hold; preserves player facing and movement and restores the previous perspective. |
| Zoom | Hold-to-zoom, 1–16x default strength, 50–500 ms smooth transition, temporary scroll adjustment in 1.2x steps, scaled sensitivity and lifecycle cleanup. Hands/HUD stay at normal size. |
| Block Overlay | Independent outline/fill switches, color and opacity; 1–6 px outline width. Actual targeted block shape, depth tested, restores vanilla when disabled. |
| Hit Color | Configurable damage color/opacity on bodies and worn armor/trims. Separate Players/Mobs/Self filters, vanilla hurt/death timing, original textures/dye/glint and held-item appearance preserved. |
| Low Fire | Lowers the first-person burning overlay; Enabled switch and assignable keybind, initially disabled/unbound. Preserves a visible fire warning and restores vanilla height when disabled. |
| Fullbright | Brightens dark areas using the client lightmap; Enabled switch and assignable keybind, initially disabled/unbound. Restores normal lighting when disabled. |
| Freecam | Detached camera, speed/sprint multiplier, WASD, jump/sneak vertical movement, block passage, visible real player body. Player movement and interactions are suspended; terrain must already be loaded. |

Freelook and Freecam are mutually exclusive and clean up on death, disconnect or dimension change. When Shield Status and Hand Chams affect a held shield, its surface uses the shield-state tint and its outline uses the Chams gradient.

## Spotify and shield limits

Windows media-session capabilities vary with Spotify versions and current playback. Unsupported buttons are omitted. Some Spotify builds advertise repeat and acknowledge its command but never report a changed repeat state. Flux checks that response on its media worker and hides the unconfirmed control for the session; it does not pretend the request worked. This was observed during acceptance on this PC. Re-enabling the module retries the connection. Other playback controls were confirmed live.

Your own shield cooldown uses game data. Other players' disabled states are **estimates**: Flux uses an identifiable nearby shield-break sound and recently observed blocking, then assumes vanilla's five-second cooldown. Ambiguous events are ignored. Server-specific timing and remote durability visibility cannot be made exact from client-only data.

## Build and checks

For the root **1.21.11** build, use JDK 21 and Visual Studio C++ Build Tools with the Windows SDK (C++/WinRT). No global Java or Windows settings need changing.

```powershell
.\gradlew.bat build --console plain
.\gradlew.bat runProductionClientGameTest --console plain
.\gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain
```

`build` compiles Java with `--release 21`, builds the x64 native bridge with the static C++ runtime, runs focused JUnit checks, and produces `build/libs/fluxtweaks-0.3.1.jar`. `runProductionClientGameTest` runs the packaged intermediary-mapped mod in an isolated single-player game. Test worlds and screenshots stay under `build/run/productionFabric` or `build/run/productionSodium`. The test mod is separate and is never included in the installation JAR. The normal development game check is also available as `runClientGameTest`.

For **26.2**, select JDK 25 and run its own wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4' # or your Java 25 installation
.\versions\26.2\gradlew.bat -p versions/26.2 build --console plain
.\versions\26.2\gradlew.bat -p versions/26.2 runProductionClientGameTest --console plain
.\versions\26.2\gradlew.bat -p versions/26.2 runProductionClientGameTest -PsodiumAcceptance --console plain
```

This build produces `versions/26.2/build/libs/fluxtweaks-mc26.2-0.3.1.jar` using unobfuscated 26.2 classes. The two build directories, test worlds and screenshots are independent. Do not run the two game clients' acceptance suites simultaneously: they exercise window focus and input. The distributable copies are named with both mod and Minecraft versions as shown in Install above.

The owner confirmed multiplayer gameplay works in 0.1.2. Automated dedicated-server testing and EULA acceptance remain **disabled**; no multiplayer test server was started. Low Fire and the three 0.1.4 additions require confirmation in the owner's multiplayer profile.

`tools/WindowsMediaAcceptance.java` contains the explicitly authorized live media check. It changes playback and must only be run when the user permits that; it restores the initial pause/repeat state. The procedural image generators require Python + Pillow for asset regeneration only; the mod does not use Python.

## Source map

- `src/main/java/dev/fluxtweaks/config`: shared catalog, validation, persistence, pure visual math.
- `src/client/java/dev/fluxtweaks`: 1.21.11 lifecycle, native screens, features, rendering, media and targeted mixins.
- `versions/26.2`: Java 25 / Minecraft 26.2 client adapters, tests and independent build.
- `src/main/java/dev/fluxtweaks/gui/motion`: toolkit animation core, with MIT attribution.
- `.agents/skills/minecraft-gui`: complete, unmodified installed toolkit and workflow guides.
- `src/test`: focused configuration/time/shield priority checks.
- `src/gametest`: packaged-game GUI, camera, item-use, particles, PNG and shield smoke checks.
- `native`: C++/WinRT media bridge and Windows build script.
- `docs/references`: the 17 supplied design references, kept separate from implementation assets.

Lato is bundled under its SIL Open Font License in `docs/licenses/lato-ofl.txt` and `META-INF/licenses` inside the JAR. Effect sprites were generated specifically for this project; GUI shapes are now drawn by the mod's anti-aliased native rendering pipeline. The supplied client screenshots are design references, not copied implementation assets.
