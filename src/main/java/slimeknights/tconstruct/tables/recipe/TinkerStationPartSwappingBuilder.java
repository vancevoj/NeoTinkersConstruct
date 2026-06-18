package slimeknights.tconstruct.tables.recipe;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolMaterialSwappingRecipe;

import java.util.ArrayList;
import java.util.List;

/** Builder for {@link TinkerStationPartSwapping} and {@link ToolMaterialSwappingRecipe} */
@RequiredArgsConstructor(staticName = "tools")
public class TinkerStationPartSwappingBuilder extends AbstractRecipeBuilder<TinkerStationPartSwappingBuilder> {
  private final Ingredient tools;
  private boolean fromTool = false;
  @Setter
  @Accessors(fluent = true)
  private int maxStackSize = 16;
  /** Additional requirements beyond the "part" */
  private final List<SizedIngredient> extraRequirements = new ArrayList<>();

  /** Sets the swapping to be from a tool instead of from a part */
  public TinkerStationPartSwappingBuilder fromTool() {
    this.fromTool = true;
    return this;
  }

  /** Adds an extra ingredient requirement */
  public TinkerStationPartSwappingBuilder addExtraRequirement(SizedIngredient ingredient) {
    extraRequirements.add(ingredient);
    return this;
  }

  /** Adds an extra ingredient requirement */
  public TinkerStationPartSwappingBuilder addExtraRequirement(Ingredient ingredient) {
    return addExtraRequirement(SizedIngredient.of(ingredient));
  }

  /** Adds an extra ingredient requirement */
  public TinkerStationPartSwappingBuilder addExtraRequirement(ItemLike... items) {
    return addExtraRequirement(SizedIngredient.fromItems(items));
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, Loadables.ITEM.getKey(tools.getItems()[0].getItem()));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (fromTool) {
      output.accept(id, new ToolMaterialSwappingRecipe(id, tools, maxStackSize, extraRequirements), null);
    } else {
      output.accept(id, new TinkerStationPartSwapping(id, tools, maxStackSize, extraRequirements), null);
    }
  }
}
