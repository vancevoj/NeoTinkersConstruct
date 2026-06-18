package slimeknights.tconstruct.smeltery.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/** Sent to clients to activate the faucet animation clientside **/
public class FaucetActivationPacket extends FluidUpdatePacket {
  public static final Type<FaucetActivationPacket> TYPE = new Type<>(TConstruct.getResource("faucet_activation"));
  public static final StreamCodec<RegistryFriendlyByteBuf,FaucetActivationPacket> STREAM_CODEC = ISimplePacket.codec(FaucetActivationPacket::new);

  private final boolean isPouring;
  public FaucetActivationPacket(BlockPos pos, FluidStack fluid, boolean isPouring) {
    super(pos, fluid);
    this.isPouring = isPouring;
  }

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    super(buffer);
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf packetBuffer) {
    super.encode(packetBuffer);
    packetBuffer.writeBoolean(isPouring);
  }

  @Override
  public Type<FaucetActivationPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(FaucetActivationPacket packet) {
      assert Minecraft.getInstance().level != null;
      BlockEntity te = Minecraft.getInstance().level.getBlockEntity(packet.pos);
      if (te instanceof FaucetBlockEntity) {
        ((FaucetBlockEntity) te).onActivationPacket(packet.fluid, packet.isPouring);
      }
    }
  }
}
