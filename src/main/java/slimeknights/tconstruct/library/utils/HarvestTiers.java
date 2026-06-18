package slimeknights.tconstruct.library.utils;

import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Harvest level display names
 */
public class HarvestTiers {
  private HarvestTiers() {}

  /** Cache of name for each tier */
  private static final Map<Tier, Component> harvestLevelNames = Maps.newHashMap();
  /** Listener to clear name cache so we get new colors */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> harvestLevelNames.clear();

  /**
   * Gets a name for the given tier.
   * <p>
   * TODO(neoport): TierSortingRegistry was removed in 1.21 along with its name registry. Vanilla tiers are mapped to a
   * minecraft namespaced ID by their enum name; any other tier falls back to a generic ID so a translation key still resolves.
   */
  private static ResourceLocation tierId(Tier tier) {
    if (tier instanceof Tiers vanilla) {
      return ResourceLocation.fromNamespaceAndPath("minecraft", vanilla.name().toLowerCase(Locale.ROOT));
    }
    return TConstruct.getResource("unknown");
  }

  /**
   * Gets the ID for the given tier, replacement for {@code TierSortingRegistry.getName}.
   * <p>
   * TODO(neoport): TierSortingRegistry removed in 1.21; only vanilla tiers have a stable ID. Returns null for unknown tiers.
   */
  @Nullable
  public static ResourceLocation getId(Tier tier) {
    if (tier instanceof Tiers vanilla) {
      return ResourceLocation.fromNamespaceAndPath("minecraft", vanilla.name().toLowerCase(Locale.ROOT));
    }
    return null;
  }

  /**
   * Gets the tier for the given ID, replacement for {@code TierSortingRegistry.byName}.
   * <p>
   * TODO(neoport): TierSortingRegistry removed in 1.21; only vanilla tiers are resolvable by ID. Returns null otherwise.
   */
  @Nullable
  public static Tier byName(ResourceLocation id) {
    if ("minecraft".equals(id.getNamespace())) {
      for (Tiers vanilla : Tiers.values()) {
        if (vanilla.name().toLowerCase(Locale.ROOT).equals(id.getPath())) {
          return vanilla;
        }
      }
    }
    return null;
  }

  /**
   * Checks if the given tier can harvest the given block for drops, replacement for {@code TierSortingRegistry.isCorrectTierForDrops}.
   * <p>
   * TODO(neoport): TierSortingRegistry removed in 1.21; cross mod tier ordering is gone. We use the vanilla incorrect-blocks tag.
   */
  public static boolean isCorrectTierForDrops(Tier tier, BlockState state) {
    return !state.is(tier.getIncorrectBlocksForDrops());
  }

  /**
   * Gets a numeric sort key for a tier so the larger and smaller helpers keep working without a sorting registry.
   * <p>
   * TODO(neoport): TierSortingRegistry removed in 1.21, true cross mod tier ordering is no longer available. We approximate
   * ordering using the vanilla Tiers ordinal; unknown tiers sort below all vanilla tiers.
   */
  private static int sortKey(Tier tier) {
    if (tier instanceof Tiers vanilla) {
      return vanilla.ordinal();
    }
    return -1;
  }

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(Tier tier) {
    String key = Util.makeTranslationKey("harvest_tier", tierId(tier));
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /**
   * Gets the harvest level name for the given level number
   * @param tier  Tier
   * @return  Level name
   */
  public static Component getName(Tier tier) {
    return harvestLevelNames.computeIfAbsent(tier, n ->  makeLevelKey(tier));
  }

  /** Gets the larger of two tiers */
  public static Tier max(Tier a, Tier b) {
    // note an unknown tier sorts below vanilla tiers, so the larger of an unknown tier and a vanilla one is the vanilla one
    if (sortKey(b) > sortKey(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smaller of two tiers */
  public static Tier min(Tier a, Tier b) {
    // note an unknown tier sorts below vanilla tiers, so the smaller of an unknown tier and a vanilla one is the unknown one
    if (sortKey(b) < sortKey(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smallest tier */
  public static Tier minTier() {
    // TODO(neoport): TierSortingRegistry removed in 1.21, the registry sorted list is gone so we default to the lowest vanilla tier
    return Tiers.WOOD;
  }
}
