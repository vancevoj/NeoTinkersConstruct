package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;

/**
 * Dispenser behavior for a modifiable arrow item.
 * <p>
 * 1.21: {@code AbstractProjectileDispenseBehavior} was removed in favor of the {@code ProjectileItem} interface. We instead
 * extend {@link DefaultDispenseItemBehavior} and build the arrow directly, preserving the old projectile dispense behavior
 * (power 1.1, inaccuracy 6, projectile launch sound/animation).
 */
public class ModifiableArrowDispenserBehavior extends DefaultDispenseItemBehavior {
  public static final ModifiableArrowDispenserBehavior INSTANCE = new ModifiableArrowDispenserBehavior();

  private ModifiableArrowDispenserBehavior() {}

  @Override
  public ItemStack execute(BlockSource source, ItemStack stack) {
    Level level = source.level();
    Position position = DispenserBlock.getDispensePosition(source);
    Direction direction = source.state().getValue(DispenserBlock.FACING);
    ModifiableArrow arrow = new ModifiableArrow(level, position.x(), position.y(), position.z());
    arrow.onCreate(stack, null);
    arrow.pickup = Pickup.ALLOWED;
    arrow.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), 1.1F, 6.0F);
    level.addFreshEntity(arrow);
    stack.shrink(1);
    return stack;
  }

  @Override
  protected void playSound(BlockSource source) {
    source.level().levelEvent(LevelEvent.SOUND_DISPENSER_PROJECTILE_LAUNCH, source.pos(), 0);
  }
}
