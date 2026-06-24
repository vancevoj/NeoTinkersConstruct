package slimeknights.tconstruct.library.modifiers.hook.behavior;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
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
 * This interface exposes two methods, {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Enchantment, int)} and {@link #updateEnchantments(IToolStackView, ModifierEntry, Map)}
 * to allow tools to claim to have enchantments to vanilla APIs without modifying NBT. For performance reasons we don't simply have one hook call the other, but their behavior must be consistent.
 * That is, whatever change you make to the level in {@link #updateEnchantmentLevel(IToolStackView, ModifierEntry, Enchantment, int)} must also be reflected in the map in {@link #updateEnchantments(IToolStackView, ModifierEntry, Map)}.
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
  int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level);

  /**
   * Adds all enchantment modifications made by this tool to the map.
   * You may reduce the value in the map to a negative, all values 0 or less will be removed after the hook runs.
   * @param tool      Tool instance
   * @param modifier  Modifier instance
   * @param map       A mutable map to add enchantments from this modifier. May contain negatives.
   * @see #addEnchantment(Map, Enchantment, int)
   */
  void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map);

  /** Adds the given enchantment to the map */
  static void addEnchantment(Map<Enchantment,Integer> map, Enchantment enchantment, int amount) {
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
  static int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
    // 1.21: vanilla enchantments live in the ItemEnchantments data component, keyed by Holder<Enchantment>.
    // We only have the raw Enchantment here (no registry access), so match against the holder value directly.
    int level = 0;
    for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
      if (entry.getKey().value() == enchantment) {
        level = entry.getIntValue();
        break;
      }
    }
    IToolStackView tool = ToolStack.from(stack);
    for (ModifierEntry entry : tool.getModifierList()) {
      level = entry.getHook(ModifierHooks.ENCHANTMENTS).updateEnchantmentLevel(tool, entry, enchantment, level);
    }
    // we allow hooks to return negative, such as to cancel out an enchantment
    return Math.max(level, 0);
  }

  /**
   * Gets all enchantments on the given stack
   * @param stack  Stack instance
   * @return  All contained enchantments
   */
  static Map<Enchantment,Integer> getAllEnchantments(ItemStack stack) {
    // 1.21: read the base enchantments from the ItemEnchantments data component, reducing the Holder keys to raw
    // Enchantment values so this hook can stay registry free (modules supply raw enchantments).
    Map<Enchantment,Integer> enchantments = new HashMap<>();
    for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
      enchantments.put(entry.getKey().value(), entry.getIntValue());
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
   * Writes the main-hand tool's full enchantment set ({@link #getAllEnchantments(ItemStack)}, including virtual modifier
   * enchantments such as fortune/silk touch that are never stored in NBT) onto the stack's
   * {@link DataComponents#ENCHANTMENTS} component, so vanilla block-drop loot actually sees them. Restore with
   * {@link slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook#restoreEnchantments}.
   * <p>
   * 1.21: in older versions the tool surfaced its enchantments to loot via {@code Item#getAllEnchantments}; that hook
   * no longer exists, so the loot context only sees enchantments actually present in the ENCHANTMENTS component. The
   * {@code HarvestEnchantmentsModifierHook} path only injects OFFHAND/armor enchantments (it skips the main hand), so
   * without this the main hand's own fortune/silk touch (issue #11) never reached block drops.
   * @param stack   Main hand tool stack
   * @param player  Player breaking the block, may be null (e.g. projectile harvest)
   * @param level   Level, used to resolve enchantment holders from the datapack registry
   * @return  Original enchantments component to restore afterwards, or null if nothing changed (so no restore needed)
   */
  @Nullable
  static ItemEnchantments updateToolEnchantments(ItemStack stack, @Nullable Player player, Level level) {
    // creative players don't generate block loot, matching HarvestEnchantmentsModifierHook's guard
    if (player != null && player.isCreative()) {
      return null;
    }
    Map<Enchantment,Integer> enchantments = getAllEnchantments(stack);
    ItemEnchantments original = stack.getEnchantments();
    // wrap the raw enchantment map back into the component, resolving holders from the datapack enchantment registry
    Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    for (Map.Entry<Enchantment,Integer> entry : enchantments.entrySet()) {
      mutable.set(registry.wrapAsHolder(entry.getKey()), entry.getValue());
    }
    ItemEnchantments updated = mutable.toImmutable();
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
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
      for (EnchantmentModifierHook module : modules) {
        level = module.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map) {
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
    Enchantment getEnchantment(IToolStackView tool, ModifierEntry modifier);

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
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
      if (enchantment == getEnchantment(tool, modifier)) {
        level += getEnchantmentLevel(tool, modifier);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map) {
      addEnchantment(map, getEnchantment(tool, modifier), getEnchantmentLevel(tool, modifier));
    }
  }

  /** Combination of {@link SingleEnchantment} with {@link BlockHarvestModifierHook.MarkHarvesting} */
  interface SingleHarvestEnchantment extends SingleEnchantment, BlockHarvestModifierHook.MarkHarvesting {
    @Override
    default int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        return SingleEnchantment.super.updateEnchantmentLevel(tool, modifier, enchantment, level);
      }
      return level;
    }

    @Override
    default void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map) {
      if (BlockHarvestModifierHook.MarkHarvesting.isHarvesting(tool)) {
        SingleEnchantment.super.updateEnchantments(tool, modifier, map);
      }
    }
  }
}
