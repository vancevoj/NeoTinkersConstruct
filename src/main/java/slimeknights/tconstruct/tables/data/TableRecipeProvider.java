package slimeknights.tconstruct.tables.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipeBuilder;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.recipe.data.ItemNameOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.data.recipe.CraftingNBTWrapper;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.material.MaterialsConsumerBuilder;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.partbuilder.recycle.PartBuilderRecycleBuilder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationDamagingRecipeBuilder;
import slimeknights.tconstruct.tables.recipe.TinkerStationPartSwappingBuilder;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.function.Function;

public class TableRecipeProvider extends BaseRecipeProvider {

  public TableRecipeProvider(PackOutput packOutput) {
    super(packOutput);
  }

  @Override
  public String getName() {
    return "Neo Tinkers Table Recipes";
  }

  @Override
  protected void buildRecipes(RecipeOutput consumer) {
    this.tableRecipes(consumer);
    this.damageRecipes(consumer);
    this.recyclingRecipes(consumer);
  }

  private void tableRecipes(RecipeOutput consumer) {
    String folder = "tables/";
    // pattern
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerTables.pattern, 6)
      .define('s', Tags.Items.RODS_WOODEN)
      .define('p', ItemTags.PLANKS)
      .pattern("ps")
      .pattern("sp")
      .unlockedBy("has_item", has(Tags.Items.RODS_WOODEN))
      .save(consumer, prefix(TinkerTables.pattern, folder));

    // book from patterns and slime
    ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.BOOK)
                          .requires(Items.PAPER)
                          .requires(Items.PAPER)
                          .requires(Items.PAPER)
                          .requires(Tags.Items.SLIMEBALLS)
                          .requires(TinkerTables.pattern)
                          .requires(TinkerTables.pattern)
                          .unlockedBy("has_item", has(TinkerTables.pattern))
                          .save(consumer, location(folder + "book_substitute"));

    // crafting station -> crafting table upgrade
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
      .define('p', TinkerTables.pattern)
      .define('w', DifferenceIngredient.of(Ingredient.of(TinkerTags.Items.WORKBENCHES), Ingredient.of(TinkerTables.craftingStation.get())))
      .pattern("p")
      .pattern("w")
      .unlockedBy("has_item", has(TinkerTables.pattern))
      .save(consumer, prefix(TinkerTables.craftingStation, folder));
    // station with log texture
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
        .define('p', TinkerTables.pattern)
        .define('w', ItemTags.LOGS)
        .pattern("p")
        .pattern("w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(ItemTags.LOGS)
      .build(consumer, wrap(TinkerTables.craftingStation, folder, "_from_logs"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.craftingStation)
        .define('p', TinkerTables.pattern)
        .define('w', DifferenceIngredient.of(Ingredient.of(TinkerTags.Items.TABLES), Ingredient.of(TinkerTables.craftingStation.get())))
        .pattern("p")
        .pattern("w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(DifferenceIngredient.of(Ingredient.of(TinkerTags.Items.TABLES), Ingredient.of(TinkerTables.craftingStation.get())))
      .build(consumer, wrap(TinkerTables.craftingStation, folder, "_from_tables"));

    // part builder
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.partBuilder)
        .define('p', TinkerTables.pattern)
        .define('w', TinkerTags.Items.PLANKLIKE)
        .pattern("pp")
        .pattern("ww")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(TinkerTags.Items.PLANKLIKE)
      .setMatchAll()
      .build(consumer, prefix(TinkerTables.partBuilder, folder));

    // tinker station
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkerStation)
        .define('p', TinkerTables.pattern)
        .define('w', TinkerTags.Items.PLANKLIKE)
        .pattern("ppp")
        .pattern("w w")
        .pattern("w w")
        .unlockedBy("has_item", has(TinkerTables.pattern)))
      .setSource(TinkerTags.Items.PLANKLIKE)
      .setMatchAll()
      .build(consumer, prefix(TinkerTables.tinkerStation, folder));

    // part chest
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.partChest)
                       .define('p', TinkerTables.pattern)
                       .define('w', ItemTags.PLANKS)
                       .define('s', Tags.Items.RODS_WOODEN)
                       .define('C', Tags.Items.CHESTS_WOODEN)
                       .pattern(" p ")
                       .pattern("sCs")
                       .pattern("sws")
                       .unlockedBy("has_item", has(TinkerTables.pattern))
                       .save(consumer, prefix(TinkerTables.partChest, folder));
    // modifier chest
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersChest)
                       .define('p', TinkerTables.pattern)
                       .define('w', ItemTags.PLANKS)
                       .define('l', Tags.Items.GEMS_LAPIS)
                       .define('C', Tags.Items.CHESTS_WOODEN)
                       .pattern(" p " )
                       .pattern("lCl")
                       .pattern("lwl")
                       .unlockedBy("has_item", has(TinkerTables.pattern))
                       .save(consumer, prefix(TinkerTables.tinkersChest, folder));
    // cast chest
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.castChest)
                       .define('c', TinkerTags.Items.GOLD_CASTS)
                       .define('b', TinkerSmeltery.searedBrick)
                       .define('B', TinkerSmeltery.searedBricks)
                       .define('C', Tags.Items.CHESTS_WOODEN)
                       .pattern(" c ")
                       .pattern("bCb")
                       .pattern("bBb")
                       .unlockedBy("has_item", has(TinkerTags.Items.GOLD_CASTS))
                       .save(consumer, prefix(TinkerTables.castChest, folder));

    // modifier worktable
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.modifierWorktable)
        .define('r', TinkerTags.Items.WORKSTATION_ROCK)
        .define('s', TinkerTags.Items.SEARED_BLOCKS)
        .pattern("sss")
        .pattern("r r")
        .pattern("r r")
        .unlockedBy("has_item", has(TinkerTags.Items.SEARED_BLOCKS)))
      .setSource(TinkerTags.Items.WORKSTATION_ROCK)
      .setMatchAll()
      .build(consumer, prefix(TinkerTables.modifierWorktable, folder));

    // tinker anvil
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersAnvil)
        .define('m', TinkerTags.Items.ANVIL_METAL)
        .define('s', TinkerTags.Items.SEARED_BLOCKS)
        .pattern("mmm")
        .pattern(" s ")
        .pattern("sss")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerTags.Items.ANVIL_METAL)
      .setMatchAll()
      .build(consumer, prefix(TinkerTables.tinkersAnvil, folder));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.scorchedAnvil)
        .define('m', TinkerTags.Items.ANVIL_METAL)
        .define('s', TinkerTags.Items.SCORCHED_BLOCKS)
        .pattern("mmm")
        .pattern(" s ")
        .pattern("sss")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerTags.Items.ANVIL_METAL)
      .setMatchAll()
      .build(consumer, prefix(TinkerTables.scorchedAnvil, folder));

    // tool forge - just a humor recipe; carry the custom display name as a CUSTOM_NAME component on the result
    RecipeOutput toolForge = CraftingNBTWrapper.wrap(consumer, DataComponentPatch.builder()
      .set(DataComponents.CUSTOM_NAME, Component.translatable("block.tconstruct.tool_forge"))
      .build());
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersAnvil)
        .define('m', TinkerTags.Items.ANVIL_METAL)
        .define('s', TinkerTags.Items.SEARED_BLOCKS)
        .define('t', TinkerTables.tinkerStation)
        .pattern("sss")
        .pattern("mtm")
        .pattern("m m")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerTags.Items.ANVIL_METAL)
      .setMatchAll()
      .build(toolForge, location(folder + "tinkers_forge"));
    ShapedRetexturedRecipeBuilder.fromShaped(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.scorchedAnvil)
        .define('m', TinkerTags.Items.ANVIL_METAL)
        .define('s', TinkerTags.Items.SCORCHED_BLOCKS)
        .define('t', TinkerTables.tinkerStation)
        .pattern("sss")
        .pattern("mtm")
        .pattern("m m")
        .unlockedBy("has_item", has(TinkerTags.Items.ANVIL_METAL)))
      .setSource(TinkerTags.Items.ANVIL_METAL)
      .setMatchAll()
      .build(toolForge, location(folder + "scorched_forge"));

    // material recipes - for the material fallbacks
    RecipeOutput materialConsumer = MaterialsConsumerBuilder.shaped("m").build(consumer);
    Ingredient fakeStorageBlock = MaterialIngredient.of(TinkerToolParts.fakeStorageBlock, MaterialPredicate.tag(TinkerTags.Materials.COMPATABILITY_ALLOYS)).toVanilla();
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerTables.tinkersAnvil)
      .define('m', fakeStorageBlock)
      .define('s', TinkerTags.Items.SEARED_BLOCKS)
      .pattern("mmm")
      .pattern(" s ")
      .pattern("sss")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, wrap(TinkerTables.tinkersAnvil, folder, "_material"));
    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinkerTables.scorchedAnvil)
      .define('m', fakeStorageBlock)
      .define('s', TinkerTags.Items.SCORCHED_BLOCKS)
      .pattern("mmm")
      .pattern(" s ")
      .pattern("sss")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, wrap(TinkerTables.scorchedAnvil, folder, "_material"));
    materialConsumer = MaterialsConsumerBuilder.shaped("m").build(toolForge);
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.tinkersAnvil)
      .define('m', fakeStorageBlock)
      .define('s', TinkerTags.Items.SEARED_BLOCKS)
      .define('t', TinkerTables.tinkerStation)
      .pattern("sss")
      .pattern("mtm")
      .pattern("m m")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, location(folder + "seared_forge_material"));
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TinkerTables.scorchedAnvil)
      .define('m', fakeStorageBlock)
      .define('s', TinkerTags.Items.SCORCHED_BLOCKS)
      .define('t', TinkerTables.tinkerStation)
      .pattern("sss")
      .pattern("mtm")
      .pattern("m m")
      .unlockedBy("has_item", has(TinkerToolParts.fakeStorageBlock))
      .save(materialConsumer, location(folder + "scorched_forge_material"));

    // part swapping
    TinkerStationPartSwappingBuilder.tools(DifferenceIngredient.of(Ingredient.of(TinkerTags.Items.MULTIPART_TOOL), Ingredient.of(TinkerTags.Items.UNSWAPPABLE_PARTS)))
      .save(consumer, location(folder + "tinker_station_part_swapping"));
    TinkerStationPartSwappingBuilder.tools(DifferenceIngredient.of(Ingredient.of(TinkerTags.Items.MULTIPART_TOOL), Ingredient.of(TinkerTags.Items.UNSWAPPABLE_TOOLS)))
      .fromTool().save(consumer, location(folder + "tool_material_swapping"));
    TinkerStationPartSwappingBuilder.tools(Ingredient.of(TinkerTools.arrow.get(), TinkerTools.shuriken.get()))
      .maxStackSize(4)
      .save(consumer, location(folder + "ammo_part_swapping"));
    TinkerStationPartSwappingBuilder.tools(Ingredient.of(TinkerTools.throwingAxe.get()))
      .maxStackSize(2)
      .save(consumer, location(folder + "throwing_axe_part_swapping"));

    // tool repair recipe
    ResourceLocation tinkerStationRepairId = location(folder + "tinker_station_repair");
    consumer.accept(tinkerStationRepairId, new TinkerStationRepairRecipe(tinkerStationRepairId), null);
    ResourceLocation craftingTableRepairId = location(folder + "crafting_table_repair");
    consumer.accept(craftingTableRepairId, new CraftingTableRepairKitRecipe(craftingTableRepairId), null);
  }

  private void damageRecipes(RecipeOutput consumer) {
    // tool damaging
    String damageFolder = "tables/tinker_station_damaging/";
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(TinkerFluids.magmaBottle), 20)
      .save(consumer, location(damageFolder + "magma_bottle"));
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(TinkerFluids.magma), 100)
      .save(consumer, location(damageFolder + "magma_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(TinkerFluids.venomBottle), 200)
      .save(consumer, location(damageFolder + "venom_bottle"));
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(TinkerFluids.venom), 1000)
      .save(consumer, location(damageFolder + "venom_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(Items.LAVA_BUCKET), 500)
      .save(consumer, location(damageFolder + "lava_bucket"));
    TinkerStationDamagingRecipeBuilder.damage(Ingredient.of(TinkerFluids.blazingBlood), 2500)
      .save(consumer, location(damageFolder + "blazing_bucket"));
  }

  @SuppressWarnings("removal")
  private void recyclingRecipes(RecipeOutput consumer) {
    // recipes for recycling vanilla tools
    String folder = "tables/recycling/";

    // default tools, though skip anything that contains metal
    // wood
    Pattern rod = new Pattern(TConstruct.MOD_ID, "rod");
    PartBuilderRecycleBuilder.tool(Items.WOODEN_PICKAXE, Items.WOODEN_AXE)
      .result(rod, Items.STICK, 8)
      .save(consumer, location(folder + "wooden_axe"));
    PartBuilderRecycleBuilder.tool(Items.WOODEN_SWORD, Items.WOODEN_HOE)
      .result(rod, Items.STICK, 5)
      .save(consumer, location(folder + "wooden_sword"));
    PartBuilderRecycleBuilder.tool(Items.WOODEN_SHOVEL)
      .result(rod, Items.STICK, 4)
      .save(consumer, location(folder + "wooden_shovel"));
    Pattern string = new Pattern(TConstruct.MOD_ID, "bowstring");
    PartBuilderRecycleBuilder.tool(Items.BOW)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 3)
      .save(consumer, location(folder + "bow"));
    Pattern ingot = new Pattern(TConstruct.MOD_ID, "ingot");
    PartBuilderRecycleBuilder.tool(Items.CROSSBOW)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 2)
      .result(ingot, Tags.Items.INGOTS_IRON, 1)
      .save(consumer, location(folder + "crossbow"));
    PartBuilderRecycleBuilder.tool(Items.FISHING_ROD)
      .result(rod, Items.STICK, 3)
      .result(string, Items.STRING, 2)
      .save(consumer, location(folder + "fishing_rod"));
    // stone
    Pattern block = new Pattern(TConstruct.MOD_ID, "block");
    PartBuilderRecycleBuilder.tool(Items.STONE_PICKAXE, Items.STONE_AXE)
      .result(block, Items.COBBLESTONE, 3)
      .save(consumer, location(folder + "stone_axe"));
    PartBuilderRecycleBuilder.tool(Items.STONE_SWORD, Items.STONE_HOE)
      .result(block, Items.COBBLESTONE, 2)
      .save(consumer, location(folder + "stone_sword"));
    PartBuilderRecycleBuilder.tool(Items.STONE_SHOVEL)
      .result(block, Items.COBBLESTONE, 1)
      .save(consumer, location(folder + "stone_shovel"));
    // while you can melt it, flint and steel is literally just two items with nothing connecting them, so let the part builder recycle them
    PartBuilderRecycleBuilder.tool(Items.FLINT_AND_STEEL)
      .result(new Pattern(TConstruct.MOD_ID, "shard"), Items.FLINT, 1)
      .result(ingot, Items.IRON_INGOT, 1)
      .save(consumer, location(folder + "flint_and_steel"));

    // leather armor
    Pattern leather = new Pattern(TConstruct.MOD_ID, "maille");
    PartBuilderRecycleBuilder.tool(Items.LEATHER_HELMET)
      .result(leather, Items.LEATHER, 5)
      .save(consumer, location(folder + "leather_helmet"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_CHESTPLATE)
      .result(leather, Items.LEATHER, 8)
      .save(consumer, location(folder + "leather_chestplate"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_LEGGINGS, Items.LEATHER_HORSE_ARMOR)
      .result(leather, Items.LEATHER, 7)
      .save(consumer, location(folder + "leather_leggings"));
    PartBuilderRecycleBuilder.tool(Items.LEATHER_BOOTS)
      .result(leather, Items.LEATHER, 4)
      .save(consumer, location(folder + "leather_boots"));

    // turtle shell
    Pattern scale = new Pattern(TConstruct.MOD_ID, "scale");
    PartBuilderRecycleBuilder.tool(Items.TURTLE_HELMET)
      .result(scale, Items.TURTLE_SCUTE, 5)
      .save(consumer, location(folder + "turtle_helmet"));

    // twilight forest
    String tfId = "twilightforest";
    Function<String,ResourceLocation> tf = name -> ResourceLocation.fromNamespaceAndPath(tfId, name);
    RecipeOutput tfConsumer = withCondition(consumer, new ModLoadedCondition(tfId));
    // naga scale armor
    ResourceLocation nagaScale = tf.apply("naga_scale");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("naga_chestplate")).toVanilla())
      .result(scale, ItemNameOutput.fromName(nagaScale, 8))
      .save(tfConsumer, location(folder + "twilightforest/naga_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("naga_leggings")).toVanilla())
      .result(scale, ItemNameOutput.fromName(nagaScale, 7))
      .save(tfConsumer, location(folder + "twilightforest/naga_leggings"));
    // ironwood armor and tools
    TagKey<Item> ironwoodIngot = ItemTags.create(Mantle.commonResource("ingots/ironwood"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_pickaxe"), tf.apply("ironwood_axe")).toVanilla())
      .result(ingot, ironwoodIngot, 3)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_axe"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_sword"), tf.apply("ironwood_hoe")).toVanilla())
      .result(ingot, ironwoodIngot, 2)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_sword"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_shovel")).toVanilla())
      .result(ingot, ironwoodIngot, 1)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_shovel"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_helmet")).toVanilla())
      .result(ingot, ironwoodIngot, 5)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_chestplate")).toVanilla())
      .result(ingot, ironwoodIngot, 8)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_leggings")).toVanilla())
      .result(ingot, ironwoodIngot, 7)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("ironwood_boots")).toVanilla())
      .result(ingot, ironwoodIngot, 4)
      .save(tfConsumer, location(folder + "twilightforest/ironwood_boots"));
    // arctic
    ResourceLocation arcticFur = tf.apply("arctic_fur");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_helmet")).toVanilla())
      .result(leather, ItemNameOutput.fromName(arcticFur, 5))
      .save(tfConsumer, location(folder + "twilightforest/arctic_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_chestplate")).toVanilla())
      .result(leather, ItemNameOutput.fromName(arcticFur, 8))
      .save(tfConsumer, location(folder + "twilightforest/arctic_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_leggings")).toVanilla())
      .result(leather, ItemNameOutput.fromName(arcticFur, 7))
      .save(tfConsumer, location(folder + "twilightforest/arctic_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("arctic_boots")).toVanilla())
      .result(leather, ItemNameOutput.fromName(arcticFur, 4))
      .save(tfConsumer, location(folder + "twilightforest/arctic_boots"));
    // arctic
    ResourceLocation alphaYetiFur = tf.apply("alpha_yeti_fur");
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_helmet")).toVanilla())
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 5))
      .save(tfConsumer, location(folder + "twilightforest/yeti_helmet"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_chestplate")).toVanilla())
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 8))
      .save(tfConsumer, location(folder + "twilightforest/yeti_chestplate"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_leggings")).toVanilla())
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 7))
      .save(tfConsumer, location(folder + "twilightforest/yeti_leggings"));
    PartBuilderRecycleBuilder.tool(ItemNameIngredient.from(tf.apply("yeti_boots")).toVanilla())
      .result(leather, ItemNameOutput.fromName(alphaYetiFur, 4))
      .save(tfConsumer, location(folder + "twilightforest/yeti_boots"));
  }
}
