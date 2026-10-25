# Butterfly Knife — 0.3.0

Added following the owner's research approval and request for glint, independent size control, and View Models / Hand Chams compatibility. The feature has native adapters for Minecraft 1.21.11 / Java 21 and 26.2 / Java 25; existing version pins and the 26.2 Iris operation chain remain authoritative.

## Controls and appearance

Open Right Shift → Butterfly Knife. Enable the module to replace locally held vanilla swords in first person. Enable Key toggles the module; Inspect Key starts a single inspection. Both are initially unbound and appear in Minecraft Controls. Use the normal binding editor to assign keyboard/mouse buttons, clear with Delete/Backspace, or cancel with Escape.

- Finish: Steel, Flux, Sunset, Violet Marble, Emerald Marble, Obsidian Red. All six use original artwork.
- Inspect Style: Flip, Spin, Flourish, or Random.
- Knife Scale: 0.50–2.00x, default 1.00x. This multiplies the selected hand's View Models scale; its position and rotation still apply. Large combined settings can intentionally push the knife beyond the viewport.
- Shimmer / Glint: enabled by default, adds Minecraft's moving glint to an ordinary sword. Turning it off preserves the normal glint of an enchanted sword.

The original model has a curved blade with a cutting bevel, two independently pivoted handles with open channels, inlays, spacers, hinge hardware, screw details and a latch. Opening and inspection animate the rigid parts independently. The visual direction follows the owner's choice between realistic metal detail and Minecraft geometry. Metallic appearance uses original textures, geometry, native lighting and glint; no shader pack is required.

## Gameplay and lifecycle

All seven vanilla sword materials qualify, including copper. The sword remains its original item: damage, reach, attack cooldown, enchantments, durability, mining and server interactions are unchanged. The knife is a local first-person cosmetic; other clients, inventory icons, dropped items and third-person models retain their normal appearance.

Attacking, using an item, switching items, disabling the feature, opening a screen, losing focus, death, camera changes and world changes cancel transient animation. Inspection starts from the displayed equip pose, ignores additional requests while an inspection is running, and never queues a backlog. Inspect the main-hand sword when present, otherwise the off-hand sword. Both dominant-hand settings are supported.

Reduced Motion suppresses automatic opening movement and replaces requested inspection with a short, restrained presentation turn; the handles remain still. The feature retains Minecraft's hand presentation rather than adding custom articulated fingers. New sound effects are outside this release.

Settings persist through the existing configuration store and Minecraft keybinding options. Older configurations add the new module disabled and keep previous feature settings. Active animation is never serialized.

## Rendering and compatibility

Both adapters use the native special-model submission path and immutable pose snapshots. The model samples monotonic elapsed time; rendering an extra Chams or shield-mask pass cannot speed up inspection or select a second random sequence. Deferred submissions apply their own saved pose. Existing item/arm transforms, lighting and enchantment glint are retained.

Hand Chams captures the blade and both moving handles through its normal collector. Shield Status continues to tint the shield and excludes the knife from its shield-only mask. The 26.2 `WrapOperation` around hand submission is preserved, retaining the previous Iris startup fix. See [the Iris investigation](LUNAR-COMPATIBILITY.md).

While enabled, the feature takes precedence over resource-pack sword geometry in its eligible local first-person contexts. Disable it to restore that presentation. Unrelated items and other display contexts retain their resolved resource-pack models. Missing knife textures cause a vanilla fallback until resources reload. Model caches are invalidated on resource reload and shutdown; textures use Minecraft's resource lifecycle.

Original texture sources, palettes, provenance and a contact sheet live in `assets-src/butterfly/`; regenerate through `tools/butterfly_assets.py`. The native mesh and keyframe source remain editable in the project. There is no animation-library runtime dependency and no change to the installed GUI toolkit.

## Verification

Implementation checks and final packaged profile results are recorded below after the final rendering revision. Earlier exploratory captures are not release evidence. Automated checks establish the named cases; owner approval in the normal multiplayer/Lunar/shader-pack profile and sustained performance measurements remain separate.

Dedicated-server testing and EULA acceptance remain disabled. No Spotify playback commands, launcher-profile changes or account operations are needed for this feature.
