package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;

/**
 * EMI recipe categories for Tinkers' Construct. Each constant mirrors a JEI category. Icons match the JEI category
 * icons; see {@link slimeknights.tconstruct.plugin.jei.JEIPlugin}.
 * <p>
 * Each category overrides {@link EmiRecipeCategory#getName()} to reuse the existing {@code jei.tconstruct.*} title
 * (EMI's default would look up {@code emi.category.tconstruct.<path>}, which Tinkers does not define, leaving titles
 * showing as raw ids — issue #6).
 */
public final class TConstructEmiCategories {
  private TConstructEmiCategories() {}

  /** Builds a category that displays the given existing JEI title translation key. */
  private static EmiRecipeCategory category(String path, EmiRenderable icon, String jeiKey) {
    Component title = Component.translatable(TConstruct.makeTranslationKey("jei", jeiKey));
    return new EmiRecipeCategory(TConstruct.getResource(path), icon) {
      @Override
      public Component getName() {
        return title;
      }
    };
  }

  // casting
  public static final EmiRecipeCategory CASTING_BASIN = category("casting_basin", EmiStack.of(TinkerSmeltery.searedBasin), "casting.basin");
  public static final EmiRecipeCategory CASTING_TABLE = category("casting_table", EmiStack.of(TinkerSmeltery.searedTable), "casting.table");
  public static final EmiRecipeCategory MOLDING = category("molding", EmiStack.of(TinkerSmeltery.blankSandCast), "molding.title");

  // melting and alloying
  public static final EmiRecipeCategory MELTING = category("melting", EmiStack.of(TinkerSmeltery.searedMelter), "melting.title");
  public static final EmiRecipeCategory FOUNDRY = category("foundry", EmiStack.of(TinkerSmeltery.foundryController), "foundry.title");
  public static final EmiRecipeCategory ALLOY = category("alloy", EmiStack.of(TinkerSmeltery.scorchedAlloyer), "alloy.title");
  public static final EmiRecipeCategory ENTITY_MELTING = category("entity_melting", EmiStack.of(TinkerSmeltery.smelteryController), "entity_melting.title");

  // tinker station
  public static final EmiRecipeCategory MODIFIERS = category("modifiers", EmiStack.of(CreativeSlotItem.withSlot(new ItemStack(TinkerModifiers.creativeSlotItem), SlotType.UPGRADE)), "modifiers.title");
  public static final EmiRecipeCategory MODIFIER_WORKTABLE = category("modifier_worktable", EmiStack.of(TinkerTables.modifierWorktable), "modifier_worktable.title");
  public static final EmiRecipeCategory TOOL_BUILDING = category("tool_building", EmiStack.of(TinkerTools.pickaxe.get().getRenderTool()), "tinkering.tool_building");
  public static final EmiRecipeCategory SEVERING = category("severing", EmiStack.of(TinkerTools.cleaver.get().getRenderTool()), "severing.title");

  // part builder
  public static final EmiRecipeCategory PART_BUILDER = category("part_builder", EmiStack.of(TinkerTables.partBuilder), "part_builder.title");
}
