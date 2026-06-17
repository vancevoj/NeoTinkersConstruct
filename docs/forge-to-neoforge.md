# Forge 1.20.1 -> NeoForge 1.21.1 migration reference

Concrete patterns for porting Tinkers' Construct and Mantle. Two independent
axes of change are happening at once:

1. **Loader swap** - Forge APIs become NeoForge APIs (mostly package renames,
   some reworks).
2. **Vanilla jump** - Minecraft 1.20.1 -> 1.21.1, whose biggest item is the
   **data-component** system replacing item NBT (1.20.5).

Work bottom-up and keep a running compile. `scripts/migrate-forge-imports.sh`
does the mechanical layer; everything below the table is manual.

## Real error surface (Mantle, measured)

After the mechanical pass, `./gradlew compileJava` reports ~1630 errors. The
distribution is the porting to-do list, roughly in descending volume:

| Count | Error | Meaning / fix |
|------:|-------|---------------|
| ~874 | `cannot find symbol` | Downstream of removed/renamed APIs below (RegistryObject, ForgeRegistries, capability + fluid + network classes). Fixes cascade once the roots are fixed. |
| ~176 | `does not override ... supertype` | Overridden method signatures changed (events, codecs, capability + fluid interfaces). Re-derive the new signature. |
| ~63 | `ResourceLocation(...)` private / wrong args | Vanilla 1.21: use `ResourceLocation.fromNamespaceAndPath(ns, path)` or `ResourceLocation.parse(str)`. The codemod handles the simple cases. |
| ~24 | `common.crafting.conditions` missing | Conditions reworked -> `net.neoforged.neoforge.common.conditions` + codec-based `ICondition`. |
| ~19 | `common.capabilities` missing | Capability system fully reworked (see below). |
| ~14 | `TriggerInstance cannot be converted to Criterion` | 1.20.5+ advancement codec change; wrap with `Criterion`. |
| ~12 | `ForgeRegistries` missing | -> vanilla `BuiltInRegistries` or `NeoForgeRegistries`. |
| ~14 | `NetworkEvent` missing | Networking is payload-based now (see below). |
| ~6 each | `RenderGuiOverlayEvent`, `VanillaGuiOverlay`, `ForgeFlowingFluid` | Renamed: `RenderGuiLayerEvent`, `VanillaGuiLayers`, `BaseFlowingFluid`. |
| ~5 | `DataResult.getOrThrow` arity | 1.20.5 codec change: `getOrThrow()` no longer takes a function; use `getOrThrow(s -> new RuntimeException(s))` only where needed. |
| several | `protected/private access` (`leftPos`, `topPos`, `cullForDirection`) | Widen via the access transformer, or use the new accessor. |

## Package rename table (mechanical - done by the codemod)

| Forge | NeoForge |
|-------|----------|
| `net.minecraftforge.eventbus.*` | `net.neoforged.bus.*` |
| `net.minecraftforge.fml.*` | `net.neoforged.fml.*` |
| `net.minecraftforge.api.*` | `net.neoforged.api.*` |
| `net.minecraftforge.*` (everything else) | `net.neoforged.neoforge.*` |
| class `MinecraftForge` | class `NeoForge` |
| class `ForgeConfigSpec` | class `ModConfigSpec` |

These remaining symbols are renamed but the codemod leaves them (they need code
changes too): `RegistryObject`, `ForgeRegistries`, `SimpleChannel`,
`IForgeMenuType`, `ForgeFlowingFluid`, `LazyOptional`.

## Reworks (manual)

### Mod entry point + event buses

```java
// Forge
@Mod(TConstruct.MOD_ID)
public class TConstruct {
  public TConstruct() {
    IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
    MinecraftForge.EVENT_BUS.register(this);
  }
}
// NeoForge 1.21.1 - the mod bus is injected into the constructor
@Mod(TConstruct.MOD_ID)
public class TConstruct {
  public TConstruct(IEventBus modBus, ModContainer container) {
    NeoForge.EVENT_BUS.register(this);          // game bus
    // register DeferredRegisters and mod-bus listeners on modBus
  }
}
```
- `@Mod.EventBusSubscriber` -> `net.neoforged.fml.common.EventBusSubscriber`
  (moved out of `Mod`). `@OnlyIn` / `Dist` -> `net.neoforged.api.distmarker.*`.

### Registration: DeferredRegister / RegistryObject

```java
// Forge
DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
RegistryObject<Item> FOO = ITEMS.register("foo", () -> new Item(props));
// NeoForge
DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
DeferredItem<Item> FOO = ITEMS.registerItem("foo", Item::new, props);
// generic registries:
DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MOD_ID);
DeferredHolder<Fluid, MyFluid> BAR = FLUIDS.register("bar", MyFluid::new);
```
- `RegistryObject<T>` -> `DeferredHolder<R, T>` (or `DeferredItem`/`DeferredBlock`).
  Both implement `Supplier<T>` and `Holder<T>`, so `.get()` still works.
- `ForgeRegistries.X` -> `BuiltInRegistries.X` (vanilla) or `NeoForgeRegistries.X`
  (NeoForge-only registries like fluid types).
- Register the `DeferredRegister` on the **mod** bus from the constructor.

### Events

- Cancellable events implement `net.neoforged.bus.api.ICancellableEvent`; use
  `event.setCanceled(true)` from that interface (base `Event` no longer has it).
- Many events moved/renamed: `RenderGuiOverlayEvent` -> `RenderGuiLayerEvent`,
  GUI overlays register via `RegisterGuiLayersEvent` (was `RegisterGuiOverlaysEvent`).
- `GatherDataEvent` is now `net.neoforged.neoforge.data.event.GatherDataEvent`.

### Capabilities (full rework)

```java
// Forge: LazyOptional + CapabilityManager tokens
// NeoForge: typed BlockCapability / ItemCapability, queried directly
public static final BlockCapability<IItemHandler, Direction> ITEM_HANDLER =
    Capabilities.ItemHandler.BLOCK;       // built-in caps live in Capabilities.*

// register a provider in RegisterCapabilitiesEvent (mod bus):
event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MY_BE.get(),
    (be, side) -> be.getItemHandler(side));

// query (no LazyOptional; returns null if absent):
IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
```
- `LazyOptional<T>` is gone. Capabilities return `@Nullable T`.
- Define custom caps with `BlockCapability.createSided(...)` / `ItemCapability.createVoid(...)`.

### Data attachments (replaces capabilities used for per-entity/-stack DATA)

Forge capabilities were also abused to *store mutable data* on entities/stacks
(e.g. Mantle's `OffhandCooldownTracker` via `AttachCapabilitiesEvent` +
`LazyOptional` + `CapabilityToken`). In NeoForge 1.21 that pattern is a **data
attachment**, not a capability:

```java
DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID);
DeferredHolder<AttachmentType<?>, AttachmentType<OffhandCooldown>> OFFHAND =
    ATTACHMENTS.register("offhand_cooldown", () -> AttachmentType
        .builder(() -> new OffhandCooldown())
        .serialize(OffhandCooldown.CODEC)   // omit for non-persistent
        .build());
// access (no LazyOptional, no AttachCapabilitiesEvent):
OffhandCooldown d = player.getData(OFFHAND);
player.setData(OFFHAND, d);
```
Delete `ICapabilityProvider`, `getCapability`, `AttachCapabilitiesEvent`,
`CapabilityManager.get(new CapabilityToken<>(){})`. Register the
`DeferredRegister` on the mod bus. Note `OffhandCooldownTracker` also depends on
the ported `slimeknights.mantle.network` layer (SwingArmPacket), so do networking
first.

### Networking (full rework)

```java
// Register on the mod bus:
@SubscribeEvent
static void register(RegisterPayloadHandlersEvent event) {
  PayloadRegistrar r = event.registrar("tconstruct").versioned("1");
  r.playToServer(MyPayload.TYPE, MyPayload.STREAM_CODEC, MyPayload::handle);
}
// A packet is a record implementing CustomPacketPayload:
public record MyPayload(int value) implements CustomPacketPayload {
  public static final Type<MyPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("tconstruct", "my"));
  public static final StreamCodec<RegistryFriendlyByteBuf, MyPayload> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.VAR_INT, MyPayload::value, MyPayload::new);
  public Type<MyPayload> type() { return TYPE; }
}
```
- `SimpleChannel`, `NetworkEvent`, `NetworkDirection` are gone. Mantle has its
  own network abstraction (`slimeknights.mantle.network`) - port it to this model
  once, and TConstruct inherits it.

### Fluids

- `FluidStack` is now component-backed: `new FluidStack(fluid, amount)` still
  works; NBT tag access changes to data components. `fluidStack.getTag()` ->
  component accessors.
- `ForgeFlowingFluid` -> `BaseFlowingFluid`; `FluidType` registration via
  `NeoForgeRegistries.FLUID_TYPES`.
- `IFluidHandler` lives in `net.neoforged.neoforge.fluids.capability` and is
  accessed through the capability system above (`Capabilities.FluidHandler.*`).

### Config

- `ForgeConfigSpec` -> `ModConfigSpec` (codemod handles the rename). Registration
  via `container.registerConfig(...)` using the injected `ModContainer`.

### Crafting conditions, recipes, codecs

- Conditions: `ICondition` is now codec-based in
  `net.neoforged.neoforge.common.conditions`; recipe wrappers use
  `WithConditions` + `ConditionalOps`.
- Recipes/ingredients moved to vanilla codecs (`MapCodec`, `StreamCodec`).
  `Ingredient` gained NeoForge extensions in `net.neoforged.neoforge.common.crafting`.
- `DataResult.getOrThrow(Function)` -> `getOrThrow()` (no-arg) or
  `getOrThrow(Function<String, E>)` for a custom exception.

### Data components (the TConstruct mountain - 1.20.5)

Item NBT is gone. Everything Tinkers stores on a tool (material list, modifier
map, stat overrides, tool data) becomes registered `DataComponentType`s.

```java
DeferredRegister.DataComponents COMPONENTS =
    DeferredRegister.createDataComponents(MOD_ID);
DeferredHolder<DataComponentType<?>, DataComponentType<ToolStack>> TOOL_DATA =
    COMPONENTS.registerComponentType("tool_data", b -> b
        .persistent(ToolStack.CODEC)
        .networkSynchronized(ToolStack.STREAM_CODEC));
// read/write:
ToolStack data = stack.get(TOOL_DATA);
stack.set(TOOL_DATA, data.withModifier(...));
```
- Plan the component schema first: `ToolStack`, `MaterialNBT`, `ModifierNBT`,
  `StatsNBT`, `MultipartToolData` all need `Codec` + `StreamCodec`.
- This is the gate for tools, tinker station, part builder, casting, melting,
  modifier logic, and tooltips. Do it before Phase 4 systems.

## Order that keeps things compiling

Mantle first (TConstruct depends on it): `util` -> `registration` -> `data` ->
`network` -> `fluid` -> `recipe` -> `item`/`inventory` -> `block` -> `client` ->
`command`/`loot`/`config` -> `plugin` (JEI). Then re-enable JEI in `build.gradle`.
Then TConstruct in the dependency order from `PORTING.md` Phase 4.
