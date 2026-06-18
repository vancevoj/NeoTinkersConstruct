package slimeknights.tconstruct.library.recipe.entitymelting;

import net.minecraft.world.item.crafting.RecipeInput;
import slimeknights.mantle.recipe.container.IEmptyContainer;

/**
 * Empty recipe input for entity melting recipes. Entity melting matches against an {@link net.minecraft.world.entity.EntityType},
 * so this container carries no items and exists only to satisfy the recipe type bounds.
 */
public interface IEntityMeltingContainer extends IEmptyContainer, RecipeInput {
  /** Empty instance, for cases where a nonnull inventory is required */
  IEntityMeltingContainer EMPTY = new IEntityMeltingContainer() {};

  /** Implements {@link RecipeInput#size()} using the legacy container size */
  @Override
  default int size() {
    return getContainerSize();
  }
}
