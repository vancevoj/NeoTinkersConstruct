package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
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
 * 117x54, so the EMI display is sized to match. Slots/tank/text use the same relative coordinates as the JEI category.
 */
public class CastingEmiRecipe extends BasicEmiRecipe {
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "time");

  private final boolean consumed;
  private final int coolingTime;
  /** Cast ingredient, or null if the recipe has no cast */
  private final EmiIngredient castIngredient;

  public CastingEmiRecipe(EmiRecipeCategory category, RecipeHolder<? extends IDisplayableCastingRecipe> holder) {
    super(category, holder.id(), 117, 54);
    IDisplayableCastingRecipe recipe = holder.value();

    this.consumed = recipe.isConsumed();
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
    // tank: JEI renders at (3,3) with a 32x32 fluid renderer, capacity = METAL_BLOCK mb
    widgets.addTank(this.inputs.get(0), 3, 3, 32, 32, FluidValues.METAL_BLOCK);

    // cooling arrow at (58,18), 24x17; total time in ms (cooling time is in ticks, 50ms each)
    widgets.addFillingArrow(58, 18, Math.max(1, this.coolingTime) * 50);

    // cast slot at (38,19); catalyst when the cast is kept (not consumed)
    if (this.castIngredient != null) {
      widgets.addSlot(this.castIngredient, 38, 19).catalyst(!this.consumed);
    }

    // output slot at (93,18)
    if (!this.outputs.isEmpty()) {
      widgets.addSlot(this.outputs.get(0), 93, 18).recipeContext(this);
    }

    // cooling time text, centered on x=72 at y=2 (matches JEI draw())
    int coolingSeconds = this.coolingTime / 20;
    Component coolingText = Component.translatable(KEY_COOLING_TIME, coolingSeconds);
    Font font = Minecraft.getInstance().font;
    int textX = 72 - font.width(coolingText) / 2;
    widgets.addText(coolingText, textX, 2, Color.GRAY.getRGB(), false);
  }
}
