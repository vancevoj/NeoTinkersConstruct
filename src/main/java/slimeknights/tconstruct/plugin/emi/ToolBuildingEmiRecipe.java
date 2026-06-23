package slimeknights.tconstruct.plugin.emi;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.SLOT_SIZE;
import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.X_OFFSET;
import static slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe.Y_OFFSET;

/**
 * EMI display for tool building recipes.
 * Mirrors {@link slimeknights.tconstruct.plugin.jei.ToolBuildingCategory} (background 134x66).
 */
public class ToolBuildingEmiRecipe extends BasicEmiRecipe {
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
  private static final int WIDTH = 134;
  private static final int HEIGHT = 66;
  private static final int ITEM_SIZE = 16;

  private final ToolBuildingRecipe recipe;
  /** Input ingredients positioned per-layout-slot */
  private final List<EmiIngredient> slotInputs;
  /** Layout slots driving slot placement */
  private final List<LayoutSlot> layoutSlots;
  /** Transparent tool preview rendered behind the slots */
  private final ItemStack previewStack;

  public ToolBuildingEmiRecipe(RecipeHolder<ToolBuildingRecipe> holder) {
    super(TConstructEmiCategories.TOOL_BUILDING, holder.id(), WIDTH, HEIGHT);
    ToolBuildingRecipe recipe = holder.value();
    this.recipe = recipe;

    // parts followed by extra ingredient requirements, each as a list of variants
    List<List<ItemStack>> partsAndExtras = Stream.concat(
      recipe.getAllToolParts().stream(),
      recipe.getExtraRequirements().stream().map(ingredient -> Arrays.asList(ingredient.getItems()))).toList();
    this.layoutSlots = recipe.getLayoutSlots();

    // pad inputs to fill empty layout slots if needed
    int missingSlots = partsAndExtras.size() - this.layoutSlots.size();
    if (missingSlots < 0) {
      partsAndExtras = new ArrayList<>(partsAndExtras);
      for (int i = 0; i > missingSlots; i--) {
        partsAndExtras.add(List.of(ItemStack.EMPTY));
      }
    }

    // build EMI inputs aligned to layout slots
    this.slotInputs = new ArrayList<>(this.layoutSlots.size());
    for (int i = 0; i < this.layoutSlots.size(); i++) {
      List<ItemStack> stacks = i < partsAndExtras.size() ? partsAndExtras.get(i) : List.of(ItemStack.EMPTY);
      List<EmiStack> variants = stacks.stream().filter(s -> !s.isEmpty()).map(EmiStack::of).toList();
      EmiIngredient ingredient = variants.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(variants);
      this.slotInputs.add(ingredient);
      this.inputs.add(ingredient);
    }

    // include the actual ingredient extras in inputs for lookup (tag-based ingredients)
    for (Ingredient ingredient : recipe.getExtraRequirements()) {
      if (!ingredient.isEmpty()) {
        this.inputs.add(EmiIngredient.of(ingredient));
      }
    }

    // hidden inputs aid recipe lookup; not displayed
    for (ItemStack hidden : recipe.getHiddenInputs()) {
      if (!hidden.isEmpty()) {
        this.inputs.add(EmiStack.of(hidden));
      }
    }

    // output
    List<ItemStack> displayOutput = recipe.getDisplayOutput();
    for (ItemStack result : displayOutput) {
      if (!result.isEmpty()) {
        this.outputs.add(EmiStack.of(result));
      }
    }

    // transparent preview of the result tool
    ItemStack output = recipe.getOutput() instanceof IModifiableDisplay modifiable
      ? modifiable.getRenderTool()
      : recipe.getOutput().asItem().getDefaultInstance();
    this.previewStack = output;
  }

  @Override
  public void addWidgets(WidgetHolder widgets) {
    // transparent tool preview, drawn behind the slots
    widgets.addDrawable(0, 0, WIDTH, HEIGHT, (graphics, mouseX, mouseY, delta) -> {
      PoseStack pose = graphics.pose();
      pose.pushPose();
      pose.translate(5, 6.5, 0);
      pose.scale(3.7f, 3.7f, 1.0f);
      graphics.renderItem(this.previewStack, 0, 0);
      pose.popPose();

      // overlay a transparent grey to make the preview appear faded
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderColor(1, 1, 1, 0.82f);
      graphics.blit(BACKGROUND_LOC, 5, 6, 122, 77, 70, 60);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
    });

    // input slots positioned by layout
    for (int i = 0; i < this.layoutSlots.size(); i++) {
      LayoutSlot layout = this.layoutSlots.get(i);
      EmiIngredient ingredient = i < this.slotInputs.size() ? this.slotInputs.get(i) : EmiStack.EMPTY;
      widgets.addSlot(ingredient, layout.getX() + X_OFFSET, layout.getY() + Y_OFFSET);
    }

    // output slot
    widgets.addSlot(this.outputs.isEmpty() ? EmiStack.EMPTY : this.outputs.get(0), WIDTH - 26, 23)
           .recipeContext(this);

    // anvil indicator if the recipe requires an anvil
    if (this.recipe.requiresAnvil()) {
      widgets.addDrawable(76, 44, ITEM_SIZE, ITEM_SIZE, (graphics, mouseX, mouseY, delta) ->
        graphics.blit(BACKGROUND_LOC, 0, 0, 128, 61, ITEM_SIZE, ITEM_SIZE));
      widgets.addTooltipText(List.of(TConstruct.makeTranslation("jei", "tinkering.tool_building.anvil")), 76, 44, ITEM_SIZE, ITEM_SIZE);
    }
  }
}
