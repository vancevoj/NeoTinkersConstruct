package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.layout.Patterns;

import java.awt.Color;
import java.util.List;

/**
 * EMI display for part builder recipes.
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory} (background 121x46).
 */
public class PartBuilderEmiRecipe extends BasicEmiRecipe {
  private static final String KEY_COST = TConstruct.makeTranslationKey("jei", "part_builder.cost");
  /** Background panel, matches the JEI part builder background (textures/gui/jei/tinker_station.png, 121x46 at 0,117). */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");

  private final IDisplayPartBuilderRecipe recipe;
  /** Material variant for the title text, may be empty */
  private final MaterialVariant material;
  /** Cost to display */
  private final int cost;
  /** Pattern shown in the pattern-type slot */
  private final Pattern pattern;
  /** True if we should render the ingot pattern placeholder (no material, no items) */
  private final boolean showPlaceholder;

  public PartBuilderEmiRecipe(RecipeHolder<IDisplayPartBuilderRecipe> holder) {
    super(TConstructEmiCategories.PART_BUILDER, holder.id(), 121, 46);
    IDisplayPartBuilderRecipe recipe = holder.value();
    this.recipe = recipe;
    this.material = recipe.getMaterial();
    this.cost = recipe.getCost();
    this.pattern = recipe.getPattern();

    // pattern items (default patterns)
    List<ItemStack> patternItems = recipe.getPatternItems();
    this.inputs.add(EmiIngredient.of(patternItems.stream().map(EmiStack::of).toList()));
    // material items
    List<ItemStack> materialItems = recipe.getMaterialItems();
    if (!materialItems.isEmpty()) {
      this.inputs.add(EmiIngredient.of(materialItems.stream().map(EmiStack::of).toList()));
    } else {
      this.inputs.add(EmiStack.EMPTY);
    }

    // output
    List<ItemStack> resultItems = recipe.getResultItems();
    for (ItemStack result : resultItems) {
      if (!result.isEmpty()) {
        this.outputs.add(EmiStack.of(result));
      }
    }

    this.showPlaceholder = material.isEmpty() && materialItems.isEmpty();
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // EMI does not auto-draw the JEI background; draw the Tinkers panel so the recipe isn't an empty floating set of
    // slots (issue #6). The panel art already includes the slot insets, so the slots use drawBack(false).
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 121, 46, 0, 117);

    // pattern slot (default patterns)
    widgets.addSlot(this.inputs.get(0), 4, 16).drawBack(false);
    // material slot
    EmiIngredient materialInput = this.inputs.get(1);
    if (this.showPlaceholder) {
      // no material items: render the ingot pattern placeholder behind an empty slot
      widgets.addSlot(materialInput, 25, 16).drawBack(false);
      widgets.addDrawable(25, 16, 16, 16, (graphics, mouseX, mouseY, delta) ->
        GuiUtil.renderPattern(graphics, Patterns.INGOT, 0, 0));
    } else {
      widgets.addSlot(materialInput, 25, 16).drawBack(false);
    }

    // pattern-type slot: draw the pattern texture defensively, falling back to an empty slot
    widgets.addSlot(46, 16).drawBack(false);
    if (this.pattern != null) {
      widgets.addDrawable(46, 16, 16, 16, (graphics, mouseX, mouseY, delta) ->
        GuiUtil.renderPattern(graphics, this.pattern, 0, 0));
    }

    // output slot
    widgets.addSlot(this.outputs.isEmpty() ? EmiStack.EMPTY : this.outputs.get(0), 96, 15).recipeContext(this);

    // text: material name + cost
    Font font = Minecraft.getInstance().font;
    if (!this.material.isEmpty()) {
      Component name = MaterialTooltipCache.getColoredDisplayName(this.material.getVariant());
      widgets.addText(name, 3, 2, -1, true);
      String costString = I18n.get(KEY_COST, this.cost);
      widgets.addText(Component.literal(costString), 3, 35, Color.GRAY.getRGB(), false);
    }
  }
}
