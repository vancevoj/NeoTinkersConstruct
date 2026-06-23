package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import slimeknights.mantle.block.entity.MantleBlockEntity;
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
   * Server-side cache of the item handler at the fuel position. {@link BlockCapabilityCache} auto-tracks invalidation
   * and refetches, so it self-heals across neighbor changes and world reloads. Null until first resolved; only valid
   * on a {@link ServerLevel}.
   */
  @Nullable
  private BlockCapabilityCache<IItemHandler,?> itemHandlerCache;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent);
    this.fuelPos = fuelPos;
  }


  /* Capability resolution */

  /** Resolves the fluid handler at the fuel position, self-healing on the server and re-querying on the client */
  @Nullable
  private IFluidHandler getFluidHandler() {
    fluidHandler = getHandlerAt(fuelPos);
    return fluidHandler;
  }

  /** Resolves the item handler at the fuel position, self-healing on the server and re-querying on the client */
  @Nullable
  private IItemHandler getItemHandler() {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      if (itemHandlerCache == null) {
        itemHandlerCache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, fuelPos, null);
      }
      return itemHandlerCache.getCapability();
    }
    // client: always re-query so a transiently-missing capability recovers after reload
    return level.getCapability(Capabilities.ItemHandler.BLOCK, fuelPos, null);
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

  @Override
  public int findFuel(boolean consume) {
    // prioritize liquid fuel - it usually goes hotter
    int temperature = 0;
    IFluidHandler fluidHandler = getFluidHandler();
    if (fluidHandler != null) {
      temperature = tryLiquidFuel(fluidHandler, consume);
    }
    // next, try solid fuel
    if (temperature == 0) {
      IItemHandler itemHandler = getItemHandler();
      if (itemHandler != null) {
        temperature = trySolidFuel(itemHandler, consume);
      }
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
    // resolve the fluid handler so the base implementation has it available (re-queried each call so it survives reload)
    getFluidHandler();

    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && getItemHandler() != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }


  /* Fluid handler */

  /** Gets the fluid handler for proxy */
  public IFluidHandler getTank() {
    IFluidHandler fluidHandler = getFluidHandler();
    if (fluidHandler != null) {
      return fluidHandler;
    }
    return EmptyFluidHandler.INSTANCE;
  }
}
