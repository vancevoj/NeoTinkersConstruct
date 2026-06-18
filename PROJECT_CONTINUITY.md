# NeoTinkersConstruct — Project Continuity Package

> Paste-ready package for resuming this port in a fresh session with zero context loss.
> Single source of truth lives in-repo; this file + `PORTING_STATUS.md` are the entry points.

---

## 1. Project Overview

Porting **Tinkers' Construct** and its hard dependency **Mantle** from Minecraft
**1.20.1/Forge** to **1.21.1/NeoForge**, so the fork works with all features and can be
re-synced with upstream over time. **Phase:** Mantle is DONE (compiles + builds a jar);
TConstruct is mid-port (~6000 compile errors, dropping) via parallel-agent orchestration.

---

## 2. GitHub Repository

| Repo | URL | Branch | State |
|------|-----|--------|-------|
| TConstruct fork | https://github.com/vancevoj/NeoTinkersConstruct | **`1.21`** (work); `1.20.1` = pristine upstream reference | mid-port |
| Mantle fork | https://github.com/vancevoj/Mantle | **`1.21`** | **DONE** (builds `Mantle-1.21.1-1.21.0.jar`) |
| Upstream (track) | SlimeKnights/TinkersConstruct (`1.20.1`), SlimeKnights/Mantle (`1.20`) | — | no official 1.21 yet |

**Structure** (both repos, standard NeoForge MDK):
- `src/main/java/slimeknights/{tconstruct,mantle}/…` — source
- `src/main/resources/META-INF/{neoforge.mods.toml, accesstransformer.cfg}`
- `docs/` — `forge-to-neoforge.md` (migration patterns), `agent-port-brief.md` (agent brief)
- `scripts/` — `migrate-forge-imports.sh` (codemod), `sync-upstream.sh`
- `PORTING_STATUS.md` (read first), `PORTING.md` (roadmap), `UPGRADING.md` (upstream sync), `PROJECT_CONTINUITY.md` (this file)

---

## 3. Local File System Mapping

| Path | What |
|------|------|
| `/ai/work/tinkers/NeoTinkersConstruct` | TConstruct working copy (branch `1.21`) |
| `/ai/work/tinkers/Mantle` | Mantle working copy (branch `1.21`); wired into TConstruct via composite build (`settings.gradle` → `includeBuild '../Mantle'`) |
| `/ai/work/tinkers/*.log` | compile logs; latest = `tcon-compile7.log` |
| `/tmp/maxerrs.init.gradle` | Gradle init script lifting javac's 100-error cap (recreate if missing — content below) |
| `.../library/tools/nbt/ToolDataComponents.java` | the `TOOL_DATA` data-component (the architectural gate) |
| `.../src/main/resources/META-INF/accesstransformer.cfg` | AT, already converted SRG→mojmap |

Recreate the init script if missing:
```
allprojects { tasks.withType(JavaCompile).configureEach {
  options.compilerArgs += ['-Xmaxerrs', '6000'] } }
```

**Build env (critical):** Java **21** required — `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
The box default `java` is 25 (Gradle 8.10 rejects it); `openjdk-21-jdk` is installed (the box
otherwise ships JRE-only, no `javac`).

---

## 4. Token-Efficient Workflow

The proven loop (one phase per session, commit between):
1. **Boot:** paste the Master Starter Prompt + current Project State. I read `PORTING_STATUS.md`.
2. **Map:** recompile → fresh per-package error counts:
   `cd /ai/work/tinkers/NeoTinkersConstruct && export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && ./gradlew :compileJava --no-daemon --console=plain -I /tmp/maxerrs.init.gradle > /ai/work/tinkers/tcon-compileN.log 2>&1`
3. **Fan out:** one agent per package/cluster. Each reads `docs/agent-port-brief.md` +
   `docs/forge-to-neoforge.md`, ports its package, does **NOT** run gradle, reports back.
4. **Reconcile:** I recompile the aggregate, fix cross-package wiring + AT entries + leftovers
   directly, **commit + push**, repeat.
5. **Limits:** agents share an account session limit (resets at set times) + occasional server
   rate limits → they return 0 tokens when hit; retry later or do main-loop codemods meanwhile.
   **Always commit WIP before stopping** (the port branch isn't expected to compile mid-way).

Why this is token-efficient: the orchestrator (main session) stays small — it only maps, fans
out, reconciles, commits. The heavy per-file reading/editing happens in disposable agent
contexts. The durable knowledge lives in the in-repo docs, not the chat.

---

## 5. Master Starter Prompt (paste at the start of every session)

```
You are resuming the NeoTinkersConstruct port (Tinkers' Construct + Mantle → MC 1.21.1 / NeoForge).

Read these in the repo first, they are the source of truth:
  /ai/work/tinkers/NeoTinkersConstruct/PROJECT_CONTINUITY.md
  /ai/work/tinkers/NeoTinkersConstruct/PORTING_STATUS.md
  /ai/work/tinkers/NeoTinkersConstruct/docs/forge-to-neoforge.md
  /ai/work/tinkers/NeoTinkersConstruct/docs/agent-port-brief.md

Build env: export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  (default java=25 is rejected by Gradle).
Repos: vancevoj/NeoTinkersConstruct & vancevoj/Mantle, both on branch 1.21. Mantle is DONE.

Workflow: recompile to get the per-package error map, then fan out one porting agent per
package/cluster (each reads the two docs above, ports its package, does NOT run gradle),
then I reconcile (compile aggregate, fix cross-package wiring + accesstransformer.cfg, commit+push), repeat.
Always commit WIP before stopping. Agents may hit token/rate limits — retry or do codemods meanwhile.

CURRENT PROJECT STATE:
<<<paste the latest NEW PROJECT STATE block here>>>

TASK THIS SESSION:
<<<e.g. "drive TConstruct to a green compile" or "fix smeltery + tables", etc.>>>
```

---

## 6. Project State Template (keep this updated each session)

```
NEW PROJECT STATE — <date>
PHASE: <Mantle done | TConstruct compiling | TConstruct building | runtime testing>
TCON head: <git short hash>   MANTLE head: <hash, DONE>
TCON errors: <N from latest compile> (cap 6000) — by pkg: <pkg:count, ...>

ARCH DECISIONS (don't re-litigate):
- TOOL_DATA = single CompoundTag DataComponentType (ToolDataComponents); ToolStack swaps the
  getTag/setTag boundary to component get/set; ToolStack PUBLIC API UNCHANGED (475 deps untouched).
- Entity data: Forge caps → NeoForge data attachments (Tinker/Persistent/EntityModifier capability classes).
- Networking: TinkerNetwork uses Mantle's ported NetworkWrapper (payload system); packets = CustomPacketPayload.
- accesstransformer.cfg MUST use mojmap names (SRG names are silent no-ops).
- Composite build: TConstruct includeBuild ../Mantle. JDK 21.
- Optional plugin integrations EXCLUDED in build.gradle (jei/jsonthings/craftingtweaks/IE/diet/dummmmmmy) — 3rd-party deps not on 1.21 yet.

DONE: <list of fully-ported packages / milestones>
IN PROGRESS: <partially-ported packages>
NEXT: <ordered next steps>
OPEN ISSUES / TODO(neoport): <cross-package wiring, behavioral gaps>
```

---

## 7. Best Practices for Compression

- **Point, don't paste.** The State references in-repo docs by path; never inline `forge-to-neoforge.md` or file contents into the chat.
- **Counts not lists.** "smeltery: 916 errs" beats enumerating files. Get them from the compile log, not memory.
- **Decisions once.** Architecture decisions go in the State's ARCH block so they're never re-derived.
- **Status as 3 buckets:** DONE / IN PROGRESS / NEXT. Drop finished detail.
- **Commit hashes = state.** The branch is the real state; the State block just orients.
- **Trim each session:** delete resolved TODOs, collapse "in progress → done", refresh error counts.
- **Cap the State at ~40 lines.** If longer, you're duplicating the repo — move detail into `PORTING_STATUS.md`.

---

## 8. Current Project State

```
NEW PROJECT STATE — 2026-06-18 (GREEN COMPILE + JAR)
PHASE: TConstruct BUILDS. `./gradlew :compileJava` = 0 errors; `./gradlew build` produces TinkersConstruct-1.21.1-3.11.2.jar. (Mantle DONE.)
TCON head: 35109e7a (+ test-defer + this doc)   MANTLE head: 673db196 (DONE; builds jar)
TCON errors: 0 (main source, was ~6000). src/test (50 files) is a DEFERRED port (still 1.20/Forge + ModDevGradle test classpath not wired); compileTestJava/test disabled in build.gradle so the mod jar builds.

ARCH DECISIONS (don't re-litigate):
- TOOL_DATA = single CompoundTag DataComponentType (library/tools/nbt/ToolDataComponents); ToolStack swaps getTag/setTag → component get/set; ToolStack PUBLIC API UNCHANGED.
- Entity data: Forge caps → data attachments (TinkerDataCapability/PersistentDataCapability/EntityModifierCapability + gadgets PiggybackCapability), all .getData(entity); registered on the mod bus from the constructor.
- Central wiring (TConstruct.java): NeoForge constructor `TConstruct(IEventBus bus, ModContainer container)`; `@EventBusSubscriber` (not @Mod.EventBusSubscriber); calls ToolDataComponents.init(bus) + the 3 attachment registers + TinkerModule.initRegisters(bus) + `new TinkerGadgets(bus)` + TinkerNetwork.setup() + `bus.addListener(TinkerNetwork::registerPackets)`; FMLEnvironment.dist client check; missing-mappings handler DROPPED (no NeoForge equivalent).
- ItemDisplayContext is now an EXTENSIBLE ENUM, not a registry: TinkerItemDisplays uses EnumProxy + META-INF/enumextensions.json.
- DeferredHolder<R, T extends R>: the codemod produced 161 illegal `DeferredHolder<?, X>`; fixed to `DeferredHolder<? super X, X>` (codemod-able). `DeferredHolder<?, ?>` is fine.
- ICommonRecipeHelper (Mantle): vanilla RecipeProvider.has/inventoryTrigger are now STATIC; the interface's criteria helpers are now PRIVATE (criterion/hasItem/hasTag) so a `extends RecipeProvider implements ICommonRecipeHelper` subclass no longer clashes; providers call the inherited vanilla statics.
- GOTCHA (critical): gradle INCREMENTAL compilation HID latent errors in unchanged files, giving false-low counts (a "72" that was really 635). ALWAYS clean-compile for a true count: `rm -rf build/classes/java/main build/tmp/compileJava` before `./gradlew :compileJava -I /tmp/maxerrs.init.gradle`.
- accesstransformer.cfg = mojmap names; Slot fields are x/y (1.21, not xPos/yPos). build.gradle EXCLUDES jei/jsonthings/craftingtweaks/IE/diet/dummmmmmy. Composite build incl. ../Mantle. JDK 21.

DONE: Mantle (full, builds jar). TConstruct: FULL main source compiles + builds jar — ALL packages (library, tools, smeltery, tables, shared, common, world, fluids, gadgets; runtime AND datagen), central wiring, AT, all codemods, data-component tool model.
NEXT: 1) `./gradlew runData` (datagen) + verify generated assets/data. 2) RegisterCapabilitiesEvent wiring (see OPEN ISSUES) then `./gradlew runServer` smoke test. 3) in-game test all features. 4) port src/test + wire ModDevGradle test classpath, re-enable compileTestJava/test. 5) re-enable + port JEI plugin.
OPEN ISSUES / TODO(neoport): (1) RegisterCapabilitiesEvent central wiring for smeltery BE item/fluid handlers NOT yet added — compiles without it, but smeltery/tank/casting caps won't expose at runtime (BE getter methods exist: Heater/Melter.getItemHandler, *.getTank, etc.; fluids item caps self-register via FluidEvents @EventBusSubscriber). (2) potion-fluid TagPredicate filtering dropped (PotionFluidEffect/PotionCloudFluidEffect read POTION_CONTENTS, predicate ignored). (3) ConsumerWrapperBuilder ceramics-kiln serializer redirect dropped (SmelteryRecipeProvider:195/643 — grout still emits, as vanilla blasting not ceramics:kiln). (4) looting via new enchantment-value system (ModifierLootingHandler.getLootingLevel). (5) tool-break XP; hide-flags → TOOLTIP_DISPLAY; dynamic rarity. (6) JEI deferred.
```

---

## Quick command reference

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd /ai/work/tinkers/NeoTinkersConstruct
# fresh error map:
./gradlew :compileJava --no-daemon --console=plain -I /tmp/maxerrs.init.gradle > /ai/work/tinkers/tcon-compileN.log 2>&1
grep -cE '\.java:[0-9]+: error:' /ai/work/tinkers/tcon-compileN.log            # total (cap 6000)
# per-package counts:
for p in $(ls -d src/main/java/slimeknights/tconstruct/*/ | sed 's#.*/tconstruct/##;s#/##'); do n=$(grep -E "/tconstruct/$p/.*error:" /ai/work/tinkers/tcon-compileN.log | grep -cE 'error:'); [ "$n" -gt 0 ] && echo "$n $p"; done | sort -rn
git add -A && git commit -m "WIP" && git push origin 1.21    # ALWAYS before stopping
# Mantle (already done) verify: cd /ai/work/tinkers/Mantle && ./gradlew build
```
