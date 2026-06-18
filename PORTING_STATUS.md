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
- **Mantle is fully ported.** `./gradlew :Mantle:build` is green and produces
  `Mantle-1.21.1-1.21.0.jar` (1675 errors -> 0). Done via the parallel-agent method
  (one agent per package cluster) + central recompile reconciliation. The optional
  JEI plugin package (`slimeknights.mantle.plugin.jei`) is temporarily excluded in
  `build.gradle` pending its own port against the 1.21.1 JEI API.

### Key gotcha solved: the access transformer
The inherited `accesstransformer.cfg` used **1.20.1 SRG names** (`f_111419_`,
`m_280092_`), which are silent no-ops on NeoForge's Mojang mappings - so every
widened field/method was actually inaccessible. Convert it to mojmap names (the
names are in the trailing `# comments`):
```bash
perl -i -pe '
  s/^([\w-]+\s+\S+\s+)m_\d+_(\([^ ]*\)[^ ]+)\s+#\s+(\w+).*$/$1$3$2/;
  s/^([\w-]+\s+\S+\s+)f_\d+_\s+#\s+(\w+).*$/$1$2/;
' src/main/resources/META-INF/accesstransformer.cfg
```
Then fix fields renamed in 1.21 (guiLeft->leftPos, lastKnownValue->prevValue,
blocks->palettes, idToFontMap->fontSets). Already done for both repos.

## In progress / next: TConstruct

Mechanical pass + AT conversion done (1460 forge refs -> 0). Baseline compile vs the
working Mantle: **~6000+ errors**. Plan, in order:

1. **Exclude the JEI plugin** (`slimeknights.tconstruct.plugin.jei`) in build.gradle
   like Mantle - removes ~350 errors (JEIPlugin + *Category files).
2. **Design the data-component tool model (the gate).** Redesign these 6 files off
   item NBT onto registered `DataComponentType`s (Codec + StreamCodec):
   `library/tools/nbt/{ToolStack,MaterialNBT,ModifierNBT,StatsNBT,ToolDataNBT,IToolStackView}.java`.
   **475 files reference `ToolStack`** - this core must be coherent before parallel
   porting, or every tool-touching file cascades.
3. **Parallel-port the packages** like Mantle (one agent per cluster: smeltery,
   fluids/`TinkerFluids`, world, shared, blocks/block-entities, then the tool/modifier
   core that depends on step 2), then reconcile by recompiling the aggregate.
4. Re-enable + port both JEI plugins; datagen pass; in-game test.

Recompile to see the full error list: `./gradlew :compileJava -I /tmp/maxerrs.init.gradle`

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
