# Porting status (living document)

Read this first to resume the port. It tracks the actual state of the work; the
roadmap is in [`PORTING.md`](PORTING.md), the upstream-sync workflow in
[`UPGRADING.md`](UPGRADING.md), and the code patterns in
[`docs/forge-to-neoforge.md`](docs/forge-to-neoforge.md).

Target: **Minecraft 1.21.1 / NeoForge 21.1.233 / Java 21 / Parchment 2024.11.17.**

## Build environment (IMPORTANT)

This box ships Java as **JREs only**; there was no `javac` until `openjdk-21-jdk`
was installed (`sudo apt-get install openjdk-21-jdk`). NeoForm needs a real JDK 21
to recompile Minecraft.

The default `java` on PATH is 25, which **Gradle 8.10.2 does not support**. Always
run Gradle on JDK 21:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew <task>
```
(or set `org.gradle.java.home` in `~/.gradle/gradle.properties` to that path).

The two repos build together. Clone them as siblings:
```
.../tinkers/NeoTinkersConstruct   (this repo, branch 1.21)
.../tinkers/Mantle                (vancevoj/Mantle, branch 1.21) - composite build
```

## Done

- **Git/upstream infra** (both repos): `1.21` live-port branch off `1.20.1`;
  `upstream` remote + `scripts/sync-upstream.sh` (auto-detects a future official
  1.21/NeoForge branch); `UPGRADING.md`.
- **Build systems converted** to ModDevGradle (both repos): NeoForge 21.1.233,
  Java 21, Parchment 2024.11.17, `neoforge.mods.toml`, gradle wrapper 8.10.2.
  TConstruct pulls Mantle via Gradle composite build (`includeBuild '../Mantle'`).
- **Toolchain verified**: `./gradlew createMinecraftArtifacts` succeeds - NeoForge
  downloads and Minecraft 1.21.1 decompiles + recompiles under JDK 21. `help`
  configures both projects.
- **Mantle mechanical pass** committed (`scripts/migrate-forge-imports.sh`): all
  452 `net.minecraftforge` references rewritten; ~49 ResourceLocation constructors
  converted. The module does **not** compile yet.

## In progress / next

### Mantle code port (the current front)
`./gradlew :Mantle:compileJava` (with `-I` to lift the error cap) reports
**~1630 errors**, all genuine reworks. Tackle bottom-up; categories and concrete
before/after snippets are in [`docs/forge-to-neoforge.md`](docs/forge-to-neoforge.md):

1. `registration` + `util` - DeferredRegister/RegistryObject -> DeferredHolder,
   ForgeRegistries -> BuiltInRegistries. Fixes the bulk of `cannot find symbol`.
2. `network` - SimpleChannel -> `RegisterPayloadHandlersEvent` + CustomPacketPayload.
3. `fluid` - FluidStack components, BaseFlowingFluid, fluid capabilities.
4. capabilities usage across `block`/`item`/`inventory` - typed BlockCapability/ItemCapability.
5. `data`/`recipe`/`loot` - codec-based conditions, advancement Criterion wrap.
6. `client` - model loaders + GUI layer event renames.
7. `command`/`config`/`plugin` - last; re-enable JEI in `build.gradle` for `plugin`.

Milestone: `./gradlew :Mantle:build` green, then a dedicated-server smoke
(`./gradlew :Mantle:runServer`, expect "Done", stop it).

**Exact next files** (Mantle, highest error count first - this is the dependency
root, port as one batch then recompile to watch the ~1630 total drop):
1. `registration/adapter/RegistryAdapter.java` (57) - base of the adapter layer
2. `registration/deferred/BlockDeferredRegister.java` (29),
   `FluidDeferredRegister.java` (22), `EntityTypeDeferredRegister.java` (10),
   and the rest of `registration/deferred/*` and `registration/adapter/*`
3. `util/CombatHelper.java` (40), `util/OffhandCooldownTracker.java` (23) -
   note `MobType` was REMOVED in 1.21; these use it. Replace MobType checks with
   entity type tags (`EntityTypeTags`) / `LivingEntity` accessors.
4. `util/JsonHelper.java` (9), `RegistrationHelper.java` (4), then `Mantle.java`
   (entangled: needs predicates, conditions, network, MobType all done first).
Recompile after each batch: `./gradlew compileJava -I /tmp/maxerrs.init.gradle`.

### TConstruct code port (blocked on Mantle)
545/1854 files import Forge (1446 imports). After Mantle compiles:
run `scripts/migrate-forge-imports.sh` here too, then the **data-components**
rewrite (Phase 3 in `PORTING.md`) before Phase 4 systems. This is the largest
single task in the whole port - schema the tool/material/modifier/stats components
first.

## Quick reference

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew createMinecraftArtifacts          # set up / verify the MC toolchain
./gradlew compileJava -I /tmp/maxerrs.init.gradle   # full error list (see below)
scripts/migrate-forge-imports.sh src/main/java      # mechanical Forge->Neo pass
scripts/sync-upstream.sh                     # pull upstream 1.20.1 changes
```
To see all compile errors at once (javac caps at 100), use an init script:
```
allprojects { tasks.withType(JavaCompile).configureEach {
  options.compilerArgs += ['-Xmaxerrs', '6000'] } }
```

## Honest scope

No complete TConstruct 1.21.1 NeoForge port exists - upstream is still 1.20.1/Forge
and the public community attempts (LopyLuna, Rainy1127, gfaraujosousa) are all
stalled at scaffolding. This is a multi-week build. The foundation here is solid
and verified; the remaining work is the code grind, which is now scoped, tooled,
and ordered.
