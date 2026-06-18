package slimeknights.tconstruct.smeltery.network;

import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent whenever the contents of the smeltery tank change
 */
@AllArgsConstructor
public class SmelteryTankUpdatePacket implements IThreadsafePacket {
  public static final Type<SmelteryTankUpdatePacket> TYPE = new Type<>(TConstruct.getResource("smeltery_tank_update"));
  public static final StreamCodec<RegistryFriendlyByteBuf,SmelteryTankUpdatePacket> STREAM_CODEC = ISimplePacket.codec(SmelteryTankUpdatePacket::new);

  private final BlockPos pos;
  private final List<FluidStack> fluids;

  public SmelteryTankUpdatePacket(FriendlyByteBuf buffer) {
    pos = buffer.readBlockPos();
    int size = buffer.readVarInt();
    fluids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeVarInt(fluids.size());
    for (FluidStack fluid : fluids) {
      FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, fluid);
    }
  }

  @Override
  public Type<SmelteryTankUpdatePacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  private static class HandleClient {
    private static void handle(SmelteryTankUpdatePacket packet) {
      BlockEntityHelper.get(ISmelteryTankHandler.class, Minecraft.getInstance().level, packet.pos).ifPresent(te -> te.updateFluidsFromPacket(packet.fluids));
    }
  }
}
