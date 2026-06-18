package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Dispatcher exposing Tinkers' tool capabilities on modifiable items.
 * <p>
 * In NeoForge 1.21 capabilities are no longer attached per stack via {@code ICapabilityProvider}; they are registered per
 * item per capability type in {@link RegisterCapabilitiesEvent}. This class keeps the modular {@link IToolCapabilityProvider}
 * registry from the Forge version: modifiers register a provider constructor through {@link #register(BiFunction)}, and
 * {@link #register(RegisterCapabilitiesEvent, ItemCapability, Item...)} wires those providers up to a concrete capability for a
 * set of items. The handler is built fresh per query and {@link ToolStack#refreshTag(ItemStack)} keeps it in sync with the stack.
 */
public class ToolCapabilityProvider {
  private ToolCapabilityProvider() {}

  private static final List<BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider>> PROVIDER_CONSTRUCTORS = new ArrayList<>();

  /** Registers a tool capability provider constructor. Every new tool will call this constructor to create your provider.
   * Is it valid for this constructor to return null, just note that it will not be called a second time if the tools state changes. Thus you should avoid conditioning on anything other than item type */
  public static void register(BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider> constructor) {
    PROVIDER_CONSTRUCTORS.add(constructor);
  }

  /**
   * Resolves the given capability for a tool stack by dispatching through the registered providers.
   * @param stack  Tool stack
   * @param cap    Capability to resolve
   * @param <T>    Capability type
   * @return  Capability instance, or null if no provider returned one
   */
  @Nullable
  private static <T> T resolve(ItemStack stack, ItemCapability<T, ?> cap) {
    ToolStack tool = ToolStack.from(stack);
    // clear the tool cache, as it may have changed since the last time a cap was fetched
    tool.refreshTag(stack);
    Supplier<ToolStack> supplier = () -> tool;
    // build the providers for this stack and return the first successful one
    for (BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider> constructor : PROVIDER_CONSTRUCTORS) {
      IToolCapabilityProvider provider = constructor.apply(stack, supplier);
      if (provider != null) {
        provider.clearCache();
        T result = provider.getCapability(tool, cap);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * Registers the given capability for the passed items, dispatching to the modular tool capability providers.
   * Call from {@link RegisterCapabilitiesEvent} on the mod bus for each modifiable item.
   * @param event  Capability registration event
   * @param cap    Capability to register (e.g. {@code Capabilities.FluidHandler.ITEM})
   * @param items  Items to register the capability for
   * @param <T>    Capability type
   */
  public static <T> void register(RegisterCapabilitiesEvent event, ItemCapability<T, Void> cap, Item... items) {
    event.registerItem(cap, (stack, ctx) -> resolve(stack, cap), items);
  }

  /**
   * Registers all the standard Tinkers tool capabilities (fluid handler, item handler, energy storage and block item provider)
   * for the given items, dispatching to the modular providers registered via {@link #register(BiFunction)}.
   * Call from {@link RegisterCapabilitiesEvent} on the mod bus with all modifiable tool items.
   * @param event  Capability registration event
   * @param items  Modifiable tool items
   */
  public static void registerToolCaps(RegisterCapabilitiesEvent event, Item... items) {
    register(event, Capabilities.FluidHandler.ITEM, items);
    register(event, Capabilities.ItemHandler.ITEM, items);
    register(event, Capabilities.EnergyStorage.ITEM, items);
    register(event, BlockItemProviderCapability.CAPABILITY, items);
  }

  /** Interface to get a capability on a tool */
  @FunctionalInterface
  public interface IToolCapabilityProvider {
    /** Gets a capability on the given tool, or null if this provider does not supply the given capability. */
    @Nullable
    <T> T getCapability(IToolStackView tool, ItemCapability<T, ?> cap);

    /** Called to clear the cache of the provider */
    default void clearCache() {}
  }
}
