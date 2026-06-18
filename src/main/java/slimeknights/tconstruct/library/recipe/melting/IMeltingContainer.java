package slimeknights.tconstruct.library.recipe.melting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

/** Interface for melting inventories */
public interface IMeltingContainer extends ISingleStackContainer, RecipeInput {
  /**
   * Gets the logic to boost an ore with the ore rate
   * @return  Nuggets per ore
   */
  IOreRate getOreRate();

  /** Resolves the clash between {@link RecipeInput#getItem(int)} and the legacy container */
  @Override
  default ItemStack getItem(int index) {
    return index == 0 ? getStack() : ItemStack.EMPTY;
  }

  /** Implements {@link RecipeInput#size()} using the legacy container size */
  @Override
  default int size() {
    return getContainerSize();
  }

  /** Resolves the clash between {@link RecipeInput#isEmpty()} and the legacy container */
  @Override
  default boolean isEmpty() {
    return getStack().isEmpty();
  }

  /** Ore rate logic in a melting container */
  interface IOreRate {
    /** Boosts the given integer by the rate */
    int applyOreBoost(OreRateType rate, int amount);

    /** Boosts the given fluid stack by the rate */
    default FluidStack applyOreBoost(OreRateType rate, FluidStack fluid, boolean forceCopy) {
      if (rate == OreRateType.DEFAULT || rate == OreRateType.NONE) {
        return forceCopy ? fluid.copy() : fluid;
      }
      return fluid.copyWithAmount(applyOreBoost(rate, fluid.getAmount()));
    }
  }

  /** Ore rate options */
  enum OreRateType {
    /** No boost */
    NONE,
    /** Metal boost, works with divisions of 9 */
    METAL,
    /** Gem boost, works with divisions of 4 */
    GEM,
    /** Default value, used in place of null to indicate the value should be fetched from another source. If o default exits acts as NONE. */
    DEFAULT;

    /** Returns the passed argument if this is default, else return self */
    public OreRateType orElse(OreRateType type) {
      if (this == DEFAULT) {
        return type;
      }
      return this;
    }
  }
}
