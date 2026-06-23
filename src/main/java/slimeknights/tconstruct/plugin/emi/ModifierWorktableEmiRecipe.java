package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
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
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI recipe for the modifier worktable category.
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.modifiers.ModifierWorktableCategory}.
 */
public class ModifierWorktableEmiRecipe extends BasicEmiRecipe {
  /** Background texture, matches the JEI background (textures/gui/jei/tinker_station.png, 121x35 at 0,166). */
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");

  private final IModifierWorktableRecipe recipe;

  public ModifierWorktableEmiRecipe(RecipeHolder<? extends IModifierWorktableRecipe> holder) {
    super(TConstructEmiCategories.MODIFIER_WORKTABLE, holder.id(), 121, 35);
    this.recipe = holder.value();

    // tool input: an input if consumed, otherwise a catalyst
    List<ItemStack> tools = recipe.getInputTools();
    if (!tools.isEmpty()) {
      EmiIngredient toolIngredient = EmiIngredient.of(tools.stream().map(EmiStack::of).toList());
      if (recipe.isToolInput()) {
        this.inputs.add(toolIngredient);
      } else {
        this.catalysts.add(toolIngredient);
      }
    }

    // up to two item inputs
    int max = Math.min(2, recipe.getInputCount());
    for (int i = 0; i < max; i++) {
      List<ItemStack> stacks = recipe.getDisplayItems(i);
      if (!stacks.isEmpty()) {
        this.inputs.add(EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()));
      }
    }
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // background (JEI region 0,166 size 121x35)
    widgets.addTexture(BACKGROUND_LOC, 0, 0, 121, 35, 0, 166);

    // tool slot at 23,16. Draw the empty tool icon (sheet 128,0) when no tool is present.
    List<ItemStack> tools = recipe.getInputTools();
    if (tools.isEmpty()) {
      widgets.addTexture(BACKGROUND_LOC, 23, 16, 16, 16, 128, 0);
    } else {
      widgets.addSlot(EmiIngredient.of(tools.stream().map(EmiStack::of).toList()), 23, 16)
        .drawBack(false)
        .catalyst(!recipe.isToolInput());
    }

    // up to two item input slots at 43+i*18,16. Draw the empty slot icon (sheet 176,0 and 208,0) when empty.
    for (int i = 0; i < 2; i++) {
      List<ItemStack> stacks = i < recipe.getInputCount() ? recipe.getDisplayItems(i) : List.of();
      int x = 43 + i * 18;
      if (stacks.isEmpty()) {
        widgets.addTexture(BACKGROUND_LOC, x, 16, 16, 16, 176 + i * 32, 0);
      } else {
        widgets.addSlot(EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()), x, 16).drawBack(false);
      }
    }

    // modifier option(s), rendered as text. JEI uses the MODIFIER_TYPE renderer at 82,16 (output if modifier is the result).
    // We render the option names stacked as text and provide a tooltip with their descriptions.
    List<ModifierEntry> modifiers = recipe.getModifierOptions(null);
    if (!modifiers.isEmpty()) {
      widgets.addDrawable(82, 16, 36, 16, (graphics, mouseX, mouseY, delta) -> {
        Font font = Minecraft.getInstance().font;
        Component name = modifiers.get(0).getDisplayName();
        graphics.drawString(font, name, 0, 4, -1, true);
      });
      List<Component> tooltip = new ArrayList<>();
      for (ModifierEntry entry : modifiers) {
        tooltip.add(entry.getDisplayName());
        tooltip.addAll(entry.getModifier().getDescriptionList(entry.getLevel()));
      }
      widgets.addTooltipText(tooltip, 82, 16, 36, 16);
    }

    // recipe title at top-left (JEI draws at 3,2)
    Component title = recipe.getTitle();
    widgets.addText(title.copy().withStyle(ChatFormatting.RESET), 3, 2, 0x404040, false);
    // title hover shows the description
    widgets.addTooltipText(List.of(recipe.getDescription(null)), 3, 2, 115, 10);
  }
}
