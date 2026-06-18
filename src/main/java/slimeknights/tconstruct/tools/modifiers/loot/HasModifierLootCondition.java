package slimeknights.tconstruct.tools.modifiers.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

/** Condition to check if a held tool has the given modifier */
public record HasModifierLootCondition(ModifierId modifier) implements LootItemCondition {
  /** Codec for this condition */
  public static final MapCodec<HasModifierLootCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    ResourceLocation.CODEC.xmap(ModifierId::new, id -> id).fieldOf("modifier").forGetter(HasModifierLootCondition::modifier)
  ).apply(inst, HasModifierLootCondition::new));

  @Override
  public LootItemConditionType getType() {
    return TinkerModifiers.hasModifierLootCondition.get();
  }

  @Override
  public boolean test(LootContext context) {
    ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
    return tool != null && tool.is(TinkerTags.Items.MODIFIABLE) && ModifierUtil.getModifierLevel(tool, modifier) > 0;
  }
}
