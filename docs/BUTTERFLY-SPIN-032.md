# Butterfly hand spin and Animation Speed - 0.3.2

**Historical 0.3.2 record:** the owner clarified that the hand was only an analogy for knife movement. Version 0.3.3 removes the added native hand/sleeve and retains the knife animation and speed control. See [the correction](BUTTERFLY-KNIFE-033.md).

The owner requested a knife that rolls around/in the hand like a pen or a CS:GO butterfly inspection, followed by an animation speed slider. This update changes motion and hand presentation on both existing Minecraft builds. The original mesh, six finishes, glint and sword gameplay remain in place.

## Motion and controls

Open **Right Shift > Butterfly Knife**. **Animation Speed** uses the existing slider and typed-value popup: 0.25-3x, in 0.05x steps, default 1x. Speed multiplies the progression of opening and inspection; 0.5x takes twice as long and 2x takes half as long. It is saved with the other settings. The speed is sampled when the animation begins, and opening settings cancels an inspect as before.

Spin uses anticipation, a continuous two-revolution roll, independent free-handle revolutions and a catch. Flourish adds safe-handle rollovers; Flip is a shorter wrist/handle rollover. The hand lifts inward and away from the camera during inspection to give the orbit room, then returns to the ready grip. Quintic easing gives continuous acceleration through each turn rather than stopping at intermediate rotation keys. Angles stay unwrapped until equivalent full-turn catch poses, preventing a backward jump through 360 degrees.

The pivot is a contact point on the safe handle. Its location follows the safe handle's actual hinge rotation, and the model is offset around that point after knife scale/rotation. Scale therefore enlarges the knife around the grip rather than pulling its handle away. Both handedness cases use the same anchor contract, without reversing face winding.

The actual native player arm renderer supplies skin, slim/wide hand shape and sleeve visibility. It is submitted in the existing hand pass, before item View Models transformations, with its own arm View Models transformations. The player wrist follows the small presentation turn while the knife rolls independently. Hand Chams captures both; shield-only submissions still exclude this hand/knife together. Player invisibility suppresses the extra arm. Missing knife resources preserve the vanilla fallback.

This is original CS:GO-inspired choreography using Minecraft's block hand; it does not add articulated fingers or copy Valve animation/model files. Deliberately different arm/item View Models offsets can separate the two, consistent with the existing independent controls. Large combined item scales can exceed the viewport.

Input repeats still do not queue inspections. Attack, use, inventory/screens, loss of focus, disable and existing world/camera lifecycle cancellation remain immediate. Reduced Motion keeps the handles still and uses a restrained presentation turn. A single monotonic frame sample is shared across Chams submissions; rendering never advances animation state.

## Source and compatibility

Shared motion/settings/logic checks live in the root source tree. The native renderer/hand integration is mirrored in the root 1.21.11 adapter and the separate 26.2 adapter. Real version-matched ItemInHandRenderer, AvatarRenderer and player-model sources were inspected, and Fabric's [1.21.11 render-state guidance](https://docs.fabricmc.net/1.21.11/develop/rendering/basic-concepts) was consulted through Context7. Pinned Java/game/Loader/API/Loom versions and the 26.2 Iris operation chain stay unchanged. No new dependencies, accounts, installations or permissions are needed.

The [0.3.1 Sodium mesh compiler correction](BUTTERFLY-SODIUM-031.md) is retained. Custom mesh polygons still use their own vertex writer; ordinary native arm cubes retain Sodium's optimized path.

## Verification

Final checks completed on 2026-10-25. Both builds pass with **59 logic cases on Java 21 and 59 on Java 25**. New checks compare normalized choreography across speed limits, require ongoing angular movement through intermediate turns, verify an equivalent catch pose without a wrap pop, validate/default/persist speed, and retain reduced-motion and cancellation coverage.

| Packaged client | Result |
| --- | --- |
| Minecraft 1.21.11 / Fabric / Java 21 | Pass |
| Minecraft 1.21.11 / Sodium 0.8.14 | Pass |
| Minecraft 26.2 / Fabric / Java 25 | Pass |
| Minecraft 26.2 / Sodium 0.9.2 | Pass |
| Minecraft 26.2 / Sodium 0.9.2 + Iris 1.11.4, no shader pack | Pass |

The actual-client geometry suite checks the held contact point throughout all styles, both hands and scale limits, using the production transform. Its 0.3.1 regression still matches all 5,888 authored vertices in idle and hinged poses; Sodium/Iris profiles require enabled vertex intrinsics. This prevents a return of the white-box bug.

Game checks click the actual Animation Speed slider, enter 0.25x through its numeric editor, and verify a 0.25x inspect remains active while a 3x inspect finishes within the same 25-tick observation window, through the same inspect binding. They capture a 36-frame slow-motion spin sequence, both dominant hands, offhand inspection, size extremes, glint and Chams/shield/View Models combinations. The scale image check isolates blue/cyan Flux metal so the fixed-size native hand cannot distort its area assertion. All previous gameplay/input/GUI/HUD regressions remain included, including unchanged server sword/enchantment/durability assertions.

Actual current-game captures were visually reviewed for contact with the skinned hand, continuous roll phases, return to the ready grip, readable controls, and the outline on both knife and arm with Chams. Examples: [1.21.11 grip](acceptance/032-12111-sodium-grip.png), [speed control](acceptance/032-12111-speed-slider.png), [Chams/View Models/glint](acceptance/032-12111-sodium-combined.png), [left hand](acceptance/032-12111-sodium-left.png), [0.25x slow-motion excerpt](acceptance/032-12111-sodium-orbit-slow.gif); [26.2 grip](acceptance/032-262-sodium-grip.png) and [left-hand combined effects](acceptance/032-262-sodium-combined-left.png). The GIF assembles actual game screenshots, resized for review, at approximately the captured slow-motion timing; it is an excerpt rather than a full-FPS recording. Captures from earlier releases are not 0.3.2 evidence.

Commands use the pinned wrappers: root `build runProductionClientGameTest -PsodiumAcceptance` and `runProductionClientGameTest`, with Java 21; the same under `versions/26.2` with Java 25, plus `runProductionClientGameTest -PirisAcceptance`. Clients ran sequentially. Logs are ignored local files `work/butterfly-032-<profile>.log`. The installed GUI toolkit dry-run reports `changes: []`.

Distribution: `build/distributions/0.3.2`, containing the two version-labelled JARs, complete source ZIP, installation notes and SHA-256 manifest. Packaging checks target metadata/bytecode, geometry classes, six texture hashes, native bridge/licenses, test-mod exclusion and source ZIP bytes.

These checks do not establish subjective approval in the owner's exact multiplayer/Lunar/shader-pack setup, sustained performance or smoothness at every FPS cap. Slim/wide arm alignment is derived from the pinned native model sources; every possible skin/third-party player renderer was not separately exercised. Dedicated-server testing and EULA acceptance remain disabled.

