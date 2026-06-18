package slimeknights.tconstruct.library.modifiers.hook.armor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerAttributes;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Hook for increasing or decreasing protection from a given damage source.
 */
public interface ProtectionModifierHook {
  /**
   * Gets the protection value of the armor from this modifier. A value of 1 blocks about 4% of damage, equivalent to 1 level of the protection enchantment.
   * Maximum effect is 80% reduction from a modifier value of 20. Can also go negative, up to 180% increase from a modifier value of -20
   * <br/>
   * Alternatives:
   * <ul>
   *   <li>{@link DamageBlockModifierHook}: Allows canceling the attack entirely, including the hurt animation.</li>
   *   <li>{@link OnAttackedModifierHook}: Allows running logic that should take place on attack, such as counterattacks.</li>
   * </ul>
   * @param tool            Worn armor
   * @param modifier        Modifier level
   * @param context         Equipment context of the entity wearing the armor
   * @param slotType        Slot containing the armor
   * @param source          Damage source
   * @param modifierValue   Modifier value from previous modifiers to add
   * @return  New modifier value
   */
  float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float modifierValue);

  /** Gets the maximum protection amount on the given entity */
  @Deprecated(forRemoval = true)
  static float getProtectionCap(@Nullable TinkerDataCapability.Holder capability) {
    float bonus = capability != null ? capability.get(TinkerDataKeys.PROTECTION_CAP, 0f) : 0f;
    return Math.min(20 + bonus, 25 * 0.95f);
  }

  /** Gets the maximum protection amount on the given entity */
  static double getProtectionCap(LivingEntity living, @Nullable TinkerDataCapability.Holder capability) {
    float bonus = capability != null ? capability.get(TinkerDataKeys.PROTECTION_CAP, 0f) : 0f;
    return Math.min(living.getAttributeValue(TinkerAttributes.PROTECTION_CAP) * 25f + bonus, 25 * 0.95f);
  }

  /** Gets the maximum protection amount on the given entity */
  static double getProtectionCap(LivingEntity living) {
    return getProtectionCap(living, TinkerDataCapability.getData(living));
  }

  /** Merger that combines all values */
  record AllMerger(Collection<ProtectionModifierHook> modules) implements ProtectionModifierHook {
    @Override
    public float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float modifierValue) {
      for (ProtectionModifierHook module : modules) {
        modifierValue = module.getProtectionModifier(tool, modifier, context, slotType, source, modifierValue);
      }
      return modifierValue;
    }
  }
}
