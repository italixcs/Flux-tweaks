# Butterfly Knife - 1.21.11 v0.3.4 / 26.2 v0.3.3

The 1.21.11 v0.3.4 build also supports Fabric Loader 0.19.3; see [the compatibility patch](LOADER-034.md). Knife behavior is unchanged.

Version 0.3.3 removes the extra player hand/sleeve from 0.3.2 at the owner's request. Only the knife animates. Spin/catch, Animation Speed, independent size, glint and the Sodium fix remain. See [the current correction and checks](BUTTERFLY-KNIFE-033.md).

The historical 0.3.2 update introduced the grip-centered spin/catch and Animation Speed slider alongside an extra native hand, which is removed in 0.3.3. See [the current motion and verification record](BUTTERFLY-SPIN-032.md). The original 0.3.0 verification section below is retained as historical evidence.

**Sodium correction:** 0.3.0 can render a large box because its custom mesh enters Sodium's cached cuboid path. The earlier image-change checks missed the incorrect shape. See [the 0.3.1 fix and stronger geometry regression](BUTTERFLY-SODIUM-031.md); the original Sodium appearance-verification claim below is superseded.

Added following the owner's research approval and request for glint, independent size control, and View Models / Hand Chams compatibility. The feature has native adapters for Minecraft 1.21.11 / Java 21 and 26.2 / Java 25; existing version pins and the 26.2 Iris operation chain remain authoritative.

The owner's follow-up requested more realism. Final geometry uses a continuous 32-section beveled blade surface, continuous rounded handle rails and circular pivot washers. Joined mesh faces and continuous UVs replace the initial stepped blade/cuboid rails. Six 256×256 original material atlases add brushed grain and restrained metal highlight bands. The model stays inside Minecraft's native lighting, glint and Chams submission path.

## Controls and appearance

Open Right Shift → Butterfly Knife. Enable the module to replace locally held vanilla swords in first person. Enable Key toggles the module; Inspect Key plays once, repeats while held, or toggles repeat playback according to Inspect Mode. Both are initially unbound and appear in Minecraft Controls. Use the normal binding editor to assign keyboard/mouse buttons, clear with Delete/Backspace, or cancel with Escape.

- Finish: Steel, Flux, Sunset, Violet Marble, Emerald Marble, Obsidian Red. All six use original artwork.
- Inspect Style: Flip, Spin, Flourish, or Random.
- Inspect Mode: Press (play once, default), Hold (repeat while held), or Toggle (repeat until pressed again). Attack/use/screens/item changes and other existing cancellations stop the loop; held keys need a fresh release/press to restart.
- Animation Speed: 0.25-3.00x, default 1.00x. Changes opening and inspection timing; a new animation samples the selected speed. Changing settings cancels an active inspect through the existing screen lifecycle.
- Knife Scale: 0.50–2.00x, default 1.00x. This multiplies the selected hand's View Models scale; its position and rotation still apply. Large combined settings can intentionally push the knife beyond the viewport.
- Shimmer / Glint: enabled by default, adds Minecraft's moving glint to an ordinary sword. Turning it off preserves the normal glint of an enchanted sword.

The original model has a curved blade with a cutting bevel, two independently pivoted handles with open channels, inlays, spacers, hinge hardware, screw details and a latch. Opening and inspection animate the rigid parts independently. The visual direction follows the owner's choice between realistic metal detail and Minecraft geometry. Metallic appearance uses original textures, geometry, native lighting and glint; no shader pack is required.

## Gameplay and lifecycle

All seven vanilla sword materials qualify, including copper. The sword remains its original item: damage, reach, attack cooldown, enchantments, durability, mining and server interactions are unchanged. The knife is a local first-person cosmetic; other clients, inventory icons, dropped items and third-person models retain their normal appearance.

Attacking, using an item, switching items, disabling the feature, opening a screen, losing focus, death, camera changes and world changes cancel transient animation. Inspection starts from the displayed equip pose and never queues a backlog. Press ignores repeated requests while a cycle is playing. Hold and Toggle start a new cycle when the previous one finishes; key repeat cannot switch a toggle repeatedly. Inspect the main-hand sword when present, otherwise the off-hand sword. Both dominant-hand settings are supported.

Reduced Motion suppresses automatic opening movement and replaces requested inspection with a short, restrained presentation turn; the handles remain still. The feature animates only the knife and adds no attached hand or sleeve. Vanilla arm rendering and its View Models controls remain unchanged. New sound effects are outside this release.

Settings persist through the existing configuration store and Minecraft keybinding options. Older configurations add the new module disabled and keep previous feature settings. Active animation is never serialized.

## Rendering and compatibility

Both adapters use the native special-model submission path and immutable pose snapshots. The model samples monotonic elapsed time; rendering an extra Chams or shield-mask pass cannot speed up inspection or select a second random sequence. Deferred submissions apply their own saved pose. Existing item/arm transforms, lighting and enchantment glint are retained.

Hand Chams captures the blade and both moving handles through its normal collector. Shield Status continues to tint the shield and excludes the knife from its shield-only mask. The 26.2 `WrapOperation` around hand submission is preserved, retaining the previous Iris startup fix. See [the Iris investigation](LUNAR-COMPATIBILITY.md).

While enabled, the feature takes precedence over resource-pack sword geometry in its eligible local first-person contexts. Disable it to restore that presentation. Unrelated items and other display contexts retain their resolved resource-pack models. Missing knife textures cause a vanilla fallback until resources reload. Model caches are invalidated on resource reload and shutdown; textures use Minecraft's resource lifecycle.

Original texture sources, palettes, provenance and a contact sheet live in `assets-src/butterfly/`; regenerate through `tools/butterfly_assets.py`. The native mesh and keyframe source remain editable in the project. There is no animation-library runtime dependency and no change to the installed GUI toolkit.

## Verification

Final checks completed on 2026-10-25 after the continuous blade/rounded handle/256×256 texture revision. Both builds pass, with **57 logic cases on Java 21 and 57 on Java 25**, including migration, persistence, bounded scale, animation interruption, equip-to-inspect continuity, Reduced Motion and repeat-sample invariance.

| Packaged client | Result |
| --- | --- |
| Minecraft 1.21.11 / Fabric / Java 21 | Pass |
| Minecraft 1.21.11 / Sodium 0.8.14 | Pass |
| Minecraft 26.2 / Fabric / Java 25 | Pass |
| Minecraft 26.2 / Sodium 0.9.2 | Pass |
| Minecraft 26.2 / Sodium 0.9.2 + Iris 1.11.4, no shader pack | Pass |

The game suite uses actual Enable/Inspect input, checks replacement of the sword, six visibly different finishes, moving poses for all three styles, attack/use/GUI/disable cancellation, and rejection for disabled/non-sword states. A color-area comparison verifies that Knife Scale enlarges actual geometry. Both dominant hands and an off-hand knife are captured. Combined checks enable glint, Chams fill/outline and the shield mask together with non-default View Models item scales/offset/rotation. Native submission probes verify optional shimmer and retained enchanted glint; server-side assertions verify the original sword identity, enchantment and durability. Existing GUI/input/camera/effect/item/HUD suites also run in each profile.

Final actual-game captures were visually inspected for blade silhouette, handle articulation, finishes, glint, Chams and controls. Saved examples: [1.21.11 Steel](acceptance/030-12111-steel.png), [glint](acceptance/030-12111-glint.png), [combined effects](acceptance/030-12111-combined.png), [scale/glint settings](acceptance/030-12111-settings.png); [26.2 Steel](acceptance/030-262-steel.png), [26.2 settings](acceptance/030-262-settings.png), [Iris combined effects](acceptance/030-262-iris.png). Earlier exploratory captures are not release evidence.

Commands used the pinned wrappers: root `build runProductionClientGameTest` and `runProductionClientGameTest -PsodiumAcceptance`, with Java 21; the same under `versions/26.2` with Java 25, plus `runProductionClientGameTest -PirisAcceptance`. Clients ran sequentially. Logs are ignored local files under `work/butterfly-release-*.log`; raw captures remain in each profile's `build/run/.../screenshots`. The installed GUI toolkit dry-run reports `changes: []`.

Distribution files are under `build/distributions/0.3.0`: two Minecraft-labelled JARs, complete source ZIP, installation notes and SHA-256 manifest. Package inspection verifies target metadata/bytecode, six matching textures, native bridge and licenses, and exclusion of the game-test mod. The source ZIP is checked byte-for-byte against the project files.

Automated checks establish the named cases; owner approval in the normal multiplayer/Lunar/shader-pack profile, subjective animation feel, sustained sessions and measured performance at different FPS caps remain separate. No blanket third-party-mod compatibility or frame-time claim is made.

Dedicated-server testing and EULA acceptance remain disabled. No Spotify playback commands, launcher-profile changes or account operations are needed for this feature.
