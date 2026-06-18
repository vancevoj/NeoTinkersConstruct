package slimeknights.tconstruct.library.modifiers.fluid.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
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

/** Effect to create a lingering cloud at the hit block */
public record PotionCloudFluidEffect(float scale, TagPredicate predicate) implements FluidEffect<FluidEffectContext.Block> {
  public static final RecordLoadable<PotionCloudFluidEffect> LOADER = RecordLoadable.create(
    FloatLoadable.FROM_ZERO.requiredField("scale", e -> e.scale),
    TagPredicate.LOADABLE.defaultField("nbt", TagPredicate.ANY, e -> e.predicate),
    PotionCloudFluidEffect::new);

  @Override
  public RecordLoadable<PotionCloudFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Block context, FluidAction action) {
    // TODO(neoport): TagPredicate (Predicate<CompoundTag>) no longer applies to component-backed fluids; potion is read from DataComponents.POTION_CONTENTS. Predicate filtering dropped pending the potion-fluid model decision.
    PotionContents contents = fluid.get(DataComponents.POTION_CONTENTS);
    if (contents != null && context.isOffsetReplaceable()) {
      List<MobEffectInstance> effects = new ArrayList<>();
      contents.getAllEffects().forEach(effects::add);
      if (!effects.isEmpty()) {
        float scale = level.value();
        if (action.execute()) {
          AreaEffectCloud cloud = MobEffectCloudFluidEffect.makeCloud(context);
          // not using set potion as we want to change the effect duration outself
          float effectScale = this.scale * scale;
          // keep track of how many effects are actually added
          boolean used = false;
          for (MobEffectInstance instance : effects) {
            if (instance.getEffect().isInstantenous()) {
              // only thing we have to scale on instant effects is the amplifier, though clouds automatically half instant effects for us
              int amplifier = (int)((instance.getAmplifier() + 1) * effectScale * 2) - 1;
              if (amplifier >= 0) {
                cloud.addEffect(new MobEffectInstance(instance.getEffect(), instance.getDuration(), amplifier, instance.isAmbient(), instance.isVisible(), instance.showIcon()));
                used = true;
              }
            } else {
              int duration = (int)(instance.getDuration() * effectScale);
              if (duration > 10) {
                cloud.addEffect(new MobEffectInstance(instance.getEffect(), duration, instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()));
                used = true;
              }
            }
          }
          // TODO: custom effects from potion NBT?
          // TODO: custom color from potion NBT?
          if (used) {
            context.getLevel().addFreshEntity(cloud);
          } else {
            cloud.discard();
            return 0;
          }
        }
        return scale;
      }
    }
    return 0;
  }
}
