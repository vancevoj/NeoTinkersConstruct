package slimeknights.tconstruct.library.recipe.modifiers.adding;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;
import slimeknights.tconstruct.library.recipe.modifiers.adding.SwappableModifierRecipe.VariantFormatter;

/** Builder for a modifier with a swappable string key */
public class SwappableModifierRecipeBuilder extends ModifierRecipeBuilder {
  private final String value;
  @Setter @Accessors(fluent = true)
  private VariantFormatter variantFormatter = VariantFormatter.DEFAULT;
  protected SwappableModifierRecipeBuilder(ModifierId modifier, String value) {
    super(modifier);
    this.value = value;
    // most variants do not want this as that will conflict, explicitly set it on the default if desired
    this.allowCrystal = false;
  }

  /** Creates a new builder */
  public static SwappableModifierRecipeBuilder modifier(ModifierId modifier, String value) {
    return new SwappableModifierRecipeBuilder(modifier, value);
  }

  /** Creates a new builder */
  public static SwappableModifierRecipeBuilder modifier(LazyModifier modifier, String value) {
    return modifier(modifier.getId(), value);
  }

  @Override
  @Deprecated
  public ModifierRecipeBuilder setMinLevel(int level) {
    throw new UnsupportedOperationException("Min level is always 1 for a swappable modifier recipe");
  }

  @Override
  @Deprecated
  public ModifierRecipeBuilder setMaxLevel(int level) {
    throw new UnsupportedOperationException("Max level is always 1 for a swappable modifier recipe");
  }

  /* Building */

  @Override
  public void save(RecipeOutput consumer, ResourceLocation id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least 1 input");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifiers");
    consumer.accept(id, new SwappableModifierRecipe(id, inputs, tools, maxToolSize, result, value, variantFormatter, slots, allowCrystal), advancement);
  }
}
