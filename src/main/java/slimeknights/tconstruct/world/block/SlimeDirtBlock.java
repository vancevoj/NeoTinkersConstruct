package slimeknights.tconstruct.world.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.common.util.TriState;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SlimeDirtBlock extends Block {

  public SlimeDirtBlock(Properties properties) {
    super(properties);
  }

  @Override
  public TriState canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction facing, BlockState plant) {
    // can sustain both slimeplants and normal plants (PlantType is gone in 1.21, so allow any bush-style plant)
    if (plant.getBlock() instanceof BushBlock) {
      return TriState.TRUE;
    }
    return TriState.DEFAULT;
  }
}
