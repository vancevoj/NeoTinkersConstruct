package slimeknights.tconstruct.smeltery.block.entity.module;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Module handling fuel consumption for the melter and smeltery
 */
@RequiredArgsConstructor
public abstract class FuelModule implements ContainerData {
  /** Parent TE */
  protected final MantleBlockEntity parent;

  /** Last fuel recipe used */
  @Nullable
  private MeltingFuel lastRecipe;
  /**
   * Last fluid handler where fluid was extracted.
   * In NeoForge capabilities are nullable rather than {@code LazyOptional}; this caches the most recent block
   * capability and is reset via {@link #resetHandler(IFluidHandler)} on structure/neighbor changes.
   */
  @Nullable
  protected IFluidHandler fluidHandler;

  /** Current amount of fluid in the TE */
  @Getter
  protected int fuel = 0;
  /** Amount of fuel produced by the last source */
  @Getter
  protected int fuelQuality = 0;
  /** Temperature of the current fuel */
  @Getter
  protected int temperature = 0;
  /** Amount to progress recipes by per time step */
  @Getter
  protected int rate = 0;


  /*
   * Helpers
   */

  /**
   * Server-side caches of the fluid handler at each fuel-tank position. {@link BlockCapabilityCache} auto-tracks
   * capability invalidation (neighbor change, BE unload/reload, etc.) and refetches, restoring the self-healing
   * behavior upstream had with {@code LazyOptional} invalidation listeners. Only valid on a {@link ServerLevel};
   * the client uses a fresh {@link Level#getCapability} query each call (see {@link #getHandlerAt}).
   */
  private final Map<BlockPos,BlockCapabilityCache<IFluidHandler,?>> serverHandlerCaches = new HashMap<>();

  /** Called when the capability invalidates to reset any cached handlers */
  protected void resetHandler(@Nullable IFluidHandler source) {
    if (source == null || source == fluidHandler) {
      fluidHandler = null;
    }
  }

  /** Gets a nonnull world instance from the parent */
  protected Level getLevel() {
    return Objects.requireNonNull(parent.getLevel(), "Parent tile entity has null world");
  }

  /**
   * Resolves the fluid handler at the given position in a way that survives world reloads and capability invalidation.
   * On the server a {@link BlockCapabilityCache} is used so the handler self-heals (refetched automatically when the
   * neighbor tank's capability invalidates). On the client (where the fuel DISPLAY runs) we cannot build a capability
   * cache (it requires a {@link ServerLevel}), so we re-query every call and never permanently cache a null or stale
   * handler - this is what makes the GUI recover after a world reload.
   * @param pos  Position to resolve, should not be mutated externally
   * @return  Fluid handler, or null if none present
   */
  @Nullable
  protected IFluidHandler getHandlerAt(BlockPos pos) {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      // immutable copy as the cache holds the reference long-term
      BlockPos key = pos.immutable();
      return serverHandlerCaches.computeIfAbsent(key, p -> BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, serverLevel, p, null)).getCapability();
    }
    // client: always re-query so a transiently-missing capability (e.g. right after a reload) recovers on a later tick
    return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
  }

  /** Clears any server-side capability caches; called when the set of relevant positions may have changed */
  protected void invalidateHandlerCaches() {
    serverHandlerCaches.clear();
  }


  /**
   * Finds a recipe for the given fluid
   * @param fluid  Fluid
   * @return  Recipe
   */
  @Nullable
  protected MeltingFuel findRecipe(Fluid fluid) {
    if (lastRecipe != null && lastRecipe.matches(fluid)) {
      return lastRecipe;
    }
    MeltingFuel recipe = MeltingFuelLookup.findFuel(fluid);
    if (recipe != null) {
      lastRecipe = recipe;
    }
    return recipe;
  }


  /* Fuel attributes */

  /**
   * Checks if we have fuel
   * @return  True if we have fuel
   */
  public boolean hasFuel() {
    return fuel > 0;
  }

  /**
   * Consumes fuel from the module
   * @param amount  Amount of fuel to consume
   */
  public void decreaseFuel(int amount) {
    fuel = Math.max(0, fuel - amount);
    parent.setChangedFast();
  }


  /* Fuel updating */

  /**
   * Trys to consume fuel from the given fluid handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  protected int tryLiquidFuel(IFluidHandler handler, boolean consume) {
    FluidStack fluid = handler.getFluidInTank(0);
    MeltingFuel recipe = findRecipe(fluid.getFluid());
    if (recipe != null) {
      int amount = recipe.getAmount(fluid.getFluid());
      if (fluid.getAmount() >= amount) {
        if (consume) {
          FluidStack drained = handler.drain(fluid.copyWithAmount(amount), FluidAction.EXECUTE);
          if (drained.getAmount() != amount) {
            TConstruct.LOG.error("Invalid amount of fuel drained from tank");
          }
          fuel += recipe.getDuration();
          fuelQuality = recipe.getDuration();
          temperature = recipe.getTemperature();
          rate = recipe.getRate();
          parent.setChangedFast();
          return temperature;
        } else {
          return recipe.getTemperature();
        }
      }
    }
    return 0;
  }

  /**
   * Attempts to consume fuel from one of the tanks
   * @return  temperature of the found fluid, 0 if none
   */
  public abstract int findFuel(boolean consume);

  /* NBT */
  private static final String TAG_FUEL = "fuel";
  private static final String TAG_TEMPERATURE = "temperature";
  private static final String TAG_RATE = "rate";

  /**
   * Reads the fuel from NBT
   * @param nbt  Tag to read from
   */
  public void readFromTag(CompoundTag nbt) {
    if (nbt.contains(TAG_FUEL, Tag.TAG_ANY_NUMERIC)) {
      fuel = nbt.getInt(TAG_FUEL);
    }
    if (nbt.contains(TAG_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) {
      temperature = nbt.getInt(TAG_TEMPERATURE);
      rate = nbt.getInt(TAG_RATE);
    }
  }

  /**
   * Writes the fuel to NBT
   * @param nbt  Tag to write to
   * @return  Tag written to
   */
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt.putInt(TAG_FUEL, fuel);
    nbt.putInt(TAG_TEMPERATURE, temperature);
    nbt.putInt(TAG_RATE, rate);
    return nbt;
  }


  /* UI syncing */
  private static final int FUEL = 0;
  private static final int FUEL_QUALITY = 1;
  private static final int TEMPERATURE = 2;
  private static final int RATE = 3;

  @Override
  public int getCount() {
    return 4;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case FUEL         -> fuel;
      case FUEL_QUALITY -> fuelQuality;
      case TEMPERATURE  -> temperature;
      case RATE         -> rate;
      default -> 0;
    };
  }

  @Override
  public void set(int index, int value) {
    switch (index) {
      case FUEL         -> fuel = value;
      case FUEL_QUALITY -> fuelQuality = value;
      case TEMPERATURE  -> temperature = value;
      case RATE         -> rate = value;
    }
  }

  /**
   * Called client side to get the fuel info for the current tank
   * Note this relies on the client side fuel handlers containing fuel, which is common for our blocks as show fluid in world.
   * If a tank does not do that this won't work.
   * @return  Fuel info
   */
  public FuelInfo getFuelInfo() {
    IFluidHandler handler = fluidHandler;
    if (handler == null) {
      return FuelInfo.EMPTY;
    }
    FluidStack fluid = handler.getFluidInTank(0);
    int temperature = 0;
    if (!fluid.isEmpty()) {
      MeltingFuel fuel = findRecipe(fluid.getFluid());
      if (fuel != null) {
        temperature = fuel.getTemperature();
      }
    }
    // if no liquid, fallback to either item or empty
    return FuelInfo.of(fluid, handler.getTankCapacity(0), temperature);
  }

  /** Data class to hold information about the current fuel */
  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class FuelInfo {
    /** Empty fuel instance */
    public static final FuelInfo EMPTY = new FuelInfo(FluidStack.EMPTY, 0, 0, 0);
    /** Item fuel instance, doesn't really matter what data we set as it will all be ignored */
    public static final FuelInfo ITEM = new FuelInfo(FluidStack.EMPTY, 0, 0, 0);

    private final FluidStack fluid;
    private int totalAmount;
    private int capacity;
    private final int temperature;

    /**
     * Gets fuel info from the given stack and capacity
     * @param fluid     Fluid
     * @param capacity  Capacity
     * @return  Fuel info
     */
    public static FuelInfo of(FluidStack fluid, int capacity, int temperature) {
      if (fluid.isEmpty()) {
        return EMPTY;
      }
      return new FuelInfo(fluid, fluid.getAmount(), Math.max(capacity, fluid.getAmount()), temperature);
    }

    /**
     * Adds an additional amount and capacity to this info
     * @param amount    Amount to add
     * @param capacity  Capacity to add
     */
    protected void add(int amount, int capacity) {
      this.totalAmount += amount;
      this.capacity += capacity;
    }

    /**
     * Checks if this fuel info is an item
     * @return  True if an item
     */
    public boolean isItem() {
      return this == ITEM;
    }

    /** Checks if this fuel info has no fluid */
    public boolean isEmpty() {
      return fluid.isEmpty() || totalAmount == 0 || capacity == 0;
    }
  }
}
