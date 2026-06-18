package slimeknights.tconstruct.shared.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/** Particle data for a fluid particle */
@RequiredArgsConstructor
public class FluidParticleData implements ParticleOptions {
  @Getter
  private final ParticleType<FluidParticleData> type;
  @Getter
  private final FluidStack fluid;

  @Override
  public ParticleType<FluidParticleData> getType() {
    return type;
  }

  /** Particle type for a fluid particle */
  public static class Type extends ParticleType<FluidParticleData> {
    private final MapCodec<FluidParticleData> codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
      FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(data -> data.fluid)
    ).apply(inst, fluid -> new FluidParticleData(this, fluid)));
    private final StreamCodec<RegistryFriendlyByteBuf, FluidParticleData> streamCodec = FluidStack.OPTIONAL_STREAM_CODEC.map(
      fluid -> new FluidParticleData(this, fluid), data -> data.fluid);

    public Type() {
      super(false);
    }

    @Override
    public MapCodec<FluidParticleData> codec() {
      return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FluidParticleData> streamCodec() {
      return streamCodec;
    }
  }
}
