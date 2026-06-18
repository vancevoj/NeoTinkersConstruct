package slimeknights.tconstruct.gadgets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import slimeknights.mantle.inventory.EmptyItemHandler;

import javax.annotation.Nullable;

public class DropperRailBlock extends RailBlock {

  public DropperRailBlock(Properties properties) {
    super(properties);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
    super.entityInside(state, world, pos, entity);
    if (!(entity instanceof AbstractMinecart cart) || !(cart instanceof Hopper)) {
      return;
    }
    // NeoForge 1.21: capabilities return null when absent rather than an empty optional
    if (Capabilities.ItemHandler.ENTITY.getCapability(cart, null) == null) {
      return;
    }
    BlockEntity tileEntity = world.getBlockEntity(pos.below());
    if (tileEntity == null || world.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.DOWN) == null) {
      return;
    }

    IItemHandler itemHandlerCart = orEmpty(Capabilities.ItemHandler.ENTITY.getCapability(cart, null));
    IItemHandler itemHandlerTE = orEmpty(world.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP));

    for (int i = 0; i < itemHandlerCart.getSlots(); i++) {
      ItemStack itemStack = itemHandlerCart.extractItem(i, 1, true);
      if (itemStack.isEmpty()) {
        continue;
      }
      if (ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, true).isEmpty()) {
        itemStack = itemHandlerCart.extractItem(i, 1, false);
        ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, false);
        break;
      }
    }
  }

  /** Returns the given handler, or the empty handler if null */
  private static IItemHandler orEmpty(@Nullable IItemHandler handler) {
    return handler == null ? EmptyItemHandler.INSTANCE : handler;
  }

}
