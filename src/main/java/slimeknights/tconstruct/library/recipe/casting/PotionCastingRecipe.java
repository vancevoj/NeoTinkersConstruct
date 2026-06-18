package slimeknights.tconstruct.library.recipe.casting;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid potion data to the item
 */
public class PotionCastingRecipe implements ICastingRecipe, IMultiRecipe<DisplayCastingRecipe> {
  protected static final LoadableField<FluidIngredient, PotionCastingRecipe> FLUID_FIELD = FluidIngredient.LOADABLE.requiredField("fluid", r -> r.fluid);
  protected static final LoadableField<Integer, PotionCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime);
  public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("bottle", r -> r.bottle),
    FLUID_FIELD,
    Loadables.ITEM.requiredField("result", r -> r.result),
    COOLING_TIME_FIELD,
    PotionCastingRecipe::new);

  /** Tag key written by the molten potion fluid, see {@code PotionFluidType}. TODO(neoport): migrate potion fluid storage to the {@code PotionContents} data component. */
  protected static final String TAG_POTION = "Potion";

  @Getter
  protected final TypeAwareRecipeSerializer<?> serializer;
  /** Recipe ID, retained for JEI display purposes (vanilla 1.21 recipes no longer expose their own ID). */
  @Getter
  protected final ResourceLocation id;
  @Getter
  protected final String group;
  /** Input on the casting table, always consumed */
  protected final Ingredient bottle;
  /** Potion ingredient, typically just the potion tag */
  protected final FluidIngredient fluid;
  /** Potion item result, will be given the proper potion contents */
  protected final Item result;
  /** Cooling time for this recipe, used for tipped arrows */
  protected final int coolingTime;

  public PotionCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient bottle, FluidIngredient fluid, Item result, int coolingTime) {
    this.serializer = serializer;
    this.id = id;
    this.group = group;
    this.bottle = bottle;
    this.fluid = fluid;
    this.result = result;
    this.coolingTime = coolingTime;
    CastingRecipeLookup.registerCastable(result);
  }

  /** Resolves a potion holder from the fluid NBT written by the potion fluid. TODO(neoport): replace with reading the {@code PotionContents} component off the fluid stack. */
  @Nullable
  protected static Holder<Potion> getPotion(@Nullable CompoundTag fluidTag) {
    if (fluidTag != null && fluidTag.contains(TAG_POTION)) {
      return BuiltInRegistries.POTION.getHolder(ResourceLocation.tryParse(fluidTag.getString(TAG_POTION))).orElse(null);
    }
    return null;
  }

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    return bottle.test(inv.getStack()) && fluid.test(inv.getFluid());
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return fluid.getAmount(inv.getFluid());
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return coolingTime;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = new ItemStack(this.result);
    Holder<Potion> potion = getPotion(inv.getFluidTag());
    if (potion != null) {
      result.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    }
    return result;
  }


  /* JEI */
  protected List<DisplayCastingRecipe> displayRecipes = null;

  @Override
  public List<DisplayCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // create a subrecipe for every potion variant
      List<ItemStack> bottles = List.of(bottle.getItems());
      displayRecipes = BuiltInRegistries.POTION.holders()
        .filter(potion -> !potion.is(Potions.WATER))
        .map(potion -> {
          ItemStack result = new ItemStack(this.result);
          result.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
          return new DisplayCastingRecipe(getType(), bottles, fluid.getFluids().stream()
                                                              .map(fluid -> {
                                                                FluidStack copy = fluid.copy();
                                                                copy.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                                                                return copy;
                                                              })
                                                              .toList(),
                                          result, coolingTime, true);
        }).toList();
    }
    return displayRecipes;
  }


  /* Recipe interface methods */

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(Ingredient.EMPTY, bottle);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.result);
  }
}
