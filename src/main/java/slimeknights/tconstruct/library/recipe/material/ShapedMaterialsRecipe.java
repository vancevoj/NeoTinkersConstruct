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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapedMaterialsRecipe extends ShapedRecipe implements MaterialsCraftingTableRecipe {
  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /**
   * If true, a part may show up multiple times in the inputs, and all copies should match.
   * If false, only the first instance of a part is checked for each input, allowing a tool with the same part multiple times.
   */
  private final boolean checkRepeats;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;
  public ShapedMaterialsRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    super(group, category, pattern, result, showNotification);
    this.parts = parts;
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  /**
   * Wraps a vanilla shaped recipe, resolving the given part pattern characters into ingredients using the recipe's key.
   * Used by {@link MaterialsConsumerBuilder} during datagen.
   * @param recipe          Vanilla shaped recipe
   * @param partPattern     String of pattern symbols indicating which ingredients are parts
   * @param extraMaterials  Extra materials to add beyond the parts
   */
  public ShapedMaterialsRecipe(ShapedRecipe recipe, String partPattern, List<MaterialVariantId> extraMaterials) {
    this(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.result, recipe.showNotification(), resolveParts(recipe.pattern, partPattern), extraMaterials);
  }

  /** Resolves the part pattern symbols into ingredients using the pattern's key map */
  private static List<Ingredient> resolveParts(ShapedRecipePattern pattern, String partPattern) {
    Map<Character,Ingredient> key = pattern.data.orElseThrow(() -> new IllegalStateException("Shaped material recipe pattern is missing its key data")).key();
    List<Ingredient> parts = new ArrayList<>(partPattern.length());
    for (int i = 0; i < partPattern.length(); i++) {
      char sym = partPattern.charAt(i);
      Ingredient ingredient = key.get(sym);
      if (ingredient == null) {
        throw new IllegalArgumentException("Parts references symbol '" + sym + "' but it's not defined in the key");
      }
      parts.add(ingredient);
    }
    return List.copyOf(parts);
  }

  @Override
  public int getPartCount() {
    return parts.size();
  }

  /**
   * Finds materials for each of the parts
   * @return Array of all matched materials. Array will have no null entries, though the array may be null if no match was found.
   */
  @Nullable
  static MaterialVariantId[] findMaterials(CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats) {
    // want one material for each
    MaterialVariantId[] materials = new MaterialVariantId[partCount];
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < partCount; p++) {
          MaterialVariantId current = materials[p];
          // if we have not found the material yet, or repeats are considered the same material, test the ingredient
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched;
            if (stack.getItem() instanceof IMaterialItem materialItem) {
              matched = materialItem.getMaterial(stack);
            } else {
              matched = MaterialRecipeCache.findRecipe(stack).getMaterial().getVariant();
            }
            // first occurrence? thats our material
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              // if same material but different variants, just discard the variant
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              } else {
                // if different materials, no match
                return null;
              }
            }
          }
        }
      }
    }
    // ensure we found all materials needed
    for (int p = 0; p < partCount; p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }
    // ensure all part materials matched and we found all parts
    return findMaterials(inventory, parts, parts.size(), checkRepeats) != null;
  }

  /** Common logic to this and {@link ShapedMaterialsRecipe} */
  public static void setMaterial(ItemStack stack, MaterialVariantId material, List<MaterialVariantId> extraMaterials) {
    if (extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
      materialItem.setMaterial(stack, material);
    } else {
      MaterialNBT.Builder builder = MaterialNBT.builder();
      builder.add(material);
      for (MaterialVariantId extraMaterial : extraMaterials) {
        builder.add(extraMaterial);
      }
      ToolStack.from(stack).setMaterials(builder.build());
    }
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    setMaterial(stack, material, extraMaterials);
  }

  /** Assembles the item with material information */
  static ItemStack assemble(ItemStack stack, CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats, List<MaterialVariantId> extraMaterials) {
    MaterialVariantId[] materials = findMaterials(inventory, parts, partCount, checkRepeats);
    if (materials != null) {
      // if the result is a tool part, and we only have the one material, set its material
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      // add each material
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      // add extra materials
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return assemble(super.assemble(inventory, registryAccess), inventory, parts, parts.size(), checkRepeats, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialsRecipe> {
    /** Vanilla codec for a single material variant ID, bridging the Mantle loadable string form */
    static final Codec<MaterialVariantId> MATERIAL = Codec.STRING.xmap(MaterialVariantId::parse, MaterialVariantId::toString);
    /** Codec for the list of extra materials, reused by the other material crafting recipes */
    static final Codec<List<MaterialVariantId>> EXTRA_MATERIALS = MATERIAL.listOf();
    /** Field name for the extra materials list */
    static final String EXTRA_MATERIALS_KEY = "extra_materials";
    static final StreamCodec<RegistryFriendlyByteBuf,List<MaterialVariantId>> EXTRA_MATERIALS_STREAM = MaterialVariantId.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final MapCodec<ShapedMaterialsRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
      CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
      ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
      ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
      Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
      Ingredient.CODEC.listOf().fieldOf("parts").forGetter(r -> r.parts),
      EXTRA_MATERIALS.optionalFieldOf(EXTRA_MATERIALS_KEY, List.of()).forGetter(r -> r.extraMaterials)
    ).apply(inst, ShapedMaterialsRecipe::new));

    @Override
    public MapCodec<ShapedMaterialsRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapedMaterialsRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      boolean showNotification = buffer.readBoolean();
      List<Ingredient> parts = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
      List<MaterialVariantId> extraMaterials = EXTRA_MATERIALS_STREAM.decode(buffer);
      return new ShapedMaterialsRecipe(group, category, pattern, result, showNotification, parts, extraMaterials);
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
      ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
      buffer.writeBoolean(recipe.showNotification());
      Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.parts);
      EXTRA_MATERIALS_STREAM.encode(buffer, recipe.extraMaterials);
    }
  }
}
