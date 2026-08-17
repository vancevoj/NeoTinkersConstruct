package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.mantle.fluid.texture.ClientInvertedFluidType;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.function.Consumer;

/** Fluid type that does not drown slimes, so slimes and magma cubes can live in their own liquid. */
public class SlimeFluidType extends TextureFluidType {
  public SlimeFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public boolean canDrownIn(LivingEntity entity) {
    return !entity.getType().is(TinkerTags.EntityTypes.SLIMES);
  }

  /** Slime fluid type with the flipped in-world texture, used by ichor. */
  public static class Inverted extends SlimeFluidType {
    public Inverted(Properties properties) {
      super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(new ClientInvertedFluidType(this));
    }
  }
}
