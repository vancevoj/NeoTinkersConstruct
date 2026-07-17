package slimeknights.tconstruct.library.modifiers.hook.behavior;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This interface exposes two methods, {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Holder, int)} and {@link #updateEnchantments(IToolStackView, ModifierEntry, Map)}
 * to allow tools to claim to have enchantments to vanilla APIs without modifying NBT. For performance reasons we don't simply have one hook call the other, but their behavior must be consistent.
 * That is, whatever change you make to the level in {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Holder, int)} must also be reflected in the map in {@link #updateEnchantments(IToolStackView, ModifierEntry, Map)}.
 * <p>
 * 1.21: enchantments are keyed by {@link Holder} rather than the raw {@link Enchantment}, matching the ItemEnchantments
 * data component. Holders must always come from the enchantment registry (never {@link Holder#direct}) as this hook
 * relies on registry holder identity for map keys.
 */
public interface EnchantmentModifierHook {
  /** Predicate to remove unneeded values from the map */
  Predicate<Integer> VALUE_REMOVER = value -> value == null || value <= 0;

  /**
   * Gets the enchantment level for the given tool
   * @param tool         Tool instance
   * @param modifier     Modifier instance
   * @param enchantment  Enchantment being queried
   * @param level        Level before this enchantment makes any changes. May be negative, will be capped to 0+ after the hook runs.
   * @return Enchantment level, typically added to {@code level} instead of replacing it. May be negative, will be capped to 0+ after the hook runs.
   */
  int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level);

  /**
   * Adds all enchantment modifications made by this tool to the map.
   * You may reduce the value in the map to a negative, all values 0 or less will be removed after the hook runs.
   * @param tool      Tool instance
   * @param modifier  Modifier instance
   * @param map       A mutable map to add enchantments from this modifier. May contain negatives.
   * @see #addEnchantment(Map, Holder, int)
   */
  void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Holder<Enchantment>,Integer> map);

  /** Adds the given enchantment to the map */
  static void addEnchantment(Map<Holder<Enchantment>,Integer> map, Holder<Enchantment> enchantment, int amount) {
    if (amount != 0) {
      map.put(enchantment, map.getOrDefault(enchantment, 0) + amount);
    }
  }

  /**
   * Gets the enchantment level for the given tool
   * @param stack        Item stack instance
   * @param enchantment  Enchantment to query
   * @return  Enchantment level
   */
  static int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
    // read the base level straight from the component, using the stack getter would recurse back into this hook
    int level = stack.getTagEnchantments().getLevel(enchantment);
    IToolStackView tool = ToolStack.from(stack);
    for (ModifierEntry entry : tool.getModifierList()) {
      level = entry.getHook(ModifierHooks.ENCHANTMENTS).updateEnchantmentLevel(tool, entry, enchantment, level);
    }
    // we allow hooks to return negative, such as to cancel out an enchantment
    return Math.max(level, 0);
  }

  /**
   * Gets all enchantments on the given stack, including virtual enchantments from modifiers which are never stored in NBT
   * @param stack  Stack instance
   * @return  All contained enchantments
   */
  static Map<Holder<Enchantment>,Integer> getAllEnchantments(ItemStack stack) {
    // read the base enchantments straight from the component, using the stack getter would recurse back into this hook
    Map<Holder<Enchantment>,Integer> enchantments = new HashMap<>();
    for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getTagEnchantments().entrySet()) {
      enchantments.put(entry.getKey(), entry.getIntValue());
    }
    IToolStackView tool = ToolStack.from(stack);
    for (ModifierEntry entry : tool.getModifierList()) {
      entry.getHook(ModifierHooks.ENCHANTMENTS).updateEnchantments(tool, entry, enchantments);
    }
    // we allow hooks to return negative, such as to cancel out an enchantment
    enchantments.values().removeIf(VALUE_REMOVER);
    return enchantments;
  }

  /**
   * Gets all enchantments on the given stack as an enchantments component, including virtual enchantments from modifiers.
   * This is the value tinker tools report to {@code ItemStack#getAllEnchantments}, which is how vanilla enchantment
   * iteration (and therefore enchantment attribute effects such as swift sneak) sees modifier enchantments.
   * <p>
   * No registry lookup is needed as modules supply registry holders directly.
   * @param stack  Stack instance
   * @return  All contained enchantments
   */
  static ItemEnchantments getEnchantmentComponent(ItemStack stack) {
    return toComponent(stack.getTagEnchantments(), getAllEnchantments(stack));
  }

  /**
   * Merges the given enchantment map into the original component, preserving its tooltip visibility flag
   * @param original      Original enchantments component, used as the base so {@code showInTooltip} is kept
   * @param enchantments  Enchantment map, must already have non-positive values removed
   * @return  Component containing exactly the passed enchantments
   */
  static ItemEnchantments toComponent(ItemEnchantments original, Map<Holder<Enchantment>,Integer> enchantments) {
    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(original);
    // drop anything the hooks removed, the map is authoritative
    mutable.removeIf(holder -> !enchantments.containsKey(holder));
    for (Map.Entry<Holder<Enchantment>,Integer> entry : enchantments.entrySet()) {
      mutable.set(entry.getKey(), entry.getValue());
    }
    return mutable.toImmutable();
  }

  /**
   * Writes the main-hand tool's full enchantment set ({@link #getAllEnchantments(ItemStack)}, including virtual modifier
   * enchantments such as fortune/silk touch that are never stored in NBT) onto the stack's
   * {@link DataComponents#ENCHANTMENTS} component, so vanilla block-drop loot actually sees them. Restore with
   * {@link slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook#restoreEnchantments}.
   * <p>
   * Block loot reads enchantments off the stack's component rather than through {@code ItemStack#getAllEnchantments}, so
   * unlike gameplay lookups it cannot see virtual modifier enchantments. The {@code HarvestEnchantmentsModifierHook} path
   * only injects OFFHAND/armor enchantments (it skips the main hand), so without this the main hand's own fortune/silk
   * touch (issue #11) never reached block drops.
   * @param stack   Main hand tool stack
   * @param player  Player breaking the block, may be null (e.g. projectile harvest)
   * @return  Original enchantments component to restore afterwards, or null if nothing changed (so no restore needed)
   */
  @Nullable
  static ItemEnchantments updateToolEnchantments(ItemStack stack, @Nullable Player player) {
    // creative players don't generate block loot, matching HarvestEnchantmentsModifierHook's guard
    if (player != null && player.isCreative()) {
      return null;
    }
    ItemEnchantments original = stack.getTagEnchantments();
    ItemEnchantments updated = toComponent(original, getAllEnchantments(stack));
    // nothing to do if the tool has no virtual enchantments beyond what is already in the component
    if (updated.equals(original)) {
      return null;
    }
    stack.set(DataComponents.ENCHANTMENTS, updated);
    return original;
  }

  /** Merger that combines all modules together */
  record AllMerger(Collection<EnchantmentModifierHook> modules) implements EnchantmentModifierHook {
    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      for (EnchantmentModifierHook module : modules) {
        level = module.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Holder<Enchantment>,Integer> map) {
      for (EnchantmentModifierHook module : modules) {
        module.updateEnchantments(tool, modifier, map);
      }
    }
  }

  /**
   * Simple implementation of this hook with just a single enchantment
   */
  @SuppressWarnings("unused") // API
  interface SingleEnchantment extends EnchantmentModifierHook {
    /**
     * Gets the enchantment this hook adds to the tool
     * @param tool      Tool instance
     * @param modifier  Modifier instance
     * @return  Enchantment for this hook to add
     */
    Holder<Enchantment> getEnchantment(IToolStackView tool, ModifierEntry modifier);

    /**
     * Gets the level of the enchantment to add
     * @param tool      Tool instance
     * @param modifier  Modifier instance
     * @return  Level of the enchantment to add
     */
    default int getEnchantmentLevel(IToolStackView tool, ModifierEntry modifier) {
      return modifier.getLevel();
    }

    @Override
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (enchantment.value() == getEnchantment(tool, modifier).value()) {
        level += getEnchantmentLevel(tool, modifier);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Holder<Enchantment>,Integer> map) {
      addEnchantment(map, getEnchantment(tool, modifier), getEnchantmentLevel(tool, modifier));
    }
  }

  /** Combination of {@link SingleEnchantment} with {@link BlockHarvestModifierHook.MarkHarvesting} */
  interface SingleHarvestEnchantment extends SingleEnchantment, BlockHarvestModifierHook.MarkHarvesting {
    @Override
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        return SingleEnchantment.super.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Holder<Enchantment>,Integer> map) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        SingleEnchantment.super.updateEnchantments(tool, modifier, map);
      }
    }
  }
}
