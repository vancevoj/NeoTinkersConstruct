package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.common.network.TinkerNetwork;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Data attachment instance tracking the entities riding on a player's back.
 * <p>
 * Does not serialize as the world saves the entities already, they just dismounted on logout. In NeoForge 1.21 this
 * replaces the old Forge capability with a {@link net.neoforged.neoforge.attachment.AttachmentType data attachment}.
 */
public class PiggybackHandler {
  /** Last found list of passengers, used in syncing */
  @Nullable
  private List<Entity> lastPassengers;

  public PiggybackHandler() {}

  /**
   * Updates the passengers on the back
   * @param riddenPlayer  Player holding this data
   */
  public void updatePassengers(Player riddenPlayer) {
    // tell the player itself if his riders changed serverside
    if (!riddenPlayer.getPassengers().equals(this.lastPassengers)) {
      if (riddenPlayer instanceof ServerPlayer) {
        TinkerNetwork.getInstance().sendVanillaPacket(riddenPlayer, new ClientboundSetPassengersPacket(riddenPlayer));
      }
    }
    this.lastPassengers = riddenPlayer.getPassengers();
  }
}
