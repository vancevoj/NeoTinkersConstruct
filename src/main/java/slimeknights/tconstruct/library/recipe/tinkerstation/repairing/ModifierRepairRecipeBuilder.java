package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

/** Builds a recipe to repair a tool using a modifier */
@RequiredArgsConstructor(staticName = "repair")
public class ModifierRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierRepairRecipeBuilder> {
  private final ModifierId modifier;
  private final Ingredient ingredient;
  private final int repairAmount;

  public static ModifierRepairRecipeBuilder repair(LazyModifier modifier, Ingredient ingredient, int repairAmount) {
    return repair(modifier.getId(), ingredient, repairAmount);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, modifier);
  }

  /** Builds the recipe for the crafting table using a repair kit */
  public ModifierRepairRecipeBuilder buildCraftingTable(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancementId = buildOptionalAdvancement(id, "tinker_station");
    consumer.accept(id, new ModifierRepairCraftingRecipe(id, modifier, ingredient, repairAmount), advancementId);
    return this;
  }

  @Override
  public void save(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancementId = buildOptionalAdvancement(id, "tinker_station");
    consumer.accept(id, new ModifierRepairTinkerStationRecipe(id, modifier, ingredient, repairAmount), advancementId);
  }
}
