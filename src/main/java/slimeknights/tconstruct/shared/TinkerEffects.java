package slimeknights.tconstruct.shared;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister;
import slimeknights.mantle.registration.deferred.PotionDeferredRegister.PotionType;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.shared.effect.AntigravityEffect;
import slimeknights.tconstruct.shared.effect.ReturningEffect;
import slimeknights.tconstruct.tools.modifiers.effect.BleedingEffect;
import slimeknights.tconstruct.tools.modifiers.effect.MagneticEffect;
import slimeknights.tconstruct.tools.modifiers.effect.RepulsiveEffect;
import slimeknights.tconstruct.tools.modifiers.traits.skull.SelfDestructiveModifier.SelfDestructiveEffect;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nullable;

/** Handles registration for all status effects and potions in the mod */
public class TinkerEffects extends TinkerModule {
  private static final PotionDeferredRegister POTIONS = new PotionDeferredRegister(TConstruct.MOD_ID);

  // slimy potions
  public static final DeferredHolder<?, TinkerEffect> experienced = MOB_EFFECTS.register("experienced", () -> new TinkerEffect(MobEffectCategory.BENEFICIAL, 0x82c873, true).addAttributeModifier(TinkerAttributes.EXPERIENCE_MULTIPLIER, TConstruct.getResource("effect.experienced"), 0.25f, Operation.ADD_MULTIPLIED_BASE));
  public static final DeferredHolder<?, TinkerEffect> ricochet = MOB_EFFECTS.register("ricochet", () -> new TinkerEffect(MobEffectCategory.NEUTRAL, 0x01cbcd, true).addAttributeModifier(TinkerAttributes.KNOCKBACK_MULTIPLIER, TConstruct.getResource("effect.ricochet"), 0.5f, Operation.ADD_MULTIPLIED_BASE));
  public static final DeferredHolder<?, TinkerEffect> enderference = MOB_EFFECTS.register("enderference", () -> new TinkerEffect(MobEffectCategory.HARMFUL, 0xD37CFF, true));
  /** Projectile persistent data key to allow ranged modifiers to hit endermen. */
  public static final ResourceLocation ENDERFERENCE_KEY = enderference.getId();

  // slimy cakes
  public static final DeferredHolder<?, TinkerEffect> bouncy = MOB_EFFECTS.register("bouncy", () -> new TinkerEffect(MobEffectCategory.BENEFICIAL, 0x71AC63, true).addAttributeModifier(TinkerAttributes.BOUNCY, TConstruct.getResource("effect.bouncy"), 1, Operation.ADD_VALUE));
  public static final DeferredHolder<?, TinkerEffect> doubleJump = MOB_EFFECTS.register("double_jump", () -> new TinkerEffect(MobEffectCategory.BENEFICIAL, 0xA99B87, true).addAttributeModifier(TinkerAttributes.JUMP_COUNT, TConstruct.getResource("effect.double_jump"), 1, Operation.ADD_VALUE));
  public static final DeferredHolder<?, AntigravityEffect> antigravity = MOB_EFFECTS.register("antigravity", AntigravityEffect::new);
  public static final DeferredHolder<?, ReturningEffect> returning = MOB_EFFECTS.register("returning", ReturningEffect::new);

  // modifier effects
  public static final DeferredHolder<?, BleedingEffect> bleeding = MOB_EFFECTS.register("bleeding", BleedingEffect::new);
  public static final DeferredHolder<?, MagneticEffect> magnetic = MOB_EFFECTS.register("magnetic", MagneticEffect::new);
  public static final DeferredHolder<?, TinkerEffect> selfDestructing = MOB_EFFECTS.register("self_destructing", SelfDestructiveEffect::new);
  public static final DeferredHolder<?, RepulsiveEffect> repulsive = MOB_EFFECTS.register("repulsive", RepulsiveEffect::new);
  public static final DeferredHolder<?, TinkerEffect> pierce = MOB_EFFECTS.register("pierce", () -> new TinkerEffect(MobEffectCategory.HARMFUL, 0xD1D37A, true).addAttributeModifier(Attributes.ARMOR, TConstruct.getResource("effect.pierce"), -1, Operation.ADD_VALUE));
  // damage boost
  public static final DeferredHolder<?, TinkerEffect> conductive = MOB_EFFECTS.register("conductive", () -> new TinkerEffect(MobEffectCategory.HARMFUL, 0xF2D500, true));
  public static final DeferredHolder<?, TinkerEffect> venom = MOB_EFFECTS.register("venom", () -> new TinkerEffect(MobEffectCategory.HARMFUL, 0xA2935E, true));

  // potions
  public static final EnumObject<PotionType,Potion> experiencedPotion = POTIONS.registerTypes("experienced", experienced, 3 * 60 * 20, 0).withStrong().withLong().build();
  public static final EnumObject<PotionType,Potion> ricochetPotion = POTIONS.registerTypes("ricochet", ricochet, 3 * 60 * 20, 0).withStrong().withLong().build();
  public static final EnumObject<PotionType,Potion> levitationPotion = POTIONS.registerTypes("levitation", () -> MobEffects.LEVITATION.value(), 15 * 20, 0).withStrong().withLong(40 * 20, 0).build();
  public static final EnumObject<PotionType,Potion> enderferencePotion = POTIONS.registerTypes("enderference", enderference, 90 * 20, 0).withLong().build();

  public TinkerEffects() {
    POTIONS.register(ModLoadingContext.get().getActiveContainer().getEventBus());
  }

  @SubscribeEvent
  void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
    PotionBrewing.Builder builder = event.getBuilder();
    brewing(builder, experiencedPotion,  Potions.AWKWARD, Ingredient.of(TinkerWorld.congealedSlime.get(SlimeType.EARTH)));
    brewing(builder, ricochetPotion,     Potions.AWKWARD, Ingredient.of(TinkerWorld.congealedSlime.get(SlimeType.SKY)));
    brewing(builder, levitationPotion,   Potions.AWKWARD, Ingredient.of(TinkerWorld.congealedSlime.get(SlimeType.ICHOR)));
    brewing(builder, enderferencePotion, Potions.AWKWARD, Ingredient.of(TinkerWorld.congealedSlime.get(SlimeType.ENDER)));
  }

  /** Wraps a potion in its registry holder */
  private static Holder<Potion> holder(Potion potion) {
    return BuiltInRegistries.POTION.wrapAsHolder(potion);
  }

  /** Registers recipes for brewing, longer and stronger potions for the given object */
  private static void brewing(PotionBrewing.Builder builder, EnumObject<PotionType,Potion> potion, Holder<Potion> base, Ingredient ingredient) {
    Holder<Potion> normal = holder(potion.get(PotionType.NORMAL));
    // base potion + custom ingredient -> normal potion (custom ingredient requires the generic recipe form)
    builder.addRecipe(Ingredient.of(PotionContents.createItemStack(Items.POTION, base)), ingredient, PotionContents.createItemStack(Items.POTION, normal));
    Potion longer = potion.getOrNull(PotionType.LONG);
    if (longer != null) {
      builder.addMix(normal, Items.REDSTONE, holder(longer));
    }
    Potion strong = potion.getOrNull(PotionType.STRONG);
    if (strong != null) {
      builder.addMix(normal, Items.GLOWSTONE_DUST, holder(strong));
    }
  }

  /** Checks if the given entity can be hit considering enderman enderference */
  public static boolean canHitWithProjectile(@Nullable LivingEntity living) {
    return living == null || living.getType() != EntityType.ENDERMAN || living.hasEffect(enderference.get());
  }

  /** Checks if the given entity needs special casing for enderference */
  public static boolean needsEnderferenceOverride(@Nullable Entity entity) {
    return entity != null && entity.getType() == EntityType.ENDERMAN && entity instanceof LivingEntity living && living.hasEffect(enderference.get());
  }

  /** Checks if the given entity needs special casing for enderference */
  public static boolean needsEnderferenceOverride(@Nullable LivingEntity living) {
    return living != null && living.getType() == EntityType.ENDERMAN && living.hasEffect(enderference.get());
  }
}
