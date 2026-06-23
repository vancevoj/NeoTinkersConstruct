package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe.AlloyIngredient;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;

import java.awt.Color;
import java.util.List;

/**
 * EMI recipe for the alloyer category.
 *
 * <p>Layout mirrors {@link slimeknights.tconstruct.plugin.jei.AlloyRecipeCategory}: the JEI background is 172x62, so the
 * EMI display is sized to match. Inputs are drawn as a horizontal row of tanks spanning x=19..67 (48px) at y=11, the
 * output tank is at x=137 y=11 (16x32), the animated arrow at x=90 y=21, the fuel tank at x=94 y=43, and the temperature
 * text centered on x=102 at y=5.
 */
public class AlloyEmiRecipe extends BasicEmiRecipe {
  /** JEI alloy GUI texture, reused as the EMI backdrop */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/alloy.png");
  private static final String KEY_TEMPERATURE = TConstruct.makeTranslationKey("jei", "temperature");

  private final int temperature;
  /** Output fluid, used to size the tanks like the JEI category */
  private final EmiStack output;
  /** Largest fluid amount in the recipe, used as the tank capacity for proportional fill */
  private final long capacityMb;

  public AlloyEmiRecipe(RecipeHolder<AlloyRecipe> holder) {
    super(TConstructEmiCategories.ALLOY, holder.id(), 172, 62);
    AlloyRecipe recipe = holder.value();
    this.temperature = recipe.getTemperature();

    // output fluid
    FluidStack outputStack = recipe.getOutput();
    this.output = TConstructEmiHelper.fluid(outputStack);
    if (!outputStack.isEmpty()) {
      this.outputs.add(this.output);
    }

    // find the largest amount in the recipe so tanks fill proportionally (matches JEI's maxAmount logic)
    long maxAmount = outputStack.getAmount();
    for (AlloyIngredient ingredient : recipe.getInputs()) {
      for (FluidStack fluid : ingredient.fluid().getFluids()) {
        if (fluid.getAmount() > maxAmount) {
          maxAmount = fluid.getAmount();
        }
      }
    }
    this.capacityMb = Math.max(1, maxAmount);

    // inputs: each alloy ingredient becomes a fluid ingredient (all matching fluid variants)
    for (AlloyIngredient ingredient : recipe.getInputs()) {
      EmiIngredient input = TConstructEmiHelper.fluidIngredient(ingredient.fluid().getFluids());
      this.inputs.add(input);
    }

    // fuels valid for this temperature shown as catalysts (mirrors the JEI fuel slot)
    List<FluidStack> fuels = MeltingFuelHandler.getUsableFuels(this.temperature);
    if (fuels != null && !fuels.isEmpty()) {
      this.catalysts.add(TConstructEmiHelper.fluidIngredient(fuels));
    }
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // backdrop: the JEI alloy background (0,0,172,62)
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 172, 62, 0, 0);

    // input tanks: spread across x=19..67 (48px wide) at y=11, height 32, like JEI's drawVariableFluids.
    // The background already provides the tank frames, so draw bare fluids.
    int count = this.inputs.size();
    if (count > 0) {
      int totalWidth = 48;
      int w = totalWidth / count;
      for (int i = 0; i < count; i++) {
        int x = 19 + i * w;
        // last tank takes the remaining width so the row fills exactly 48px
        int tankWidth = (i == count - 1) ? totalWidth - (w * (count - 1)) : w;
        widgets.addTank(this.inputs.get(i), x, 11, tankWidth, 32, (int) this.capacityMb).drawBack(false);
      }
    }

    // animated arrow at (90,21), 24x17
    widgets.addFillingArrow(90, 21, 200 * 50);

    // output tank at (137,11), 16x32
    if (!this.outputs.isEmpty()) {
      widgets.addTank(this.output, 137, 11, 16, 32, (int) this.capacityMb).drawBack(false).recipeContext(this);
    }

    // fuel tank at (94,43), 16x16, with the JEI fuel tank overlay frame (sheet 172,17 16x16)
    if (!this.catalysts.isEmpty()) {
      widgets.addTank(this.catalysts.get(0), 94, 43, 16, 16, 1).drawBack(false).catalyst(true);
      widgets.addTexture(BACKGROUND_LOC, 94, 43, 16, 16, 172, 17);
    }

    // temperature text centered on x=102 at y=5 (matches JEI draw())
    Component tempText = Component.translatable(KEY_TEMPERATURE, this.temperature);
    Font font = Minecraft.getInstance().font;
    int textX = 102 - font.width(tempText) / 2;
    widgets.addText(tempText, textX, 5, Color.GRAY.getRGB(), false);
  }
}
