package slimeknights.tconstruct.library.recipe.material;

import net.minecraft.world.item.crafting.RecipeInput;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

/**
 * Single-stack recipe input for material recipes. Bridges the legacy Mantle {@link ISingleStackContainer} to the
 * vanilla {@link RecipeInput} required by recipe type bounds in 1.21.
 */
public interface IMaterialRecipeContainer extends ISingleStackContainer, RecipeInput {
  /** Implements {@link RecipeInput#size()} using the legacy container size */
  @Override
  default int size() {
    return getContainerSize();
  }
}
