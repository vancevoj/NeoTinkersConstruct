package slimeknights.tconstruct.plugin.jei.material;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;

import java.util.List;
import java.util.stream.IntStream;

/** Logic to show {@link ShapedMaterialsRecipe} in JEI */
public class ShapedMaterialsExtension extends MaterialsCraftingExtension<ShapedMaterialsRecipe> {
  /** {@return Instance of the shaped extension} */
  public static ShapedMaterialsExtension create() {
    return new ShapedMaterialsExtension();
  }

  @Override
  protected int[] getMaterialSlots(ShapedMaterialsRecipe recipe, Ingredient firstPart) {
    List<Ingredient> inputs = recipe.getIngredients();
    return IntStream.range(0, inputs.size()).filter(i -> inputs.get(i) == firstPart).toArray();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedMaterialsRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedMaterialsRecipe> holder) {
    return holder.value().getHeight();
  }
}
