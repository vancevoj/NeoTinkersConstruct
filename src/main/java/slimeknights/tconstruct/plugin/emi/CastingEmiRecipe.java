package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;

import java.awt.Color;
import java.util.List;

/**
 * EMI recipe for the casting basin and casting table categories. Both categories share the same layout, so a single
 * class is parameterized by the {@link EmiRecipeCategory} (basin vs table) passed in the constructor.
 *
 * <p>Layout mirrors {@link slimeknights.tconstruct.plugin.jei.casting.AbstractCastingCategory}: the JEI background is
 * {@code casting.png} (0,0,117,54), so the EMI display is sized to match and the same texture is drawn as a backdrop.
 * Slots/tank/arrow/text use the same coordinates as the JEI category; item slots are inset by 1px so EMI's 18x18 slot
 * background lines up with the JEI texture's slot at the JEI ingredient coordinate.
 */
public class CastingEmiRecipe extends BasicEmiRecipe {
  /** JEI casting GUI texture, reused as the EMI backdrop */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/casting.png");
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "time");
  private static final Component CAST_KEPT = Component.translatable(TConstruct.makeTranslationKey("jei", "casting.cast_kept"));
  private static final Component CAST_CONSUMED = Component.translatable(TConstruct.makeTranslationKey("jei", "casting.cast_consumed"));

  private final boolean consumed;
  private final boolean basin;
  private final int coolingTime;
  /** Cast ingredient, or null if the recipe has no cast */
  private final EmiIngredient castIngredient;

  public CastingEmiRecipe(EmiRecipeCategory category, RecipeHolder<? extends IDisplayableCastingRecipe> holder, boolean basin) {
    super(category, holder.id(), 117, 54);
    IDisplayableCastingRecipe recipe = holder.value();

    this.consumed = recipe.isConsumed();
    this.basin = basin;
    this.coolingTime = recipe.getCoolingTime();

    // fluids: the tank shows all fluid variants for this recipe
    List<FluidStack> fluids = recipe.getFluids();
    EmiIngredient fluidInput = TConstructEmiHelper.fluidIngredient(fluids);
    this.inputs.add(fluidInput);

    // cast item (input if consumed, catalyst if kept). Each cast variant becomes one EmiStack folded into a single
    // ingredient so EMI cycles through the material variants like JEI does.
    List<EmiIngredient> castStacks = recipe.getCastItems().stream()
                                          .filter(stack -> !stack.isEmpty())
                                          .map(stack -> (EmiIngredient) EmiStack.of(stack))
                                          .toList();
    if (castStacks.isEmpty()) {
      this.castIngredient = null;
    } else {
      this.castIngredient = EmiIngredient.of(castStacks);
      if (this.consumed) {
        this.inputs.add(this.castIngredient);
      } else {
        this.catalysts.add(this.castIngredient);
      }
    }

    // outputs
    for (ItemStack output : recipe.getOutputs()) {
      if (!output.isEmpty()) {
        this.outputs.add(EmiStack.of(output));
      }
    }
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // backdrop: the JEI casting background (0,0,117,54)
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 117, 54, 0, 0);

    // tank: JEI renders a 32x32 fluid at (3,3) with the tank overlay sheet at (133,0). We draw our own tank without the
    // default slot background and overlay the JEI tank frame on top.
    widgets.addTank(this.inputs.get(0), 3, 3, 32, 32, FluidValues.METAL_BLOCK).drawBack(false);
    widgets.addTexture(BACKGROUND_LOC, 3, 3, 32, 32, 133, 0);

    // block icon under the cast: table at (117,0), basin at (117,16), 16x16, drawn at (38,35)
    widgets.addTexture(BACKGROUND_LOC, 38, 35, 16, 16, 117, this.basin ? 16 : 0);

    // cooling arrow at (58,18), 24x17; total time in ms (cooling time is in ticks, 50ms each)
    widgets.addFillingArrow(58, 18, Math.max(1, this.coolingTime) * 50);

    // cast slot at (38,19); catalyst when the cast is kept (not consumed). Inset 1px so the 18x18 slot lines up.
    if (this.castIngredient != null) {
      widgets.addSlot(this.castIngredient, 37, 18).drawBack(false).catalyst(!this.consumed);
      // cast kept/consumed overlay at (63,39), 13x11 (kept at sheet 141,43; consumed at 141,32)
      widgets.addTexture(BACKGROUND_LOC, 63, 39, 13, 11, 141, this.consumed ? 32 : 43);
      widgets.addTooltipText(List.of(this.consumed ? CAST_CONSUMED : CAST_KEPT), 63, 39, 13, 11);
    }

    // output slot at (93,18). Inset 1px for the 18x18 slot background.
    if (!this.outputs.isEmpty()) {
      widgets.addSlot(this.outputs.get(0), 92, 17).recipeContext(this);
    }

    // cooling time text, centered on x=72 at y=2 (matches JEI draw())
    int coolingSeconds = this.coolingTime / 20;
    Component coolingText = Component.translatable(KEY_COOLING_TIME, coolingSeconds);
    Font font = Minecraft.getInstance().font;
    int textX = 72 - font.width(coolingText) / 2;
    widgets.addText(coolingText, textX, 2, Color.GRAY.getRGB(), false);
  }
}
