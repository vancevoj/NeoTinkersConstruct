package slimeknights.tconstruct.tools.logic;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.fml.loading.FMLEnvironment;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.events.ToolEquipmentChangeEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Logic to make it easy for modifiers to respond to equipment changes, primarily used for armor.
 * <p>
 * In NeoForge 1.21 the old Forge capability that stored the last-known equipment (used to poll changes client-side) is replaced
 * with a transient {@link AttachmentType data attachment}. The serverside hook still relies on {@link LivingEquipmentChangeEvent}.
 */
public class EquipmentChangeWatcher {
  private EquipmentChangeWatcher() {}

  /** Deferred register for the attachment type. Must be registered on the mod bus during construction via {@link #register(IEventBus)}. */
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Attachment holding the last known equipment for a client player. Transient: it is rebuilt every tick and never needs saving. */
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerLastEquipment>> LAST_EQUIPMENT =
    ATTACHMENTS.register("equipment_watcher", () -> AttachmentType.builder(PlayerLastEquipment::new).build());

  /** Registers the attachment type and the equipment change listeners. Call during mod construction with the mod bus. */
  public static void register(IEventBus modBus) {
    ATTACHMENTS.register(modBus);

    // equipment change is used on both sides
    NeoForge.EVENT_BUS.addListener(EquipmentChangeWatcher::onEquipmentChange);

    // only need to poll equipment via the player tick on the client (server gets the change event directly)
    if (FMLEnvironment.dist == Dist.CLIENT) {
      NeoForge.EVENT_BUS.addListener(EquipmentChangeWatcher::onPlayerTick);
    }
  }


  /* Events */

  /** Serverside modifier hooks */
  private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
    runModifierHooks(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
  }

  /** Client side modifier hooks */
  private static void onPlayerTick(PlayerTickEvent.Post event) {
    // only run for client side players
    Player player = event.getEntity();
    if (player.level().isClientSide) {
      player.getData(LAST_EQUIPMENT).update(player);
    }
  }


  /* Helpers */

  /** Shared modifier hook logic */
  private static void runModifierHooks(LivingEntity entity, EquipmentSlot changedSlot, ItemStack original, ItemStack replacement) {
    EquipmentChangeContext context = new EquipmentChangeContext(entity, changedSlot, original, replacement);

    // first, fire event to notify an item was removed
    IToolStackView tool = context.getOriginalTool();
    if (tool != null && ModifierUtil.validArmorSlot(tool, changedSlot)) {
      for (ModifierEntry entry : tool.getModifierList()) {
        entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onUnequip(tool, entry, context);
      }
    }

    // next, fire event to notify an item was added
    tool = context.getReplacementTool();
    if (tool != null && ModifierUtil.validArmorSlot(tool, changedSlot)) {
      for (ModifierEntry entry : tool.getModifierList()) {
        entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onEquip(tool, entry, context);
      }
    }

    // finally, fire events on all other slots to say something changed
    for (EquipmentSlot otherSlot : EquipmentSlot.values()) {
      if (otherSlot != changedSlot) {
        tool = context.getValidTool(otherSlot);
        if (tool != null) {
          for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onEquipmentChange(tool, entry, context, otherSlot);
          }
        }
      }
    }
    // fire event for modifiers that want to watch equipment when not equipped
    NeoForge.EVENT_BUS.post(new ToolEquipmentChangeEvent(context));
  }

  /* Required methods */

  /** Data class that runs actual update logic, stored as a transient data attachment on client players */
  protected static class PlayerLastEquipment {
    @Nullable
    private final Player player;
    private final Map<EquipmentSlot,ItemStack> lastItems = new EnumMap<>(EquipmentSlot.class);

    /** Attachment factory constructor; the holder is the player the attachment is attached to */
    private PlayerLastEquipment(net.neoforged.neoforge.attachment.IAttachmentHolder holder) {
      this.player = holder instanceof Player p ? p : null;
      for (EquipmentSlot slot : EquipmentSlot.values()) {
        lastItems.put(slot, ItemStack.EMPTY);
      }
    }

    /** Called on player tick to update the stacks and run the event */
    public void update(Player player) {
      if (player != null) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
          ItemStack newStack = player.getItemBySlot(slot);
          ItemStack oldStack = lastItems.get(slot);
          if (!ItemStack.matches(oldStack, newStack)) {
            lastItems.put(slot, newStack.copy());
            runModifierHooks(player, slot, oldStack, newStack);
          }
        }
      }
    }
  }
}
