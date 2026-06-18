package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  /** Gets the potion contents stored on the fluid stack, or empty if none */
  private static PotionContents getContents(FluidStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null ? contents : PotionContents.EMPTY;
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    return Potion.getName(getContents(stack).potion(), "item.minecraft.potion.effect.");
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucket());
    PotionContents contents = fluidStack.get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      itemStack.set(DataComponents.POTION_CONTENTS, contents);
    }
    return itemStack;
  }

  @Override
  public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
    consumer.accept(new ClientTextureFluidType(this) {
      /**
       * Gets the color, based on {@link PotionContents#getColor()}
       * @param stack  Fluid stack instance
       * @return  Color for the fluid
       */
      @Override
      public int getTintColor(FluidStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents == PotionContents.EMPTY) {
          return getTintColor();
        }
        if (contents.customColor().isPresent()) {
          return contents.customColor().get() | 0xFF000000;
        }
        return contents.getColor() | 0xFF000000;
      }
    });
  }

  /** Creates the legacy potion tag, used for datagen serialization of potion fluid outputs */
  private static CompoundTag potionTag(ResourceLocation location) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Potion", location.toString());
    return tag;
  }

  /** Creates a component patch holding the given potion contents */
  private static DataComponentPatch potionPatch(Holder<Potion> potion) {
    return DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build();
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    return potionFluid(BuiltInRegistries.POTION.getHolderOrThrow(potion), size);
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(Holder<Potion> potion, int size) {
    return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(TinkerFluids.potion.get()), size, potionPatch(potion));
  }

  /** Creates a fluid output for the given potion */
  public static FluidOutput potionResult(Holder<Potion> potion, int size) {
    CompoundTag tag = potionTag(potion.unwrapKey().map(ResourceKey::location).orElseThrow());
    return FluidOutput.fromTag(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, tag);
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    return potionBucket(BuiltInRegistries.POTION.getHolderOrThrow(potion));
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(Holder<Potion> potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Reads the potion contents stored on the fluid stack, or null if none. Used by potion casting recipes. */
  @Nullable
  public static Holder<Potion> getPotion(FluidStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      Optional<Holder<Potion>> potion = contents.potion();
      if (potion.isPresent()) {
        return potion.get();
      }
    }
    return null;
  }
}
