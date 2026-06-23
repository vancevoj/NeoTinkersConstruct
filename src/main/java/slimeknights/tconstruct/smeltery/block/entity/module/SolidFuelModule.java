package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

import javax.annotation.Nullable;

/** Fuel module variant that supports both item and fluid fuels. Only supports a single fluid position which should not change. */
public class SolidFuelModule extends FuelModule {
  /** Location of the fuel tank */
  private final BlockPos fuelPos;
  /**
   * Last item handler where items were extracted. Null when not yet fetched.
   * Distinct from a successfully fetched but absent handler, tracked via {@link #fetched}.
   */
  @Nullable
  private IItemHandler itemHandler;
  /** True once we have queried the neighbor caps; cleared by {@link #resetHandler} so we re-query */
  private boolean fetched = false;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent);
    this.fuelPos = fuelPos;
  }

  @Override
  protected void resetHandler(@Nullable IFluidHandler source) {
    // clear both handlers to ensure cleanest refetch
    if (source == null || source == fluidHandler) {
      itemHandler = null;
      fluidHandler = null;
      fetched = false;
    }
  }


  /* Fuel updating */

  /**
   * Tries to consume fuel from the given fluid handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int trySolidFuel(IItemHandler handler, boolean consume) {
    for (int i = 0; i < handler.getSlots(); i++) {
      ItemStack stack = handler.getStackInSlot(i);
      int time = stack.getBurnTime(TinkerRecipeTypes.FUEL.get()) / 4;
      if (time > 0) {
        MeltingFuel solid = MeltingFuelLookup.getSolid();
        if (consume) {
          ItemStack extracted = handler.extractItem(i, 1, false);
          if (ItemStack.isSameItem(extracted, stack)) {
            fuel += time;
            fuelQuality = time;
            temperature = solid.getTemperature();
            rate = solid.getRate();
            parent.setChangedFast();
            // return the container
            ItemStack container = extracted.getCraftingRemainingItem();
            if (!container.isEmpty()) {
              // if we cannot insert the container back, spit it on the ground
              ItemStack notInserted = ItemHandlerHelper.insertItem(handler, container, false);
              if (!notInserted.isEmpty()) {
                Level world = getLevel();
                double x = (world.random.nextFloat() * 0.5F) + 0.25D;
                double y = (world.random.nextFloat() * 0.5F) + 0.25D;
                double z = (world.random.nextFloat() * 0.5F) + 0.25D;
                ItemEntity itementity = new ItemEntity(world, fuelPos.getX() + x, fuelPos.getY() + y, fuelPos.getZ() + z, container);
                itementity.setDefaultPickUpDelay();
                world.addFreshEntity(itementity);
              }
            }
          } else {
            TConstruct.LOG.error("Invalid item removed from solid fuel handler");
          }
        }
        return solid.getTemperature();
      }
    }
    return 0;
  }

  /** Fetches any relevant fuel handlers from the target position */
  private void fetchHandlers() {
    // if we already resolved a non-null handler, nothing to do
    if (fluidHandler != null || itemHandler != null) {
      return;
    }
    // NeoForge block capabilities resolve lazily: on the client (and immediately after a structure/neighbor change)
    // the neighbor tank's capability may not be available the first time the GUI queries it. Upstream wrapped these in
    // LazyOptionals with invalidation listeners so the cache self-healed; here we instead re-query every time both
    // handlers are still null so a transiently-missing capability (which made the fuel tank render empty / read 0
    // temperature -> "not hot enough") recovers on a later display tick. resetHandler() still clears them on changes.
    Level level = getLevel();
    // first, identify a capability that has what we need
    // on the chance both are present, we prioritize fluid; we don't expect that to change
    fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, fuelPos, null);
    itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, fuelPos, null);
    fetched = true;
  }

  @Override
  public int findFuel(boolean consume) {
    fetchHandlers();

    // prioritize liquid fuel - it usually goes hotter
    int temperature = 0;
    if (fluidHandler != null) {
      temperature = tryLiquidFuel(fluidHandler, consume);
    }
    // next, try solid fuel
    if (temperature == 0 && itemHandler != null) {
      temperature = trySolidFuel(itemHandler, consume);
    }
    // no handler found, tell client of the lack of fuel
    if (temperature == 0 && consume) {
      this.temperature = 0;
      this.rate = 0;
    }
    return temperature;
  }


  /* UI Syncing */

  @Override
  public FuelInfo getFuelInfo() {
    fetchHandlers();

    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && itemHandler != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }


  /* Fluid handler */

  /** Gets the fluid handler for proxy */
  public IFluidHandler getTank() {
    if (fluidHandler != null) {
      return fluidHandler;
    }
    return EmptyFluidHandler.INSTANCE;
  }
}
