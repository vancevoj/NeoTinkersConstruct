package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

/**
 * Condition checking that a tag is non-empty, usable both as a recipe condition and a loot condition.
 * @deprecated use {@link slimeknights.mantle.recipe.condition.TagFilledCondition}
 */
@Deprecated(forRemoval = true)
public class TagNotEmptyCondition<T> implements LootItemCondition, ICondition {
  public static final ResourceLocation NAME = TConstruct.getResource("tag_not_empty");
  private final TagKey<T> tag;

  public TagNotEmptyCondition(TagKey<T> tag) {
    this.tag = tag;
  }

  public TagNotEmptyCondition(ResourceKey<? extends Registry<T>> registry, ResourceLocation name) {
    this(TagKey.create(registry, name));
  }

  /** Codec for this condition */
  public static final MapCodec<TagNotEmptyCondition<?>> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
    .group(
      // save some space in JSON by not setting registry if item (most common)
      ResourceLocation.CODEC.optionalFieldOf("registry", Registries.ITEM.location()).forGetter(c -> c.tag.registry().location()),
      ResourceLocation.CODEC.fieldOf("tag").forGetter(c -> c.tag.location()))
    .apply(builder, TagNotEmptyCondition::create));

  /** Builds a condition from the codec primitives */
  private static TagNotEmptyCondition<?> create(ResourceLocation registryName, ResourceLocation tag) {
    ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(registryName);
    return new TagNotEmptyCondition<>(TagKey.create(registry, tag));
  }

  @SuppressWarnings("removal")
  @Override
  public LootItemConditionType getType() {
    return TinkerCommons.lootTagNotEmptyCondition.get();
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    return !context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    Registry<T> registry = RegistryHelper.getRegistry(tag.registry());
    return registry != null && registry.getTagOrEmpty(tag).iterator().hasNext();
  }
}
