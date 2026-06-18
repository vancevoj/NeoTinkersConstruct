package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Item ingredient matching items with a block form in the given tag */
public class BlockTagIngredient implements ICustomIngredient {
  public static final net.minecraft.resources.ResourceLocation ID = TConstruct.getResource("block_tag");
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<BlockTagIngredient> LOADABLE = RecordLoadable.create(
    Loadables.BLOCK_TAG.requiredField("tag", i -> i.tag),
    BlockTagIngredient::new);
  /** Ingredient type instance */
  // TODO(neoport): register this IngredientType on NeoForgeRegistries.Keys.INGREDIENT_TYPES under id tconstruct:block_tag
  public static final IngredientType<BlockTagIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final TagKey<Block> tag;
  @Nullable
  private Set<Item> matchingItems;

  public BlockTagIngredient(TagKey<Block> tag) {
    this.tag = tag;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && getMatchingItems().contains(stack.getItem());
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  /** Gets the ordered matching items set */
  private Set<Item> getMatchingItems() {
    if (matchingItems == null) {
      matchingItems = RegistryHelper.getTagValueStream(BuiltInRegistries.BLOCK, tag)
                                    .map(Block::asItem)
                                    .filter(item -> item != Items.AIR)
                                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return matchingItems;
  }

  @Override
  public Stream<ItemStack> getItems() {
    return getMatchingItems().stream().map(ItemStack::new);
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }
}
