package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.List;

/**
 * Shapeless recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapelessMaterialsRecipe extends ShapelessRecipe implements MaterialsCraftingTableRecipe {
  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapelessMaterialsRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    super(group, category, result, ingredients);
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  /**
   * Wraps a vanilla shapeless recipe. Used by {@link MaterialsConsumerBuilder} during datagen.
   * @param recipe          Vanilla shapeless recipe
   * @param partCount       Number of leading ingredients that count as parts
   * @param extraMaterials  Extra materials to add beyond the parts
   */
  public ShapelessMaterialsRecipe(ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    this(recipe.getGroup(), recipe.category(), recipe.result, recipe.getIngredients(), partCount, extraMaterials);
  }

  @Override
  public List<Ingredient> getParts() {
    return getIngredients();
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return ShapedMaterialsRecipe.assemble(super.assemble(inventory, registryAccess), inventory, getIngredients(), partCount, false, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapelessMaterialsRecipe> {
    private static final StreamCodec<RegistryFriendlyByteBuf,List<MaterialVariantId>> EXTRA_MATERIALS_STREAM = MaterialVariantId.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf,List<Ingredient>> INGREDIENTS_STREAM = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final MapCodec<ShapelessMaterialsRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
      CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
      ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
      Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").xmap(
        ingredients -> {
          NonNullList<Ingredient> list = NonNullList.create();
          list.addAll(ingredients);
          return list;
        },
        list -> list
      ).forGetter(ShapelessRecipe::getIngredients),
      Codec.INT.fieldOf("parts").forGetter(r -> r.partCount),
      ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS.optionalFieldOf(ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_KEY, List.of()).forGetter(r -> r.extraMaterials)
    ).apply(inst, ShapelessMaterialsRecipe::new));

    @Override
    public MapCodec<ShapelessMaterialsRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapelessMaterialsRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      List<Ingredient> ingredientList = INGREDIENTS_STREAM.decode(buffer);
      NonNullList<Ingredient> ingredients = NonNullList.create();
      ingredients.addAll(ingredientList);
      int partCount = buffer.readByte();
      List<MaterialVariantId> extraMaterials = EXTRA_MATERIALS_STREAM.decode(buffer);
      return new ShapelessMaterialsRecipe(group, category, result, ingredients, partCount, extraMaterials);
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapelessMaterialsRecipe recipe) {
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
      INGREDIENTS_STREAM.encode(buffer, recipe.getIngredients());
      buffer.writeByte(recipe.partCount);
      EXTRA_MATERIALS_STREAM.encode(buffer, recipe.extraMaterials);
    }
  }
}
