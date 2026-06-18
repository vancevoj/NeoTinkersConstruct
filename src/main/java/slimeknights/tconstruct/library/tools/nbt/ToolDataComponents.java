package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;

import java.util.function.Consumer;

/**
 * Registration for the data components backing the Tinkers' tool model.
 * <p>
 * In 1.20.5+ item NBT was replaced by data components. Tinkers historically stored the entire tool
 * (materials, modifiers, stats, persistent/volatile mod data, damage) in a single {@link CompoundTag} on the stack.
 * To keep that serialization intact while moving off raw item NBT, the whole compound is stored in one
 * component, {@link #TOOL_DATA}. {@link ToolStack} reads from and writes back to this component.
 */
public class ToolDataComponents {
  private ToolDataComponents() {}

  /** Deferred register for data components */
  private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(TConstruct.MOD_ID);

  /**
   * Component holding the full tool NBT compound. Replaces the legacy item NBT tag {@link ToolStack} used to read.
   * Persistent via {@link CompoundTag#CODEC} and synced using the trusted compound stream codec (server is trusted to send tool data).
   */
  public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> TOOL_DATA =
    COMPONENTS.registerComponentType("tool_data", builder -> builder
      .persistent(CompoundTag.CODEC)
      .networkSynchronized(ByteBufCodecs.TRUSTED_COMPOUND_TAG));

  /** Registers the deferred register onto the mod bus. Must be called from the main mod constructor. */
  public static void init(IEventBus bus) {
    COMPONENTS.register(bus);
  }


  /* Stack helpers - replace the legacy stack.getTag()/getOrCreateTag()/setTag() calls */

  /**
   * Gets the tool data compound stored on the stack, or null if absent.
   * Direct replacement for the legacy {@code stack.getTag()} when used for read-only tool data access.
   * The returned tag is the stored component value and should be treated as read-only; use {@link #update} to mutate.
   * @param stack  Stack to read
   * @return  Tool data compound, or null if the component is absent
   */
  @Nullable
  public static CompoundTag getTag(ItemStack stack) {
    return stack.get(TOOL_DATA.get());
  }

  /**
   * Gets the tool data compound stored on the stack, or an empty compound if absent. Never null.
   * The returned tag should be treated as read-only; use {@link #update} or {@link #setTag} to write changes back.
   * @param stack  Stack to read
   * @return  Tool data compound, or an empty compound if absent
   */
  public static CompoundTag getOrEmpty(ItemStack stack) {
    CompoundTag tag = stack.get(TOOL_DATA.get());
    return tag == null ? new CompoundTag() : tag;
  }

  /**
   * Sets the tool data compound on the stack. Direct replacement for the legacy {@code stack.setTag(tag)}.
   * @param stack  Stack to write
   * @param tag    Tag to store
   */
  public static void setTag(ItemStack stack, CompoundTag tag) {
    stack.set(TOOL_DATA.get(), tag);
  }

  /**
   * Reads the current tool data (copying it), applies the mutation, then writes it back to the stack.
   * Direct replacement for the legacy {@code stack.getOrCreateTag().putX(...)} mutate-in-place idiom.
   * @param stack     Stack to update
   * @param mutator   Consumer mutating the tool data compound
   * @return  The same stack, for chaining
   */
  public static ItemStack update(ItemStack stack, Consumer<CompoundTag> mutator) {
    CompoundTag tag = getOrEmpty(stack).copy();
    mutator.accept(tag);
    stack.set(TOOL_DATA.get(), tag);
    return stack;
  }
}
