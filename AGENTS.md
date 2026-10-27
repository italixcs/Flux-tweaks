<!-- minecraft-gui:begin -->
## Minecraft GUI toolkit

For Minecraft GUI work, use the [minecraft-gui skill](.agents/skills/minecraft-gui/SKILL.md). Reuse this project's shared theme and components, follow its design and motion rules, and verify against the project's actual Minecraft stack. Explicit user instructions and the project's existing rules still apply.
<!-- minecraft-gui:end -->

## Flux Tweaks project rules

- Use `minecraft-gui` for all Minecraft GUI work: menus, HUDs, controls, typography, themes, motion, implementation and reviews. Read its relevant workflow references and use the actual version-matched Minecraft source before changing rendering hooks.
- This project's build is authoritative: Minecraft Java **1.21.11**, Fabric Loader **0.19.3**, Fabric API **0.141.6+1.21.11**, official Mojang mappings, Java **21**, Loom **1.15.5**, Gradle **9.2.1**. Retain Fabric-alone and Sodium **0.8.14** compatibility. Do not upgrade these to match bundled examples.
- Reuse the established native UI in `src/client/java/dev/fluxtweaks/gui/`: `Ui`, `SmoothGui`, `FluxScreen`, `HudEditorScreen` and the current Lato font resources, theme and settings APIs. Existing charcoal/purple/cyan styling and user customization remain authoritative over toolkit example tokens. Preserve the current gameplay, controls and configuration behavior.
- The installed toolkit's ModernUI adapter/showcase targets **26.1.2 / Java 25**. It is a complete reference snapshot, not a verified 1.21.11 runtime dependency. Keep the installed payload unchanged. Put project adaptations in project-owned files and prove compatibility before adopting a different renderer; do not import the starter over this project's build/source.
- Use Ponytail for coding and Context7 for version-specific documentation. Ask the user before acting on missing requirements that cannot be resolved by inspecting this project. Preserve existing guidance in `PROJECT_PLAN.md`, `CHECKLIST.md`, `README.md` and `docs/`.
- When checks are requested or required by an approved task, use Java 21 and this project's wrapper. Relevant commands: `.\gradlew.bat build --console plain`, `.\gradlew.bat runProductionClientGameTest --console plain`, and `.\gradlew.bat runProductionClientGameTest -PsodiumAcceptance --console plain`. Keep automated evidence distinct from user-reported multiplayer confirmation. Dedicated-server EULA acceptance remains disabled.
- Toolkit provenance, installation checks and compatibility limits are recorded in `docs/GUI-TOOLKIT.md`; `gui-toolkit.lock.json` owns the vendored file identities. Updates must preserve project rules and reject local payload conflicts rather than overwrite them.

## User-authorized second target (2026-10-22)

The user explicitly requested the GUI redesign for **1.21.11 and 26.2** (confirmed 26.2, not 26.1.2). The version pins above govern the root 1.21.11 build. Maintain the separate Java 25 / Minecraft 26.2 build under `versions/26.2`, following its scoped instructions. Preserve both targets and keep configuration and GUI behavior aligned; do not replace either build with the toolkit's 26.1.2 showcase.

## User-authorized Loader compatibility correction (2026-10-25)

The owner supplied a 1.21.11 startup error showing their profile uses Fabric Loader 0.19.3 and asked for a matching build. The root dependency and minimum Loader declaration are now 0.19.3; all other root pins remain unchanged. The separate 26.2 target retains its scoped Loader 0.19.5 pin. Verify root packaged-client runs actually use 0.19.3 before claiming runtime compatibility.
