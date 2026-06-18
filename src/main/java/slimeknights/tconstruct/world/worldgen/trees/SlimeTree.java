package slimeknights.tconstruct.world.worldgen.trees;

import net.minecraft.world.level.block.grower.TreeGrower;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Optional;

/**
 * Factory producing the vanilla {@link TreeGrower} for each slime foliage type.
 * In 1.21 {@link TreeGrower} is final and codec backed, so we build instances instead of subclassing the old grower.
 */
public final class SlimeTree {
  private SlimeTree() {}

  /** Tree grower for each foliage type, keyed by name so the vanilla codec can resolve it */
  public static final TreeGrower EARTH = new TreeGrower("tconstruct:earth_slime", Optional.empty(), Optional.of(TinkerStructures.earthSlimeTree), Optional.empty());
  public static final TreeGrower SKY = new TreeGrower("tconstruct:sky_slime", Optional.empty(), Optional.of(TinkerStructures.skySlimeTree), Optional.empty());
  // ender picks the tall variant 85% of the time, modeled with the secondary tree chance
  public static final TreeGrower ENDER = new TreeGrower("tconstruct:ender_slime", 0.85f, Optional.empty(), Optional.empty(), Optional.of(TinkerStructures.enderSlimeTree), Optional.of(TinkerStructures.enderSlimeTreeTall), Optional.empty(), Optional.empty());

  /** Gets the tree grower for the given foliage type */
  public static TreeGrower create(FoliageType foliageType) {
    return switch (foliageType) {
      case EARTH -> EARTH;
      case SKY -> SKY;
      case ENDER -> ENDER;
      // nether types grow via SlimeFungusBlock, not a sapling grower, but keep a fallback
      case BLOOD, ICHOR -> EARTH;
    };
  }
}
