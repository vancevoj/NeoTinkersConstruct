package slimeknights.tconstruct.common.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TConstructLootTableProvider extends LootTableProvider {
  private static final Set<ResourceKey<LootTable>> REQUIRED_TABLES = Set.of();

  public TConstructLootTableProvider(PackOutput packOutput) {
    super(packOutput, REQUIRED_TABLES, List.of(
      new LootTableProvider.SubProviderEntry(BlockLootTableProvider::new, LootContextParamSets.BLOCK),
      new LootTableProvider.SubProviderEntry(AdvancementLootTableProvider::new, LootContextParamSets.ADVANCEMENT_REWARD),
      new LootTableProvider.SubProviderEntry(EntityLootTableProvider::new, LootContextParamSets.ENTITY)),
      CompletableFuture.completedFuture(VanillaRegistries.createLookup()));
  }

  /*
  @Override
  protected void validate(Map<ResourceLocation,LootTable> map, ValidationContext validationtracker) {
    map.forEach((loc, table) -> LootTables.validate(validationtracker, loc, table));
    // Remove vanilla's tables, which we also loaded so we can redirect stuff to them.
    // This ensures the remaining generator logic doesn't write those to files.
    map.keySet().removeIf((loc) -> !loc.getNamespace().equals(TConstruct.MOD_ID));
  }
   */

  /*
   * Gets a name for this provider, to use in logging.
   *
  @Override
  public String getName() {
    return "TConstruct LootTables";
  }*/
}
