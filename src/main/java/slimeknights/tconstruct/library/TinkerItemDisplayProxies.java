package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

/**
 * Holds ONLY the {@link EnumProxy} instances for Tinkers' custom {@link ItemDisplayContext} values.
 *
 * <p>These fields are referenced from {@code META-INF/enumextensions.json}. While initializing
 * {@link ItemDisplayContext}, the enum-extension loader reads each field and assigns the proxy its live
 * enum constant.
 *
 * <p>CRITICAL: this class must contain ONLY proxies and must NEVER call {@link EnumProxy#getValue()}.
 * The loader triggers this class's static init reentrantly while {@code ItemDisplayContext} is still
 * initializing (notably when another mod such as Twilight Forest also extends the enum and forces its
 * init first); at that point the proxies have no value yet. Value resolution lives in
 * {@link TinkerItemDisplays}, which the loader never touches, so it only runs after init completes.
 */
public class TinkerItemDisplayProxies {
  private TinkerItemDisplayProxies() {}

  /* Params match the modded ItemDisplayContext constructor (int id, String name, String fallback): the int id is
     passed as -1 (the loader replaces it with the assigned ordinal), name is the serialized id, fallback is the
     ENUM CONSTANT name of the vanilla context to fall back to (resolved via ItemDisplayContext.valueOf). */
  public static final EnumProxy<ItemDisplayContext> MELTER_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:melter", "NONE");
  public static final EnumProxy<ItemDisplayContext> TABLE_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:table", "NONE");
  public static final EnumProxy<ItemDisplayContext> CASTING_TABLE_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:casting_table", "FIXED");
  public static final EnumProxy<ItemDisplayContext> CASTING_BASIN_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:casting_basin", "NONE");
  public static final EnumProxy<ItemDisplayContext> FLUID_CANNON_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:fluid_cannon", "FIXED");
  public static final EnumProxy<ItemDisplayContext> THROWN_PROXY = new EnumProxy<>(ItemDisplayContext.class, -1, "tconstruct:thrown", "FIXED");
}
