package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.recipe.helper.TagPreference;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.List;
import java.util.function.Consumer;

/** @deprecated use {@link slimeknights.mantle.loot.entry.TagPreferenceLootEntry} */
@Deprecated(forRemoval = true)
public class TagPreferenceLootEntry extends LootPoolSingletonContainer {
  /** Codec for this entry */
  public static final MapCodec<TagPreferenceLootEntry> CODEC = RecordCodecBuilder.mapCodec(inst ->
    inst.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(e -> e.tag))
        .and(singletonFields(inst))
        .apply(inst, TagPreferenceLootEntry::new));

  private final TagKey<Item> tag;
  protected TagPreferenceLootEntry(TagKey<Item> tag, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
    super(weight, quality, conditions, functions);
    this.tag = tag;
  }

  @SuppressWarnings("removal")
  @Override
  public LootPoolEntryType getType() {
    return TinkerCommons.lootTagPreference.get();
  }

  @Override
  protected void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
    TagPreference.getPreference(tag).ifPresent(item -> consumer.accept(new ItemStack(item)));
  }

  /** @deprecated use {@link slimeknights.mantle.loot.entry.TagPreferenceLootEntry#tagPreference(TagKey)} */
  @Deprecated(forRemoval = true)
  public static LootPoolSingletonContainer.Builder<?> tagPreference(TagKey<Item> tag) {
    return slimeknights.mantle.loot.entry.TagPreferenceLootEntry.tagPreference(tag);
  }
}
