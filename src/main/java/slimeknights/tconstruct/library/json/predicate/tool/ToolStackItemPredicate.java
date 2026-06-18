package slimeknights.tconstruct.library.json.predicate.tool;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags.Items;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Variant of {@link ItemSubPredicate} for matching Tinker tools using a {@link ToolStackPredicate}.
 * <p>
 * In 1.20 this extended the (then non-final) {@code ItemPredicate} and (de)serialized itself via the
 * legacy Forge item-predicate registry. In 1.21 {@code ItemPredicate} is a final record composed of
 * {@link ItemSubPredicate}s, so we implement {@link ItemSubPredicate} instead.
 */
@RequiredArgsConstructor(staticName = "ofTool")
public class ToolStackItemPredicate implements ItemSubPredicate {
  public static final ResourceLocation ID = TConstruct.getResource("tool_stack");

  // TODO(neoport): cross-package - register an ItemSubPredicate.Type<ToolStackItemPredicate> (BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE)
  //  from TinkerTools and have AdvancementsProvider wrap instances via ItemPredicate.Builder#withSubPredicate. The codec for the Type
  //  needs a Codec bridge for the mantle ToolStackPredicate loadable (the mantle predicate registries are JSON-loadable, not Codec based),
  //  which is owned by the mantle/registration agent. The matching logic below is final and behavior-preserving.

  private final IJsonPredicate<IToolStackView> predicate;

  /** Gets the wrapped tool predicate */
  public IJsonPredicate<IToolStackView> predicate() {
    return predicate;
  }

  public static ToolStackItemPredicate ofContext(IJsonPredicate<IToolContext> predicate) {
    return new ToolStackItemPredicate(ToolStackPredicate.context(predicate));
  }

  @Override
  public boolean matches(ItemStack stack) {
    // tag check is important to prevent accidently modifying the NBT of non-tools
    return stack.is(Items.MODIFIABLE) && predicate.matches(ToolStack.from(stack));
  }
}
