# Minecraft 1.21.11 / Fabric Loader 0.19.3 - Flux 0.3.4

The owner's startup dialog identifies a dependency mismatch: the launcher has Fabric Loader 0.19.3, while Flux Tweaks 0.3.3 requires >=0.19.5. The requested 1.21.11 compatibility patch lowers the root compile/runtime Loader dependency and `fabric.mod.json` minimum to 0.19.3. It keeps Minecraft 1.21.11, Java 21, Fabric API 0.141.6+1.21.11, Sodium 0.8.14 and the existing build tools.

All knife/model/input/settings code is retained, including the Sodium geometry correction, removed extra arm, Animation Speed and Press/Hold/Toggle. The 26.2 target remains 0.3.3 / Loader 0.19.5+.

Fabric's [mod metadata documentation](https://docs.fabricmc.net/1.21.11/develop/getting-started/project-structure) was consulted through Context7; the declared Loader floor governs dependency resolution. The pinned Fabric API itself declares a minimum Loader of 0.17.3. Loom's production-client task derives its Loader classpath from the project's Loader dependency, so the startup log must confirm the actual 0.19.3 runtime.

Compiling against Loader 0.19.3 also rebuilds the Mixin annotations against its bundled API. Some compiled annotation encodings differ from the 0.19.5 build; this is a rebuilt compatibility release, not a manual edit of the old JAR metadata. Feature source and assets are unchanged.

## Verification

- Root dependency insight resolves exactly `net.fabricmc:fabric-loader:0.19.3` on `clientRuntimeClasspath`.
- Java 21 build and all **64 logic checks** pass.
- Full packaged Fabric-alone and Sodium 0.8.14 client suites pass. Both startup logs explicitly report `Loading Minecraft 1.21.11 with Fabric Loader 0.19.3`, and load Flux 0.3.4.
- Geometry/pivot checks pass, including all 5,888 vertices in idle/hinged poses with Sodium intrinsics active. Press/Hold/Toggle, speed, finishes, size, glint, Chams/View Models, shield mask, handedness/offhand and existing gameplay/GUI/HUD checks pass.
- Representative knife/model/input classes are byte-identical to 0.3.3. Recompiled Mixin annotation encodings match the older bundled API. No feature source or texture changed; the native bridge was rebuilt by the normal build task.
- Packaged metadata declares Flux 0.3.4, Minecraft ~1.21.11, Java >=21 and Loader >=0.19.3. Packaging verifies Java 21 classes, original texture hashes, native bridge/license inclusion, exclusion of the test mod, source archive contents and SHA-256 checksums.
- The installed GUI toolkit dry-run reports `changes: []`. Its managed files/rules block remain unchanged; the Loader pin correction is in project-owned build/rules/docs.

Commands used Java 21 and the root wrapper: `dependencyInsight --dependency fabric-loader --configuration clientRuntimeClasspath`, `build runProductionClientGameTest -PsodiumAcceptance`, then `runProductionClientGameTest`, all with `--console plain`. Clients ran sequentially. Ignored logs are `work/loader-034-dependency.log`, `work/loader-034-sodium.log` and `work/loader-034-fabric.log`.

Actual 0.19.3/Sodium game captures: [knife](acceptance/034-loader-0193-knife.png), [Inspect Mode](acceptance/034-loader-0193-mode.png). The installable JAR, source ZIP, installation notes and checksum manifest are in `build/distributions/0.3.4`.

Replace the old 1.21.11 Flux Tweaks JAR with `fluxtweaks-0.3.4-mc1.21.11.jar`; keep the current Loader and configuration. The exact owner multiplayer/Lunar/shader modpack remains separately unverified. The 26.2 target is unchanged and was not rebuilt for this request. Dedicated-server testing/EULA acceptance remain disabled.
