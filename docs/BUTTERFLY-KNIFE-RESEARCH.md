# Butterfly Knife — research and proposed implementation

Research date: 2026-10-24. Status: research only; no feature implemented or runtime compatibility claimed. Three subagents investigated Counter-Strike references, the Flux integration, and tooling. Checkout inspected: root project on `main`, existing version 0.2.4; pre-existing untracked `bin/` was left untouched.

The user confirmed the visual direction: **a middle ground between the realistic metal reference and the Minecraft-style Actual Knives reference**. The implementation scope below is proposed, not an amendment to the approved feature plan.

Supplied references are preserved as [realistic reference](references/butterfly/realistic-reference.png) and [Minecraft reference](references/butterfly/minecraft-reference.png), for design review only, separate from runtime assets.

## Findings from the references

The supplied images show a curved, pointed blade, two curved dark handles, visible pivot hardware, bevels and real thickness. The second image's separated handles are central to the requested behavior. The model needs independently animated rigid parts; rotating one solid knife would not reproduce opening and inspection.

Valve introduced the Butterfly Knife with [Operation Breakout on July 1, 2014](https://blog.counter-strike.net/2014/07/9804/). Its visual identity combines a folding blade/handle mechanism with conspicuous draw and inspect motions.

Observed/reference behavior to reproduce in original animation:

- Equip: move from a folded or transitional pose through handle flips into a ready grip.
- Ready: open blade, handles together, restrained idle motion.
- Inspect: separate handle movement, rotation of the complete knife, grip repositioning and return to ready.
- Attack: a readable cosmetic sword swing that can interrupt inspection immediately. Counter-Strike's primary/secondary knife attacks are not a reason to change Minecraft's attack or use controls.

The [community CS.MONEY catalogue](https://wiki.cs.money/weapons/butterfly-knife) describes two draw and three inspect variants. These are community observations, not an official animation contract. No reliable official rare-animation probabilities were established. Its historical release attribution conflicts with Valve, so use Valve for that fact.

Current CS2 and historical CS:GO footage should not be mixed as exact timing references. Valve's [July 2025 update feed](https://store.steampowered.com/news/posts/?appids=730&enddate=1753835380&feed=steam_community_announcements) records replacement first-person animations; the [August updates](https://store.steampowered.com/news/posts/?appids=730&enddate=1758150954&feed=steam_community_announcements) include quick-inspect and Butterfly deploy adjustments. The proposed Flux motions are original CS-inspired sequences; exact CS:GO/CS2 reproduction would need a selected version and direct reference review.

The pictured [Actual Knives project](https://www.curseforge.com/minecraft-bedrock/addons/actual-knives-addon) is a **Bedrock add-on**, lists All Rights Reserved, supports inspection on some knives and 3D drops, and says skins are not yet available. It supplies visual inspiration, not a Java/Fabric integration or permission to reuse its assets. The first image also credits creators; it supplies no underlying asset license.

## Finish reference catalogue

| Reference family | Recognizable appearance |
| --- | --- |
| Vanilla / steel | Metallic blade and dark handles; clearest baseline for evaluating geometry |
| Fade | Warm-to-cool color gradient over a metallic base |
| Doppler | Colored marble-like fields; community phase and gem variants |
| Gamma Doppler | Green-oriented marble; community phase and Emerald variants |
| Case Hardened | Irregular heat-color patches with appearance dependent on placement |
| Marble Fade / Tiger Tooth | Multicolor marble / gold-orange with dark stripes |
| Crimson Web / Slaughter | Red with dark web-like markings / red patterned metal |
| Damascus Steel / Rust Coat | Layered steel pattern / aged rusty surface |
| Ultraviolet / Black Laminate | Restrained dark-purple / black-gray finishes |
| Lore / Autotronic | Ornate gold / red and steel accents |

These are reference categories from the [community catalogue](https://wiki.cs.money/weapons/butterfly-knife), not a commitment to copy or ship every CS finish. Valve's own item descriptions illustrate [Fade](https://steamcommunity.com/market/listings/730/G18830420263003), [Doppler](https://steamcommunity.com/market/listings/730/G188304209F033003), [Gamma Doppler](https://steamcommunity.com/market/listings/730/G18830420B8043003) and [Case Hardened](https://steamcommunity.com/market/listings/730/G188304202C3003).

Keep three concepts separate: **finish** selects the artwork/material family; **pattern** changes placement or distribution; **wear** changes scratches and exposed substrate. Valve documents pattern transforms, wear and material rendering in its [finish guide](https://www.counter-strike.net/workshop/workshopfinishes/). Wear need not track Minecraft durability. No trading, cases, paid unlocks or market pricing is needed for Flux.

Suggested first set of original artwork: Steel, Flux purple/cyan, Sunset gradient, Violet marble, Emerald marble, and Obsidian/red. Names and selection remain proposals. Begin with one steel texture to judge the shape; add the remaining finishes after the motion works. Baseline Minecraft lighting and painted highlights will not reproduce CS2's material response exactly; shader-specific reflective materials are a separate extension.

## Proposed feature contract

| Area | Proposed behavior |
| --- | --- |
| Model | Original low-poly mesh, beveled curved blade, two separately pivoted handles, hinge details; optional latch |
| Style | User-confirmed middle ground: recognizable curves and metal detail, restrained texture resolution and geometry that fit Minecraft |
| Toggle | New Butterfly Knife module; disabled initially; disabling immediately restores normal sword presentation |
| Keys | Enable key, one-shot Inspect key, optional Next Finish key; rebindable keyboard/mouse inputs, initially unbound |
| Inspect options | Short Flip, Spin, Flourish; explicit selection or Random; deliberate repeated-press policy without an unbounded queue |
| Equip | One polished opening sequence initially; second variation only after the first passes visual review |
| Combat | Preserve item identity, damage, reach, cooldown, durability, enchantments, attacks, mining and off-hand use |
| Settings | Existing FluxScreen controls, theme, scale, focus, search and persistence; finish/style choices with clear selected state |
| Persistence | Save enabled/finish/style/bindings; never save an active animation; migrate old settings without resetting other modules |
| Motion | Shared elapsed-time animation, one stable pose per frame across rendering passes; GUI respects Reduced Motion |
| Optional sound | Original/licensed mechanical clicks, local only, volume/mute; off until selected as scope |

Recommended initial visibility is the local first-person view, in either logical hand and with either handedness setting. Inventory, dropped items, third-person self, other entities and remote viewers are distinct decisions. A client-only visual replacement does not make unmodified remote clients see the knife.

Proposed eligibility is vanilla swords, with the same chosen cosmetic finish across sword materials. Confirm whether selected materials, modded swords or items with custom resource-pack models must participate. Do not mutate an ItemStack or send custom animation packets for a cosmetic view.

Animation priorities: actual attack/use wins over inspection; slot/item changes and disable cancel it. Screen opening, focus loss, death, disconnect and dimension change clean up transient state. Suppress inspect while Freecam blocks interaction. Switching finishes should not retrigger equip accidentally. Decide whether repeated Inspect restarts the current sequence or starts another after completion; recommended initial behavior is one sequence per fresh press with no queued backlog.

The Minecraft hand is block-shaped and has no normal articulated fingers. Keeping the player's skin and a believable grip/arm pose is feasible design scope; exact CS finger choreography would require a separate custom hand rig. Recommended first version retains the Minecraft hand. Global Reduced Motion should suppress automatic flourish; whether an explicit Inspect press still performs the requested full motion is a product decision.

## Integration evidence and recommended approach

The root and `versions/26.2/AGENTS.md`, README, PROJECT_PLAN, CHECKLIST and module/Iris/GUI documents were inspected. Maintain the two authoritative builds:

| Target | Toolchain and runtime pins |
| --- | --- |
| Root 1.21.11 | Java 21, Gradle 9.2.1, Loom 1.15.5, Loader 0.19.5, Fabric API 0.141.6+1.21.11, Mojang mappings; optional Sodium 0.8.14 |
| `versions/26.2` | Java 25, Gradle 9.7.1, Loom 1.18.4, Loader 0.19.5, Fabric API 0.161.0+26.2, unobfuscated APIs; optional Sodium 0.9.2 |

Prefer shared model/texture/keyframe data and a compact shared animation state machine, with small native rendering adapters for each target. This follows the existing no-new-runtime-dependency direction. It needs an in-game prototype before compatibility can be claimed.

Concrete project integration points:

- `src/main/java/dev/fluxtweaks/config/Feature.java` and the existing setting/configuration catalogue: register module and persistent choices. `hasAction()` currently describes cameras and Zoom; it is not a general inspection dispatcher.
- Both `src/client/java/dev/fluxtweaks/FluxTweaks.java` adapters: input registration, enable state, lifecycle and one-shot inspect dispatch. Extend deliberately rather than routing inspect into camera/Zoom activation logic.
- Both `gui/FluxScreen.java` adapters and `gui/ModuleIcons.java`: existing settings and bindings UI, module icon and notification behavior. Enable/disable notifications can use existing conventions; inspecting should not flood the notification HUD.
- Both `mixin/ViewModelMixin.java` adapters and `feature/ViewModels.java`: preserve user item/arm transforms and logical main/off-hand mapping. Avoid cancelling between existing pose push/pop hooks.
- Both `mixin/ItemModelResolverMixin.java` adapters already touch resolved item render state for shield tint. Scope a replacement narrowly to an eligible local sword and chosen display contexts.
- Both `render/HandChams.java` adapters and the 26.2 `mixin/GameRendererMixin.java`: preserve capture, submission and the original operation chain.

Cached version-specific Minecraft sources were inspected under `work/sources/` and `work/sources26/`. Both render-state implementations expose `LayerRenderState.setupSpecialModel(renderer, argument)`, making a native special-model path worth prototyping. The special renderer's submission signature differs between versions; context/hand data must be carried explicitly where the newer signature omits it. This is a candidate integration point, not completed code.

**Critical:** 26.2 previously had an Iris startup crash from an exclusive hand-render redirect. Its current `WrapOperation` chain must remain intact. Chams may render the same hand more than once; do not advance animation, randomly select an inspect, or trigger sound from a render callback. Sample a stable pose and submit immutable state so deferred passes, both hands and model reuse cannot overwrite each other's poses. Preserve shield-only mask behavior. See [the existing incident and fix](LUNAR-COMPATIBILITY.md).

Context7 was consulted for Fabric 1.21.11 rendering and current special-renderer guidance. Its 1.21.11 results were broad extraction/submission guidance, not an exact animated-sword recipe. The [official custom shield example](https://docs.fabricmc.net/develop/items/custom-shields) explains a special-renderer approach; the actual pinned sources remain authoritative for signatures. Do not assume a new custom Item example proves replacement of vanilla swords.

Load/bake geometry and textures on resource loading, with reload/disposal support; avoid parsing files or uploading textures per frame. Missing/invalid knife assets must fall back to normal sword rendering. Preserve glint, light and original item components. Resource-pack precedence needs an explicit policy, especially for servers that use custom item models. Add the module's translated labels in both adapters' applicable language resources.

## Compatibility checklist for implementation

| Existing feature/system | Required check |
| --- | --- |
| Hand Chams | Correct outline/fill for every animated part; matching color/mask pose; no doubled sound or animation speed |
| View Models | Main/off item and arm transforms apply once; normal and extreme settings, left-handed player |
| Shield Status | Off-hand blocking/tint/outline unchanged; knife does not enter shield-only mask |
| Freecam / Freelook | Respect hand visibility and interaction suppression; no stale animation on return |
| Zoom | Existing hand-FOV behavior and scroll handling preserved; key collisions handled by existing conventions |
| Hit Particles / Hit Color | Actual hit events and entity effects preserved; knife does not generate fake hits or pick up unintended entity tint |
| Fullbright / Ambience | Correct lighting, fog and reload behavior; no changes to world/time/weather mechanics |
| Low Fire | Existing overlay position and depth behavior remain intact |
| Jump Circle | No change to movement/takeoff/event handling |
| Spotify HUD / Notifications | No key capture, HUD placement or media lifecycle regressions; tests use existing media fixtures |
| GUI and config | Existing defaults/migration, binding edit/clear, search, scroll, theme, GUI scale and Reduced Motion |
| Item mechanics | Shields, bows, food, maps, empty hand, glint and unrelated items remain normal; resources restore when disabled |
| Renderer profiles | Both Fabric builds, both pinned Sodium versions, plus 26.2 Sodium/Iris 1.11.4; actual shader packs and Lunar separately |

No claim of compatibility with every third-party mod follows from this research. The goal is regression coverage of Flux's fourteen existing modules and the named renderer profiles.

## Tools, skills and asset pipeline

No installation was necessary. Inventory checks found:

| Tool | Verified local location / evidence |
| --- | --- |
| Blockbench 5.2.2 | `%LOCALAPPDATA%/Programs/Blockbench/Blockbench.exe`; file version 5.2.2.0, application not launched |
| GIMP 2.10.38 | `%LOCALAPPDATA%/Programs/GIMP 2/bin/gimp-2.10.exe`; file metadata |
| Java 21 compiler | `%USERPROFILE%/.vscode/extensions/redhat.java-1.56.0-win32-x64/jre/21.0.12.1-win32-x86_64/bin/javac.exe`; executed, reports 21.0.12.1 |
| Java 25 compiler | `C:/Program Files/Java/jdk-25.0.4/bin/javac.exe`; executed, reports 25.0.4 |
| Git / supporting tools | Git 2.55.0.windows.5; Node, Python and winget commands available |

The Temurin 21 registry location under Program Files is stale. Use the verified VS Code bundled JDK for root checks, setting JAVA_HOME only for the build process. Recheck that path after VS Code extension updates. Both project Gradle wrappers already exist; no global Java or PATH changes were made.

[Blockbench](https://blockbench.net/) provides modeling, UV painting and animation. Its [format comparison](https://blockbench.net/wiki/blockbench/formats/) is important: vanilla Java Block/Item JSON is not a general skeletal animation export. Proposed pipeline: original Generic Model with separate part pivots -> editable `.bbmodel`, PNG textures and authored keyframes -> a small, explicitly defined project-owned conversion/export -> native runtime model and animation data. The converter does not exist yet. An optional [glTF export](https://blockbench.net/wiki/guides/model-rendering/) supports independent review, not automatic Minecraft rendering.

Blockbench is already sufficient for this level of modeling. Blender is optional for more complex mesh work/baking and was not installed. No need to install CS2, its Workshop tools, an asset extractor, a paid model service or another GUI framework.

[GeckoLib](https://github.com/bernie-g/geckolib) is an alternative keyframe-animation library, but its custom-item examples do not establish transparent vanilla sword replacement or Chams compatibility. Fabric artifacts for both target versions were found; wiki and live version listings differed. A dependency decision would require exact artifact verification and an integration prototype, rather than picking a universal latest. No GeckoLib runtime or Blockbench plugin was installed; the native approach is preferred for this small rigid model and the project's existing dependency constraint.

Skills used: Ponytail for implementation scope/dependency decisions, the installed Minecraft GUI skill for future controls/motion and Context7 for documentation. No additional skill is needed, and the vendored toolkit remains untouched. The toolkit's 26.1.2 ModernUI showcase is not a runtime dependency for either build.

## Permissions and remaining product decisions

The user already authorized necessary installations; none was needed. The proposed local workflow needs no paid account, API key, cloud upload or server component. No account settings, launcher profile, Spotify playback or dedicated-server EULA were changed. No external messages were sent.

Author original model, textures, motion and sounds, or obtain explicit asset permissions before incorporating third-party files. The attached pictures are references, not a grant to redistribute the pictured models. Keep any future license/attribution records with the assets; [Blockbench's FAQ](https://blockbench.net/wiki/blockbench/faq/) confirms that work created in the editor remains the creator's artwork.

Before full implementation, settle these product choices; they do not block this research:

1. Local first-person only initially, or also inventory/dropped/third-person views? Recommended first milestone: local first-person.
2. All vanilla swords, selected sword materials, or modded swords/custom pack models too? Recommended starting scope: vanilla swords.
3. Initial finish shortlist and whether pattern/wear controls are wanted now. Recommended: six original finishes, no seed/wear controls initially.
4. Original mechanical sound effects, and vanilla Minecraft hand versus custom fingers. Recommended: vanilla hand; sound optional and muted by default.
5. Explicit-inspect behavior with Reduced Motion, and whether Next Finish needs its own binding immediately.

The user has selected the art direction. No remaining installation approval is needed. This turn deliberately stops at research because the user requested research for now.

## Suggested next milestone and evidence required

First milestone: one original steel knife and one inspect sequence, represented as editable assets and exercised through both native renderers with View Models + Hand Chams. Review actual in-game captures before expanding finishes and animation variants. Then complete controls, migration, cleanup, sound if selected, and regression checks.

Focused automated checks should cover animation endpoints/interruption, one event per fresh press, independent hands, same-frame multi-pass invariance, invalid setting recovery and old-config migration. Use the existing wrappers with Java 21 at root and Java 25 under `versions/26.2`: `build`, `runProductionClientGameTest`, and `runProductionClientGameTest -PsodiumAcceptance`; add the existing 26.2 `-PirisAcceptance` profile. Run game clients sequentially. Test resource reload, rapid swaps, camera transitions, focus loss, 30/60/144 FPS caps and repeated inspection; measure frame time before making performance claims.

No build or game tests were run for this documentation-only research. No model was authored, no animation was previewed, and no renderer implementation was tested. Existing automated acceptance and older user-reported multiplayer confirmation do not verify this new feature. Real Lunar/shader-pack rendering and multiplayer acceptance remain separate final checks; dedicated-server testing/EULA acceptance remains disabled.
