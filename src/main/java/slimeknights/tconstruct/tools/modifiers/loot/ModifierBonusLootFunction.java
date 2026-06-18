package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.BinomialWithBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.Formula;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.OreDrops;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.UniformBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;
import java.util.Set;

/** Boosts drop rates based on modifier level */
public class ModifierBonusLootFunction extends LootItemConditionalFunction {
  /** Codec for the modifier ID field, stored as a resource location to match the legacy JSON */
  private static final Codec<ModifierId> MODIFIER_CODEC = ResourceLocation.CODEC.xmap(ModifierId::new, id -> id);

  /** Codec for this function */
  public static final MapCodec<ModifierBonusLootFunction> CODEC = RecordCodecBuilder.mapCodec(inst -> commonFields(inst).and(inst.group(
    MODIFIER_CODEC.fieldOf("modifier").forGetter(f -> f.modifier),
    ChrysophiliteBonusFunction.FORMULA_CODEC.forGetter(f -> f.formula),
    Codec.BOOL.optionalFieldOf("include_base", true).forGetter(f -> f.includeBase)
  )).apply(inst, ModifierBonusLootFunction::new));

  /** Modifier ID to use for multiplier bonus */
  private final ModifierId modifier;
  /** Formula to apply */
  private final Formula formula;
  /** If true, considers level 1 as bonus, if false considers level 1 as no bonus */
  private final boolean includeBase;

  protected ModifierBonusLootFunction(List<LootItemCondition> conditions, ModifierId modifier, Formula formula, boolean includeBase) {
    super(conditions);
    this.modifier = modifier;
    this.formula = formula;
    this.includeBase = includeBase;
  }

  /** Creates a generic builder */
  public static Builder<?> builder(ModifierId modifier, Formula formula, boolean includeBase) {
    return simpleBuilder(conditions -> new ModifierBonusLootFunction(conditions, modifier, formula, includeBase));
  }

  /** Creates a builder for the binomial with bonus formula */
  public static Builder<?> binomialWithBonusCount(ModifierId modifier, float probability, int extra, boolean includeBase) {
    return builder(modifier, new BinomialWithBonusCount(extra, probability), includeBase);
  }

  /** Creates a builder for the ore drops formula */
  public static Builder<?> oreDrops(ModifierId modifier, boolean includeBase) {
    return builder(modifier, new OreDrops(), includeBase);
  }

  /** Creates a builder for the uniform bonus count */
  public static Builder<?> uniformBonusCount(ModifierId modifier, int bonusMultiplier, boolean includeBase) {
    return builder(modifier, new UniformBonusCount(bonusMultiplier), includeBase);
  }

  @Override
  public LootItemFunctionType<ModifierBonusLootFunction> getType() {
    return TinkerModifiers.modifierBonusFunction.get();
  }

  @Override
  public Set<LootContextParam<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.TOOL);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    int level = ModifierUtil.getModifierLevel(context.getParam(LootContextParams.TOOL), modifier);
    if (!includeBase) {
      level--;
    }
    if (level > 0) {
      stack.setCount(formula.calculateNewCount(context.getRandom(), stack.getCount(), level));
    }
    return stack;
  }
}
