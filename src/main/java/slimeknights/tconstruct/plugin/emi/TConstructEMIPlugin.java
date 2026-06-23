package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * EMI integration for Tinkers' Construct. Native EMI plugin (separate from the JEI plugin) that exposes the smeltery
 * and tinkering recipe categories to the EMI recipe viewer. Mirrors the JEI category/workstation/recipe registration
 * found in {@link slimeknights.tconstruct.plugin.jei.JEIPlugin}.
 */
@EmiEntrypoint
public class TConstructEMIPlugin implements EmiPlugin {
  @Override
  public void register(EmiRegistry registry) {
    // EMI is client-side; the recipe manager is required to load recipes
    RecipeManager manager = registry.getRecipeManager();
    if (manager == null) {
      return;
    }

    // categories
    registry.addCategory(TConstructEmiCategories.CASTING_BASIN);
    registry.addCategory(TConstructEmiCategories.CASTING_TABLE);
    registry.addCategory(TConstructEmiCategories.MOLDING);
    registry.addCategory(TConstructEmiCategories.MELTING);
    registry.addCategory(TConstructEmiCategories.FOUNDRY);
    registry.addCategory(TConstructEmiCategories.ALLOY);
    registry.addCategory(TConstructEmiCategories.ENTITY_MELTING);
    registry.addCategory(TConstructEmiCategories.MODIFIERS);
    registry.addCategory(TConstructEmiCategories.MODIFIER_WORKTABLE);
    registry.addCategory(TConstructEmiCategories.TOOL_BUILDING);
    registry.addCategory(TConstructEmiCategories.SEVERING);
    registry.addCategory(TConstructEmiCategories.PART_BUILDER);

    // workstations (mirrors the JEI recipe catalysts)
    addWorkstation(registry, TConstructEmiCategories.CASTING_BASIN, TinkerSmeltery.searedBasin, TinkerSmeltery.scorchedBasin);
    addWorkstation(registry, TConstructEmiCategories.CASTING_TABLE, TinkerSmeltery.searedTable, TinkerSmeltery.scorchedTable);
    addWorkstation(registry, TConstructEmiCategories.MOLDING, TinkerSmeltery.searedTable, TinkerSmeltery.scorchedTable, TinkerSmeltery.searedBasin, TinkerSmeltery.scorchedBasin);
    addWorkstation(registry, TConstructEmiCategories.MELTING, TinkerSmeltery.searedMelter, TinkerSmeltery.smelteryController);
    addWorkstation(registry, TConstructEmiCategories.FOUNDRY, TinkerSmeltery.foundryController);
    addWorkstation(registry, TConstructEmiCategories.ALLOY, TinkerSmeltery.scorchedAlloyer);
    addWorkstation(registry, TConstructEmiCategories.ENTITY_MELTING, TinkerSmeltery.smelteryController, TinkerSmeltery.searedMelter);
    addWorkstation(registry, TConstructEmiCategories.MODIFIERS, TinkerTables.tinkerStation);
    addWorkstation(registry, TConstructEmiCategories.MODIFIER_WORKTABLE, TinkerTables.modifierWorktable);
    addWorkstation(registry, TConstructEmiCategories.PART_BUILDER, TinkerTables.partBuilder);
    registry.addWorkstation(TConstructEmiCategories.TOOL_BUILDING, EmiStack.of(TinkerTools.pickaxe.get().getRenderTool()));
    registry.addWorkstation(TConstructEmiCategories.SEVERING, EmiStack.of(TinkerTools.cleaver.get().getRenderTool()));

    // populate the fuel cache so the fuel tanks render (JEI does this in registerRecipes)
    MeltingFuelHandler.setMeltngFuels(RecipeHelper.getRecipes(manager, TinkerRecipeTypes.FUEL.get(), MeltingFuel.class));

    // casting: the type yields ICastingRecipe, filter to IDisplayableCastingRecipe
    addRecipes(registry, manager, TinkerRecipeTypes.CASTING_BASIN.get(), IDisplayableCastingRecipe.class,
               holder -> new CastingEmiRecipe(TConstructEmiCategories.CASTING_BASIN, narrow(holder), true));
    addRecipes(registry, manager, TinkerRecipeTypes.CASTING_TABLE.get(), IDisplayableCastingRecipe.class,
               holder -> new CastingEmiRecipe(TConstructEmiCategories.CASTING_TABLE, narrow(holder), false));

    // molding: both table and basin recipes feed the single molding category
    addRecipes(registry, manager, TinkerRecipeTypes.MOLDING_TABLE.get(), MoldingRecipe.class,
               holder -> new MoldingEmiRecipe(TConstructEmiCategories.MOLDING, holder));
    addRecipes(registry, manager, TinkerRecipeTypes.MOLDING_BASIN.get(), MoldingRecipe.class,
               holder -> new MoldingEmiRecipe(TConstructEmiCategories.MOLDING, holder));

    // melting + foundry: the type yields IMeltingRecipe, filter to MeltingRecipe; the same recipes feed both categories
    addRecipes(registry, manager, TinkerRecipeTypes.MELTING.get(), MeltingRecipe.class,
               holder -> new MeltingEmiRecipe(TConstructEmiCategories.MELTING, narrow(holder)));
    addRecipes(registry, manager, TinkerRecipeTypes.MELTING.get(), MeltingRecipe.class,
               holder -> new FoundryEmiRecipe(TConstructEmiCategories.FOUNDRY, narrow(holder)));

    // alloying
    addRecipes(registry, manager, TinkerRecipeTypes.ALLOYING.get(), AlloyRecipe.class, holder -> new AlloyEmiRecipe(narrow(holder)));

    // entity melting
    addRecipes(registry, manager, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class, holder -> new EntityMeltingEmiRecipe(narrow(holder)));

    // severing
    addRecipes(registry, manager, TinkerRecipeTypes.SEVERING.get(), SeveringRecipe.class,
               holder -> new SeveringEmiRecipe(TConstructEmiCategories.SEVERING, narrow(holder)));

    // modifiers + tool building both share the tinker station recipe type, filtered by interface
    addRecipes(registry, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class, holder -> new ModifierEmiRecipe(narrow(holder)));
    addRecipes(registry, manager, TinkerRecipeTypes.TINKER_STATION.get(), ToolBuildingRecipe.class, holder -> new ToolBuildingEmiRecipe(narrow(holder)));

    // part builder: the type yields IPartBuilderRecipe, filter to IDisplayPartBuilderRecipe
    addRecipes(registry, manager, TinkerRecipeTypes.PART_BUILDER.get(), IDisplayPartBuilderRecipe.class, holder -> new PartBuilderEmiRecipe(narrow(holder)));

    // modifier worktable
    addRecipes(registry, manager, TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), IModifierWorktableRecipe.class, holder -> new ModifierWorktableEmiRecipe(narrow(holder)));
  }

  /**
   * Reinterprets a recipe holder as a holder of a narrower (possibly non-{@link net.minecraft.world.item.crafting.Recipe})
   * display type. The caller must have already verified the value is an instance of the target type via the
   * {@code clazz} filter in {@link #addRecipes}. This bridges the fact that display interfaces such as
   * {@code IDisplayableCastingRecipe} are not {@code Recipe} subtypes and so cannot be the {@code RecipeHolder} bound.
   */
  @SuppressWarnings({"rawtypes"})
  private static RecipeHolder narrow(RecipeHolder<?> holder) {
    return holder;
  }

  /** Adds one or more item workstations for a category */
  private static void addWorkstation(EmiRegistry registry, EmiRecipeCategory category, ItemLike... items) {
    for (ItemLike item : items) {
      registry.addWorkstation(category, EmiStack.of(item));
    }
  }

  /**
   * Loads all recipes of the given type whose value is an instance of {@code clazz}, builds an EMI recipe via the
   * factory, and registers it. The factory receives the raw holder (its value is guaranteed to be an instance of
   * {@code clazz}); narrowing casts happen inside the factory lambda since the display interfaces are not themselves
   * {@link net.minecraft.world.item.crafting.Recipe} subtypes.
   * @param registry  EMI registry
   * @param manager   recipe manager
   * @param type      recipe type to load
   * @param clazz     class to filter recipe values by (handles types whose value interface is broader than the display interface)
   * @param factory   builds an EMI recipe from a holder whose value passed the filter
   */
  private static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends net.minecraft.world.item.crafting.Recipe<I>> void addRecipes(
      EmiRegistry registry, RecipeManager manager, RecipeType<R> type,
      Class<?> clazz, Function<RecipeHolder<R>, ? extends EmiRecipe> factory) {
    for (RecipeHolder<R> holder : manager.getAllRecipesFor(type)) {
      if (clazz.isInstance(holder.value())) {
        registry.addRecipe(factory.apply(holder));
      }
    }
  }
}
