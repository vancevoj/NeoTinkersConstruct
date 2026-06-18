package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.world.effect.MobEffectCategory;
import slimeknights.tconstruct.common.TinkerEffect;

/**
 * Effect that cannot be cured with milk
 * TODO 1.21: move to {@link slimeknights.tconstruct.shared.effect}
 */
public class NoMilkEffect extends TinkerEffect {
  public NoMilkEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color, show);
  }

  // TODO(neoport): no-milk cure behavior now handled via MobEffectInstance cure tags, override removed
}
