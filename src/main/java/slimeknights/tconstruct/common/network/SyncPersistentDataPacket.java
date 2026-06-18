package slimeknights.tconstruct.common.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;

/** Packet to sync player persistent data to the client */
@RequiredArgsConstructor
public class SyncPersistentDataPacket implements IThreadsafePacket {
  public static final Type<SyncPersistentDataPacket> TYPE = new Type<>(TConstruct.getResource("sync_persistent_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf,SyncPersistentDataPacket> STREAM_CODEC = ISimplePacket.codec(SyncPersistentDataPacket::new);

  private final CompoundTag data;

  public SyncPersistentDataPacket(FriendlyByteBuf buffer) {
    data = buffer.readNbt();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeNbt(data);
  }

  @Override
  public Type<SyncPersistentDataPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    HandleClient.handle(this);
  }

  /** Handles client side only code safely */
  private static class HandleClient {
    private static void handle(SyncPersistentDataPacket packet) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
        PersistentDataCapability.getData(player).copyFrom(packet.data);
      }
    }
  }
}
