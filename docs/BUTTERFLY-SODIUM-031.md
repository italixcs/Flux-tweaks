# Butterfly Knife Sodium geometry fix — 0.3.1

The owner reported a large white box in place of the knife on Minecraft 1.21.11 with Sodium. The 0.3.0 isolated Sodium capture reproduces the same problem: [before](acceptance/031-sodium-before.png), [owner's view](acceptance/031-user-box.png), [owner's glint view](acceptance/031-user-box-glint.png).

## Cause and correction

The custom blade/handle/pivot surfaces were stored by replacing the polygons of a native `ModelPart.Cube` after construction. Sodium's optimized cube compiler caches cuboid positions and UVs from the constructor arguments. When the actual vertex writer supports Sodium intrinsics, it renders that cached box and never reads the edited polygons.

This behavior was verified against the exact [Sodium 0.8.14 source](https://raw.githubusercontent.com/CaffeineMC/sodium/mc1.21.11-0.8.14/common/src/main/java/net/caffeinemc/mods/sodium/mixin/features/render/entity/CubeMixin.java), its installed bytecode, and [Sodium 0.9.2 for 26.2](https://raw.githubusercontent.com/CaffeineMC/sodium/mc26.2-0.9.2/common/src/main/java/net/caffeinemc/mods/sodium/mixin/features/render/entity/CubeMixin.java).

Both native model adapters now use a private `MeshCube` subclass for custom surfaces. Its `compile` override emits the authored polygons with the original UVs, transformed normals, light, overlay and color. It does not call the constructor-cached cube compiler. Ordinary hardware cubes still use the normal optimized path. The model submission collector, glint, animation snapshots, Chams, shield mask and View Models transforms are unchanged. No Sodium dependency, global mixin override or user-setting change is added.

## Why the earlier checks missed it

The previous automated checks verified changing pixels, animation and finish differences, but a moving/colored box also satisfied those conditions. The earlier claim that the Sodium knife appearance had been visually verified was incorrect: the saved Sodium screenshot already contained the box. The 0.3.0 renderer-profile results must not be treated as evidence of the correct knife shape under Sodium.

## Regression evidence

`ButterflyGeometryAcceptance` now runs before GUI/world checks in both packaged test adapters. It renders through a real native entity-format `BufferBuilder`. With Sodium installed, it requires that the buffer implements Sodium's vertex writer and has intrinsics enabled, then compares the entire emitted vertex multiset against the authored mesh, with a 0.0001-unit position tolerance. It checks both idle and rotated-handle poses; counting vertices or changed pixels alone cannot pass this regression.

Before applying the fix, the packaged 1.21.11 Sodium run failed as intended:

```text
Butterfly geometry check starting: pose=ready, sodiumIntrinsics=true, expectedVertices=5888
AssertionError: Butterfly ready emitted unexpected vertex 0 ...
actual BufferBuilder output differs from authored polygons; sodiumIntrinsics=true
```

The exact local reproduction log is `work/butterfly-031-before-sodium.log`.

After the fix, both poses match all **5,888 vertices**. The entire packaged test matrix passes:

| Profile | Geometry and full client suite |
| --- | --- |
| 1.21.11 Fabric / Java 21 | Pass |
| 1.21.11 Sodium 0.8.14 | Pass, intrinsics verified active |
| 26.2 Fabric / Java 25 | Pass |
| 26.2 Sodium 0.9.2 | Pass, intrinsics verified active |
| 26.2 Sodium 0.9.2 + Iris 1.11.4, no shader pack | Pass, intrinsics verified active |

Both builds and all 57 existing logic cases per JDK pass. The game checks retain six finishes, three inspect styles, actual enable/inspect bindings, interruption, rendered size changes, both dominant hands/offhand, glint and the Chams/View Models/Shield combination. Clients ran sequentially using the pinned wrappers. Fixed logs are `work/butterfly-031-<profile>.log`; `butterfly-031-before-sodium.log` is the deliberately failing pre-fix run.

Actual corrected captures were visually checked: [1.21.11 Sodium Steel](acceptance/031-12111-sodium-steel.png), [Sodium glint](acceptance/031-12111-sodium-glint.png), [Sodium combined effects](acceptance/031-12111-sodium-combined.png), [26.2 Sodium Steel](acceptance/031-262-sodium-steel.png), [26.2 Sodium combined effects](acceptance/031-262-sodium-combined.png). The installed GUI toolkit dry-run still reports `changes: []`.

The 0.3.1 distribution is in `build/distributions/0.3.1`, with both target JARs, complete source, installation notes and SHA-256 sums. Replace the previous Flux Tweaks JAR in the matching profile; retain the existing config. No Sodium setting changes are required. The exact owner's full modpack/Lunar/shader-pack profile remains a separate gameplay check.

The source fix covers both maintained Minecraft targets. Existing sword gameplay, configurations and bindings remain compatible. Dedicated-server testing and EULA acceptance remain disabled.
