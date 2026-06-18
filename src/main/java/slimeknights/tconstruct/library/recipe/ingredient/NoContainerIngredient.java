package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;

/** Ingredient matching an item with no container item, used to ensure NBT fluid items are empty */
public class NoContainerIngredient extends NestedIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("no_container");
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<NoContainerIngredient> LOADABLE = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.requiredField("match", i -> i.nested),
    NoContainerIngredient::new);
  /** Ingredient type instance */
  // TODO(neoport): register this IngredientType on NeoForgeRegistries.Keys.INGREDIENT_TYPES under id tconstruct:no_container
  public static final IngredientType<NoContainerIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  protected NoContainerIngredient(Ingredient nested) {
    super(nested);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && super.test(stack) && !stack.hasCraftingRemainingItem();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }


  /* Static constructors */

  /** Creates an instance from the given nested ingredient */
  public static NoContainerIngredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient);
  }

  /** Creates an instance from the given items */
  public static NoContainerIngredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  /** Creates an instance from the given stacks */
  public static NoContainerIngredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  /** Creates an instance from the given tag */
  public static NoContainerIngredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }
}
