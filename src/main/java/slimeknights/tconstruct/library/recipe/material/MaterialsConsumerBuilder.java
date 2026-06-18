package slimeknights.tconstruct.library.recipe.material;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.recipe.data.ConsumerWrapperBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Special variant of {@link ConsumerWrapperBuilder} for {@link ShapedMaterialsRecipe} and {@link ShapelessMaterialsRecipe}.
 * <p>
 * Wraps a {@link RecipeOutput}, converting any vanilla {@link ShapedRecipe} or {@link ShapelessRecipe} passed through it
 * into the matching materials recipe with the configured parts and extra materials.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialsConsumerBuilder {
  private final String parts;
  private final int partCount;
  private final List<MaterialVariantId> materials = new ArrayList<>();

  /** Creates a new shaped recipe with the given ingredients as parts */
  public static MaterialsConsumerBuilder shaped(String parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("Parts may not be empty");
    }
    return new MaterialsConsumerBuilder(parts, 0);
  }

  /** Creates a new shapeless recipe with the first ingredients as parts */
  public static MaterialsConsumerBuilder shapeless(int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("Parts must be greater than 0");
    }
    return new MaterialsConsumerBuilder("", parts);
  }

  /** Adds a material to the builder */
  public MaterialsConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  /** Builds the wrapped output */
  public RecipeOutput build(RecipeOutput output) {
    return new Wrapped(output, List.copyOf(materials), parts, partCount);
  }

  private record Wrapped(RecipeOutput original, List<MaterialVariantId> materials, String parts, int partCount) implements RecipeOutput {
    @Override
    public Advancement.Builder advancement() {
      return original.advancement();
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      Recipe<?> wrapped;
      if (partCount > 0) {
        // shapeless: wrap the vanilla shapeless recipe, the first partCount ingredients are the parts
        if (recipe instanceof ShapelessRecipe shapeless) {
          wrapped = new ShapelessMaterialsRecipe(shapeless, partCount, materials);
        } else {
          throw error(id, recipe, "shapeless");
        }
      } else {
        // shaped: wrap the vanilla shaped recipe, mapping the part pattern symbols to ingredients
        if (recipe instanceof ShapedRecipe shaped) {
          wrapped = new ShapedMaterialsRecipe(shaped, parts, materials);
        } else {
          throw error(id, recipe, "shaped");
        }
      }
      original.accept(id, wrapped, advancement, conditions);
    }

    /** Logs a useful error before failing the conversion */
    private static IllegalStateException error(ResourceLocation id, Recipe<?> recipe, String type) {
      TConstruct.LOG.error("Material consumer builder for {} expected a {} recipe but got {}", id, type, recipe.getClass().getName());
      return new IllegalStateException("Material consumer builder for " + id + " expected a " + type + " recipe");
    }
  }
}
