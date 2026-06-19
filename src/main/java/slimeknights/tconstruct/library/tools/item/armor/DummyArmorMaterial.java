package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Armor material that returns 0 for all stats except the equip sound and name, since Tinkers bypasses all the vanilla
 * armor stat usages and computes them dynamically from the tool.
 * <p>
 * In 1.21 {@link ArmorMaterial} is a final record and can no longer be implemented, so this class instead acts as an
 * identity holder ({@link #getId()}/{@link #getName()}) that produces a zeroed {@link ArmorMaterial} via
 * {@link #getArmorMaterial()} for the {@link ArmorItem} super constructor.
 */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  /** Map of zeroed defense values, used by the dummy material */
  private static final Map<ArmorItem.Type,Integer> NO_DEFENSE;
  static {
    NO_DEFENSE = new EnumMap<>(ArmorItem.Type.class);
    for (ArmorItem.Type type : ArmorItem.Type.values()) {
      NO_DEFENSE.put(type, 0);
    }
  }

  private final ResourceLocation id;
  private final SoundEvent equipSound;
  /** Holder wrapping a zeroed armor material, lazily passed to the {@link ArmorItem} super constructor */
  private final Holder<ArmorMaterial> armorMaterial;

  public DummyArmorMaterial(ResourceLocation id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
    // 1.21: NeoForge only invokes the custom armor model (getGenericArmorModel) INSIDE the per-layer
    // loop in HumanoidArmorLayer.renderArmorPiece. With an empty layer list the loop never runs and
    // all Tinkers armor renders invisible, so declare a single dummy layer. Its texture is never
    // drawn (MultilayerArmorModel renders to its own buffer), it only makes the loop iterate once.
    this.armorMaterial = Holder.direct(new ArmorMaterial(NO_DEFENSE, 0, Holder.direct(equipSound), () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(id)), 0, 0));
  }

  /** Gets the name of this material, matching the ID. Used as the registry key by Tinkers' display logic. */
  public String getName() {
    return id.toString();
  }
}
