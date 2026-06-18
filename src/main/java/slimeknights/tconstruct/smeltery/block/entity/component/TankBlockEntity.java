package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TankBlockEntity extends SmelteryComponentBlockEntity implements ITankBlockEntity {
  /** Max capacity for the tank */
  public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;

  /**
   * Gets the capacity for the given block
   * @param block  block
   * @return  Capacity
   */
  public static int getCapacity(Block block) {
    if (block instanceof ITankBlock) {
      return ((ITankBlock) block).getCapacity();
    }
    return DEFAULT_CAPACITY;
  }

  /**
   * Gets the capacity for the given item
   * @param item  item
   * @return  Capacity
   */
  public static int getCapacity(Item item) {
    if (item instanceof BlockItem) {
      return getCapacity(((BlockItem)item).getBlock());
    }
    return DEFAULT_CAPACITY;
  }

  /** Internal fluid tank instance */
  @Getter
  protected final FluidTankAnimated tank;
  /** Last comparator strength to reduce block updates */
  @Getter @Setter
  private int lastStrength = -1;

  public TankBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, state.getBlock() instanceof ITankBlock tank
                     ? tank
                     : TinkerSmeltery.searedTank.get(TankType.FUEL_TANK));
  }

  /** Main constructor */
  public TankBlockEntity(BlockPos pos, BlockState state, ITankBlock block) {
    this(TinkerSmeltery.tank.get(), pos, state, block);
  }

  /** Extendable constructor */
  @SuppressWarnings("WeakerAccess")
  protected TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ITankBlock block) {
    super(type, pos, state);
    tank = new FluidTankAnimated(block.getCapacity(), this);
  }


  /*
   * Tank methods
   */

  /** Gets the fluid handler for the given side, used by {@code RegisterCapabilitiesEvent} */
  @Nullable
  public IFluidHandler getFluidHandler(@Nullable Direction side) {
    return tank;
  }

  @Nonnull
  @Override
  public ModelData getModelData() {
    return ModelData.builder()
                    .with(ModelProperties.FLUID_STACK, tank.getFluid())
                    .with(ModelProperties.TANK_CAPACITY, tank.getCapacity()).build();
  }

  /** Updates the light for this tank using {@link SearedTankBlock#LIGHT} */
  public static void updateLight(BlockEntity be, IFluidTank tank) {
    Level level = be.getLevel();
    if (level != null && !level.isClientSide) {
      FluidStack fluid = tank.getFluid();
      int light = fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getLightLevel(fluid);
      BlockState state = be.getBlockState();
      if (light != state.getValue(SearedTankBlock.LIGHT)) {
        level.setBlock(be.getBlockPos(), state.setValue(SearedTankBlock.LIGHT, light), Block.UPDATE_CLIENTS);
      }
    }
  }

  @Override
  public void onTankContentsChanged() {
    ITankBlockEntity.super.onTankContentsChanged();
    if (this.level != null) {
      updateLight(this, tank);
      this.requestModelDataUpdate();
    }
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (level != null && !level.isClientSide) {
      BlockPos masterPos = getMasterPos();
      if (masterPos != null && level.getBlockEntity(masterPos) instanceof IMasterLogic master) {
        master.onServantLoad(this);
      }
    }
  }

  /*
   * NBT
   */

  /**
   * Sets the tag on the stack based on the contained tank
   * @param stack  Stack
   */
  public void setTankTag(ItemStack stack) {
    TankItem.setTank(stack, tank);
  }

  /**
   * Updates the tank from an NBT tag, used in the block. Resolves the registry provider from the level.
   * @param nbt  tank NBT
   */
  public void updateTank(CompoundTag nbt) {
    updateTank(Objects.requireNonNull(getLevel(), "Tank block entity has no level").registryAccess(), nbt);
  }

  /**
   * Updates the tank from an NBT tag, used in the block
   * @param registries  Registry lookup provider for deserializing the component-backed fluid
   * @param nbt         tank NBT
   */
  public void updateTank(HolderLookup.Provider registries, CompoundTag nbt) {
    if (nbt.isEmpty()) {
      tank.setFluid(FluidStack.EMPTY);
    } else {
      tank.readFromNBT(registries, nbt);
      updateLight(this, tank);
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    tank.setCapacity(getCapacity(getBlockState().getBlock()));
    updateTank(registries, tag.getCompound(NBTTags.TANK));
    super.loadAdditional(tag, registries);
  }

  @Override
  public void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveSynced(tag, registries);
    // want tank on the client on world load
    if (!tank.isEmpty()) {
      tag.put(NBTTags.TANK, tank.writeToNBT(registries, new CompoundTag()));
    }
  }

  /** Interface for blocks to return their capacity */
  public interface ITankBlock {
    /** Gets the capacity for this tank */
    int getCapacity();
  }
}
