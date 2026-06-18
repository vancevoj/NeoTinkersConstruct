package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;

/**
 * Holds the piggyback data attachment, replacing the old Forge capability.
 * <p>
 * In NeoForge 1.21 per-entity mutable data uses a {@link AttachmentType data attachment} instead of a capability, so the
 * attachment is non-persistent (matching the old non-serializing capability) and only exists on players.
 */
public class PiggybackCapability {
  private PiggybackCapability() {}

  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Attachment holding the piggyback handler. Not serialized as the riders are saved with the world. */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<PiggybackHandler>> PIGGYBACK =
    ATTACHMENTS.register("piggyback", () -> AttachmentType.builder(PiggybackHandler::new).build());

  /** Gets the piggyback handler for the given player, creating it if missing */
  public static PiggybackHandler get(Player player) {
    return player.getData(PIGGYBACK);
  }

  /** Registers the attachment type with the mod event bus. Call during mod construction. */
  public static void register(IEventBus bus) {
    ATTACHMENTS.register(bus);
  }
}
