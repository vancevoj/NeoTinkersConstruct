package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
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
 */
public final class TConstructEmiCategories {
  private TConstructEmiCategories() {}

  // casting
  public static final EmiRecipeCategory CASTING_BASIN = new EmiRecipeCategory(TConstruct.getResource("casting_basin"), EmiStack.of(TinkerSmeltery.searedBasin));
  public static final EmiRecipeCategory CASTING_TABLE = new EmiRecipeCategory(TConstruct.getResource("casting_table"), EmiStack.of(TinkerSmeltery.searedTable));
  public static final EmiRecipeCategory MOLDING = new EmiRecipeCategory(TConstruct.getResource("molding"), EmiStack.of(TinkerSmeltery.blankSandCast));

  // melting and alloying
  public static final EmiRecipeCategory MELTING = new EmiRecipeCategory(TConstruct.getResource("melting"), EmiStack.of(TinkerSmeltery.searedMelter));
  public static final EmiRecipeCategory FOUNDRY = new EmiRecipeCategory(TConstruct.getResource("foundry"), EmiStack.of(TinkerSmeltery.foundryController));
  public static final EmiRecipeCategory ALLOY = new EmiRecipeCategory(TConstruct.getResource("alloy"), EmiStack.of(TinkerSmeltery.scorchedAlloyer));
  public static final EmiRecipeCategory ENTITY_MELTING = new EmiRecipeCategory(TConstruct.getResource("entity_melting"), EmiStack.of(TinkerSmeltery.smelteryController));

  // tinker station
  public static final EmiRecipeCategory MODIFIERS = new EmiRecipeCategory(TConstruct.getResource("modifiers"), EmiStack.of(CreativeSlotItem.withSlot(new ItemStack(TinkerModifiers.creativeSlotItem), SlotType.UPGRADE)));
  public static final EmiRecipeCategory MODIFIER_WORKTABLE = new EmiRecipeCategory(TConstruct.getResource("modifier_worktable"), EmiStack.of(TinkerTables.modifierWorktable));
  public static final EmiRecipeCategory TOOL_BUILDING = new EmiRecipeCategory(TConstruct.getResource("tool_building"), EmiStack.of(TinkerTools.pickaxe.get().getRenderTool()));
  public static final EmiRecipeCategory SEVERING = new EmiRecipeCategory(TConstruct.getResource("severing"), EmiStack.of(TinkerTools.cleaver.get().getRenderTool()));

  // part builder
  public static final EmiRecipeCategory PART_BUILDER = new EmiRecipeCategory(TConstruct.getResource("part_builder"), EmiStack.of(TinkerTables.partBuilder));
}
