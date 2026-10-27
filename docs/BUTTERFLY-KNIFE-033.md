# Knife-only butterfly spin and playback modes - 0.3.3

The owner clarified that spinning around the hand was an analogy for the knife's movement. The extra visible hand/sleeve and its movement are removed on both Minecraft targets. The native arm-rendering hook for ordinary empty hands/maps remains unchanged.

The continuous knife spin, handle flips, catch, elapsed-time timing and 0.25-3x Animation Speed slider are unchanged. Knife Scale, finishes, glint, View Models item adjustments, Hand Chams capture and the Sodium mesh correction remain. Configurations and keybinds carry over.

Implementation deletes the extra `submitHand` helper and its call/imports from the existing item View Models hook. No new renderer hooks or dependencies are introduced. The prior 0.3.2 native-hand captures are historical and do not represent 0.3.3.

## Inspect Mode

Open **Right Shift > Butterfly Knife > Inspect Mode**:

- **Press** plays a single inspect when the key is pressed. This is the default and preserves existing configurations.
- **Hold** repeats complete cycles while the Inspect Key is held; release stops immediately.
- **Toggle** starts repeating on a press and continues after release; the next fresh press stops it. Keyboard auto-repeat does not toggle it repeatedly.

Each cycle uses the selected style and speed; Random selects a style once per new cycle. Attack, use, screens, loss of focus, swapping the inspected sword, disable, world/camera changes and the existing reduced-motion preference cancellation stop active loops. Releasing and pressing again is required after interrupting a held key. Closing a menu does not resume a stopped Toggle loop. Enabling availability is still a separate binding. The chosen mode persists; running loops are transient and are not saved.

Input reuses the existing ActionGate policy with a shared ButterflyPlayback state for both targets. Key/binding/mode changes block stale input. A completed cycle can restart only from a normal client input tick; render passes still use the same immutable frame sample. The native arm stays removed.

## Verification

Both builds pass with **64 logic checks per JDK**. The new input-policy cases cover one-shot presses, held-key repeat suppression, Hold release, Toggle release/second-press behavior, interruption and fresh-key restart. Mode defaults, invalid-value handling and persistence are covered by the existing settings checks.

Packaged client checks passed on **Minecraft 1.21.11 / Java 21 / Sodium 0.8.14** and **Minecraft 26.2 / Java 25 / Sodium 0.9.2**. Both full suites use the actual Inspect Key to run Hold and Toggle across multiple completed cycles, stop on release/second press, interrupt a held key with attack, and cancel Toggle on opening a screen without resuming afterward. The actual mode choice is operated through the settings UI. Existing speed, all styles/finishes, size, glint, Chams/View Models/shield mask, handedness/offhand and unchanged server-sword checks remain included. The native geometry suite still matches all 5,888 vertices with Sodium intrinsics active and checks the spin pivot across styles/handedness/scale.

Current captures were visually checked: [knife only](acceptance/033-12111-knife-only.png), [spin without an attached hand](acceptance/033-12111-spin-only.png), [Inspect Mode selector](acceptance/033-12111-inspect-mode.png), [Toggle playback](acceptance/033-12111-toggle-loop.png), and [26.2 knife only](acceptance/033-262-knife-only.png). These replace the native-hand presentation shown in historical 0.3.2 captures.

Packaging verifies that the extra `submitHand` method/call is absent from both release JARs and that the pure ButterflyMotion/Pose bytecode is identical to 0.3.2. Metadata/Java bytecode targets, six textures, native bridge/licenses, test-mod exclusion, source archive contents and checksums are also checked. Output is `build/distributions/0.3.3`.

Logs: ignored `work/butterfly-033-12111-sodium.log` and `work/butterfly-033-262-sodium.log`. Commands use each pinned wrapper with `build runProductionClientGameTest -PsodiumAcceptance --console plain`; Java 21 at the root and Java 25 under `versions/26.2`. The clients run sequentially. The existing toolkit dry-run remains unchanged.

Fabric-alone and 26.2 Iris profiles last passed on 0.3.2 and were not rerun for this update. No new rendering hook was introduced; the only hand-rendering change removes the extra arm. The owner's normal multiplayer/Lunar/shader setup and sustained performance are not confirmed by these checks. Dedicated-server testing and EULA acceptance remain disabled.

