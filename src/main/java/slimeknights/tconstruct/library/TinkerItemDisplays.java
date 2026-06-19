package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.IEventBus;

import java.util.Objects;

/**
 * Custom transform types used for tinkers item rendering.
 *
 * <p>In 1.21 {@link ItemDisplayContext} is no longer a registry; custom values are added via the NeoForge
 * extensible-enum system. The {@link net.neoforged.fml.common.asm.enumextension.EnumProxy} instances live in
 * {@link TinkerItemDisplayProxies} (referenced from {@code META-INF/enumextensions.json}); the loader assigns
 * them their live enum constants while {@code ItemDisplayContext} initializes. The fields here resolve those
 * proxies to the constants so the rest of the codebase keeps using them directly.
 *
 * <p>The proxies are deliberately in a SEPARATE class: the loader reads the proxy fields reentrantly during
 * {@code ItemDisplayContext} init, so if the proxies and these resolved values shared a class, resolving
 * (calling {@code getValue()}) would run mid-init before the loader assigned values and throw. That is exactly
 * what happened when another mod (Twilight Forest) also extended the enum and forced its init first.
 */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  static {
    // Ensure ItemDisplayContext is fully initialized (so the extension loader has already assigned our proxy
    // values) before we call getValue() below, covering the case where game code touches this class before
    // anything else has initialized the enum.
    Objects.requireNonNull(ItemDisplayContext.NONE);
  }

  /** No-op retained for the mod constructor call; extension happens via enumextensions.json before construction. */
  public static void init(IEventBus bus) {}

  /** Used by the melter and smeltery for display of items its melting */
  public static final ItemDisplayContext MELTER = TinkerItemDisplayProxies.MELTER_PROXY.getValue();
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static final ItemDisplayContext TABLE = TinkerItemDisplayProxies.TABLE_PROXY.getValue();
  /** Used by the casting table for item rendering */
  public static final ItemDisplayContext CASTING_TABLE = TinkerItemDisplayProxies.CASTING_TABLE_PROXY.getValue();
  /** Used by the casting basin for item rendering */
  public static final ItemDisplayContext CASTING_BASIN = TinkerItemDisplayProxies.CASTING_BASIN_PROXY.getValue();
  /** Used by the fluid cannon for display of the item in front */
  public static final ItemDisplayContext FLUID_CANNON = TinkerItemDisplayProxies.FLUID_CANNON_PROXY.getValue();
  /** Used by throwing to allow adjusting the tool position */
  public static final ItemDisplayContext THROWN = TinkerItemDisplayProxies.THROWN_PROXY.getValue();
}
