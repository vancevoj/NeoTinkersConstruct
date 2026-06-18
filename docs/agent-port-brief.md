# TConstruct NeoForge 1.21.1 port - parallel agent brief

Porting **Tinkers' Construct** from MC 1.20.1/Forge to **1.21.1/NeoForge**. Repo:
`/ai/work/tinkers/NeoTinkersConstruct`. Build runs with JDK 21:
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. The build system, the
mechanical Forge->NeoForge import pass (no `net.minecraftforge.*` remain), the
RegistryObject->DeferredHolder pass, and the access-transformer SRG->Mojmap
conversion are ALL DONE. Mantle (the dependency) is fully ported and compiles.

**Read first:** [`forge-to-neoforge.md`](forge-to-neoforge.md) - every API pattern
(registration, capabilities, networking, fluids, codecs, data attachments, GUI
layers, etc.) with before/after code. Those patterns were already validated porting
Mantle. Then read a few of your package's files + their errors.

See your package's errors: `./gradlew :compileJava -I /tmp/maxerrs.init.gradle 2>&1 | grep "/<yourpkg>/"`
**Do NOT run gradle yourself** if other agents are active (concurrent builds
conflict) - the orchestrator compiles the aggregate. Fix by reading code + applying
patterns.

## Rules
- ONLY edit files in your assigned package(s). If you need another package's API,
  assume it keeps its current public signature. You MAY add entries to
  `src/main/resources/META-INF/accesstransformer.cfg` (mojmap names only, e.g.
  `public net.minecraft.world.food.FoodData foodLevel`) when a vanilla field/method
  needs widening.
- Use `// TODO(neoport): <reason>` only where a fix needs a cross-package decision or
  the optional JEI API (which is dep-disabled). Minimize.
- Preserve behavior and javadoc.

## THE DATA-COMPONENT TOOL MODEL (critical - owned by the nbt-core agent only)

Tinkers stores all tool data in item NBT today. In 1.21 item NBT is gone. The port
keeps the existing serialization intact and only swaps the storage boundary:

- Register ONE component: `DataComponentType<CompoundTag> TOOL_DATA` (a
  `DeferredRegister.DataComponents` on `tconstruct`), persistent codec
  `CompoundTag.CODEC`, network `ByteBufCodecs.TRUSTED_COMPOUND_TAG`. Put it in the
  registration class the nbt-core agent owns (e.g. `TinkerToolData` or in
  `TConstruct`/`library/tools/nbt`).
- In `ToolStack`: the `CompoundTag nbt` it holds now comes from / writes back to that
  component instead of `stack.getTag()`/`stack.setTag()`/`stack.tag`:
  - read: `CompoundTag nbt = stack.getOrDefault(TOOL_DATA, CompoundTag::new)` (copy
    it so mutations are local), or `new CompoundTag()` when absent.
  - write-back: ToolStack must hold the `ItemStack` it came from and, wherever it
    previously shared the tag or called `setTag`, call `stack.set(TOOL_DATA, nbt)`.
    `createStack()`/`createStack(int)` -> `stack.set(TOOL_DATA, nbt)` instead of
    `stack.tag = nbt`. `refreshTag(stack)` reads the component.
  - Damage: keep TConstruct's existing `Damage` key inside the TOOL_DATA compound
    (do NOT split to vanilla DataComponents.DAMAGE in this pass; keep behavior).
  - `RestrictedCompoundTag` / restricted-tag logic stays unchanged.
- **`ToolStack`'s public API does NOT change** (from/getStats/getMaterials/etc.). All
  475 files using ToolStack stay as-is. Only ToolStack's internals + the new
  component registration change.

## Key conventions (full code in forge-to-neoforge.md)
- DeferredRegister/DeferredHolder (RegistryObject already converted); ForgeRegistries
  -> BuiltInRegistries/NeoForgeRegistries; mod bus injected via `@Mod` constructor.
- Networking: CustomPacketPayload + PayloadRegistrar (Mantle's network is ported;
  TConstruct has its own packets to port the same way).
- Capabilities for behavior -> BlockCapability/ItemCapability + RegisterCapabilitiesEvent;
  capabilities used for DATA -> data attachments (AttachmentType). LazyOptional gone.
- Recipes/ingredients/conditions -> MapCodec + StreamCodec (RecipeSerializer has
  codec()/streamCodec(); recipes use RecipeInput; RecipeManager.getAllRecipesFor returns
  List<RecipeHolder<T>>). Loot modifiers -> MapCodec.
- FluidStack is component-backed (`new FluidStack(fluid, amount)`; `.getTag()` ->
  components; copy via `stack.copyWithAmount(n)`). Use Mantle's ported fluid helpers.
- `new ResourceLocation(a,b)` -> `ResourceLocation.fromNamespaceAndPath`/`.parse`.
- ItemStack NBT gone: `stack.getTag()`/`getOrCreateTag()`/`setTag()` ->
  `DataComponents.CUSTOM_DATA` + `CustomData` (or a dedicated component).
- `appendHoverText(stack, Item.TooltipContext, list, flag)`; `use`->`useWithoutItem`
  for blocks; GuiGraphics replaces PoseStack; GUI overlay->layer events.
- MobType removed -> entity type tags. AdvancementHolder vs ResourceLocation: datagen
  `save` now returns/takes AdvancementHolder.

## Output (final message = report to orchestrator, not human-facing)
1. Files changed + key API decision each (one line).
2. Cross-package assumptions.
3. Remaining `TODO(neoport)` as `file:line - reason`.
No code dumps.
