package slimeknights.tconstruct.plugin.jei.material;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.MaterialsCraftingTableRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Common logic for {@link ShapedMaterialsExtension} and {@link ShapelessMaterialsExtension} */
public class MaterialsCraftingExtension<T extends CraftingRecipe & MaterialsCraftingTableRecipe> implements ICraftingCategoryExtension<T> {

  /** {@return the list of display outputs for the given recipe} */
  protected List<ItemStack> getResult(T recipe, ItemStack plainResult) {
    // if we have just the one part, set the output to match its material
    if (recipe.getPartCount() == 1) {
      Ingredient firstPart = recipe.getParts().get(0);
      return Arrays.stream(firstPart.getItems()).map(variant -> {
        ItemStack stack = plainResult.copy();
        if (variant.getItem() instanceof IMaterialItem materialItem) {
          recipe.setMaterial(stack, materialItem.getMaterial(variant));
        } else {
          recipe.setMaterial(stack, MaterialRecipeCache.findRecipe(variant).getMaterial().getVariant());
        }
        return stack;
      }).toList();
      // otherwise, use a display material. allow display tool part if it has just 1 material
    } else if (recipe.getExtraMaterials().isEmpty() && plainResult.getItem() instanceof IMaterialItem materialItem) {
      return List.of(materialItem.setMaterialForced(plainResult, ToolBuildHandler.getRenderMaterial(0)));
    } else {
      // display tool
      return List.of(IModifiableDisplay.getDisplayStack(plainResult));
    }
  }

  /** {@return the material slots for the given recipe, or null if there are no material slots to focus} */
  @Nullable
  protected int[] getMaterialSlots(T recipe) {
    // only the single part case has focus links
    if (recipe.getPartCount() == 1) {
      return getMaterialSlots(recipe, recipe.getParts().get(0));
    }
    return null;
  }

  /** Gets the material slots for the given recipe */
  protected int[] getMaterialSlots(T recipe, Ingredient firstPart) {
    return new int[] {0};
  }

  /** {@return Instance of the shapeless extension} */
  public static MaterialsCraftingExtension<ShapelessMaterialsRecipe> shapeless() {
    return new ShapelessMaterialsExtension();
  }

  /** {@return whether the recipe is valid for display} */
  protected boolean isValid(T recipe) {
    List<Ingredient> parts = recipe.getParts();
    for (int i = 0; i < recipe.getPartCount(); i++) {
      if (parts.get(i).getItems().length == 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void setRecipe(RecipeHolder<T> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    T recipe = holder.value();
    if (!isValid(recipe)) {
      return;
    }
    ItemStack plainResult = recipe.getResultItem(Objects.requireNonNull(SafeClientAccess.getRegistryAccess()));
    List<ItemStack> result = getResult(recipe, plainResult);
    int[] materialSlots = getMaterialSlots(recipe);
    setRecipe(getWidth(holder), getHeight(holder), builder, craftingGridHelper, recipe, result, plainResult, materialSlots);
  }

  /** Sets the recipe in the builder */
  public static void setRecipe(int width, int height, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, CraftingRecipe recipe, List<ItemStack> result, ItemStack plainResult, @Nullable int[] materialSlots) {
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(plainResult);

    // apply ingredient stacks
    List<List<ItemStack>> inputStacks = recipe.getIngredients().stream().map(ingredient -> List.of(ingredient.getItems())).toList();
    // shapeless needs its width and height set, but we also want to recover those sizes, so calculate it locally
    if (width <= 0 || height <= 0) {
      width = height = getShapelessSize(inputStacks.size());
      builder.setShapeless();
    }
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, result);
    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3", recipe, builder.getClass().getName());
    } else if (materialSlots != null) {
      // apply focus links
      int finalWidth = width;
      int finalHeight = height;
      builder.createFocusLink(Streams.concat(
        Stream.of(output),
        Arrays.stream(materialSlots).mapToObj(i -> inputs.get(MantleJEIConstants.getCraftingIndex(i, finalWidth, finalHeight)))
      ).toArray(IRecipeSlotBuilder[]::new));
    }
  }

  /** Gets the width and height of the grid for a shapeless recipe. */
  private static int getShapelessSize(int total) {
    if (total > 4) {
      return 3;
    } else if (total > 1) {
      return 2;
    } else {
      return 1;
    }
  }
}
