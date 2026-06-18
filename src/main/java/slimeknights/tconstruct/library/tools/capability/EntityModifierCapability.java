package slimeknights.tconstruct.library.tools.capability;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Capability to allow an entity to store modifiers, used on projectiles fired from modifiable items.
 * <p>
 * In NeoForge 1.21 this is backed by a serialized {@link AttachmentType data attachment} replacing the old Forge capability.
 */
public class EntityModifierCapability {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}

    @Override
    public void addModifiers(ModifierNBT nbt) {}
  };

  private EntityModifierCapability() {}

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Attachment holding the entity modifiers. Serialized so projectiles keep their modifiers across save/load. */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<Storage>> ATTACHMENT =
    ATTACHMENTS.register("modifiers", () -> AttachmentType.serializable(Storage::new).build());

  /** Gets the capability for the entity or an empty instance if missing */
  public static EntityModifiers getCapability(Entity entity) {
    // only create the attachment for supported entities, returning the read-only empty instance otherwise to preserve old behavior
    if (entity.hasData(ATTACHMENT) || supportCapability(entity)) {
      return entity.getData(ATTACHMENT);
    }
    return EMPTY;
  }

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    Storage storage = entity.getExistingDataOrNull(ATTACHMENT);
    return storage != null ? storage.getModifiers() : ModifierNBT.EMPTY;
  }

  /** Checks if the given entity supports this capability */
  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers the attachment type with the mod event bus. Call during mod construction. */
  public static void register(IEventBus bus) {
    ATTACHMENTS.register(bus);
  }

  /** Mutable storage backing the attachment, serialized via the modifier NBT list. */
  public static class Storage implements INBTSerializable<ListTag>, EntityModifiers {
    @Getter @Setter
    private ModifierNBT modifiers = ModifierNBT.EMPTY;

    @Override
    public ListTag serializeNBT(HolderLookup.Provider provider) {
      return modifiers.serializeToNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, ListTag nbt) {
      modifiers = ModifierNBT.readFromNBT(nbt);
    }
  }

  /** Interface for callers to use */
  public interface EntityModifiers {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);

    /** Adds additional modifiers to the stored modifiers */
    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
