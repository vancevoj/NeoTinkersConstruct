package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FLUID_TAG = "fluid_tag";

  public CopperCanItem(Properties properties) {
    super(properties);
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return getFluid(stack) != Fluids.EMPTY;
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    if (hasCraftingRemainingItem(stack)) {
      return new ItemStack(this);
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    Fluid fluid = getFluid(stack);
    if (fluid != Fluids.EMPTY) {
      MutableComponent text = new FluidStack(fluid, FluidValues.INGOT).getDisplayName().plainCopy();
      tooltip.add(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.add(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid)).withStyle(ChatFormatting.DARK_GRAY));
      }
    } else {
      tooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
  }

  /** Removes the fluid from the given stack */
  public static void removeFluid(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      nbt.remove(TAG_FLUID);
      nbt.remove(TAG_FLUID_TAG);
      if (nbt.isEmpty()) {
        stack.remove(DataComponents.CUSTOM_DATA);
      } else {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
      }
    }
  }

  /** Sets the fluid on the given stack whether or not its valiid */
  private static void setFluidInternal(ItemStack stack, ResourceLocation fluid, @Nullable CompoundTag fluidTag) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
      nbt.putString(TAG_FLUID, fluid.toString());
      if (fluidTag != null) {
        nbt.put(TAG_FLUID_TAG, fluidTag.copy());
      } else {
        nbt.remove(TAG_FLUID_TAG);
      }
    });
  }


  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, ResourceLocation fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid.equals(BuiltInRegistries.FLUID.getDefaultKey())) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, fluid, fluidTag);
    }
    return stack;
  }
  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Fluid fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid == Fluids.EMPTY) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, BuiltInRegistries.FLUID.getKey(fluid), fluidTag);
    }
    return stack;
  }

  /** Sets the fluid on the given stack */
  public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
    // TODO(neoport): FluidStack component data not preserved on copper can (was NBT tag); revisit if component fluids need it
    return setFluid(stack, fluid.getFluid(), null);
  }

  /** Gets the fluid from the given stack */
  public static Fluid getFluid(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      if (nbt.contains(TAG_FLUID, Tag.TAG_STRING)) {
        ResourceLocation location = ResourceLocation.tryParse(nbt.getString(TAG_FLUID));
        if (location != null && BuiltInRegistries.FLUID.containsKey(location)) {
          Fluid fluid = BuiltInRegistries.FLUID.get(location);
          if (fluid != null) {
            return fluid;
          }
        }
      }
    }
    return Fluids.EMPTY;
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.holders().filter(holder -> {
      Fluid fluid = holder.value();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), holder.key().location(), null));
    });
  }

  /** Gets the fluid NBT from the given stack */
  @Nullable
  public static CompoundTag getFluidTag(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      if (nbt.contains(TAG_FLUID_TAG, Tag.TAG_COMPOUND)) {
        return nbt.getCompound(TAG_FLUID_TAG);
      }
    }
    return null;
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      return data.copyTag().getString(TAG_FLUID);
    }
    return "";
  }
}
