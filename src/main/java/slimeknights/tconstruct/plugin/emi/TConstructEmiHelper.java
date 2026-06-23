package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers used by the Tinkers' Construct EMI recipe classes. Centralizes the conversion between NeoForge
 * {@link FluidStack}s (measured in millibuckets) and EMI {@link EmiStack}s (measured in droplets, where 1mb = 81
 * droplets).
 */
public final class TConstructEmiHelper {
  /** Number of EMI droplets per millibucket */
  public static final long DROPLETS_PER_MB = 81L;

  private TConstructEmiHelper() {}

  /**
   * Converts a NeoForge {@link FluidStack} into an EMI {@link EmiStack}, applying the millibucket to droplet conversion.
   * @param stack  fluid stack to convert
   * @return  EMI stack with the droplet amount
   */
  public static EmiStack fluid(FluidStack stack) {
    return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount() * DROPLETS_PER_MB);
  }

  /**
   * Folds a list of fluid variants into a single cycling EMI ingredient.
   * @param stacks  fluid variants
   * @return  EMI ingredient cycling through each variant, or {@link EmiStack#EMPTY} if none are valid
   */
  public static EmiIngredient fluidIngredient(List<FluidStack> stacks) {
    List<EmiIngredient> ingredients = new ArrayList<>(stacks.size());
    for (FluidStack stack : stacks) {
      if (!stack.isEmpty()) {
        ingredients.add(fluid(stack));
      }
    }
    if (ingredients.isEmpty()) {
      return EmiStack.EMPTY;
    }
    return EmiIngredient.of(ingredients);
  }
}
