package slimeknights.tconstruct.shared.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.client.ClientGeneratePartTexturesCommand;

/** Packet to tell the client to generate tool textures */
@RequiredArgsConstructor
public class GeneratePartTexturesPacket implements IThreadsafePacket {
  public static final Type<GeneratePartTexturesPacket> TYPE = new Type<>(TConstruct.getResource("generate_part_textures"));
  public static final StreamCodec<RegistryFriendlyByteBuf,GeneratePartTexturesPacket> STREAM_CODEC = ISimplePacket.codec(GeneratePartTexturesPacket::new);

  private final Operation operation;
  private final String modId;
  private final String materialPath;

  public GeneratePartTexturesPacket(FriendlyByteBuf buffer) {
    operation = buffer.readEnum(Operation.class);
    modId = buffer.readUtf(Short.MAX_VALUE);
    materialPath = buffer.readUtf(Short.MAX_VALUE);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeEnum(operation);
    buffer.writeUtf(modId);
    buffer.writeUtf(materialPath);
  }

  @Override
  public Type<GeneratePartTexturesPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    ClientGeneratePartTexturesCommand.generateTextures(operation, modId, materialPath);
  }

  public enum Operation { ALL, MISSING }
}
