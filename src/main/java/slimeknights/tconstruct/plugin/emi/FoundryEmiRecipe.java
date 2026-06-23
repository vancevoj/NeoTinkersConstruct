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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * EMI recipe for the foundry category. Mirrors the JEI
 * {@link slimeknights.tconstruct.plugin.jei.melting.FoundryCategory}: input item, melted output plus byproducts,
 * and usable liquid fuels. Unlike melting, the foundry has no solid fuel. Layout matches the JEI 132x40 background.
 */
public class FoundryEmiRecipe extends BasicEmiRecipe {
  /** JEI melting GUI texture, reused as the EMI backdrop (shared by melting and foundry) */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/melting.png");
  private static final String KEY_TEMPERATURE = TConstruct.makeTranslationKey("jei", "temperature");
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "melting.time");

  /** Recipe being displayed, kept for draw-time access to temperature/time */
  private final MeltingRecipe recipe;
  /** Output fluids (result + byproducts), each an EmiIngredient that may cycle alternatives */
  private final List<EmiIngredient> outputFluids;
  /** Merged liquid fuel ingredient, may be empty */
  private final EmiIngredient liquidFuels;

  public FoundryEmiRecipe(EmiRecipeCategory category, RecipeHolder<MeltingRecipe> holder) {
    super(category, holder.id(), 132, 40);
    this.recipe = holder.value();

    // input
    this.inputs = List.of(EmiIngredient.of(recipe.getInput()));

    // outputs: result fluid + byproducts. getOutputWithByproducts returns one list per output slot.
    this.outputFluids = new ArrayList<>();
    this.outputs = new ArrayList<>();
    for (List<FluidStack> alternatives : recipe.getOutputWithByproducts()) {
      List<EmiStack> stacks = new ArrayList<>(alternatives.size());
      for (FluidStack fluid : alternatives) {
        if (!fluid.isEmpty()) {
          EmiStack stack = TConstructEmiHelper.fluid(fluid);
          stacks.add(stack);
          this.outputs.add(stack);
        }
      }
      if (!stacks.isEmpty()) {
        // EmiIngredient.of cycles through the alternatives for this slot
        this.outputFluids.add(EmiIngredient.of(new ArrayList<>(stacks)));
      }
    }

    // liquid fuels usable at this temperature (foundry has no solid fuel)
    this.liquidFuels = usableLiquidFuels(recipe.getTemperature());
    this.catalysts = liquidFuels.isEmpty() ? List.of() : List.of(liquidFuels);
  }

  /** Builds an ingredient of all liquid fuels usable at the given temperature */
  private static EmiIngredient usableLiquidFuels(int temperature) {
    List<FluidStack> fuels = MeltingFuelHandler.getUsableFuels(temperature);
    List<EmiIngredient> stacks = new ArrayList<>(fuels.size());
    for (FluidStack fuel : fuels) {
      if (!fuel.isEmpty()) {
        stacks.add(TConstructEmiHelper.fluid(fuel));
      }
    }
    return stacks.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(stacks);
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // backdrop: the JEI melting background (0,0,132,40)
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 132, 40, 0, 0);

    // melting arrow (animated over the melting time, JEI uses time * 5 ticks; 50ms per tick)
    widgets.addFillingArrow(56, 18, recipe.getTime() * 5 * 50);
    widgets.addTooltipText(List.of(Component.translatable(KEY_COOLING_TIME, recipe.getTime() / 4)), 56, 18, 24, 17);

    // temperature text, centered on the arrow (x = 56)
    Font font = Minecraft.getInstance().font;
    String tempString = Component.translatable(KEY_TEMPERATURE, recipe.getTemperature()).getString();
    int textX = 56 - font.width(tempString) / 2;
    widgets.addText(Component.literal(tempString), textX, 3, Color.GRAY.getRGB(), false);

    // input slot at (24,18). Inset 1px so the 18x18 slot background lines up with the JEI texture's slot.
    widgets.addSlot(this.inputs.get(0), 23, 17).drawBack(false);

    // output fluid tanks (result + byproducts) split across the 32x32 output region at (96, 4)
    int count = outputFluids.size();
    if (count > 0) {
      int totalWidth = 32;
      int slotWidth = totalWidth / count;
      for (int i = 0; i < count; i++) {
        int slotX = 96 + i * slotWidth;
        // last slot takes the remaining width to fill the region
        int w = (i == count - 1) ? totalWidth - slotWidth * (count - 1) : slotWidth;
        widgets.addTank(outputFluids.get(i), slotX, 4, w, 32, FluidValues.METAL_BLOCK)
               .drawBack(false)
               .recipeContext(this);
      }
      // JEI tank overlay frame drawn once over the full output region (sheet 132,0 32x32)
      widgets.addTexture(BACKGROUND_LOC, 96, 4, 32, 32, 132, 0);
    }

    // liquid fuel tank, drawn on the left at full height as a bare fluid (no solid fuel on the foundry)
    if (!liquidFuels.isEmpty()) {
      widgets.addTank(liquidFuels, 4, 4, 12, 32, 1)
             .drawBack(false)
             .catalyst(true);
    }
  }
}
