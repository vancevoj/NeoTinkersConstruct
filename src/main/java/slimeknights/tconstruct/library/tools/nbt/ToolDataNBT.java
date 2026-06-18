package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.SlotType;

/**
 * Extension of {@link ModDataNBT} adding support for modifier slots.
 * Typically, slots are only directly modifiable by the tool, though all contexts are free to view them.
 */
public class ToolDataNBT extends ModDataNBT {
  /** Callback invoked on mutation so an owning tool stack can flush the change to its data component. May be null. */
  @Nullable
  private final Runnable onDirty;

  public ToolDataNBT() {
    this.onDirty = null;
  }

  protected ToolDataNBT(CompoundTag nbt) {
    super(nbt);
    this.onDirty = null;
  }

  protected ToolDataNBT(CompoundTag nbt, @Nullable Runnable onDirty) {
    super(nbt);
    this.onDirty = onDirty;
  }

  @Override
  protected void markDirty() {
    if (onDirty != null) {
      onDirty.run();
    }
  }

  @Override
  public int getSlots(SlotType type) {
    return getData().getInt(type.getName());
  }

  /**
   * Sets the slots for the given type
   * @param type   Slot type
   * @param value  New value
   */
  public void setSlots(SlotType type, int value) {
    if (value == 0) {
      getData().remove(type.getName());
    } else {
      getData().putInt(type.getName(), value);
    }
    markDirty();
  }

  /**
   * Adds the given number of slots
   * @param type   Slot type
   * @param add    Value to add, use negative to remove
   */
  public void addSlots(SlotType type, int add) {
    if (add != 0) {
      setSlots(type, getSlots(type) + add);
    }
  }


  /**
   * Parses the mod data from NBT
   * @param data  data
   * @return  Parsed mod data
   */
  public static ToolDataNBT readFromNBT(CompoundTag data) {
    return new ToolDataNBT(data);
  }

  /**
   * Parses the mod data from NBT, wiring a dirty callback so mutations can be flushed to an owning tool stack's component.
   * @param data     data compound (a child of the tool's full NBT)
   * @param onDirty  callback to run after any mutation, or null for standalone data
   * @return  Parsed mod data
   */
  public static ToolDataNBT readFromNBT(CompoundTag data, @Nullable Runnable onDirty) {
    return new ToolDataNBT(data, onDirty);
  }
}
