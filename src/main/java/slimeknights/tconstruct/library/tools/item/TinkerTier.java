package slimeknights.tconstruct.library.tools.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Dummy tier implementation to allow Tinkers' Construct to make modifiable items extend TieredItem, for piglin compat */
public enum TinkerTier implements Tier {
  INSTANCE;

  @Override
  public int getUses() {
    return 0;
  }

  @Override
  public float getSpeed() {
    return 0;
  }

  @Override
  public float getAttackDamageBonus() {
    return 0;
  }

  @Override
  public TagKey<Block> getIncorrectBlocksForDrops() {
    // we override isCorrectToolForDrops on the item, so this is unused; return the vanilla "needs nothing" set
    return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
  }

  @Override
  public int getEnchantmentValue() {
    return 0;
  }

  @Override
  public Ingredient getRepairIngredient() {
    return Ingredient.EMPTY;
  }
}
