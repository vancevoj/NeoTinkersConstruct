package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;

/**
 * EMI recipe for molding casts on the casting table or casting basin.
 * <p>
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.MoldingRecipeCategory}: a material item is combined with an
 * optional pattern (mold) to produce a result. If a pattern is present the material is pressed into the block (a
 * downward arrow is drawn); if there is no pattern, the item is "picked up" off the block (an upward arrow is drawn).
 */
public class MoldingEmiRecipe extends BasicEmiRecipe {
  /** Shared JEI background texture, reused for the block icons and arrows */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/casting.png");

  /** True if this recipe uses the casting basin block rather than the casting table */
  private final boolean basin;
  /** True if a pattern (mold) is part of the recipe */
  private final boolean hasPattern;
  /** True if the pattern is consumed when the recipe is performed */
  private final boolean patternConsumed;
  /** Cached result for rendering, may be empty */
  private final EmiStack result;
  /** Pattern ingredient, may be empty */
  private final EmiIngredient pattern;

  public MoldingEmiRecipe(EmiRecipeCategory category, RecipeHolder<MoldingRecipe> holder) {
    super(category, holder.id(), 70, 57);
    MoldingRecipe recipe = holder.value();
    this.basin = recipe.getType() == TinkerRecipeTypes.MOLDING_BASIN.get();
    Ingredient patternIngredient = recipe.getPattern();
    this.hasPattern = !patternIngredient.isEmpty();
    this.patternConsumed = recipe.isPatternConsumed();

    // material input (always present)
    this.inputs.add(EmiIngredient.of(recipe.getMaterial()));
    // pattern input (optional)
    if (this.hasPattern) {
      this.pattern = EmiIngredient.of(patternIngredient);
      this.inputs.add(this.pattern);
    } else {
      this.pattern = EmiStack.EMPTY;
    }

    // output: needs a registry access, fall back to empty if unavailable
    ItemStack output = ItemStack.EMPTY;
    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.level != null) {
      output = recipe.getResultItem(mc.level.registryAccess());
    }
    this.result = output.isEmpty() ? EmiStack.EMPTY : EmiStack.of(output);
    this.outputs.add(this.result);
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // block icon comes from the JEI sheet: table at (117,0), basin at (117,16), each 16x16
    int blockV = this.basin ? 16 : 0;
    widgets.addTexture(BACKGROUND_LOC, 3, 40, 16, 16, 117, blockV);

    // material slot and result slot (matches JEI 3,24 and 51,24)
    widgets.addSlot(this.inputs.get(0), 3, 24);
    widgets.addSlot(this.result, 51, 24).recipeContext(this);

    if (this.hasPattern) {
      // pressing into the block: draw the block on the output side too, plus a downward arrow
      widgets.addTexture(BACKGROUND_LOC, 51, 40, 16, 16, 117, blockV);
      // down arrow at (70,55) on the sheet, 6x6, drawn at (8,17)
      widgets.addTexture(BACKGROUND_LOC, 8, 17, 6, 6, 70, 55);

      // pattern slot on the material side (JEI 3,1)
      widgets.addSlot(this.pattern, 3, 1);
      // if not consumed, show the preserved pattern on the output side (JEI 51,8)
      if (!this.patternConsumed) {
        widgets.addSlot(this.pattern, 51, 8).drawBack(false);
      }
    } else {
      // picking up the item: upward arrow at (76,55) on the sheet, 6x6, drawn at (8,17)
      widgets.addTexture(BACKGROUND_LOC, 8, 17, 6, 6, 76, 55);
    }
  }
}
