package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;

import java.util.List;

/**
 * EMI recipe for the severing modifier, converting an entity into a head or other item.
 * <p>
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.entity.SeveringCategory}. EMI cannot render entities directly the
 * way JEI's custom entity renderer can, so the entity input is represented by the matching spawn eggs (the same list
 * JEI uses for focus links). Entity types without a spawn egg are skipped.
 */
public class SeveringEmiRecipe extends BasicEmiRecipe {
  public SeveringEmiRecipe(EmiRecipeCategory category, RecipeHolder<SeveringRecipe> holder) {
    super(category, holder.id(), 100, 38);
    SeveringRecipe recipe = holder.value();

    // entity input represented by spawn eggs (drops AIR placeholders for entities lacking an egg)
    EntityIngredient ingredient = recipe.getIngredient();
    List<EmiStack> eggs = ingredient.getEggs().stream()
                                    .filter(stack -> !stack.isEmpty())
                                    .map(EmiStack::of)
                                    .toList();
    if (eggs.isEmpty()) {
      this.inputs.add(EmiStack.EMPTY);
    } else {
      // a single slot that rotates through every matching spawn egg
      this.inputs.add(EmiIngredient.of(eggs));
    }

    // output
    ItemStack output = recipe.getOutput();
    EmiStack result = output.isEmpty() ? EmiStack.EMPTY : EmiStack.of(output);
    this.outputs.add(result);
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // input slot mirrors JEI (3,3); the JEI renderer is 32px so use a larger slot region
    widgets.addSlot(this.inputs.get(0), 3, 3).drawBack(true);
    // output slot mirrors JEI (76,11)
    widgets.addSlot(this.outputs.get(0), 76, 11).recipeContext(this);
  }
}
