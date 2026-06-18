package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

/**
 * Custom transform types used for tinkers item rendering.
 *
 * <p>In 1.21 {@link ItemDisplayContext} is no longer a registry; custom values are added via the NeoForge
 * extensible-enum system. Each value is declared as an {@link EnumProxy} referenced from
 * {@code META-INF/enumextensions.json}; the loader instantiates them before mod construction. The public fields
 * here resolve the proxies to the live enum constants so the rest of the codebase keeps using them directly.
 */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  /** No-op retained for the mod constructor call; extension now happens via enumextensions.json before construction. */
  public static void init(IEventBus bus) {}

  /* Enum proxies, referenced by META-INF/enumextensions.json. The constructor parameter is the serialized name of the
     fallback display context (or null for none), matching ItemDisplayContext(int id, String name, String fallback). */
  public static final EnumProxy<ItemDisplayContext> MELTER_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:melter", (Object) null);
  public static final EnumProxy<ItemDisplayContext> TABLE_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:table", (Object) null);
  public static final EnumProxy<ItemDisplayContext> CASTING_TABLE_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:casting_table", "fixed");
  public static final EnumProxy<ItemDisplayContext> CASTING_BASIN_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:casting_basin", (Object) null);
  public static final EnumProxy<ItemDisplayContext> FLUID_CANNON_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:fluid_cannon", "fixed");
  public static final EnumProxy<ItemDisplayContext> THROWN_PROXY = new EnumProxy<>(ItemDisplayContext.class, "tconstruct:thrown", "fixed");

  /** Used by the melter and smeltery for display of items its melting */
  public static final ItemDisplayContext MELTER = MELTER_PROXY.getValue();
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static final ItemDisplayContext TABLE = TABLE_PROXY.getValue();
  /** Used by the casting table for item rendering */
  public static final ItemDisplayContext CASTING_TABLE = CASTING_TABLE_PROXY.getValue();
  /** Used by the casting basin for item rendering */
  public static final ItemDisplayContext CASTING_BASIN = CASTING_BASIN_PROXY.getValue();
  /** Used by the fluid cannon for display of the item in front */
  public static final ItemDisplayContext FLUID_CANNON = FLUID_CANNON_PROXY.getValue();
  /** Used by throwing to allow adjusting the tool position */
  public static final ItemDisplayContext THROWN = THROWN_PROXY.getValue();
}
