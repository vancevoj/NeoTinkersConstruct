package slimeknights.tconstruct.library;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import slimeknights.tconstruct.TConstruct;

import java.util.Locale;

/** Custom transform types used for tinkers item rendering */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  public static void init(IEventBus bus) {
    bus.addListener(TinkerItemDisplays::registerDisplay);
  }

  /** Used by the melter and smeltery for display of items its melting */
  public static ItemDisplayContext MELTER = create("melter", ItemDisplayContext.NONE);
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static ItemDisplayContext TABLE = create("table", ItemDisplayContext.NONE);
  /** Used by the casting table for item rendering */
  public static ItemDisplayContext CASTING_TABLE = create("casting_table", ItemDisplayContext.FIXED);
  /** Used by the casting basin for item rendering */
  public static ItemDisplayContext CASTING_BASIN = create("casting_basin", ItemDisplayContext.NONE);
  /** Used by the fluid cannon for display of the item in front */
  public static ItemDisplayContext FLUID_CANNON = create("fluid_cannon", ItemDisplayContext.FIXED);
  /** Used by throwing to allow adjusting the tool position */
  public static ItemDisplayContext THROWN = create("thrown", ItemDisplayContext.FIXED);

  /** Creates a transform type */
  private static ItemDisplayContext create(String name, ItemDisplayContext fallback) {
    String key = "TCONSTRUCT_" + name.toUpperCase(Locale.ROOT);
    if (fallback == ItemDisplayContext.NONE) {
      return ItemDisplayContext.create(key, TConstruct.getResource(name), null);
    }
    return ItemDisplayContext.create(key, TConstruct.getResource(name), fallback);
  }

  /** Registers all item display types */
  private static void registerDisplay(RegisterEvent event) {
    event.register(NeoForgeRegistries.Keys.DISPLAY_CONTEXTS, helper -> {
      register(helper, MELTER);
      register(helper, TABLE);
      register(helper, CASTING_TABLE);
      register(helper, CASTING_BASIN);
      register(helper, FLUID_CANNON);
      register(helper, THROWN);
    });
  }

  /** Registers a display type */
  private static void register(RegisterEvent.RegisterHelper<ItemDisplayContext> registry, ItemDisplayContext context) {
    registry.register(ResourceLocation.parse(context.getSerializedName()), context);
  }
}
