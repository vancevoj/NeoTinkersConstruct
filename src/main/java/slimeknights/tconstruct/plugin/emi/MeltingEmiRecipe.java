package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * EMI recipe for the melting category (melter and smeltery). Mirrors the JEI
 * {@link slimeknights.tconstruct.plugin.jei.melting.MeltingCategory}: input item, melted fluid output,
 * usable liquid fuels and (for low temperatures) solid fuels. Layout positions match the JEI 132x40 background.
 */
public class MeltingEmiRecipe extends BasicEmiRecipe {
  private static final String KEY_TEMPERATURE = TConstruct.makeTranslationKey("jei", "temperature");
  private static final String KEY_MULTIPLIER = TConstruct.makeTranslationKey("jei", "melting.multiplier");
  private static final String KEY_COOLING_TIME = TConstruct.makeTranslationKey("jei", "melting.time");
  private static final Component TOOLTIP_ORE = Component.translatable(TConstruct.makeTranslationKey("jei", "melting.ore"));

  /** Recipe being displayed, kept for draw-time access to temperature/time/ore */
  private final MeltingRecipe recipe;
  /** Whether the recipe is cool enough to use solid fuel */
  private final boolean hasSolidFuel;
  /** Merged liquid fuel ingredient, may be empty */
  private final EmiIngredient liquidFuels;
  /** Merged solid fuel ingredient, may be empty */
  private final EmiIngredient solidFuels;

  public MeltingEmiRecipe(EmiRecipeCategory category, RecipeHolder<MeltingRecipe> holder) {
    super(category, holder.id(), 132, 40);
    this.recipe = holder.value();

    // input
    this.inputs = List.of(EmiIngredient.of(recipe.getInput()));

    // output fluid
    FluidStack output = recipe.getOutput();
    this.outputs = output.isEmpty() ? List.of() : List.of(TConstructEmiHelper.fluid(output));

    // fuels: liquid fuels usable at this temperature, plus solid fuels if cool enough
    int temperature = recipe.getTemperature();
    this.hasSolidFuel = temperature <= MeltingFuelLookup.getSolid().getTemperature();
    this.liquidFuels = usableLiquidFuels(temperature);
    this.solidFuels = hasSolidFuel ? solidFuels() : EmiStack.EMPTY;

    // expose fuels as catalysts so EMI can do recipe lookups / right-click
    this.catalysts = new ArrayList<>();
    if (!liquidFuels.isEmpty()) {
      this.catalysts.add(liquidFuels);
    }
    if (!solidFuels.isEmpty()) {
      this.catalysts.add(solidFuels);
    }
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

  /** Builds an ingredient of all solid fuels */
  private static EmiIngredient solidFuels() {
    List<ItemStack> fuels = MeltingFuelHandler.SOLID_FUELS.get();
    List<EmiIngredient> stacks = new ArrayList<>(fuels.size());
    for (ItemStack fuel : fuels) {
      if (!fuel.isEmpty()) {
        stacks.add(EmiStack.of(fuel));
      }
    }
    return stacks.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(stacks);
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // melting arrow (animated over the melting time, JEI uses time * 5 ticks; 50ms per tick)
    widgets.addFillingArrow(56, 18, recipe.getTime() * 5 * 50);
    widgets.addTooltipText(List.of(Component.translatable(KEY_COOLING_TIME, recipe.getTime() / 4)), 56, 18, 24, 17);

    // temperature text, centered on the arrow (x = 56)
    Font font = Minecraft.getInstance().font;
    String tempString = Component.translatable(KEY_TEMPERATURE, recipe.getTemperature()).getString();
    int textX = 56 - font.width(tempString) / 2;
    widgets.addText(Component.literal(tempString), textX, 3, Color.GRAY.getRGB(), false);

    // input slot
    widgets.addSlot(this.inputs.get(0), 24, 18);

    // output fluid tank (32x32), capacity one metal block
    if (!this.outputs.isEmpty()) {
      widgets.addTank(this.outputs.get(0), 96, 4, 32, 32, FluidValues.METAL_BLOCK)
             .recipeContext(this);
    }

    // liquid fuel tank, drawn on the left. shrinks to make room for the solid fuel when present
    int fuelHeight = hasSolidFuel ? 15 : 32;
    if (!liquidFuels.isEmpty()) {
      widgets.addTank(liquidFuels, 4, 4, 12, fuelHeight, 1)
             .drawBack(false)
             .catalyst(true);
    }

    // solid fuel slot when the recipe is cool enough, with the solid fuel temperature/multiplier tooltip
    if (!solidFuels.isEmpty()) {
      MeltingFuel solid = MeltingFuelLookup.getSolid();
      widgets.addSlot(solidFuels, 2, 22)
             .drawBack(false)
             .catalyst(true)
             .appendTooltip(Component.translatable(KEY_TEMPERATURE, solid.getTemperature()).withStyle(ChatFormatting.GRAY))
             .appendTooltip(Component.translatable(KEY_MULTIPLIER, solid.getRate() / 10f).withStyle(ChatFormatting.GRAY));
    }

    // ore indicator tooltip over the output (JEI draws a "+" at 87,31)
    if (recipe.getOreType() != null) {
      widgets.addTooltipText(List.of(TOOLTIP_ORE), 87, 31, 16, 16);
    }
  }
}
