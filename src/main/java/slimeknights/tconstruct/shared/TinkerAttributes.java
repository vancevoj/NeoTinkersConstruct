package slimeknights.tconstruct.shared;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.deferred.AttributeDeferredRegister;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;

public class TinkerAttributes {
  private static final AttributeDeferredRegister ATTRIBUTES = new AttributeDeferredRegister(TConstruct.MOD_ID);

  public TinkerAttributes() {
    ATTRIBUTES.register(ModLoadingContext.get().getActiveContainer().getEventBus());
  }

  /* Helpers narrowing the mantle register's {@code DeferredHolder<? super Attribute, Attribute>} return to
   * {@code DeferredHolder<Attribute, Attribute>} so the holders implement {@code Holder<Attribute>} (the attribute
   * registry guarantees the registry type is {@link Attribute}). */
  @SuppressWarnings("unchecked")
  private static DeferredHolder<Attribute, Attribute> register(String name, double defaultValue, double min, double max, boolean syncable) {
    return (DeferredHolder<Attribute, Attribute>) ATTRIBUTES.register(name, defaultValue, min, max, syncable);
  }

  @SuppressWarnings("unchecked")
  private static DeferredHolder<Attribute, Attribute> registerPercent(String name, double defaultValue, boolean syncable) {
    return (DeferredHolder<Attribute, Attribute>) ATTRIBUTES.registerPercent(name, defaultValue, syncable);
  }

  @SuppressWarnings("unchecked")
  private static DeferredHolder<Attribute, Attribute> registerMultiplier(String name, boolean syncable) {
    return (DeferredHolder<Attribute, Attribute>) ATTRIBUTES.registerMultiplier(name, syncable);
  }

  // booleans
  /** If true, the entity will bounce. Used to implement slime boots */
  public static final DeferredHolder<Attribute, Attribute> BOUNCY = registerPercent("generic.bouncy", 0f, true);

  // stat replacements
  /** Changes the speed debuff percentage when the player moves while using an item */
  public static final DeferredHolder<Attribute, Attribute> USE_ITEM_SPEED = registerPercent("player.use_item_speed", 0.2f, true);
  /** Changes the speed debuff when the player moves while using an item */
  public static final DeferredHolder<Attribute, Attribute> PROTECTION_CAP = register("generic.protection_cap", 0.8, 0, 0.95f, true);
  /** Percentage boost to critical hits for any airborne attacker, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#dragonborn} */
  public static final DeferredHolder<Attribute, Attribute> CRITICAL_DAMAGE = register("player.critical_damage", 1.5f, 0, 100, false);

  // stat bonuses
  /** Bonus jump height in blocks */
  public static final DeferredHolder<Attribute, Attribute> JUMP_BOOST = register("generic.jump_boost", 0, 0, 100, true);
  /** Distance you can safely fall without damage */
  public static final DeferredHolder<Attribute, Attribute> SAFE_FALL_DISTANCE = register("generic.safe_fall_distance", 0, -10, 100, true);
  /** Number of jumps the player may perform, used by the double jump modifier. */
  public static final DeferredHolder<Attribute, Attribute> JUMP_COUNT = register("player.jump_count", 1, 1, 100, true);

  // stat multipliers
  /** Multiplier for knockback this entity takes. Similar to {@link net.minecraft.world.entity.ai.attributes.Attributes#KNOCKBACK_RESISTANCE} but can be used to increase knockback */
  public static final DeferredHolder<Attribute, Attribute> KNOCKBACK_MULTIPLIER = registerMultiplier("generic.knockback_multiplier", true);
  /** Player modifier data key for mining speed multiplier as an additive percentage boost on mining speed. Used for armor haste. */
  public static final DeferredHolder<Attribute, Attribute> MINING_SPEED_MULTIPLIER = registerMultiplier("player.mining_speed_multiplier", true);
  /** Attribute for experience from all sources */
  public static final DeferredHolder<Attribute, Attribute> EXPERIENCE_MULTIPLIER = registerMultiplier("player.experience_multiplier", false);
  /** Percentage boost to damage while crouching, used by {@link slimeknights.tconstruct.tools.data.ModifierIds#shulking} */
  public static final DeferredHolder<Attribute, Attribute> CROUCH_DAMAGE_MULTIPLIER = registerMultiplier("generic.crouch_damage_multiplier", false);
  // effect durations
  /** Percentage boost to positive potion effects */
  public static final DeferredHolder<Attribute, Attribute> GOOD_EFFECT_DURATION = registerMultiplier("generic.good_effect_duration_multiplier", false);
  /** Percentage boost to negative potion effects, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#magicProtection} */
  public static final DeferredHolder<Attribute, Attribute> BAD_EFFECT_DURATION = registerMultiplier("generic.bad_effect_duration_multiplier", false);


  @SubscribeEvent
  void addAttributes(EntityAttributeModificationEvent event) {
    // player attributes
    event.add(EntityType.PLAYER, USE_ITEM_SPEED);
    event.add(EntityType.PLAYER, CRITICAL_DAMAGE);
    event.add(EntityType.PLAYER, MINING_SPEED_MULTIPLIER);
    event.add(EntityType.PLAYER, EXPERIENCE_MULTIPLIER);
    event.add(EntityType.PLAYER, JUMP_COUNT);
    // general attributes
    addToAll(event, BOUNCY);
    addToAll(event, PROTECTION_CAP);
    addToAll(event, JUMP_BOOST);
    addToAll(event, SAFE_FALL_DISTANCE);
    addToAll(event, CROUCH_DAMAGE_MULTIPLIER);
    addToAll(event, KNOCKBACK_MULTIPLIER);
    addToAll(event, GOOD_EFFECT_DURATION);
    addToAll(event, BAD_EFFECT_DURATION);
  }

  @SubscribeEvent
  void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      // make knockback resistance syncable, as we need that info clientside
      if (Config.COMMON.syncKnockbackResistance.get()) {
        Attributes.KNOCKBACK_RESISTANCE.value().setSyncable(true);
      }
    });
  }

  /** Adds an attribute to all entities */
  private static void addToAll(EntityAttributeModificationEvent event, DeferredHolder<Attribute, Attribute> attribute, double defaultValue) {
    for (EntityType<? extends LivingEntity> entity : event.getTypes()) {
      event.add(entity, attribute, defaultValue);
    }
  }

  /** Adds an attribute to all entities */
  private static void addToAll(EntityAttributeModificationEvent event, DeferredHolder<Attribute, Attribute> attribute) {
    addToAll(event, attribute, attribute.get().getDefaultValue());
  }
}
