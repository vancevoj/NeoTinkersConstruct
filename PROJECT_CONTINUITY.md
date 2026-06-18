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
NEW PROJECT STATE — 2026-06-18
PHASE: TConstruct compiling (Mantle DONE)
TCON head: f8f29621   MANTLE head: 3581c40e (DONE — builds Mantle-1.21.1-1.21.0.jar)
TCON errors: ~6000 (cap) — by pkg: library 1894, tools 1672, smeltery 916, tables 343, shared 319, common 315, world 197, fluids 196, gadgets 128

ARCH DECISIONS (don't re-litigate):
- TOOL_DATA = single CompoundTag DataComponentType (library/tools/nbt/ToolDataComponents); ToolStack swaps getTag/setTag → component get/set + write-back; ToolStack PUBLIC API UNCHANGED.
- Entity data: Forge caps → data attachments (TinkerDataCapability/PersistentDataCapability/EntityModifierCapability, all .getData(entity), NOT getCapability).
- Networking: TinkerNetwork (common/network) → Mantle NetworkWrapper payload system; packets = CustomPacketPayload (Type+StreamCodec+handle(IPayloadContext)).
- accesstransformer.cfg = mojmap names (converted; perl recipe in PORTING_STATUS.md). Composite build incl. ../Mantle. JDK 21.
- build.gradle EXCLUDES plugin/jei, plugin/jsonthings, plugin/craftingtweaks, ImmersiveEngineeringPlugin, DietPlugin, DummmmmmyPlugin (deps unavailable on 1.21).
- Mechanical codemods DONE: forge-imports, RegistryObject→DeferredHolder, ToolAction→ItemAbility, AttributeModifier.Operation enum, crafting.conditions→conditions pkg, ForgeMod→NeoForgeMod.

DONE: Mantle (full, builds jar). TConstruct: build system, AT conversion, all codemods, data-component tool model (gate), library/materials, library/tools (nbt/helper/capability/item), tools/data.
IN PROGRESS (partial agent edits committed, not compiling): library/{recipe,modifiers,client,json,events,utils}, smeltery/{block,data,client,item,network,menu}, tools/{modules,logic,modifiers,recipe,entity,client,item,network}, common (TinkerNetwork partial), shared, tables, gadgets, fluids, world.
NEXT: 1) recompile (tcon-compileN.log) → fresh map. 2) re-fan agents on partial pkgs (esp. common/TinkerNetwork — every packet depends on it). 3) central TConstruct.java wiring: register ToolDataComponents + the 3 attachment DeferredRegisters + TinkerNetwork.registerPackets on the mod bus; register tool/block item+fluid caps in RegisterCapabilitiesEvent; DELETE item initCapabilities/verifyTagAfterLoad overrides. 4) reconcile to green compile. 5) ./gradlew build + datagen. 6) runServer smoke. 7) in-game test all features.
OPEN ISSUES / TODO(neoport): tool-break XP (BreakEvent no longer carries exp); hide-flags → DataComponents.TOOLTIP_DISPLAY; AoE block-break via game-bus BreakEvent; dynamic rarity via RARITY component; model-loader API (client) some deeper redesign; re-enable+port JEI later.
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
