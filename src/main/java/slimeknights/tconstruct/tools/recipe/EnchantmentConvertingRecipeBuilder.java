package slimeknights.tconstruct.tools.recipe;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

/** Builder for an enchantment converting recipe */
@RequiredArgsConstructor(staticName = "converting")
public class EnchantmentConvertingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<EnchantmentConvertingRecipeBuilder> {
  private final String name;
  private final boolean matchBook;
  private boolean returnInput = false;
  @Setter
  @Accessors(fluent = true)
  private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

  /**
   * If true, returns the unenchanted form of the item as an extra result
   */
  public EnchantmentConvertingRecipeBuilder returnInput() {
    returnInput = true;
    return this;
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, TConstruct.getResource(name));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one input");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifiers");
    output.accept(id, new EnchantmentConvertingRecipe(id, name, inputs, matchBook, returnInput, modifierPredicate), advancement);
  }
}
