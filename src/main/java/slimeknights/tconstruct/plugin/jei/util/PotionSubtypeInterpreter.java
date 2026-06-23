package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

import javax.annotation.Nullable;

/**
 * Common logic for subtype interpreter between the fluid and item form of our potion. Based on a JEI class with the
 * same name.
 * <p>
 * 1.21: potion data moved from a legacy {@code CompoundTag} (read via the removed {@code PotionUtils}) to the
 * {@link PotionContents} data component, so implementations now supply the {@link PotionContents} directly.
 */
public interface PotionSubtypeInterpreter<T> extends IIngredientSubtypeInterpreter<T> {
  /** Gets the potion contents from the ingredient, or null if it has none */
  @Nullable
  PotionContents getPotionContents(T ingredient);

  @Override
  default String apply(T ingredient, UidContext context) {
    PotionContents contents = getPotionContents(ingredient);
    if (contents == null || contents.equals(PotionContents.EMPTY)) {
      return IIngredientSubtypeInterpreter.NONE;
    }
    StringBuilder stringBuilder = new StringBuilder();
    contents.potion().ifPresent(potion -> stringBuilder.append(potion.getRegisteredName()));
    for (MobEffectInstance effect : contents.getAllEffects()) {
      stringBuilder.append(";").append(effect);
    }
    String result = stringBuilder.toString();
    return result.isEmpty() ? IIngredientSubtypeInterpreter.NONE : result;
  }
}
