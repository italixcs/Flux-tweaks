# Lunar Client / Flux Tweaks 26.2 compatibility investigation

Investigated 2026-10-23 using Context7, official Lunar guides, local crash reports, and the exact Iris version installed in the owner's Lunar profile.

## Relevant documentation

Context7 resolved Lunar Client to `/websites/lunarclient_dev`. Its indexed material mainly covers Apollo server integrations, server mappings and client-brand data; it did not return the launcher's custom Fabric mod guides. Official documentation was therefore fetched directly:

- [Adding your own mods](https://www.lunarclient.com/news/how-to-add-your-own-mods-to-lunar-client): Fabric add-on, version selection, Mods tab/drag-drop, enable/disable/uninstall, modpack loader settings, and limitations.
- [Troubleshooting mod loading](https://www.lunarclient.com/news/troubleshooting-mod-loading-guide): version/dependency conflicts, Show Conflicts, unexpected launch failures and support-log diagnosis.
- [Modding / Vanilla add-on](https://www.lunarclient.com/news/how-lunar-client-simplifies-minecraft-modding): bundled Fabric mods and the optional Vanilla add-on for bypassing Lunar-specific features.
- [FAQ](https://www.lunarclient.com/faq): external Fabric-mod support on modern versions and support contact.

The guides describe modern Fabric mod loading, not a guarantee that every external mod coexists with Lunar's own hooks. They list bundled mods including Iris, Sodium and Fabric API. Use the launcher's version-specific Mods tab and install the JAR matching Minecraft, Java and loader requirements. Do not install the 1.21.11 Flux JAR in a 26.2 profile.

## Exact local evidence

The owner's disabled JAR is `fluxtweaks-0.2.1-mc26.2.jar.disabled`. Their successful launch after disabling it uses Minecraft 26.2 with Iris 1.11.4+mc26.2, Sodium 0.9.2+mc26.2 and Fabric API 0.161.0+26.2. Their crash reports at 2026-10-23 11:48:20 and 11:49:04 identify Minecraft 26.2, Java 25.0.1, Loader 0.19.5, Flux 0.2.1 and Iris 1.11.4.

The fatal error is in `GameRenderer` transformation:

```text
Critical injection failure: Redirector iris$disableVanillaHandRendering
mixins.iris.json:MixinGameRenderer from mod iris
(0/1) succeeded. Scanned 0 target(s).
```

Flux and Iris both used `@Redirect` on `ItemInHandRenderer.submitHandsWithItems(...)` inside `GameRenderer.renderItemInHand`. Flux replaces that instruction first, so Iris cannot find its required target. Mixins apply at startup, regardless of whether the Hand Chams setting is enabled.

Launcher history also reports an earlier Loader 0.19.3 mismatch (Flux requires >=0.19.5), plus an older attempt to use the 1.21.11 JAR on 26.2. Those are separate from the final runtime crash, whose own mod list already shows Loader 0.19.5 and the correct 26.2 JAR. The Java 25 runtime is also correct.

## Reproduction and targeted patch

An isolated ordinary Fabric 26.2 client with Flux 0.2.2, Sodium 0.9.2 and Iris 1.11.4 reproduces the same failure. Its log explicitly says Iris's redirect was skipped because Flux already redirected that exact instruction. Thus this failure is reproducible without Lunar itself.

Context7's `/llamalad7/mixinextras` documentation recommends [WrapOperation](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation) for wrapping calls that must compose with other mods. The 26.2 patch changes Flux's redirect to a wrapper and passes the original operation through the Hand Chams queue and shield-mask pass. Disabled Chams calls the original operation directly; active Chams preserves the operation chain. This avoids bypassing Iris's rendering decision.

Only the 26.2 adapter is patched, released as 0.2.3; the 1.21.11 build remains 0.2.2. MixinExtras is already provided by the existing loader stack. Iris is a test-profile dependency only, not a newly bundled requirement for Flux.

Reproduce/check with Java 25:

```powershell
versions\26.2\gradlew.bat -p versions\26.2 build runProductionClientGameTest -PirisAcceptance --console plain
```

The `irisAcceptance` profile uses Iris Modrinth version `gxZWWnKH` (1.11.4+26.2-fabric) plus Sodium 0.9.2, in its own `build/run/productionIris` directory. Baseline failure log: ignored `work/lunar-iris-before.log`. Patched packaged checks pass on Java 25 for all three 26.2 profiles: Fabric alone, Fabric + Sodium 0.9.2, and Fabric + Sodium 0.9.2 + Iris 1.11.4 with no shader pack selected. All 43 logic cases pass. Full client checks include GUI/input/persistence, cameras, particles, overlays, Spotify fixtures, normal hands and Hand Chams + Shield Status pixel comparisons. The Iris Maven test JAR is SHA-256-identical to the JAR installed in the owner's Lunar profile.

Logs: ignored `work/lunar-iris-after.log`, `work/lunar023-fabric.log`, `work/lunar023-sodium.log`. Captures: [Iris hand outlines/shield](acceptance/023-iris-hands.png) and [Iris GUI](acceptance/023-iris-gui.png). These are ordinary Fabric client captures with Iris, not a Lunar launcher test.

The final JAR is inspected for matching version/Minecraft/Java metadata, the existing native bridge and licenses, and absence of Iris/test classes. The complete source archive is checked byte-for-byte against the project files. Prior releases are retained.

## Scope and remaining checks

No Lunar profile, cache, account, mod enable state or launcher settings were modified. No support message or upload was sent. Local private launcher/authentication data is excluded from project artifacts. This report is based on the exact crash, not a blanket claim about Lunar compatibility.

The owner must confirm the patched JAR in the real Lunar profile. Shader-pack rendering (especially shader-owned hand passes), Vulkan and every Lunar feature combination are outside the isolated no-shader-pack check. Retain existing multiplayer and GPU/performance limitations. Dedicated-server testing/EULA acceptance remains disabled.
