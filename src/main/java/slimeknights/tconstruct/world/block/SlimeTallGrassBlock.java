package slimeknights.tconstruct.world.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.IShearable;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SlimeTallGrassBlock extends BushBlock implements IShearable {
  public static final MapCodec<SlimeTallGrassBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    propertiesCodec(),
    StringRepresentable.fromEnum(FoliageType::values).fieldOf("foliage").forGetter(SlimeTallGrassBlock::getFoliageType)
  ).apply(inst, SlimeTallGrassBlock::new));

  private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

  @Getter
  private final FoliageType foliageType;
  public SlimeTallGrassBlock(Properties properties, FoliageType foliageType) {
    super(properties);
    this.foliageType = foliageType;
  }

  @Override
  protected MapCodec<? extends BushBlock> codec() {
    return CODEC;
  }

  @Deprecated
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  /* Forge/MC callbacks */
  @Nonnull
  @Override
  public List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level world, BlockPos pos) {
    return Lists.newArrayList(new ItemStack(this, 1));
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter worldIn, BlockPos pos) {
    Block block = state.getBlock();
    return TinkerWorld.slimeDirt.contains(block) || TinkerWorld.vanillaSlimeGrass.contains(block) || TinkerWorld.earthSlimeGrass.contains(block) || TinkerWorld.skySlimeGrass.contains(block) || TinkerWorld.enderSlimeGrass.contains(block) || TinkerWorld.ichorSlimeGrass.contains(block);
  }
}
