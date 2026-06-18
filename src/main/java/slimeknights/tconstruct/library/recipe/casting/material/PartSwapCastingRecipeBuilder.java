package slimeknights.tconstruct.library.recipe.casting.material;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/** Builder for {@link PartSwapCastingRecipe} */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "castingRecipe")
public class PartSwapCastingRecipeBuilder extends AbstractRecipeBuilder<PartSwapCastingRecipeBuilder> {
  private final Ingredient tools;
  private final int itemCost;
  private final TypeAwareRecipeSerializer<PartSwapCastingRecipe> recipeSerializer;
  @Setter
  @Accessors(fluent = true)
  private int index = 0;
  @Setter
  private IJsonPredicate<MaterialVariantId> allowedMaterials = MaterialPredicate.ANY;

  /**
   * Creates a new part swapping recipe
   * @param tools     List of tools
   * @param itemCost  Amount needed to cast to swap
   * @return  Builder instance
   */
  public static PartSwapCastingRecipeBuilder basinRecipe(Ingredient tools, int itemCost) {
    return castingRecipe(tools, itemCost, TinkerSmeltery.basinPartSwappingSerializer.get());
  }

  /**
   * Creates a new part swapping recipe
   * @param itemCost  Amount needed to cast to swap
   * @return  Builder instance
   */
  public static PartSwapCastingRecipeBuilder tableRecipe(Ingredient tools, int itemCost) {
    return castingRecipe(tools, itemCost, TinkerSmeltery.tablePartSwappingSerializer.get());
  }

  @SuppressWarnings("deprecation")
  @Override
  public void save(RecipeOutput output) {
    save(output, BuiltInRegistries.ITEM.getKey(tools.getItems()[0].getItem()));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    AdvancementHolder advancement = this.buildOptionalAdvancement(id, "materials");
    output.accept(id, new PartSwapCastingRecipe(recipeSerializer, id, group, tools, itemCost, index, allowedMaterials), advancement);
  }
}
