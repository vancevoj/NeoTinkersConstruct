package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.client.render.IRenderBoundingBox;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;

import javax.annotation.Nonnull;

public abstract class RetexturedTableBlockEntity extends TableBlockEntity implements IRetexturedBlockEntity, IRenderBoundingBox {
  private static final String TAG_TEXTURE = "texture";

  @Nonnull @Getter
  protected Block texture = Blocks.AIR;
  public RetexturedTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int size) {
    super(type, pos, state, name, size);
  }

  /**
   * Enlarged render bounding box so tall items displayed floating above the table are not culled early.
   * In 1.21 {@code getRenderBoundingBox} moved onto the renderer; {@link slimeknights.mantle.client.render.InventoryBlockEntityRenderer}
   * delegates to this via {@link IRenderBoundingBox}.
   */
  @Override
  public AABB getRenderBoundingBox() {
    return AABB.encapsulatingFullBlocks(worldPosition, worldPosition.offset(1, 2, 1));
  }


  /* Textures */

  @Nonnull
  @Override
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(texture);
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  private void textureUpdated() {
    // update the texture in BE data
    if (level != null && level.isClientSide) {
      Block normalizedTexture = texture == Blocks.AIR ? null : texture;
      ModelData data = getModelData();
      if (data.get(RetexturedHelper.BLOCK_PROPERTY) != normalizedTexture) {
        requestModelDataUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
      }
    }
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      textureUpdated();
    }
  }

  @Override
  public void saveSynced(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveSynced(tags, registries);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    if (tags.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
      texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
      textureUpdated();
    }
  }
}
