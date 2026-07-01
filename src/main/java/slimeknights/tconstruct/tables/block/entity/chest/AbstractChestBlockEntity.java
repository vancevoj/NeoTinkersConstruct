package slimeknights.tconstruct.tables.block.entity.chest;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;

import javax.annotation.Nullable;

/** Shared base logic for all Tinkers' chest tile entities */
public abstract class AbstractChestBlockEntity extends NameableBlockEntity {
  private static final String KEY_ITEMS = "Items";

  @Getter
  private final IChestItemHandler itemHandler;
  protected AbstractChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, IChestItemHandler itemHandler) {
    super(type, pos, state, name);
    itemHandler.setParent(this);
    this.itemHandler = itemHandler;
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new TinkerChestContainerMenu(menuId, playerInventory, this);
  }

  /**
   * Checks if the given item should be inserted into the chest on interact
   * @param player    Player inserting
   * @param heldItem  Stack to insert
   * @return  Return true
   */
  public boolean canInsert(Player player, ItemStack heldItem) {
    return true;
  }

  @Override
  public void saveAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveAdditional(tags, registries);
    // move the items from the serialized result
    // we don't care about the size and need it here for compat with old worlds
    CompoundTag handlerNBT = itemHandler.serializeNBT(registries);
    tags.put(KEY_ITEMS, handlerNBT.getList(KEY_ITEMS, Tag.TAG_COMPOUND));
  }

  /**
   * Reads the inventory from NBT, resiliently. Each stack is parsed independently so a single corrupt or
   * unparseable item can never take down the whole load. Previously any exception here propagated out of
   * {@link #loadAdditional}, and vanilla's {@code BlockEntity.loadStatic} discards the entire block entity when
   * loading throws, so the chest reloaded completely empty (every stored item lost). Bad slots are now logged
   * and skipped, keeping every item that still parses.
   */
  public void readInventory(CompoundTag tags, HolderLookup.Provider registries) {
    ListTag items = tags.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
    int slots = itemHandler.getSlots();
    for (int i = 0; i < items.size(); i++) {
      CompoundTag itemTags = items.getCompound(i);
      int slot = itemTags.getInt("Slot");
      if (slot < 0 || slot >= slots) {
        continue;
      }
      try {
        ItemStack.parse(registries, itemTags).ifPresent(stack -> itemHandler.setStackInSlot(slot, stack));
      } catch (Exception e) {
        TConstruct.LOG.error("Skipping unloadable item in Tinkers' chest at {} slot {}: {}", getBlockPos(), slot, e.toString());
      }
    }
  }

  @Override
  public void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    readInventory(tags, registries);
  }
}
