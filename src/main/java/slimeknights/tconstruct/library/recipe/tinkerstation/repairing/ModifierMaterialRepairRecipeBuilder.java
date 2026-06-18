package slimeknights.tconstruct.library.recipe.tinkerstation.repairing;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;

/** @deprecated use {@link slimeknights.tconstruct.library.modifiers.modules.behavior.MaterialRepairModule} */
@Deprecated(forRemoval = true)
@RequiredArgsConstructor(staticName = "repair")
public class ModifierMaterialRepairRecipeBuilder extends AbstractRecipeBuilder<ModifierMaterialRepairRecipeBuilder> {
  private final ModifierId modifier;
  private final MaterialId material;
  private final MaterialStatsId statType;

  public static ModifierMaterialRepairRecipeBuilder repair(LazyModifier modifier, MaterialId material, MaterialStatsId statType) {
    return repair(modifier.getId(), material, statType);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, modifier);
  }

  /** Builds the recipe for the crafting table using a repair kit */
  @SuppressWarnings("removal")
  public ModifierMaterialRepairRecipeBuilder saveCraftingTable(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancementId = buildOptionalAdvancement(id, "tinker_station");
    consumer.accept(id, new ModifierMaterialRepairKitRecipe(id, modifier, material, statType), advancementId);
    return this;
  }

  @SuppressWarnings("removal")
  @Override
  public void save(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancementId = buildOptionalAdvancement(id, "tinker_station");
    consumer.accept(id, new ModifierMaterialRepairRecipe(id, modifier, material, statType), advancementId);
  }
}
