package slimeknights.tconstruct.tools.modifiers.traits.ranged;

import net.minecraft.tags.EntityTypeTags;
import slimeknights.mantle.data.predicate.entity.MobTypePredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.combat.ConditionalPowerModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;

/** @deprecated use {@link ConditionalPowerModule} */
@Deprecated(forRemoval = true)
public class HolyModifier extends Modifier {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    // MobType was removed in 1.21; undead is now an entity type tag
    hookBuilder.addModule(ConditionalPowerModule.builder().target(new MobTypePredicate(EntityTypeTags.UNDEAD)).eachLevel(0.75f));
  }
}
