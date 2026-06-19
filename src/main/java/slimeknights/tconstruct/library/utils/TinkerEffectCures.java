package slimeknights.tconstruct.library.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.EffectCure;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Helper for restoring Forge's per-stack curative items behavior on top of NeoForge 1.21's {@link EffectCure} tokens.
 * <p>
 * In 1.20.1 each {@link MobEffectInstance} carried a {@code List<ItemStack>} of curative items (default {@code [milk_bucket]}),
 * and {@code LivingEntity#curePotionEffects(ItemStack)} removed effects whose list contained that stack. In 1.21 NeoForge replaced
 * this with named {@link EffectCure} tokens stored in {@link MobEffectInstance#getCures()}, where milk fires {@code removeEffectsCuredBy(EffectCures.MILK)}.
 * <p>
 * To preserve "cured only by this specific item" behavior we mint a stable {@link EffectCure} per {@link Item} (namespaced by the item id).
 * Binding an effect to a list of items clears its default cures and adds the per-item tokens; curing with an item removes effects carrying that item's token.
 */
public final class TinkerEffectCures {
  private TinkerEffectCures() {}

  /** Prefix for per-item cure tokens, keeps them distinct from vanilla/NeoForge cures like {@code milk}. */
  private static final String ITEM_CURE_PREFIX = "tconstruct:item/";

  /**
   * Gets the {@link EffectCure} token associated with curing via the given item. Mirrors the legacy {@code new ItemStack(item)} curative item.
   * @param item  Item used to cure the effect
   * @return  Stable cure token for the item
   */
  public static EffectCure itemCure(Item item) {
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
    return EffectCure.get(ITEM_CURE_PREFIX + id);
  }

  /**
   * Applies a list of curative items to an effect instance, mirroring the legacy {@code setCurativeItems} behavior.
   * <ul>
   *   <li>{@code null}: leave the effect's default cures untouched (e.g. still curable by milk).</li>
   *   <li>empty list: clear all cures, making the effect uncurable (legacy {@code setCurativeItems(new ArrayList<>())}).</li>
   *   <li>non-empty list: clear default cures and bind the effect to exactly the per-item cure tokens (legacy {@code clear()} then {@code add(item)}).</li>
   * </ul>
   * @param instance        Effect instance to modify
   * @param curativeItems   Items allowed to cure the effect, or null for defaults
   */
  public static void setCures(MobEffectInstance instance, @Nullable List<Item> curativeItems) {
    if (curativeItems == null) {
      return;
    }
    Set<EffectCure> cures = instance.getCures();
    cures.clear();
    for (Item item : curativeItems) {
      cures.add(itemCure(item));
    }
  }
}
