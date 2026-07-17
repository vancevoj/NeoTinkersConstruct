package slimeknights.tconstruct.fluids;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.item.ContainerFoodItem.FluidContainerFoodItem;
import slimeknights.tconstruct.fluids.item.MagmaBottleItem;
import slimeknights.tconstruct.fluids.item.PotionBucketItem;
import slimeknights.tconstruct.fluids.util.ConstantFluidContainerWrapper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.item.CopperCanFluidHandler;

/**
 * Event subscriber for modifier events
 * Note the way the subscribers are set up, technically works on anything that has the tic_modifiers tag
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.GAME)
public class FluidEvents {
  @SubscribeEvent
  static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
    if (event.getItemStack().getItem() == TinkerFluids.blazingBlood.asItem()) {
      // 150% efficiency compared to lava bucket, compare to casting blaze rods, which cast into 120%
      event.setBurnTime(30000);
    }
  }

  /**
   * Mod-bus subscriber registering the item fluid handler capabilities.
   * <p>
   * In NeoForge 1.21.1 item capabilities are no longer attached via {@code AttachCapabilitiesEvent}; they are registered
   * once per item in {@link RegisterCapabilitiesEvent} using {@code Capabilities.FluidHandler.ITEM}.
   */
  @EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.MOD)
  public static class ModBusEvents {
    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
      // fluid food containers (bottles of slime/venom)
      event.registerItem(Capabilities.FluidHandler.ITEM,
        (stack, ctx) -> ((FluidContainerFoodItem)stack.getItem()).getFluidHandler(stack), TinkerFluids.venomBottle.get());
      for (Item item : TinkerFluids.slimeBottle.values()) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
          (stack, ctx) -> ((FluidContainerFoodItem)stack.getItem()).getFluidHandler(stack), item);
      }
      // magma bottle
      event.registerItem(Capabilities.FluidHandler.ITEM,
        (stack, ctx) -> ((MagmaBottleItem)stack.getItem()).getFluidHandler(stack), TinkerFluids.magmaBottle.get());
      // potion bucket
      event.registerItem(Capabilities.FluidHandler.ITEM,
        (stack, ctx) -> ((PotionBucketItem)stack.getItem()).getFluidHandler(stack), TinkerFluids.potion.asItem());
      // vanilla powder snow bucket gets a constant powdered snow fluid handler
      event.registerItem(Capabilities.FluidHandler.ITEM,
        (stack, ctx) -> new ConstantFluidContainerWrapper(new FluidStack(TinkerFluids.powderedSnow.get(), FluidType.BUCKET_VOLUME), stack, Items.BUCKET.getDefaultInstance()),
        Items.POWDER_SNOW_BUCKET);
      // copper can, holds a single ingot of fluid
      event.registerItem(Capabilities.FluidHandler.ITEM,
        (stack, ctx) -> new CopperCanFluidHandler(stack), TinkerSmeltery.copperCan.get());
    }
  }
}
