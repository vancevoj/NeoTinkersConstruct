package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.shared.TinkerEffects;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to handle the looting event for all main tinker tools
 */
public class ModifierLootingHandler {
  /** If contained in the set, they should use the offhand for looting */
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    // 1.21 removed LootingLevelEvent, so there is no event to bind for looting. Mob drops now read the
    // minecraft:looting enchantment level off the killer's main hand (see the minecraft:enchanted_count_increase loot
    // function), which our tools never carry. Instead of an event, ToolAttackUtil calls applyLooting around the attack
    // to temporarily write the computed level onto the stack, mirroring the fortune/silk touch block drop fix.
    NeoForge.EVENT_BUS.addListener(ModifierLootingHandler::onLeaveServer);
  }

  /**
   * Sets the hand used for looting, so the tool is fetched from the proper context
   * @param entity    Player to set
   * @param slotType  Slot type
   */
  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  /** Gets the slot to use for looting */
  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /**
   * Computes the looting level for the given attack. Kept as a standalone method (decoupled from the removed
   * {@code LootingLevelEvent}) so the modifier looting hooks stay exercised.
   * @param damageSource  Damage source causing the kill
   * @param target        Entity being killed
   * @param baseLooting   Looting level from vanilla/other mods. This is the level vanilla would have used on its own,
   *                      as the hooks are allowed to both add to and cancel it.
   * @return  Looting level to apply, never negative
   * @see #applyLooting(LivingEntity, ItemStack, DamageSource, LivingEntity)
   */
  public static int getLootingLevel(DamageSource damageSource, LivingEntity target, int baseLooting) {
    // bleeding kills use the level of the effect for looting
    if (damageSource.is(TinkerDamageTypes.BLEEDING)) {
      return Math.max(0, TinkerEffect.getAmplifier(target, TinkerEffects.bleeding.get()));
    }

    // otherwise, use the proper tool
    int level = baseLooting;
    Entity source = damageSource.getEntity();
    if (source instanceof LivingEntity holder) {
      Entity direct = damageSource.getDirectEntity();

      // determine who is in charge of the looting
      LootingContext context;
      IToolStackView tool = null;
      if (direct instanceof Projectile) {
        // need to build a context from the relevant capabilities to use the modifier
        ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(direct);
        context = new LootingContext(holder, target, damageSource, null);
        // no modifiers means its not a projectile we fired, so just defer to dumb vanilla behavior of whatever looting
        // since we don't set the enchantment on our tools, our looting modifiers won't set anything here anyways
        if (!modifiers.isEmpty()) {
          ModDataNBT persistentData = PersistentDataCapability.getData(direct);
          level = LootingModifierHook.getLooting(new DummyToolStack(Items.AIR, modifiers, persistentData), context, 0);
        }
      } else {
        // not an arrow? means the held tool is to blame
        EquipmentSlot slotType = getLootingSlot(holder);
        context = new LootingContext(holder, target, damageSource, slotType);
        ItemStack held = holder.getItemBySlot(slotType);

        // if its modifiable, let it increase the level
        if (held.is(TinkerTags.Items.MODIFIABLE)) {
          tool = ToolStack.from(held);
          level = LootingModifierHook.getLooting(tool, context, level);
        } else if (slotType != EquipmentSlot.MAINHAND) {
          // if it's not modifiable, yet we have a lot marked to blame for looting, ignore the event value
          level = 0;
        }
      }
      // boost looting with armor regardless, hopefully you did not switch your pants mid arrow firing
      level = ArmorLootingModifierHook.getLooting(tool, context, level);
    }
    // we allow the hook to return negatives to cancel out looting, so ensure its at least 0
    return Math.max(level, 0);
  }

  /**
   * Temporarily writes the looting level from {@link #getLootingLevel(DamageSource, LivingEntity, int)} onto the
   * attacker's main hand stack, so vanilla mob drops actually see it. Must be called immediately before the attack and
   * paired with {@link slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook#restoreEnchantments}
   * on the same stack instance in a finally block, as leaving a real looting enchantment behind would be permanent.
   * <p>
   * 1.21 dropped {@code LootingLevelEvent}, so there is no longer a hook to report a looting level to. Mob loot instead
   * runs the {@code minecraft:enchanted_count_increase} loot function, which reads the {@code minecraft:looting}
   * enchantment level from the killer and bails out early when it is 0. Tinker tools carry no real looting enchantment,
   * so it always read 0 (issue #18). This mirrors the fortune/silk touch block drop fix (issue #11), see
   * {@link slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook#updateToolEnchantments}.
   * <p>
   * The total level (weapon plus armor) goes on the main hand because the vanilla looting enchantment only defines the
   * main hand slot, so that is the only stack the loot function will ever read, even for an offhand attack.
   * @param attacker      Entity doing the attacking
   * @param mainHand      Attacker's main hand stack, passed in so the caller restores the exact instance it mutated
   * @param damageSource  Damage source causing the kill
   * @param target        Entity being killed
   * @return  Original enchantments component to restore afterwards, or null if nothing changed (so no restore needed)
   */
  @Nullable
  public static ItemEnchantments applyLooting(LivingEntity attacker, ItemStack mainHand, DamageSource damageSource, LivingEntity target) {
    // never write components to an empty stack, ItemStack.EMPTY is a shared singleton and the change would be global.
    // costs us nothing as vanilla skips empty slots when reading looting, so there is no level we could report anyway
    if (mainHand.isEmpty()) {
      return null;
    }
    // holder must come from the registry, the enchantment hooks key their maps on registry holder identity
    Holder<Enchantment> looting = attacker.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING);
    // base is whatever vanilla would have used without us, the hooks may raise or cancel it
    int base = EnchantmentHelper.getItemEnchantmentLevel(looting, mainHand);
    int level = getLootingLevel(damageSource, target, base);
    // if the hooks did not change anything, vanilla already reports the right level, so skip the write and the restore
    if (level == base) {
      return null;
    }
    ItemEnchantments original = mainHand.getTagEnchantments();
    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(original);
    // set handles a level of 0 by removing the enchantment, which is what we want when a hook cancelled looting
    mutable.set(looting, level);
    mainHand.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    return original;
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(PlayerLoggedOutEvent event) {
    LOOTING_OFFHAND.remove(event.getEntity().getUUID());
  }
}
