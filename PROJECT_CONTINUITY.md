# NeoTinkersConstruct — Project Continuity Package

> Paste-ready package for resuming this port in a fresh session with zero context loss.
> Single source of truth lives in-repo; this file + `PORTING_STATUS.md` are the entry points.

---

## 1. Project Overview

Porting **Tinkers' Construct** and its hard dependency **Mantle** from Minecraft
**1.20.1/Forge** to **1.21.1/NeoForge**, so the fork works with all features and can be
re-synced with upstream over time. **Phase: PLAYABLE.** Both compile green, datagen
completes, and the client loads, joins a singleplayer world, and survives normal play
(movement, mob spawning, opening the inventory/creative tabs). Jars published to the
`releases` branch (NeoForge 21.1.233). Remaining: smeltery `RegisterCapabilitiesEvent`
wiring, advancements, and global loot modifiers (all currently stubbed/disabled, none fatal).

**Runtime fixes that made it playable (2026-06-18):**
- `update_recipes` packet encode failure on world join: Mantle `SimpleRecipeSerializer`
  used `StreamCodec.unit(constructor.get())` (asserts instance identity on encode); now
  encodes nothing and builds a fresh instance on decode.
- Client froze on the Mojang splash: Mantle `fluid.vsh` used the old 3-arg `fog_distance`;
  fixed to the 1.21 2-arg signature.
- Crashes on mob tick and on opening the inventory from 1.21's new `BODY` equipment slot
  (filter flag 6, animal armor): slot-indexed arrays sized by `EquipmentSlot.values().length`
  (`EquipmentContext`, `ModifierMaxLevel`, `SlotInChargeModule`); `ArmorItem.Type.BODY`
  skipped in creative-tab/book/tinker-station code that only covers the 4 humanoid types.

---

## 2. GitHub Repository

| Repo | URL | Branch | State |
|------|-----|--------|-------|
| TConstruct fork | https://github.com/vancevoj/NeoTinkersConstruct | **`1.21.1`** (only branch; now the default) | playable |
| Mantle fork | https://github.com/vancevoj/Mantle | **`1.21`** | **DONE** (builds `Mantle-1.21.1-1.21.0.jar`) |
| Upstream (track) | SlimeKnights/TinkersConstruct (`1.20.1`), SlimeKnights/Mantle (`1.20`) | — | no official 1.21 yet |

Branch policy (2026-06-18): the TConstruct repo keeps a **single** branch, `1.21.1` (renamed from
`1.21`; also the default). The old `releases` branch and the pristine `1.20.1` reference branch were
deleted per request. The pristine 1.20.1 source is still available from the `upstream` remote
(`git fetch upstream 1.20.1`) for diffing. (Mantle's branch is still named `1.21`.)
**Jars ship via a GitHub Release, not a branch:** release `v3.11.2-1.21.1` carries both jars as assets
(https://github.com/vancevoj/NeoTinkersConstruct/releases/tag/v3.11.2-1.21.1). Build new jars with
`./gradlew build` (Java 21), then attach to a new/updated release with `gh release upload` or the API.

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
| `/ai/work/tinkers/NeoTinkersConstruct` | TConstruct working copy (branch `1.21.1`) |
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
NEW PROJECT STATE — 2026-06-18 (LOADS + PARTIAL DATAGEN; jar on `releases` branch)
PHASE: TConstruct BUILDS + LOADS through registration. `./gradlew :compileJava` = 0 errors; jar builds; mod construction + full registration succeed (runData reaches the data providers). Runtime world load + full datagen still WIP. Release-preview jar pushed to the `releases` branch (jars/ + README). (Mantle DONE.)
TCON head: da8a9493 (1.21) / releases branch = jars   MANTLE head: a55a6f89 (DisplayContextLoadable parse fix)
RUNTIME-LOAD FIXES DONE (this pass): enumextensions.json schema {entries:[...]} + EnumProxy(-1 id, name, fallback); RegisterBrewingRecipesEvent moved to game bus (NeoForge.EVENT_BUS); modules w/o @SubscribeEvent constructed not bus.register'd (TinkerEffects/TinkerToolParts); 1.21 added ArmorItem.Type.BODY (5th) + EquipmentSlot.BODY (filter flag 6) -> size arrays by values().length, register armor/plating for the 4 humanoid types only (ModifiableArmorMaterial.ARMOR_TYPES); custom ItemDisplayContext serialized names are namespaced (tconstruct:melter) -> Mantle DisplayContextLoadable uses ResourceLocation.parse; ICondition codecs (config/tag_intersection/tag_difference) + custom IngredientTypes (no_container/block_tag/material/material_value) registered in TinkerTools RegisterEvent; forge:item/* model parents -> neoforge:; ShieldBannerModifierSpriteSource type registered via RegisterSpriteSourceTypesEvent in ToolClientEvents. GlobalLootModifiersProvider datagen temp-disabled.
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
NEXT (to reach playable): 1) **EnchantmentModule** -> refactor off bare `Enchantment` onto `Holder<Enchantment>`/`ResourceKey<Enchantment>` (1.21 enchantments are a DATA-DRIVEN datapack registry, so Mantle `RegistryLoadable<Enchantment>` getKey fails: static registry empty). This is the current `runData` blocker (ModifierProvider). 4 files reference EnchantmentModule. Also affects EnchantmentToModifierProvider + StatLoadable:169 TODO. 2) finish remaining datagen providers (FluidEffectProvider, smeltery/tool/material recipes, ToolDefinitionDataProvider) and complete `runData` so ALL resources (esp. worldgen) regenerate. 3) DELETE stale src/generated then full runData (stale 1.20.1 worldgen = `minecraft:grass`/old uniform format = FATAL at world load). 4) add central RegisterCapabilitiesEvent handler. 5) `runServer` to a loaded world, then `runClient`; re-enable GlobalLootModifiersProvider; in-game test.
OPEN ISSUES / TODO(neoport): (0) **runData blocker = EnchantmentModule enchantment-registry (see NEXT 1)**. (1) RegisterCapabilitiesEvent central wiring for smeltery BE item/fluid handlers NOT yet added (BE getters exist: Heater/Melter.getItemHandler, *.getTank; fluids item caps self-register via FluidEvents). (2) GlobalLootModifiersProvider datagen disabled (loot-modifier condition codec). (3) potion-fluid TagPredicate filtering dropped. (4) ConsumerWrapperBuilder ceramics-kiln redirect dropped (SmelteryRecipeProvider:195/643). (5) looting via enchantment-value system. (6) tool-break XP; hide-flags->TOOLTIP_DISPLAY; dynamic rarity. (7) src/test deferred; JEI deferred.
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
