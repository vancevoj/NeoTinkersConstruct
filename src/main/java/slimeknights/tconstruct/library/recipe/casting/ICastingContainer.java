package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

import javax.annotation.Nullable;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends ISingleStackContainer, RecipeInput {
  @Override
  default int size() {
    return 1;
  }

  // disambiguate getItem(int): ISingleStackContainer (via Container) supplies a default, RecipeInput declares it abstract
  @Override
  default ItemStack getItem(int index) {
    return index == 0 ? getStack() : ItemStack.EMPTY;
  }

  // disambiguate the unrelated isEmpty() defaults inherited from Container (via ISingleStackContainer) and RecipeInput
  @Override
  default boolean isEmpty() {
    return getStack().isEmpty();
  }

  /**
   * Gets the contained fluid in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();

  /**
   * Gets the NBT for the contained fluid
   * @return  Fluid's NBT
   */
  @Nullable
  default CompoundTag getFluidTag() {
    return null;
  }

  /**
   * Gets the full component-backed fluid stack in this inventory. Needed in 1.21 as potion data moved from fluid NBT
   * to {@code DataComponents.POTION_CONTENTS}.
   * @return  Contained fluid stack, or {@link FluidStack#EMPTY} if unavailable
   */
  default FluidStack getFluidStack() {
    return FluidStack.EMPTY;
  }
}
