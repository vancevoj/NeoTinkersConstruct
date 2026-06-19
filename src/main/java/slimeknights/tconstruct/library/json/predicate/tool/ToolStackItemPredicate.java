package slimeknights.tconstruct.library.json.predicate.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.JsonCodec;
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

  /**
   * Codec bridging this predicate to/from JSON via the mantle {@link ToolStackPredicate#LOADER} (a JSON loadable, not a Codec).
   * Serializes as an object with a single {@code predicate} field, matching the legacy 1.20 JSON shape.
   */
  public static final Codec<ToolStackItemPredicate> CODEC = new JsonCodec<ToolStackItemPredicate>() {
    @Override
    public ToolStackItemPredicate deserialize(JsonElement element, DynamicOps<?> ops) {
      JsonObject json = element.getAsJsonObject();
      return new ToolStackItemPredicate(ToolStackPredicate.LOADER.getIfPresent(json, "predicate"));
    }

    @Override
    public JsonElement serialize(ToolStackItemPredicate object, DynamicOps<?> ops) {
      JsonObject json = new JsonObject();
      json.add("predicate", ToolStackPredicate.LOADER.serialize(object.predicate));
      return json;
    }

    @Override
    public String codecError() {
      return "Tinkers Tool Stack Item Predicate";
    }
  };

  /** Registered sub-predicate type; the instance here must be the one put into {@code BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE} (see TinkerTools). */
  public static final ItemSubPredicate.Type<ToolStackItemPredicate> TYPE = new ItemSubPredicate.Type<>(CODEC);

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
