package slimeknights.tconstruct.fluids.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.function.Supplier;

public class ContainerFoodItem extends Item {
  public ContainerFoodItem(Properties props) {
    super(props);
  }

  @Override
  public int getUseDuration(ItemStack pStack, LivingEntity living) {
    return 32;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack pStack) {
    return UseAnim.DRINK;
  }

  /** Adds effects to the tooltip */
  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip) {
    // add effects to the tooltip, code based on potion items
    for (PossibleEffect possible : food.effects()) {
      MobEffectInstance effect = possible.effect();
      MutableComponent mutable = Component.translatable(effect.getDescriptionId());
      if (effect.getAmplifier() > 0) {
        mutable = Component.translatable("potion.withAmplifier", mutable, Component.translatable("potion.potency." + effect.getAmplifier()));
      }
      if (effect.getDuration() > 20) {
        mutable = Component.translatable("potion.withDuration", mutable, MobEffectUtil.formatDuration(effect, 1.0f, 20.0f));
      }
      tooltip.add(mutable.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
    }
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    FoodProperties food = stack.get(DataComponents.FOOD);
    if (food != null) {
      addEffectTooltip(food, tooltip);
    }
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    ItemStack container = stack.getCraftingRemainingItem();
    ItemStack result = super.finishUsingItem(stack, level, living);
    Player player = living instanceof Player p ? p : null;
    if (player == null || !player.getAbilities().instabuild) {
      container = container.copy();
      if (result.isEmpty()) {
        return container;
      }
      if (player != null) {
        if (!player.getInventory().add(container)) {
          player.drop(container, false);
        }
      }
    }
    return result;
  }

  /**
   * Fluid containing variant of {@link ContainerFoodItem}.
   * <p>
   * In NeoForge 1.21.1 item capabilities are no longer provided via {@code initCapabilities}; the owning mod registers
   * the fluid handler for this item in {@code RegisterCapabilitiesEvent} via
   * {@code event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> item.getFluidHandler(stack), item)}.
   * {@link #getFluidHandler(ItemStack)} builds the handler for that registration.
   */
  public static class FluidContainerFoodItem extends ContainerFoodItem {
    private final Supplier<FluidStack> fluid;
    public FluidContainerFoodItem(Properties props, Supplier<FluidStack> fluid) {
      super(props);
      this.fluid = fluid;
    }

    /** Builds the fluid handler for the given stack, for registration in {@code RegisterCapabilitiesEvent}. */
    public IFluidHandlerItem getFluidHandler(ItemStack stack) {
      return new ConstantFluidContainerWrapper(fluid.get(), stack);
    }
  }
}
