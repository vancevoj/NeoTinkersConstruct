package slimeknights.tconstruct.library.tools.item;

import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Map;

/**
 * Interface for tools to display in books and other similar contexts
 */
public interface IModifiableDisplay extends IModifiable, ITinkerStationDisplay {
  /**
   * Gets a tool meant for rendering in a screen, can (and should) return the same stack on multiple calls
   *
   * @return the tool to use for rendering
   */
  ItemStack getRenderTool();

  /**
   * Bridges the Tinkers dynamic attribute hook ({@link #getAttributeModifiers(slimeknights.tconstruct.library.tools.nbt.IToolStackView, EquipmentSlot)},
   * which still works on a {@link Multimap} keyed by raw {@link Attribute}) to the 1.21 {@link ItemAttributeModifiers}
   * data-component value returned by {@code getDefaultAttributeModifiers(ItemStack)}.
   * @param stack  Stack to compute attributes for
   * @param slots  Slots to include attributes for; typically the hand slots for held tools or the armor slot for armor
   * @return  Component value for the stack
   */
  default ItemAttributeModifiers buildAttributeModifiers(ItemStack stack, EquipmentSlot... slots) {
    if (ToolStack.isInitialized(stack)) {
      ToolStack tool = ToolStack.from(stack);
      ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
      boolean empty = true;
      for (EquipmentSlot slot : slots) {
        Multimap<Attribute,AttributeModifier> attributes = getAttributeModifiers(tool, slot);
        if (!attributes.isEmpty()) {
          EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
          for (Map.Entry<Attribute,AttributeModifier> entry : attributes.entries()) {
            builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(entry.getKey()), entry.getValue(), group);
            empty = false;
          }
        }
      }
      if (!empty) {
        return builder.build();
      }
    }
    return ItemAttributeModifiers.EMPTY;
  }

  /** Helper method to convert an item into its display tool, if it uses this interface */
  static ItemStack getDisplayStack(Item item) {
    return item instanceof IModifiableDisplay display ? display.getRenderTool() : new ItemStack(item);
  }

  /** Helper method to convert a stack into its display tool, if it uses this interface */
  static ItemStack getDisplayStack(ItemStack stack) {
    if (stack.getItem() instanceof IModifiableDisplay display) {
      ItemStack tool = display.getRenderTool();
      return stack.getCount() > 1 ? tool.copyWithCount(stack.getCount()) : tool;
    }
    return stack;
  }
}
