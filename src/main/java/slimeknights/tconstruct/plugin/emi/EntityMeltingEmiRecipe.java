package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
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
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;

import java.awt.Color;
import java.util.List;

/**
 * EMI recipe for the entity melting category.
 *
 * <p>Layout mirrors {@link slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory}: the JEI background is
 * 150x62, so the EMI display is sized to match. The input slot is at x=19 y=11, the animated arrow at x=71 y=21, the
 * output tank at x=115 y=11 (16x32), the fuel tank at x=75 y=43, and the damage (hearts) text drawn right-aligned to
 * x=84 at y=8.
 *
 * <p>EMI has no entity ingredient type, so (like the JEI category falling back to spawn eggs for focus links) the input
 * is rendered as the set of spawn eggs matching the recipe's {@link slimeknights.mantle.recipe.ingredient.EntityIngredient}.
 * Entities without a spawn egg (e.g. the wither, ender dragon) simply contribute no egg; the slot is omitted entirely if
 * no eggs are available so the display stays defensive rather than showing an empty slot.
 */
public class EntityMeltingEmiRecipe extends BasicEmiRecipe {
  /** JEI melting GUI texture; the entity melting layout lives in the lower region (v=41) */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/melting.png");
  private final int damage;

  public EntityMeltingEmiRecipe(RecipeHolder<EntityMeltingRecipe> holder) {
    super(TConstructEmiCategories.ENTITY_MELTING, holder.id(), 150, 62);
    EntityMeltingRecipe recipe = holder.value();
    this.damage = recipe.getDamage();

    // input: spawn eggs for the matched entity types (EMI has no entity ingredient)
    List<EmiStack> eggs = recipe.getIngredient().getEggs().stream()
                                .filter(stack -> !stack.isEmpty())
                                .map(EmiStack::of)
                                .map(stack -> (EmiStack) stack)
                                .toList();
    if (!eggs.isEmpty()) {
      this.inputs.add(EmiIngredient.of(eggs.stream().map(s -> (EmiIngredient) s).toList()));
    }

    // output fluid
    FluidStack outputStack = recipe.getOutput();
    if (!outputStack.isEmpty()) {
      this.outputs.add(TConstructEmiHelper.fluid(outputStack));
    }

    // any fuel works for entity melting (JEI shows all fuels at temperature 1)
    List<FluidStack> fuels = MeltingFuelHandler.getUsableFuels(1);
    if (fuels != null && !fuels.isEmpty()) {
      this.catalysts.add(TConstructEmiHelper.fluidIngredient(fuels));
    }
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // backdrop: the JEI entity melting background lives at sheet (0,41) size 150x62
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 150, 62, 0, 41);

    // input slot at (19,11). Inset 1px so the 18x18 slot background lines up with the JEI texture's slot.
    if (!this.inputs.isEmpty()) {
      widgets.addSlot(this.inputs.get(0), 18, 10).drawBack(false);
    }

    // animated arrow at (71,21), 24x17
    widgets.addFillingArrow(71, 21, 200 * 50);

    // output tank at (115,11), 16x32, capacity matches JEI's INGOT*2 renderer
    if (!this.outputs.isEmpty()) {
      widgets.addTank(this.outputs.get(0), 115, 11, 16, 32, FluidValues.INGOT * 2).drawBack(false).recipeContext(this);
    }

    // fuel tank at (75,43), 16x16, with the JEI fuel tank overlay frame (sheet 150,74 16x16)
    if (!this.catalysts.isEmpty()) {
      widgets.addTank(this.catalysts.get(0), 75, 43, 16, 16, 1).drawBack(false).catalyst(true);
      widgets.addTexture(BACKGROUND_LOC, 75, 43, 16, 16, 150, 74);
    }

    // damage as hearts, drawn right-aligned to x=84 at y=8 (matches JEI draw())
    String hearts = Float.toString(this.damage / 2f);
    Font font = Minecraft.getInstance().font;
    int x = 84 - font.width(hearts);
    widgets.addText(Component.literal(hearts), x, 8, Color.RED.getRGB(), false);
  }
}
