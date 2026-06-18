package slimeknights.tconstruct.fluids.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Supplier;

/** Implements filling a bucket with an NBT fluid */
public class PotionBucketItem extends PotionItem {
  private final Supplier<? extends Fluid> supplier;
  public PotionBucketItem(Supplier<? extends Fluid> supplier, Properties builder) {
    super(builder);
    this.supplier = supplier;
  }

  public Fluid getFluid() {
    return supplier.get();
  }

  /** Gets the potion contents stored on the stack */
  private static PotionContents getContents(ItemStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null ? contents : PotionContents.EMPTY;
  }

  @Override
  public String getDescriptionId(ItemStack stack) {
    String bucketKey = Potion.getName(getContents(stack).potion(), getDescriptionId() + ".effect.");
    if (Util.canTranslate(bucketKey)) {
      return bucketKey;
    }
    return super.getDescriptionId();
  }

  @Override
  public Component getName(ItemStack stack) {
    Optional<Holder<Potion>> potion = getContents(stack).potion();
    String bucketKey = Potion.getName(potion, getDescriptionId() + ".effect.");
    if (Util.canTranslate(bucketKey)) {
      return Component.translatable(bucketKey);
    }
    // default to filling with the contents
    return Component.translatable(getDescriptionId() + ".contents", Component.translatable(Potion.getName(potion, "item.minecraft.potion.effect.")));
  }

  @Override
  public ItemStack getDefaultInstance() {
    return PotionContents.createItemStack(this, Potions.AWKWARD);
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    Player player = living instanceof Player p ? p : null;
    if (player instanceof ServerPlayer serverPlayer) {
      CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
    }

    // effects are 2x duration
    if (!level.isClientSide) {
      for (MobEffectInstance effect : getContents(stack).getAllEffects()) {
        Holder<MobEffect> mobEffect = effect.getEffect();
        if (mobEffect.value().isInstantenous()) {
          mobEffect.value().applyInstantenousEffect(player, player, living, effect.getAmplifier(), 2.5D);
        } else {
          MobEffectInstance newEffect = new MobEffectInstance(effect);
          newEffect.duration = newEffect.duration * 5 / 2;
          living.addEffect(newEffect);
        }
      }
    }

    if (player != null) {
      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
        stack.shrink(1);
      }
    }

    if (player == null || !player.getAbilities().instabuild) {
      if (stack.isEmpty()) {
        return new ItemStack(Items.BUCKET);
      }
      if (player != null) {
        player.getInventory().add(new ItemStack(Items.BUCKET));
      }
    }
    living.gameEvent(GameEvent.DRINK);
    return stack;
  }

  @Override
  public int getUseDuration(ItemStack pStack, LivingEntity living) {
    return 96; // 3x duration of potion bottles
  }

  /** Builds the fluid handler for the given stack, for registration in {@code RegisterCapabilitiesEvent}. */
  public IFluidHandlerItem getFluidHandler(ItemStack stack) {
    return new PotionBucketWrapper(stack);
  }

  public static class PotionBucketWrapper extends FluidBucketWrapper {
    public PotionBucketWrapper(ItemStack container) {
      super(container);
    }

    @Nonnull
    @Override
    public FluidStack getFluid() {
      FluidStack fluid = new FluidStack(((PotionBucketItem)container.getItem()).getFluid(), FluidType.BUCKET_VOLUME);
      PotionContents contents = container.get(DataComponents.POTION_CONTENTS);
      if (contents != null) {
        fluid.set(DataComponents.POTION_CONTENTS, contents);
      }
      return fluid;
    }
  }
}
