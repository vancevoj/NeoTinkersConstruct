package slimeknights.tconstruct.smeltery.network;

import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

/**
 * Packet sent when a fluid is clicked in the smeltery UI
 */
@AllArgsConstructor
public class SmelteryFluidClickedPacket implements IThreadsafePacket {
  public static final Type<SmelteryFluidClickedPacket> TYPE = new Type<>(TConstruct.getResource("smeltery_fluid_clicked"));
  public static final StreamCodec<RegistryFriendlyByteBuf,SmelteryFluidClickedPacket> STREAM_CODEC = ISimplePacket.codec(SmelteryFluidClickedPacket::new);

  private final int index;

  public SmelteryFluidClickedPacket(FriendlyByteBuf buffer) {
    index = buffer.readVarInt();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(index);
  }

  @Override
  public Type<SmelteryFluidClickedPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    if (context.player() instanceof ServerPlayer sender && !sender.isSpectator()) {
      AbstractContainerMenu container = sender.containerMenu;
      if (container instanceof BaseContainerMenu<?> base && base.getTile() instanceof ISmelteryTankHandler tank) {
        tank.getTank().moveFluidToBottom(index);
      }
    }
  }
}
