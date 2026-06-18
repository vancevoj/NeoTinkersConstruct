package slimeknights.tconstruct.tools.recipe.severing;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;

import java.util.Objects;
import java.util.function.Supplier;

/** Builder for severing recipes that have only the base chance and looting bonus as fields */
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "serializer")
public class SpecialSeveringRecipeBuilder extends AbstractRecipeBuilder<SpecialSeveringRecipeBuilder> {
  private final RecipeSerializer<? extends SeveringRecipe> serializer;
  private float baseChance = 0.05f;
  private float lootingBonus = 0.01f;

  /** Creates a new builder for the given serializer. */
  public static SpecialSeveringRecipeBuilder serializer(Supplier<? extends RecipeSerializer<? extends SeveringRecipe>> supplier) {
    return serializer(supplier.get());
  }

  /** Doubles the drop chances for this rare mob */
  public SpecialSeveringRecipeBuilder rareMob() {
    baseChance = 0.1f;
    lootingBonus = 0.02f;
    return this;
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer)));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    AdvancementHolder advancement = buildOptionalAdvancement(id, "modifier");
    output.accept(id, new Finished(id), advancement);
  }

  /**
   * Recipe instance for datagen output. The special severing serializers only serialize the per-level chance and looting
   * bonus (plus the recipe ID), so the entity and output values are unused placeholders here. {@link #getSerializer()} is
   * overridden so the loadable serializer wired by the builder is used to encode the recipe.
   */
  private class Finished extends SeveringRecipe {
    public Finished(ResourceLocation id) {
      super(id, EntityIngredient.of(EntityType.PLAYER), ItemOutput.fromItem(Items.AIR), baseChance, lootingBonus);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
      return serializer;
    }
  }
}
