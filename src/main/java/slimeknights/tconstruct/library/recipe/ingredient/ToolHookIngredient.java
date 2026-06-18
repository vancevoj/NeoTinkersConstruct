package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/** Ingredient that only matches tools with a specific hook */
public class ToolHookIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("tool_hook");
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<ToolHookIngredient> LOADABLE = RecordLoadable.create(
    Loadables.ITEM_TAG.defaultField("tag", TinkerTags.Items.MODIFIABLE, i -> i.tag),
    ToolHooks.LOADER.requiredField("hook", i -> i.hook),
    ToolHookIngredient::new);
  /** Ingredient type instance */
  // TODO(neoport): register this IngredientType on NeoForgeRegistries.Keys.INGREDIENT_TYPES under id tconstruct:tool_hook
  public static final IngredientType<ToolHookIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final TagKey<Item> tag;
  private final ModuleHook<?> hook;

  protected ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
    this.tag = tag;
    this.hook = hook;
  }

  public static ToolHookIngredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook);
  }

  public static ToolHookIngredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && stack.is(tag) && stack.getItem() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    Stream.Builder<ItemStack> builder = Stream.builder();
    // filtered version of tag values
    boolean empty = true;
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (holder.value() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook)) {
        builder.add(new ItemStack(modifiable));
        empty = false;
      }
    }
    if (empty) {
      ItemStack barrier = new ItemStack(Blocks.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      builder.add(barrier);
    }
    return builder.build();
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }
}
