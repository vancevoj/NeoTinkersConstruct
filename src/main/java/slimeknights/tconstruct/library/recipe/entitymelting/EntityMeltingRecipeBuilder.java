package slimeknights.tconstruct.library.recipe.entitymelting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

/** Builder for entity melting recipes */
@RequiredArgsConstructor(staticName = "melting")
public class EntityMeltingRecipeBuilder extends AbstractRecipeBuilder<EntityMeltingRecipeBuilder> {
  private final EntityIngredient ingredient;
  private final FluidOutput output;
  private final int damage;

  /** Creates a new builder */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output, int damage) {
    return melting(ingredient, FluidOutput.fromStack(output), damage);
  }

  /** Creates a new builder doing 2 damage */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidOutput output) {
    return melting(ingredient, output, 2);
  }

  /** Creates a new builder doing 2 damage */
  public static EntityMeltingRecipeBuilder melting(EntityIngredient ingredient, FluidStack output) {
    return melting(ingredient, output, 2);
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, BuiltInRegistries.FLUID.getKey(output.get().getFluid()));
  }

  @Override
  public void save(RecipeOutput consumer, ResourceLocation id) {
    AdvancementHolder advancement = this.buildOptionalAdvancement(id, "entity_melting");
    consumer.accept(id, new EntityMeltingRecipe(id, ingredient, output, damage), advancement);
  }
}
