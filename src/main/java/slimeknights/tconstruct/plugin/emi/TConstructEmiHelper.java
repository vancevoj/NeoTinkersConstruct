package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers used by the Tinkers' Construct EMI recipe classes. Wraps NeoForge {@link FluidStack}s as EMI
 * {@link EmiStack}s.
 * <p>
 * NOTE: EMI on NeoForge measures fluids in raw NeoForge millibuckets (its own {@code NeoForgeEmiStack.of(FluidStack)}
 * stores {@code FluidStack.getAmount()} verbatim) — NOT in Fabric "droplets". Multiplying by 81 here made every
 * displayed fluid amount 81x too large and broke tank fill ratios against the mB-based capacities (issue #6), so the
 * amount is passed through unchanged.
 */
public final class TConstructEmiHelper {

  private TConstructEmiHelper() {}

  /**
   * Wraps a NeoForge {@link FluidStack} as an EMI {@link EmiStack}, keeping the raw millibucket amount.
   * @param stack  fluid stack to convert
   * @return  EMI stack with the same millibucket amount
   */
  public static EmiStack fluid(FluidStack stack) {
    return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount());
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
