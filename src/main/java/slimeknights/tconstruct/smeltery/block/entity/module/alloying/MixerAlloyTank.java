package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Alloy tank that takes inputs from neighboring blocks
 */
@RequiredArgsConstructor
public class MixerAlloyTank implements IMutableAlloyTank {
  // parameters
  /** Handler parent */
  private final MantleBlockEntity parent;
  /** Tank for outputs */
  private final IFluidHandler outputTank;

  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter
  @Setter
  private int temperature = 0;

  // side tank cache
  /** Cache of present fluid handlers for each of the sides; absent key with present {@link #checked} entry means no handler */
  private final Map<Direction,IFluidHandler> inputs = new EnumMap<>(Direction.class);
  /** Set of sides already queried since the last refresh, so we can distinguish "no handler" from "not yet fetched" */
  private final Set<Direction> checked = EnumSet.noneOf(Direction.class);
  /** Map of tank index to tank on the side */
  @Nullable
  private IFluidHandler[] indexedList = null;

  // state
  /** If true, tanks are marked for refresh later */
  private boolean needsRefresh = true;
  /** Number of currently held tanks */
  private int currentTanks = 0;

  @Override
  public int getTanks() {
    checkTanks();
    return currentTanks;
  }

  /** Gets the map of index to direction */
  private IFluidHandler[] indexTanks() {
    // convert map into indexed list of fluid handlers, will be cleared next time a side updates
    if (indexedList == null) {
      indexedList = new IFluidHandler[currentTanks];
      if (currentTanks > 0) {
        int nextTank = 0;
        for (Direction direction : Direction.values()) {
          if (direction != Direction.DOWN) {
            IFluidHandler handler = inputs.get(direction);
            if (handler != null) {
              indexedList[nextTank] = handler;
              nextTank++;
            }
          }
        }
      }
    }
    return indexedList;
  }

  /** Gets the fluid handler for the given tank index */
  public IFluidHandler getFluidHandler(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return EmptyFluidHandler.INSTANCE;
    }
    return indexTanks()[tank];
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    // get the first fluid from the proper tank, we do not support multiple fluids on a side
    return indexTanks()[tank].getFluidInTank(0);
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    return indexTanks()[tank].drain(fluidStack, FluidAction.EXECUTE);
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    checkTanks();
    return outputTank.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount();
  }

  @Override
  public int fill(FluidStack fluidStack) {
    return outputTank.fill(fluidStack, FluidAction.EXECUTE);
  }

  /**
   * Refreshes the cached tanks if needed
   * After calling this method, all five tank sides will have been fetched
   */
  private void checkTanks() {
    // need world to do anything
    Level world = parent.getLevel();
    if (world == null) {
      return;
    }
    if (needsRefresh) {
      for (Direction direction : Direction.values()) {
        // update each direction we are missing
        if (direction != Direction.DOWN && !checked.contains(direction)) {
          BlockPos target = parent.getBlockPos().relative(direction);
          // limit by blocks as that gives the modpack more control, say they want to allow only scorched tanks
          if (world.getBlockState(target).is(TinkerTags.Blocks.ALLOYER_TANKS)) {
            // if we found a tank, increment the number of tanks
            IFluidHandler capability = world.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
            checked.add(direction);
            if (capability != null) {
              inputs.put(direction, capability);
              currentTanks++;
            }
          }
        }
      }
      needsRefresh = false;
    }
  }

  /**
   * Called on block update or when a capability invalidates to mark that a direction needs updates
   * @param direction  Side updating
   * @param checkInput If true, validates that the side contains an input before reducing tank count. False when invalidated through the capability
   * */
  public void refresh(Direction direction, boolean checkInput) {
    if (direction == Direction.DOWN) {
      return;
    }
    if (!checkInput || inputs.get(direction) != null) {
      currentTanks--;
    }
    inputs.remove(direction);
    checked.remove(direction);
    needsRefresh = true;
    indexedList = null;
  }
}
