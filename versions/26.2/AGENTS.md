# Flux Tweaks for Minecraft 26.2

This is the second version explicitly requested by the user. Follow the root AGENTS.md and [minecraft-gui skill](../../.agents/skills/minecraft-gui/SKILL.md) for all GUI work, plus Ponytail and Context7. Its target is **26.2**, not the toolkit showcase's 26.1.2.

- Use Java 25, this directory's Gradle 9.7.1 wrapper, Loom 1.18.4, Loader 0.19.5 and Fabric API 0.161.0+26.2. Minecraft is unobfuscated; do not add remapping or Mojang mappings dependencies.
- Shared configuration, animation core, logic tests, assets, licensed fonts and notices live in the root source tree. The client adapters and gameplay checks here use the actual 26.2 APIs. Keep mirrored screen behavior aligned with the root GUI without changing the installed toolkit payload.
- Native Spotify bridge builds from the shared `../../native` source. No credentials or server component are required.
- Run `gradlew.bat build` and `gradlew.bat runProductionClientGameTest`. The latter supports `-PsodiumAcceptance` for pinned Sodium 0.9.2. Keep these test runs sequential with other clients to avoid window-focus interference. Tests are an isolated mod and must not enter the release JAR.
- Dedicated-server testing/EULA acceptance remain disabled. Report the root build's evidence and this build's evidence separately; user multiplayer confirmation for older releases does not verify this port.
