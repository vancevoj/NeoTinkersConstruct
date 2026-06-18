package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

/** Ingredient that contains another ingredient nested inside */
public abstract class NestedIngredient implements ICustomIngredient {
  protected final Ingredient nested;

  protected NestedIngredient(Ingredient nested) {
    this.nested = nested;
  }


  /* Defer to nested */

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return nested.test(stack);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return Arrays.stream(nested.getItems());
  }

  @Override
  public boolean isSimple() {
    return nested.isSimple();
  }
}
