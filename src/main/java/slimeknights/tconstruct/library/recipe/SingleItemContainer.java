package slimeknights.tconstruct.library.recipe;

import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

/** Simple class for an inventory containing just one item */
public class SingleItemContainer implements ISingleStackContainer {
  private ItemStack stack = ItemStack.EMPTY;

  @Override
  public ItemStack getStack() {
    return stack;
  }

  public void setStack(ItemStack stack) {
    this.stack = stack;
  }
}
