# Porting Tinkers' Construct to Minecraft 1.21.1 (NeoForge)

This fork tracks an effort to bring Tinkers' Construct from its upstream **1.20.1 / Forge**
branch to **1.21.1 / NeoForge**, so it can run alongside Create 6.x and Create Aeronautics
on modern NeoForge packs (the Tinkreate pack in particular).

This is a large, long-running effort, not a drop-in update. The notes below scope the work
and fix the order of operations so progress stays incremental and testable.

## Order of operations

Port bottom-up. Each phase should compile and launch before the next one starts.

### Phase 0: Dependency (Mantle)
- Tinkers depends on Mantle (SlimeKnights/Mantle). Nothing here builds until Mantle exists
  for 1.21.1 NeoForge.
- Either port Mantle in a sibling fork first, or vendor only the pieces TConstruct uses.

### Phase 1: Build system
- `gradle.properties`: set `minecraft_version=1.21.1`, drop `forge_version`, add a NeoForge
  version, switch to official (Mojang) mappings.
- Replace ForgeGradle with ModDevGradle (or NeoGradle 7); rewrite `settings.gradle` and
  `build.gradle`.
- Convert `META-INF/mods.toml` to `META-INF/neoforge.mods.toml` (new dependency block format).
- Goal: an empty shell that compiles and launches on NeoForge 1.21.1 before any mod code is touched.

### Phase 2: Loader API migration (Forge to NeoForge)
- Registration: Forge `DeferredRegister` / `RegistryObject` to NeoForge `DeferredRegister` /
  `DeferredHolder`.
- Events: `MinecraftForge.EVENT_BUS` to `NeoForge.EVENT_BUS`; mod-bus events; renamed and
  moved event classes.
- Capabilities: Forge capabilities to NeoForge `BlockCapability` / `ItemCapability` /
  `EntityCapability` (this system is fully reworked).
- Networking: `SimpleChannel` to NeoForge payload records plus handler registration.
- Config and datagen API moves.

### Phase 3: Vanilla 1.20.1 to 1.21.1 changes
- **Data components (the single largest task).** Item NBT is gone. Tinkers stores tool data,
  materials, modifiers, and stats in NBT today; all of it has to move to `DataComponents`
  (custom component types for the tool stack, material lists, modifier maps, and so on). This
  reaches into tools, the tinker station, part builder, casting, melting, modifier logic, and
  tooltips.
- Registry and Holder changes (1.21 registries, `RegistryAccess` plumbing).
- Rendering: `RenderType`, `GuiGraphics`, model loaders, and the tool-part / retexture model system.
- Recipes and datapacks: 1.21 recipe codec changes; regenerate through datagen.
- Fluids, particles, sounds, GUIs, fonts.

### Phase 4: TConstruct systems (in dependency order)
1. Materials and material stats (JSON plus registry).
2. Tool definitions, parts, and the tool data model on data components.
3. Modifiers and modifier hooks.
4. Smeltery / foundry multiblocks, fluids, melting and casting.
5. Tinker station, part builder, crafting tables.
6. World gen (slime islands, ores), entities (slimes), commands.
7. Integrations (JEI / EMI and friends).

### Phase 5: Polish
- Datagen every asset, recipe, and advancement.
- Model and texture parity with upstream.
- An in-game testing pass per system.

## Branch layout
- `1.20.1` (default): upstream source, kept unchanged as the porting reference.
- `1.21` (to create): the live port. Keep it compiling at every phase boundary.

## License and credit
This is a derivative work of Tinkers' Construct by SlimeKnights; upstream licensing in
`LICENSE` applies. Full credit to the SlimeKnights team for the original mod.
