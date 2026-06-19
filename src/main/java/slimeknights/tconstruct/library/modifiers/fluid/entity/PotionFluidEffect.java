package slimeknights.tconstruct.library.modifiers.fluid.entity;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.recipe.TagPredicate;

import java.util.ArrayList;
import java.util.List;

/** Spilling effect that pulls the potion from a NBT potion fluid and applies it */
public record PotionFluidEffect(float scale, TagPredicate predicate) implements FluidEffect<FluidEffectContext.Entity> {
  public static final RecordLoadable<PotionFluidEffect> LOADER = RecordLoadable.create(
    FloatLoadable.FROM_ZERO.requiredField("scale", e -> e.scale),
    TagPredicate.LOADABLE.defaultField("nbt", TagPredicate.ANY, e -> e.predicate),
    PotionFluidEffect::new);

  @Override
  public RecordLoadable<PotionFluidEffect> getLoader() {
    return LOADER;
  }

  /**
   * Rebuilds the legacy potion fluid tag ({@code {Potion:"<id>"}}, matching {@code PotionFluidType.potionTag}) so the
   * {@link TagPredicate} keeps working against component-backed fluids. The {@link TagPredicate#ANY} default matches a
   * null tag, so this preserves the 1.20 behavior for the in-mod potion fluid.
   */
  public static boolean testPredicate(TagPredicate predicate, PotionContents contents) {
    if (predicate == TagPredicate.ANY) {
      return true;
    }
    CompoundTag tag = null;
    Holder<Potion> potion = contents.potion().orElse(null);
    if (potion != null) {
      ResourceKey<Potion> key = potion.unwrapKey().orElse(null);
      if (key != null) {
        tag = new CompoundTag();
        tag.putString("Potion", key.location().toString());
      }
    }
    return predicate.test(tag);
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Entity context, FluidAction action) {
    LivingEntity target = context.getLivingTarget();
    PotionContents contents = fluid.get(DataComponents.POTION_CONTENTS);
    if (target != null && contents != null && testPredicate(predicate, contents)) {
      List<MobEffectInstance> effects = new ArrayList<>();
      contents.getAllEffects().forEach(effects::add);
      if (!effects.isEmpty()) {
        LivingEntity attacker = context.getEntity();
        Entity directSource = context.getDirectSource();
        Entity effectSource = context.getEffectSource();
        // prevent effects like instant damage from hitting hurt resistance
        int oldInvulnerableTime = target.invulnerableTime;
        // report whichever effect used the most
        float used = 0;
        for (MobEffectInstance instance : effects) {
          Holder<MobEffect> effect = instance.getEffect();
          if (effect.value().isInstantenous()) {
            // instant effects just apply full value always
            used = level.value();
            if (action.execute()) {
              target.invulnerableTime = 0;
              effect.value().applyInstantenousEffect(directSource, attacker, target, instance.getAmplifier(), used * scale);
            }
          } else {
            // if the potion already exists, we scale up the existing time
            MobEffectInstance existingEffect = target.getEffect(effect);
            int duration;
            if (existingEffect != null && existingEffect.getAmplifier() >= instance.getAmplifier()) {
              // if the existing level is larger, just skip, would be a cheese to increase said level
              // lower levels we treat as not having the effect, must be exact match to extend
              if (existingEffect.getAmplifier() > instance.getAmplifier()) {
                continue;
              }
              float existingLevel = existingEffect.getDuration() / scale / instance.getDuration();
              float effective = level.effective(existingLevel);
              // no potion to add? just save effort and stop here
              if (effective <= existingLevel) {
                continue;
              }
              duration = (int) (instance.getDuration() * scale * effective);
              // update how much we used, which is likely less than our max possible
              used = Math.max(used, effective - existingLevel);
            } else {
              // no relevant effect? just compute duration directly
              used = level.value();
              duration = (int) (instance.getDuration() * scale * used);
            }
            if (action.execute()) {
              target.addEffect(new MobEffectInstance(effect, duration, instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()), effectSource);
            }
          }
        }
        target.invulnerableTime = oldInvulnerableTime;
        return used;
      }
    }
    return 0;
  }
}
