package slimeknights.tconstruct.tools.recipe;

import com.mojang.datafixers.util.Function6;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.ArrayList;
import java.util.List;

/** Builder for {@link ModifierRemovalRecipe} and {@link ExtractModifierRecipe} */
@RequiredArgsConstructor(staticName = "removal")
public class ModifierRemovalRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierRemovalRecipeBuilder> {
  private final Function6<ResourceLocation,String,SizedIngredient,List<SizedIngredient>,List<ItemStack>,IJsonPredicate<ModifierId>,ModifierRemovalRecipe> constructor;
  private final List<ItemStack> leftovers = new ArrayList<>();
  @Accessors(chain = true)
  @Setter
  private String name = "modifiers";
  private SizedIngredient tools = ModifierRemovalRecipe.DEFAULT_TOOLS;
  @Accessors(fluent = true)
  @Setter
  private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

  public static ModifierRemovalRecipeBuilder removal() {
    return removal(ModifierRemovalRecipe::new);
  }

  public static ModifierRemovalRecipeBuilder extract() {
    return removal(ExtractModifierRecipe::new);
  }

  /** Sets the name from the given slot */
  public ModifierRemovalRecipeBuilder slotName(SlotType slot) {
    return setName(slot.getName());
  }

  /**
   * Sets the tool requirement for this recipe
   */
  public ModifierRemovalRecipeBuilder setTools(SizedIngredient ingredient) {
    this.tools = ingredient;
    return this;
  }

  /**
   * Sets the tool requirement for this recipe
   */
  public ModifierRemovalRecipeBuilder setTools(Ingredient ingredient) {
    return setTools(SizedIngredient.of(ingredient));
  }

  /**
   * Adds a leftover stack to the recipe
   */
  public ModifierRemovalRecipeBuilder addLeftover(ItemStack stack) {
    leftovers.add(stack);
    return this;
  }

  /**
   * Adds a leftover stack to the recipe
   */
  public ModifierRemovalRecipeBuilder addLeftover(ItemLike item) {
    return addLeftover(new ItemStack(item));
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, BuiltInRegistries.ITEM.getKey(leftovers.get(0).getItem()));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one input");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifiers");
    output.accept(id, constructor.apply(id, name, tools, inputs, leftovers, modifierPredicate), advancement);
  }
}
