package slimeknights.tconstruct.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.UpdateMaterialsPacket;
import slimeknights.tconstruct.library.materials.stats.UpdateMaterialStatsPacket;
import slimeknights.tconstruct.library.materials.traits.UpdateMaterialTraitsPacket;
import slimeknights.tconstruct.library.modifiers.UpdateModifiersPacket;
import slimeknights.tconstruct.library.modifiers.fluid.UpdateFluidEffectsPacket;
import slimeknights.tconstruct.library.tools.definition.UpdateToolDefinitionDataPacket;
import slimeknights.tconstruct.library.tools.layout.UpdateTinkerSlotLayoutsPacket;
import slimeknights.tconstruct.shared.network.GeneratePartTexturesPacket;
import slimeknights.tconstruct.smeltery.network.ChannelFlowPacket;
import slimeknights.tconstruct.smeltery.network.FaucetActivationPacket;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;
import slimeknights.tconstruct.smeltery.network.SmelteryFluidClickedPacket;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;
import slimeknights.tconstruct.smeltery.network.StructureErrorPositionPacket;
import slimeknights.tconstruct.smeltery.network.StructureUpdatePacket;
import slimeknights.tconstruct.tables.network.StationTabPacket;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;
import slimeknights.tconstruct.tables.network.UpdateCraftingRecipePacket;
import slimeknights.tconstruct.tables.network.UpdateStationScreenPacket;
import slimeknights.tconstruct.tables.network.UpdateTinkerStationRecipePacket;
import slimeknights.tconstruct.tools.network.EntityMovementChangePacket;
import slimeknights.tconstruct.tools.network.InteractWithAirPacket;
import slimeknights.tconstruct.tools.network.PushBlockRowPacket;
import slimeknights.tconstruct.tools.network.SyncProjectileModifiersPacket;
import slimeknights.tconstruct.tools.network.TinkerControlPacket;
import slimeknights.tconstruct.tools.network.ToolContainerFluidUpdatePacket;

import javax.annotation.Nullable;

/**
 * Base network class for all tinkers logic
 * <p>
 * In general, if you need to send packets you should use your own network class
 */
public class TinkerNetwork extends NetworkWrapper {
  private static TinkerNetwork instance = null;

  /*
   * Network versions:
   * 1: 3.10.1 and before
   * 2: 3.10.2 - new material stat type; item removal
   * 3: 3.11.2+ - lost track of how much changed but its a lot
   */
  private TinkerNetwork() {
    super(TConstruct.getResource("network"), "3");
  }

  /** Gets the instance of the network */
  public static TinkerNetwork getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Attempt to call network getInstance before network is setup");
    }
    return instance;
  }

  /**
   * Called during mod construction to setup the network. Registers the payload handler listener on the mod event bus.
   */
  public static void setup() {
    if (instance == null) {
      instance = new TinkerNetwork();
    }
  }

  /**
   * Registers all TConstruct packets on the payload registrar. Invoked from {@link RegisterPayloadHandlersEvent} on the
   * mod bus; the orchestrator ({@code TConstruct}) wires this method as the event listener.
   * @param event  Payload registration event
   */
  public static void registerPackets(RegisterPayloadHandlersEvent event) {
    setup();
    PayloadRegistrar registrar = instance.getRegistrar(event);

    // shared
    instance.registerToClient(registrar, InventorySlotSyncPacket.TYPE, InventorySlotSyncPacket::new);
    instance.registerToClient(registrar, UpdateNeighborsPacket.TYPE, UpdateNeighborsPacket::new);
    instance.registerToClient(registrar, GeneratePartTexturesPacket.TYPE, GeneratePartTexturesPacket::new);
    instance.registerToClient(registrar, SyncPersistentDataPacket.TYPE, SyncPersistentDataPacket::new);

    // gadgets
    instance.registerToClient(registrar, EntityMovementChangePacket.TYPE, EntityMovementChangePacket::new);

    // tables
    instance.registerToServer(registrar, StationTabPacket.TYPE, StationTabPacket::new);
    instance.registerToServer(registrar, TinkerStationRenamePacket.TYPE, TinkerStationRenamePacket::new);
    instance.registerToClient(registrar, UpdateCraftingRecipePacket.TYPE, UpdateCraftingRecipePacket::new);
    instance.registerToServer(registrar, TinkerStationSelectionPacket.TYPE, TinkerStationSelectionPacket::new);
    instance.registerToClient(registrar, UpdateTinkerSlotLayoutsPacket.TYPE, UpdateTinkerSlotLayoutsPacket::new);
    instance.registerToClient(registrar, UpdateStationScreenPacket.TYPE, buf -> UpdateStationScreenPacket.INSTANCE);
    instance.registerToClient(registrar, UpdateTinkerStationRecipePacket.TYPE, UpdateTinkerStationRecipePacket::new);

    // tools
    instance.registerToClient(registrar, UpdateMaterialsPacket.TYPE, UpdateMaterialsPacket::new);
    instance.registerToClient(registrar, UpdateMaterialStatsPacket.TYPE, UpdateMaterialStatsPacket::new);
    instance.registerToClient(registrar, UpdateMaterialTraitsPacket.TYPE, UpdateMaterialTraitsPacket::new);
    instance.registerToClient(registrar, UpdateToolDefinitionDataPacket.TYPE, UpdateToolDefinitionDataPacket::new);
    instance.registerToClient(registrar, ToolContainerFluidUpdatePacket.TYPE, ToolContainerFluidUpdatePacket::new);
    instance.registerToClient(registrar, SyncProjectileModifiersPacket.TYPE, SyncProjectileModifiersPacket::new);

    // modifiers
    instance.registerToServer(registrar, TinkerControlPacket.TYPE, TinkerControlPacket::read);
    instance.registerToServer(registrar, InteractWithAirPacket.TYPE, InteractWithAirPacket::read);
    instance.registerToClient(registrar, UpdateModifiersPacket.TYPE, UpdateModifiersPacket::new);
    instance.registerToClient(registrar, UpdateFluidEffectsPacket.TYPE, UpdateFluidEffectsPacket::decode);
    instance.registerToClient(registrar, PushBlockRowPacket.TYPE, PushBlockRowPacket::new);

    // smeltery
    instance.registerToClient(registrar, FluidUpdatePacket.TYPE, FluidUpdatePacket::new);
    instance.registerToClient(registrar, FaucetActivationPacket.TYPE, FaucetActivationPacket::new);
    instance.registerToClient(registrar, ChannelFlowPacket.TYPE, ChannelFlowPacket::new);
    instance.registerToClient(registrar, SmelteryTankUpdatePacket.TYPE, SmelteryTankUpdatePacket::new);
    instance.registerToClient(registrar, StructureUpdatePacket.TYPE, StructureUpdatePacket::new);
    instance.registerToServer(registrar, SmelteryFluidClickedPacket.TYPE, SmelteryFluidClickedPacket::new);
    instance.registerToClient(registrar, StructureErrorPositionPacket.TYPE, StructureErrorPositionPacket::new);
  }

  /**
   * Sends a vanilla packet to the given player
   * @param player  Player
   * @param packet  Packet
   */
  public void sendVanillaPacket(Entity player, Packet<?> packet) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(packet);
    }
  }

  /**
   * Same as {@link NetworkWrapper#sendToClientsAround(CustomPacketPayload, ServerLevel, BlockPos)}, but checks that the world is a server world
   * @param msg       Packet to send
   * @param world     World instance
   * @param position  Target position
   */
  public void sendToClientsAround(CustomPacketPayload msg, @Nullable LevelAccessor world, BlockPos position) {
    if (world instanceof ServerLevel server) {
      sendToClientsAround(msg, server, position);
    }
  }

  /**
   * Sends a packet to the whole player list
   * @param targetedPlayer  Main player to target, if null uses whole list
   * @param playerList      Player list to use if main player is null
   * @param msg             Message to send
   */
  public void sendToPlayerList(@Nullable ServerPlayer targetedPlayer, PlayerList playerList, CustomPacketPayload msg) {
    if (targetedPlayer != null) {
      sendTo(msg, targetedPlayer);
    } else {
      for (ServerPlayer player : playerList.getPlayers()) {
        sendTo(msg, player);
      }
    }
  }
}
