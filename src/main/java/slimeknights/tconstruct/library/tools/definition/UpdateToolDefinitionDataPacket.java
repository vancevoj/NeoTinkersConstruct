package slimeknights.tconstruct.library.tools.definition;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;

import java.util.Map;
import java.util.Map.Entry;

/** Packet to sync tool definitions to the client */
@RequiredArgsConstructor
public class UpdateToolDefinitionDataPacket implements IThreadsafePacket {
  public static final Type<UpdateToolDefinitionDataPacket> TYPE = new Type<>(TConstruct.getResource("update_tool_definition_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf,UpdateToolDefinitionDataPacket> STREAM_CODEC = ISimplePacket.codec(UpdateToolDefinitionDataPacket::new);

  @Getter(AccessLevel.PROTECTED)
  private final Map<ResourceLocation, ToolDefinitionData> dataMap;

  public UpdateToolDefinitionDataPacket(FriendlyByteBuf buffer) {
    int size = buffer.readVarInt();
    ImmutableMap.Builder<ResourceLocation, ToolDefinitionData> builder = ImmutableMap.builder();
    for (int i = 0; i < size; i++) {
      ResourceLocation name = buffer.readResourceLocation();
      try {
        ToolDefinitionData data = ToolDefinitionData.LOADABLE.decode(buffer, ToolDefinitionLoader.contextBuilder(name).build());
        builder.put(name, data);
      } catch (RuntimeException e) {
        TConstruct.LOG.error("Failed to decode Tool Definition for {}", name, e);
        throw e;
      }
    }
    dataMap = builder.build();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeVarInt(dataMap.size());
    for (Entry<ResourceLocation, ToolDefinitionData> entry : dataMap.entrySet()) {
      ResourceLocation name = entry.getKey();
      buffer.writeResourceLocation(name);
      try {
        ToolDefinitionData.LOADABLE.encode(buffer, entry.getValue());
      } catch (RuntimeException e) {
        TConstruct.LOG.error("Failed to encode Tool Definition for {}", name, e);
        throw e;
      }
    }
  }

  @Override
  public Type<UpdateToolDefinitionDataPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    ToolDefinitionLoader.getInstance().updateDataFromServer(dataMap);
  }
}
