package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * Capability to store persistent NBT data on an entity. For players, this is automatically synced to the client on load, but not during gameplay.
 * Persists after death, will reassess if we need some data to not persist death.
 * <p>
 * In NeoForge 1.21 this is backed by a persistent {@link AttachmentType data attachment} replacing the old Forge capability.
 * The data is serialized via the {@link ModDataNBT} compound and copied on death/dimension change explicitly via the player events below.
 */
public class PersistentDataCapability {
  private PersistentDataCapability() {}

  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /**
   * Attachment storing the persistent mod data on an entity. Serialized via {@link CompoundTag} mapped through {@link ModDataNBT}.
   * Copy on death is handled manually in {@link #playerClone} so it can merge non-empty data without overwriting.
   */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<ModDataNBT>> ATTACHMENT =
    ATTACHMENTS.register("persistent_data", () -> AttachmentType
      .builder(holder -> new ModDataNBT())
      .serialize(CompoundTag.CODEC.xmap(ModDataNBT::readFromNBT, ModDataNBT::getCopy))
      .build());

  /**
   * Gets the persistent data for an entity, never null. Direct replacement for the old {@code getCapability(CAPABILITY)} access.
   * @param entity  Entity to fetch data for
   * @return  Persistent mod data, created if missing
   */
  public static ModDataNBT getData(Entity entity) {
    return entity.getData(ATTACHMENT);
  }

  /** Gets the data or warns if its missing */
  public static ModDataNBT getOrWarn(Entity entity) {
    // attachments always provide the default value, so this can never be missing, but keep the helper for call site compatibility
    return entity.getData(ATTACHMENT);
  }

  /** Registers the attachment type and the player sync/copy event listeners. Call during mod construction with the mod bus. */
  public static void register(IEventBus modBus) {
    ATTACHMENTS.register(modBus);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerClone);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerRespawn);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerChangeDimension);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerLoggedIn);
  }

  /** Syncs the data to the given player */
  private static void sync(Player player) {
    TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(getData(player).getCopy()), player);
  }

  /** copy data when the player respawns/returns from the end */
  private static void playerClone(PlayerEvent.Clone event) {
    CompoundTag nbt = getData(event.getOriginal()).getCopy();
    if (!nbt.isEmpty()) {
      getData(event.getEntity()).copyFrom(nbt);
    }
  }

  /** sync data when the player respawns/returns from the end */
  private static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    sync(event.getEntity());
  }

  /** sync data when the player changes dimensions */
  private static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    sync(event.getEntity());
  }

  /** sync data when the player logs in */
  private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    sync(event.getEntity());
  }

  /**
   * Checks if the given entity is a type that should hold persistent data. Kept for parity with the old attach predicate;
   * attachments lazily create the data on access so this is only used to decide whether non-living entities want the data.
   */
  @SuppressWarnings("unused")
  public static boolean supportEntity(Entity entity) {
    return entity instanceof LivingEntity || EntityModifierCapability.supportCapability(entity);
  }
}
