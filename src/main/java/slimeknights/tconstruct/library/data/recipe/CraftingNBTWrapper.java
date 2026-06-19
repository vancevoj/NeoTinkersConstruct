package slimeknights.tconstruct.library.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

/**
 * Helper to add data components to vanilla recipe results.
 * <p>
 * In 1.20.1 this added raw NBT to the serialized recipe JSON; in 1.21 recipe results are {@link ItemStack}s carrying
 * {@link net.minecraft.core.component.DataComponentMap data components}, so this wrapper applies the given
 * {@link DataComponentPatch} directly to the recipe's stored result before passing it to the base output. The result
 * stack returned by {@link Recipe#getResultItem(net.minecraft.core.HolderLookup.Provider)} is the backing field for
 * shaped/shapeless recipes, so applying components mutates the result that will be serialized and later assembled.
 */
public record CraftingNBTWrapper(RecipeOutput base, DataComponentPatch components) implements RecipeOutput {
  @Override
  public Advancement.Builder advancement() {
    return base.advancement();
  }

  @Override
  public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
    if (!components.isEmpty()) {
      // getResultItem returns the stored result field for shaped/shapeless recipes, ignoring the provider
      ItemStack result = recipe.getResultItem(null);
      if (result != null && !result.isEmpty()) {
        result.applyComponents(components);
      }
    }
    base.accept(id, recipe, advancement, conditions);
  }

  /** Creates a wrapped recipe output, applying the given data components to results passed through it */
  public static RecipeOutput wrap(RecipeOutput base, DataComponentPatch components) {
    return new CraftingNBTWrapper(base, components);
  }
}
