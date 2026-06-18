package slimeknights.tconstruct.tables.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

/** Builder for tinker station damaging recipes */
@RequiredArgsConstructor(staticName = "damage")
public class TinkerStationDamagingRecipeBuilder extends AbstractRecipeBuilder<TinkerStationDamagingRecipeBuilder> {

  private final Ingredient ingredient;
  private final int damageAmount;

  @Override
  public void save(RecipeOutput output) {
    ItemStack[] stacks = ingredient.getItems();
    if (stacks.length == 0) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    save(output, BuiltInRegistries.ITEM.getKey(stacks[0].getItem()));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (ingredient == Ingredient.EMPTY) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "tinker_station");
    output.accept(id, new TinkerStationDamagingRecipe(id, ingredient, damageAmount), advancement);
  }
}
