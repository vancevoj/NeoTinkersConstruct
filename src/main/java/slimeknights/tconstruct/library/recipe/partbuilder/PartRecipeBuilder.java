package slimeknights.tconstruct.library.recipe.partbuilder;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/**
 * Builder for a material item part crafting recipe
 */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "partRecipe")
public class PartRecipeBuilder extends AbstractRecipeBuilder<PartRecipeBuilder> {
  private final IMaterialItem output;
  private final int outputAmount;
  @Setter
  private int cost = 1;
  @Setter
  private ResourceLocation pattern = null;
  @Setter
  private Ingredient patternItem = IPartBuilderRecipe.DEFAULT_PATTERNS;
  @Setter
  private boolean allowUncraftable = false;

  /**
   * Creates a new part recipe that outputs a single item
   * @param output  Output item
   * @return  Builder instance
   */
  public static PartRecipeBuilder partRecipe(IMaterialItem output) {
    return partRecipe(output, 1);
  }

  @Override
  public void save(RecipeOutput consumerIn) {
    this.save(consumerIn, BuiltInRegistries.ITEM.getKey(this.output.asItem()));
  }

  @Override
  public void save(RecipeOutput consumerIn, ResourceLocation id) {
    if (this.outputAmount <= 0) {
      throw new IllegalStateException("recipe " + id + " must output at least 1");
    }
    if (this.cost <= 0) {
      throw new IllegalStateException("recipe " + id + " has no cost associated with it");
    }
    if (this.pattern == null) {
      throw new IllegalStateException("recipe " + id + " has no pattern associated with it");
    }
    AdvancementHolder advancementId = this.buildOptionalAdvancement(id, "parts");
    consumerIn.accept(id, new PartRecipe(id, group, new Pattern(pattern), patternItem, cost, allowUncraftable, output, outputAmount), advancementId);
  }
}
