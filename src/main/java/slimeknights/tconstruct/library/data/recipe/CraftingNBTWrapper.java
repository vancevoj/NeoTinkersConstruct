package slimeknights.tconstruct.library.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

/**
 * Helper to add NBT to vanilla recipe results.
 * <p>
 * TODO(neoport): NBT-on-vanilla-recipe-result wrapper - FinishedRecipe SPI gone, result NBT must become DataComponents;
 * needs cross-package decision. In 1.21 recipes are codec-based {@link Recipe} objects and results carry
 * {@link net.minecraft.core.component.DataComponentMap} rather than JSON NBT, so this wrapper can no longer rewrite the
 * serialized result. For now it delegates to the base {@link RecipeOutput} unchanged (the NBT is dropped). The only
 * caller is the humor "tinkers_forge"/"scorched_forge" recipe display name, so the missing component is cosmetic until
 * a proper component-applying recipe wrapper is introduced.
 */
public record CraftingNBTWrapper(RecipeOutput base, CompoundTag nbt) implements RecipeOutput {
  @Override
  public Advancement.Builder advancement() {
    return base.advancement();
  }

  @Override
  public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
    // TODO(neoport): apply nbt as DataComponents on the recipe result once a component-aware recipe wrapper exists
    base.accept(id, recipe, advancement, conditions);
  }

  /** Creates a wrapped recipe output, intended to add the given NBT to results passed through it */
  public static RecipeOutput wrap(RecipeOutput base, CompoundTag nbt) {
    return new CraftingNBTWrapper(base, nbt);
  }
}
