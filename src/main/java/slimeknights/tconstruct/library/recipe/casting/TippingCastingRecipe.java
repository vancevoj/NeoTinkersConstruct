package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Arrays;
import java.util.List;

/** Casting recipe applying a potion to a tool */
public class TippingCastingRecipe extends PotionCastingRecipe {
  protected static final LoadableField<Ingredient, PotionCastingRecipe> TOOL_FIELD = IngredientLoadable.DISALLOW_EMPTY.requiredField("tools", r -> r.bottle);
  public static final RecordLoadable<TippingCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    TOOL_FIELD, FLUID_FIELD, COOLING_TIME_FIELD,
    ModifierId.PARSER.requiredField("modifier", r -> r.modifier),
    TippingCastingRecipe::new);

  private final ModifierId modifier;
  public TippingCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient tool, FluidIngredient fluid, int coolingTime, ModifierId modifier) {
    super(serializer, id, group, tool, fluid, Items.AIR, coolingTime);
    this.modifier = modifier;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    // must have the modifier to cast
    ItemStack stack = inv.getStack();
    if (super.matches(inv, level) && ModifierUtil.getModifierLevel(stack, modifier) > 0) {
      // must also have a specific potion, it's what we are going to copy
      // but it can't match what is already on the stack
      CompoundTag fluidTag = inv.getFluidTag();
      return fluidTag != null && fluidTag.contains(TAG_POTION, Tag.TAG_STRING)
        && !ModifierUtil.getPersistentString(stack, modifier).equals(fluidTag.getString(TAG_POTION));
    }
    return false;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = inv.getStack().copy();
    CompoundTag tag = inv.getFluidTag();
    if (tag != null) {
      ToolStack.from(result).getPersistentData().putString(modifier, tag.getString(TAG_POTION));
    }
    return result;
  }


  /* JEI */

  @Override
  public List<DisplayCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // create a list of tools with the modifier
      List<ItemStack> tools = Arrays.stream(bottle.getItems())
        .map(stack -> IDisplayModifierRecipe.withModifiers(IModifiableDisplay.getDisplayStack(stack), List.of(new ModifierEntry(modifier, 1))))
        .toList();
      displayRecipes = BuiltInRegistries.POTION.holders()
        .filter(potion -> !potion.is(Potions.WATER))
        .map(potion -> {
          // add the potion to the tool list
          String id = Loadables.POTION.getString(potion.value());
          List<ItemStack> results = tools.stream().map(stack -> {
            ToolStack tool = ToolStack.copyFrom(stack);
            tool.getPersistentData().putString(modifier, id);
            return tool.copyStack(stack);
          }).toList();
          // create the recipe, copying the potion onto the display fluids
          Holder<Potion> holder = potion;
          return new DisplayCastingRecipe(getId(), getType(), tools, fluid.getFluids().stream()
            .map(fluid -> {
              FluidStack copy = fluid.copy();
              copy.set(DataComponents.POTION_CONTENTS, new PotionContents(holder));
              return copy;
            })
            .toList(),
            results, coolingTime, true);
        }).toList();
    }
    return displayRecipes;
  }
}
