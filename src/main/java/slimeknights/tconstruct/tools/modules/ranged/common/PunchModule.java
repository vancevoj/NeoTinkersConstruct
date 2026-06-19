package slimeknights.tconstruct.tools.modules.ranged.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithKnockback;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;
import java.util.List;

/** Module implementing the punch modifier */
public record PunchModule(LevelingValue amount, ModifierCondition<IToolStackView> condition) implements ModifierModule, ProjectileLaunchModifierHook.NoShooter, ProjectileHitModifierHook, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<PunchModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_SHOT, ModifierHooks.PROJECTILE_HIT);
  /** Persistent data key storing punch knockback for arrows that are not {@link ProjectileWithKnockback} (e.g. vanilla arrows fired from a Tinkers' bow). Applied on hit, replicating {@code AbstractArrow#doKnockback}. */
  private static final ResourceLocation KNOCKBACK_KEY = TConstruct.getResource("punch_knockback");
  public static final RecordLoadable<PunchModule> LOADER = RecordLoadable.create(LevelingValue.LOADABLE.directField(PunchModule::amount), ModifierCondition.TOOL_FIELD, PunchModule::new);

  @Override
  public RecordLoadable<PunchModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity shooter, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    if (condition.matches(tool, modifier)) {
      float amount = this.amount.compute(modifier.getEffectiveLevel());
      if (amount > 0) {
        if (arrow instanceof ProjectileWithKnockback withKnockback) {
          // ModifiableArrow implements ProjectileWithKnockback to add knockback in its own doKnockback override
          withKnockback.addKnockback(amount);
        } else if (arrow != null) {
          // AbstractArrow.setKnockback(int) was removed in 1.21 (vanilla arrow knockback is now derived from the firing weapon's
          // Punch enchantment). For non-ModifiableArrow arrows store the punch knockback in persistent data and apply it on hit
          // via onProjectileHitEntity, replicating vanilla AbstractArrow#doKnockback.
          persistentData.putFloat(KNOCKBACK_KEY, persistentData.getFloat(KNOCKBACK_KEY) + amount);
        } else if (projectile instanceof ProjectileWithKnockback withKnockback) {
          withKnockback.addKnockback(amount);
        }
      }
    }
  }

  @Override
  public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target, boolean notBlocked) {
    // applies the knockback stored at launch for non-ModifiableArrow arrows, matching vanilla AbstractArrow#doKnockback
    if (notBlocked && target != null && projectile.level() instanceof ServerLevel) {
      float knockback = persistentData.getFloat(KNOCKBACK_KEY);
      if (knockback > 0) {
        double resistance = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = projectile.getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.6 * resistance);
        if (push.lengthSqr() > 0) {
          target.push(push.x, 0.1, push.z);
        }
      }
    }
    return false;
  }
}
