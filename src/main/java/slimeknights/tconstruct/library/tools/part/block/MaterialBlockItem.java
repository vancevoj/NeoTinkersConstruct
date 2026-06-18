package slimeknights.tconstruct.library.tools.part.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.MaterialItem;

import java.util.List;

/** Implementation of {@link MaterialItem} on a {@link BlockItem}. */
public class MaterialBlockItem extends BlockItem implements IMaterialItem {
  public MaterialBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public MaterialVariantId getMaterial(ItemStack stack) {
    // 1.21: read the material string from the CUSTOM_DATA component compound instead of item NBT
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return MaterialItem.getMaterialId(data == null ? null : data.copyTag());
  }

  @Override
  public Component getName(ItemStack stack) {
    return MaterialItem.getName(this, stack);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    MaterialItem.appendHoverText(this, stack, tooltip, flag);
    super.appendHoverText(stack, context, tooltip, flag);
  }

  @Override
  public String getCreatorModId(ItemStack stack) {
    return MaterialItem.getCreatorModId(this, stack);
  }

  // TODO(neoport): Item#verifyTagAfterLoad was removed in 1.21 (no per-load NBT validation hook). Material remapping on load
  // needs a different home; helper kept for explicit callers.
  public void verifyTagAfterLoad(CompoundTag tag) {
    MaterialItem.verifyTag(tag);
  }
}
