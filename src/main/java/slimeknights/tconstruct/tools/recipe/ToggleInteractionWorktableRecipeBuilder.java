package slimeknights.tconstruct.tools.recipe;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;
import slimeknights.tconstruct.library.recipe.worktable.AbstractWorktableRecipe;

/** Builder for {@link ToggleInteractionWorktableRecipe} */
@Accessors(fluent = true)
@Setter
@NoArgsConstructor(staticName = "builder")
public class ToggleInteractionWorktableRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ToggleInteractionWorktableRecipeBuilder> {
  private Ingredient tools = AbstractWorktableRecipe.DEFAULT_TOOLS;

  @Override
  public void save(RecipeOutput output) {
    save(output, Loadables.ITEM.getKey(tools.getItems()[0].getItem()));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one ingredient");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifiers");
    output.accept(id, new ToggleInteractionWorktableRecipe(id, tools, inputs), advancement);
  }
}
