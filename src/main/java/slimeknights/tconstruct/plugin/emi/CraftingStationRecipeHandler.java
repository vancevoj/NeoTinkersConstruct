package slimeknights.tconstruct.plugin.emi;

import com.google.common.collect.Lists;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EMI "+" recipe transfer handler for the Crafting Station, letting vanilla crafting recipes auto-fill into its
 * 3x3 grid exactly like the vanilla crafting table. Without this, EMI has no handler for the crafting station's
 * menu type and shows "Current workstation does not support recipe". Mirrors EMI's own
 * {@code dev.emi.emi.handler.CraftingRecipeHandler} and the JEI-side
 * {@link slimeknights.tconstruct.plugin.jei.transfer.CraftingStationTransferInfo}.
 *
 * Slot layout of {@link CraftingStationContainerMenu}: 0-8 crafting grid, 9 result,
 * 10..(size-36) optional side chest inventory, final 36 the player inventory.
 */
public class CraftingStationRecipeHandler implements StandardRecipeHandler<CraftingStationContainerMenu> {
  @Override
  public List<Slot> getInputSources(CraftingStationContainerMenu handler) {
    List<Slot> list = Lists.newArrayList();
    // the crafting grid itself, so EMI may rearrange items already placed
    for (int i = 0; i < 9; i++) {
      list.add(handler.getSlot(i));
    }
    // everything from the side chest inventory through the player inventory; skips the result slot (9)
    int size = handler.slots.size();
    for (int i = 10; i < size; i++) {
      list.add(handler.getSlot(i));
    }
    return list;
  }

  @Override
  public List<Slot> getCraftingSlots(CraftingStationContainerMenu handler) {
    List<Slot> list = Lists.newArrayList();
    for (int i = 0; i < 9; i++) {
      list.add(handler.getSlot(i));
    }
    return list;
  }

  @Nullable
  @Override
  public Slot getOutputSlot(CraftingStationContainerMenu handler) {
    return handler.getSlot(9);
  }

  @Override
  public boolean supportsRecipe(EmiRecipe recipe) {
    return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
  }
}
