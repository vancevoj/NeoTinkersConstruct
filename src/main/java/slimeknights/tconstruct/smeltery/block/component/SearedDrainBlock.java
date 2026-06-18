package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.DrainBlockEntity;

/** Extenson to include interaction behavior */
public class SearedDrainBlock extends RetexturedOrientableSmelteryBlock {
  public SearedDrainBlock(Properties properties) {
    super(properties, DrainBlockEntity::new);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, InteractionHand.MAIN_HAND, hit.getDirection(), state.getValue(FACING).getOpposite())) {
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
