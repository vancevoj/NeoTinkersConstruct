package slimeknights.tconstruct.tools.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

/** Builder for modifier sorting recipes */
@RequiredArgsConstructor(staticName = "sorting")
public class ModifierSortingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierSortingRecipeBuilder> {

  @Override
  public void save(RecipeOutput output) {
    save(output, BuiltInRegistries.ITEM.getKey((inputs.get(0).getMatchingStacks().get(0).getItem())));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one ingredient");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifiers");
    output.accept(id, new ModifierSortingRecipe(id, inputs), advancement);
  }
}
