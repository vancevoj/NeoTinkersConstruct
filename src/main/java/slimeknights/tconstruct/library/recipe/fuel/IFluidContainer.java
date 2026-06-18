package slimeknights.tconstruct.library.recipe.fuel;

import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.container.IEmptyContainer;

/**
 * Inventory containing just a single fluid
 */
public interface IFluidContainer extends IEmptyContainer, RecipeInput {
  /**
   * Gets the fluid contained in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();

  /** Implements {@link RecipeInput#size()} using the legacy container size */
  @Override
  default int size() {
    return getContainerSize();
  }
}
