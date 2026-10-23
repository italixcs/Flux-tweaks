# Flux Tweaks 0.2.4: module icons, action controls and notification HUD

## Approved screen contract

Add smooth outline icons for all fourteen modules, a visible Disabled/Ready/Active state, and separate Enable and Action controls for Freecam, Freelook and Zoom. Continuous effects keep one Enable key. Both supported Minecraft adapters retain their native renderer and 26.2 retains the Iris operation-chain fix.

Enable changes saved availability; Action changes temporary runtime activity. Disabled camera modules ignore Action. Disabling an active module immediately stops its action and restores the camera. Enabling a camera does not activate it. Cameras remain mutually exclusive while availability can stay enabled for both. Active states are never serialized, reset at launch/world change/death, and require a fresh press after input cancellation. Toggle cameras retain their view while editing, with movement paused; hold actions cancel and require a fresh press. Freelook and Freecam offer Hold/Toggle; Zoom remains Hold. Existing binding identifiers are retained as Action keys for these three modules; new Enable binds are unbound. Other modules keep their original binding identifiers as Enable keys.

Notifications are compact, silent theme cards with module icon/name, explicit Enabled/Disabled/Activated/Deactivated wording, a lifetime indicator, and bounded stacking (three cards, 2.5 seconds). Defaults were offered as optional preferences; proceed with these stated defaults unless the owner steers otherwise. Only actual changes produce events; startup/config loading and repeated unchanged states do not spam cards. World changes clear transient cards. Use monotonic elapsed time, the installed toolkit easing, 220/160 ms arrival/departure and Reduced Motion.

HUD Editor shows independent Spotify and Notifications previews. Select by clicking the preview or labeled selector; drag, arrow nudge and wheel scale the selected element. Reset affects the selected element; layout recovery resets both positions. Save notification placement/scale in schema-1 JSON. Keep preview, real hit bounds and constrained on-screen placement consistent across resolutions. Preview remains available even with no real events or Spotify session.

Reuse charcoal rounded surfaces, primary/secondary colors, Lato metrics and shared vector primitives. Feature list names remain visible; icons do not replace text. Distinguish states using words, not only color. New controls retain search, clipping, keyboard binding assignment/clearing and saved Minecraft Controls integration.

## Acceptance

Focused configuration/state/notification lifetime tests; real GUI assignment of Enable/Action keys; ignored actions while disabled; enable-without-activation; toggle/hold and cancellation; mutual exclusion without losing availability; Zoom separation; exact notification transitions and capped/expired queue; independent HUD drag/scale/reset/persistence; icon and notification captures at multiple GUI scales. Package both versions after checks on Fabric and Sodium, plus 26.2 Iris with no shader pack. Actual Lunar and shader-pack-specific acceptance remain separate.

## Implementation and migration

- Common configuration, action gate, notification queue and vector-icon definitions use the existing feature registry and schema 1. No renderer dependency was added.
- `key.fluxtweaks.freecam`, `.freelook` and `.zoom` remain their existing Action binding identifiers, so Minecraft options retain assigned keys. New `key.fluxtweaks.enable.<id>` bindings start unbound. Continuous modules keep their original binding IDs.
- Camera availability persists, replacing the old load-time forced-disable behavior. `CameraModes.current` and Zoom's hold state are transient. An older configuration can load a camera as Ready, but cannot restore a detached/rotated camera.
- Bind/mode changes cancel the old action, held-key gates prevent unintended restarts, and deactivating one camera never clears the other's saved availability.
- Notification placement uses normalized coordinates with on-screen bounds; notification scale is 50–200%, independent of Theme GUI Scale and Spotify scale. Reset/Home in HUD Editor affects the selected element. Theme Reset Layout restores both HUD positions.
- Theme → Module Notifications hides events and clears the current queue. Editor previews remain available when notifications are disabled.

## Verification record — 2026-10-23

52 JUnit cases pass on Java 21 and Java 25: configuration/migration, saved availability/notification placement, input gating, queue capacity/lifetime/clear, exact easing and Reduced Motion, plus the existing settings/Zoom/layout/motion checks.

The production client suite covers real GUI Enable/Action assignment, disabled-action rejection, Ready without camera activation, toggle and hold operation, fresh-press behavior after editing, exact disable/deactivate event order, unchanged-state deduplication, expiry, Zoom action events, independent HUD drag/scale/nudge/reset and persisted placement. Existing suites retain input/search/layout/scaling, camera movement/perspective/mutual exclusion/world exit, effects, first-person rendering and Spotify fixtures. Updated old camera checks explicitly arm and activate instead of assuming Enabled starts the action.

| Packaged profile | Result |
| --- | --- |
| 1.21.11 Fabric / Java 21 | Pass |
| 1.21.11 + Sodium 0.8.14 | Pass |
| 26.2 Fabric / Java 25 | Pass |
| 26.2 + Sodium 0.9.2 | Pass |
| 26.2 + Sodium 0.9.2 + Iris 1.11.4, no shader pack | Pass |

Commands use the existing wrappers: root `build runProductionClientGameTest`, then `runProductionClientGameTest -PsodiumAcceptance`; the same under `versions/26.2`, plus `-PirisAcceptance`. Tests run sequentially in isolated production-profile directories. Logs are ignored files under `work/024-*.log`; screenshots below are copied from the actual client runs.

Captures: [1.21.11 keys](acceptance/024-12111-keys.png), [notifications](acceptance/024-12111-notifications.png), [HUD Editor](acceptance/024-12111-editor.png); [26.2 keys](acceptance/024-262-keys.png), [notifications](acceptance/024-262-notifications.png), [HUD Editor](acceptance/024-262-editor.png).

The first Fabric run exposed an old test activating directly during a binding/mode change; the pending binding change correctly cancelled it. The test now waits for input configuration to settle before explicitly starting the camera. The existing assertions remain in place. Fresh final captures were visually reviewed for text/icon alignment, card clipping and independent HUD selection.

No Spotify playback commands or dedicated server were needed. Owner multiplayer confirmation covers the previous release; these additions still need approval in the owner's usual server/Lunar profile. Shader-pack-specific rendering, sustained GPU measurements and subjective motion approval remain separate from the passing packaged checks. The installed toolkit dry-run reports `changes: []`.

## Release inspection

Both 0.2.4 JARs have matching Fabric/Minecraft/Java metadata (class versions 65 and 69), bundled Windows x64 media bridge and licenses, new icons/HUD classes, and no game-test classes. The complete source ZIP contains both adapters, wrappers, native source, installed toolkit, documentation and captures; every entry is verified against the current project bytes. SHA-256 sums accompany the two JARs, source ZIP and installation notes. The toolkit and project instructions match the preceding 0.2.3 source snapshot.
