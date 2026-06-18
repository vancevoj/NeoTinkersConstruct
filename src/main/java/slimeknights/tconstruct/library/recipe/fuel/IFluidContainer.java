package slimeknights.tconstruct.library.recipe.fuel;

import net.minecraft.world.item.ItemStack;
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

  /** Implements {@link RecipeInput#getItem(int)}; this container holds no items */
  @Override
  default ItemStack getItem(int index) {
    return ItemStack.EMPTY;
  }

  /** Resolves the clash between {@link RecipeInput#isEmpty()} and the legacy container */
  @Override
  default boolean isEmpty() {
    return true;
  }
}
