package slimeknights.tconstruct.plugin.emi;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;

import java.awt.Color;
import java.util.List;

/**
 * EMI recipe for the modifier recipe category (adding modifiers at the tinker station).
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory}.
 */
public class ModifierEmiRecipe extends BasicEmiRecipe {
  /** Background texture, matches the JEI background (textures/gui/jei/tinker_station.png, 128x77 at 0,0). */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");

  // translation keys (mirror JEI)
  private static final Component TEXT_INCREMENTAL = TConstruct.makeTranslation("jei", "modifiers.incremental");
  private static final String KEY_MIN = TConstruct.makeTranslationKey("jei", "modifiers.level.min");
  private static final String KEY_MAX = TConstruct.makeTranslationKey("jei", "modifiers.level.max");
  private static final String KEY_RANGE = TConstruct.makeTranslationKey("jei", "modifiers.level.range");
  private static final String KEY_EXACT = TConstruct.makeTranslationKey("jei", "modifiers.level.exact");

  /** Slot positions from JEI setRecipe (input items) */
  private static final int[][] INPUT_POS = {{3, 33}, {25, 15}, {47, 33}, {43, 58}, {7, 58}};
  /** Slot icon positions from JEI draw (background unfilled slot, +1 offset relative to the slot) */
  private static final int[][] SLOT_ICON_POS = {{2, 32}, {24, 14}, {46, 32}, {42, 57}, {6, 57}};

  private final IDisplayModifierRecipe recipe;

  public ModifierEmiRecipe(RecipeHolder<? extends IDisplayModifierRecipe> holder) {
    super(TConstructEmiCategories.MODIFIERS, holder.id(), 128, 77);
    this.recipe = holder.value();

    // inputs: the five modifier item slots
    int inputCount = Math.min(5, recipe.getInputCount());
    for (int i = 0; i < inputCount; i++) {
      List<ItemStack> stacks = recipe.getDisplayItems(i);
      if (!stacks.isEmpty()) {
        this.inputs.add(EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()));
      }
    }

    // tool with and without the modifier are catalysts (consumed tool shown both before and after)
    List<ItemStack> toolWithout = recipe.getToolWithoutModifier();
    if (!toolWithout.isEmpty()) {
      this.catalysts.add(EmiIngredient.of(toolWithout.stream().map(EmiStack::of).toList()));
    }
    List<ItemStack> toolWith = recipe.getToolWithModifier();
    if (!toolWith.isEmpty()) {
      // the resulting tool is the meaningful output of the recipe
      this.outputs.add(EmiStack.of(toolWith.get(0)));
    }
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // background (full JEI region)
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 128, 77, 0, 0);

    // input item slots; draw the empty-slot icon underneath when there is no item (matches JEI drawSlot)
    int inputCount = Math.min(5, recipe.getInputCount());
    for (int i = 0; i < 5; i++) {
      List<ItemStack> stacks = i < inputCount ? recipe.getDisplayItems(i) : List.of();
      if (stacks.isEmpty()) {
        // empty slot icon from the texture sheet at 128 + i*16, 0
        widgets.addTexture(BACKGROUND_LOC, SLOT_ICON_POS[i][0] + 1, SLOT_ICON_POS[i][1] + 1, 16, 16, 128 + i * 16, 0);
      } else {
        widgets.addSlot(EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()), INPUT_POS[i][0], INPUT_POS[i][1]).drawBack(false);
      }
    }

    // tool slots (catalysts in JEI). Position from JEI: without at 25,38; with at 105,34
    List<ItemStack> toolWithout = recipe.getToolWithoutModifier();
    if (!toolWithout.isEmpty()) {
      widgets.addSlot(EmiIngredient.of(toolWithout.stream().map(EmiStack::of).toList()), 25, 38).catalyst(true).drawBack(false);
    }
    List<ItemStack> toolWith = recipe.getToolWithModifier();
    if (!toolWith.isEmpty()) {
      widgets.addSlot(EmiStack.of(toolWith.get(0)), 105, 34).recipeContext(this).drawBack(false);
    }

    // modifier result name, rendered as centered text at the top (JEI uses a custom ModifierIngredientRenderer at 3,3 width 124)
    ModifierEntry result = recipe.getDisplayResult();
    if (result != null) {
      Component name = result.getDisplayName();
      widgets.addDrawable(3, 3, 124, 10, (graphics, mouseX, mouseY, delta) -> {
        Font font = Minecraft.getInstance().font;
        int x = (124 - font.width(name)) / 2;
        graphics.drawString(font, name, x, 1, -1, true);
      });
    }

    // info icons: requirements (66,58) and incremental (83,59); textures live at 128,17 and 128,33
    if (result != null && result.getHook(ModifierHooks.REQUIREMENTS).requirementsError(result) != null) {
      Component error = result.getHook(ModifierHooks.REQUIREMENTS).requirementsError(result);
      widgets.addTexture(BACKGROUND_LOC, 66, 58, 16, 16, 128, 17);
      if (error != null) {
        widgets.addTooltipText(List.of(error), 66, 58, 16, 16);
      }
    }
    if (recipe.isIncremental()) {
      widgets.addTexture(BACKGROUND_LOC, 83, 59, 16, 16, 128, 33);
      widgets.addTooltipText(List.of(TEXT_INCREMENTAL), 83, 59, 16, 16);
    }

    // level / variant text, centered at x=86, y=16 (matches JEI draw)
    Component levelText = levelText();
    if (levelText != null) {
      Component text = levelText;
      widgets.addDrawable(0, 0, 128, 77, (graphics, mouseX, mouseY, delta) -> {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, 86 - font.width(text) / 2, 16, Color.GRAY.getRGB(), false);
      });
    }

    // modifier slot cost icon. JEI renders SlotIngredientRenderer.INPUT at 102,58 (24x16 widget).
    // We reuse the existing renderer so the correct slot sprite (or "slotless") is shown.
    SlotCount slots = recipe.getSlots();
    widgets.addDrawable(102, 58, 24, 16, (graphics, mouseX, mouseY, delta) -> {
      PoseStack pose = graphics.pose();
      pose.pushPose();
      SlotIngredientRenderer.INPUT.render(graphics, slots);
      pose.popPose();
    });
    widgets.addTooltipText(SlotIngredientRenderer.INPUT.getTooltip(slots, TooltipFlag.NORMAL), 102, 58, 24, 16);
  }

  /** Computes the level/variant text shown in the recipe, or null if none (mirrors JEI draw). */
  private Component levelText() {
    Component variant = recipe.getVariant();
    if (variant != null) {
      return variant;
    }
    IntRange level = recipe.getLevel();
    int min = level.min();
    int max = level.max();
    if (min == 1) {
      if (max < ModifierEntry.VALID_LEVEL.max()) {
        return Component.translatable(KEY_MAX, max);
      }
      return null;
    } else if (min == max) {
      return Component.translatable(KEY_EXACT, min);
    } else if (max == ModifierEntry.VALID_LEVEL.max()) {
      return Component.translatable(KEY_MIN, min);
    } else {
      return Component.translatable(KEY_RANGE, min, max);
    }
  }
}
