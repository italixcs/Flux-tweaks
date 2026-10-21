# Flux Tweaks 0.1.4

A client-only Minecraft Java 1.21.11 mod for Windows x64. Includes 14 modules: the original nine features, Fullbright, Low Fire, Zoom, Block Overlay and Hit Color, with the refined GUI. The owner confirmed the existing features work in multiplayer on 2026-10-21; see [CHECKLIST.md](CHECKLIST.md) and [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md).

## Install

1. Use a Minecraft **1.21.11 Fabric** profile, Java **21**, Fabric Loader **0.19.5 or newer**, and Fabric API **0.141.6+1.21.11**.
2. Put **fluxtweaks-0.1.4.jar** in that profile's `mods` folder. The Spotify bridge is bundled; no Spotify credentials or web login are needed.
3. Optionally add **Sodium 0.8.14 for 1.21.11 Fabric**, which was checked alongside this build.
4. Launch the game, join a world, and press **Right Shift**. All features start disabled.

The server does not need this mod. The supported rendering configurations are Fabric alone and Fabric + Sodium. Iris/shader packs and other rendering modifications have not been checked.

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
- Each feature has an Enabled switch and keybind row. Click the row and press a keyboard or mouse button; **Delete/Backspace** clears it and **Esc** cancels. Feature binds also appear in Minecraft Controls and initially remain unbound. **Zoom is a hold binding:** its Enabled switch arms it; the key temporarily zooms. Other module keys toggle, except Freelook when configured to Hold.
- Drag a panel's heading to move it. Select a feature to open its settings; scroll the feature list or long settings. Search filters the flat list.
- Drag sliders, or click their displayed value to type. Range values accept `min, max`. Colors offer a picker and six-digit hex entry.
- **Theme:** primary/secondary/background colors and background transparency; **Reset Layout** restores both panels and HUD placement. **Home** also resets layout.
- **HUD Editor:** drag the Spotify card, scroll to scale, arrow keys to nudge, Esc to return. Playback buttons work while the editor or Flux GUI is open.
- **Ctrl+F:** focus search. **Tab/Shift+Tab:** move control focus, with narrator labels. **Enter/Space:** activate a focused control. **Page Up/Down:** scroll settings.

The world keeps running in Flux screens. Gameplay movement and attack input are suspended while editing. Settings are saved to `config/fluxtweaks.json`; imported circles are copied to `config/fluxtweaks/jump-circle.png`. Invalid settings files are preserved as `.invalid-<timestamp>` before defaults are recovered. Camera features always reset on launch and world changes.

## Features

| Feature | Included controls and behavior |
| --- | --- |
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

Use JDK 21 and Visual Studio C++ Build Tools with the Windows SDK (C++/WinRT). No global Java or Windows settings need changing.

```powershell
.\gradlew.bat build --console plain
.\gradlew.bat runProductionClientGameTest --console plain
.\gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain
```

`build` compiles Java with `--release 21`, builds the x64 native bridge with the static C++ runtime, runs focused JUnit checks, and produces `build/libs/fluxtweaks-0.1.4.jar`. `runProductionClientGameTest` runs the packaged intermediary-mapped mod in an isolated single-player game. Test worlds and screenshots stay under `build/run/productionFabric` or `build/run/productionSodium`. The test mod is separate and is never included in the installation JAR. The normal development game check is also available as `runClientGameTest`.

The owner confirmed multiplayer gameplay works in 0.1.2. Automated dedicated-server testing and EULA acceptance remain **disabled**; no multiplayer test server was started. Low Fire and the three 0.1.4 additions require confirmation in the owner's multiplayer profile.

`tools/WindowsMediaAcceptance.java` contains the explicitly authorized live media check. It changes playback and must only be run when the user permits that; it restores the initial pause/repeat state. The procedural image generators require Python + Pillow for asset regeneration only; the mod does not use Python.

## Source map

- `src/main/java/dev/fluxtweaks/config`: shared catalog, validation, persistence, pure visual math.
- `src/client/java/dev/fluxtweaks`: lifecycle, native screens, features, rendering, media and targeted mixins.
- `src/test`: focused configuration/time/shield priority checks.
- `src/gametest`: packaged-game GUI, camera, item-use, particles, PNG and shield smoke checks.
- `native`: C++/WinRT media bridge and Windows build script.
- `docs/references`: the 17 supplied design references, kept separate from implementation assets.

Lato is bundled under its SIL Open Font License in `docs/licenses/lato-ofl.txt` and `META-INF/licenses` inside the JAR. Effect sprites were generated specifically for this project; GUI shapes are now drawn by the mod's anti-aliased native rendering pipeline. The supplied client screenshots are design references, not copied implementation assets.
