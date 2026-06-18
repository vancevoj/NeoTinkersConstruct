package slimeknights.tconstruct.gadgets.entity.shuriken;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** @deprecated use {@link ThrownShuriken} */
@Deprecated
public abstract class ShurikenEntityBase extends ThrowableItemProjectile implements IEntityWithComplexSpawn, ToolProjectile {

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, Level worldIn) {
    super(type, worldIn);
  }

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, double x, double y, double z, Level worldIn) {
    super(type, x, y, z, worldIn);
  }

  public ShurikenEntityBase(EntityType<? extends ShurikenEntityBase> type, LivingEntity livingEntityIn, Level worldIn) {
    super(type, livingEntityIn, worldIn);
  }

    /**
   * Get damage dealt by Shuriken
   * Should be <= 20.0F
   * @return float damage
   */
  public abstract float getDamage();

  /**
   * Get knockback dealt by Shuriken
   * Should be <= 1.0F, Minecraft
   * typically uses values from 0.2F-0.6F
   * @return float knockback
   */
  public abstract float getKnockback();

  @Override
  protected void onHit(HitResult result) {
    super.onHit(result);

    Level level = level();
    if (!level.isClientSide) {
      level.broadcastEntityEvent(this, (byte) 3); // TODO: find the proper constant for this event ID
      this.discard();
    }
  }

  @Override
  protected void onHitBlock(BlockHitResult result) {
    super.onHitBlock(result);

    this.spawnAtLocation(getDefaultItem());
  }

  @Override
  protected void onHitEntity(EntityHitResult result) {
    Entity entity = result.getEntity();
    entity.hurt(damageSources().thrown(this, this.getOwner()), this.getDamage());

    if (!level().isClientSide() && entity instanceof LivingEntity) {
      Vec3 motion = this.getDeltaMovement().normalize();
      ((LivingEntity) entity).knockback(this.getKnockback(), -motion.x, -motion.z);
    }
  }

  @Override
  public ItemStack getDisplayTool() {
    return getItem();
  }

  @Override
  public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.getItemRaw());
  }

  @Override
  public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
    this.setItem(ItemStack.OPTIONAL_STREAM_CODEC.decode(additionalData));
  }
}
