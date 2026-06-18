package slimeknights.tconstruct.shared.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

import javax.annotation.Nullable;
import java.util.Optional;

/** Criteria that triggers when a container is opened */
public class BlockContainerOpenedTrigger extends SimpleCriterionTrigger<BlockContainerOpenedTrigger.Instance> {
  private static final ResourceLocation ID = TConstruct.getResource("block_container_opened");

  /** Gets the registry ID for this trigger, used during registration */
  public ResourceLocation getId() {
    return ID;
  }

  @Override
  public Codec<Instance> codec() {
    return Instance.CODEC;
  }

  /** Triggers this criteria */
  public void trigger(@Nullable BlockEntity tileEntity, @Nullable Inventory inv) {
    if (tileEntity != null && inv != null && inv.player instanceof ServerPlayer) {
      this.trigger((ServerPlayer)inv.player, instance -> instance.test(tileEntity.getType()));
    }
  }

  /** Instance of this trigger */
  public record Instance(Optional<ContextAwarePredicate> player, BlockEntityType<?> type) implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
      BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(Instance::type)
    ).apply(inst, Instance::new));

    public static Criterion<Instance> container(BlockEntityType<?> type) {
      return TinkerCommons.CONTAINER_OPENED_TRIGGER.createCriterion(new Instance(Optional.empty(), type));
    }

    /** Tests if this instance matches */
    public boolean test(BlockEntityType<?> type) {
      return this.type == type;
    }
  }
}
