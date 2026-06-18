package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SkullBlock.Type;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;
import java.util.Map;

/** Generics do not match to use the vanilla armor layer, so this is a reimplementation of some of {@link HumanoidArmorLayer} */
public class SlimeArmorLayer<T extends Slime, M extends HierarchicalModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T,M> {
  private final A armorModel;
  public final Map<Type,SkullModelBase> skullModels;
  private final boolean lavaSlime;
  public SlimeArmorLayer(RenderLayerParent<T,M> pRenderer, A armorModel, EntityModelSet modelSet, boolean lavaSlime) {
    super(pRenderer);
    this.armorModel = armorModel;
    this.skullModels = SkullBlockRenderer.createSkullRenderers(modelSet);
    this.lavaSlime = lavaSlime;
  }

  @Override
  public void render(PoseStack matrices, MultiBufferSource buffer, int packedLight, T entity, float pLimbSwing, float swing, float partialTicks, float age, float headYaw, float headPitch) {
    ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
    if (!helmet.isEmpty()) {
      matrices.pushPose();
      if (lavaSlime) {
        float squish = Mth.lerp(partialTicks, entity.oSquish, entity.squish);
        if (squish < 0) {
          squish = 0;
        }
        matrices.translate(0, 1.5 - 0.425 * squish, 0);
      } else {
        matrices.translate(0, 1.5, 0);
      }
      matrices.scale(0.9f, 0.9f, 0.9f);

      Item item = helmet.getItem();
      // helmet renderer, based on humanoid armor layer
      if (item instanceof ArmorItem armor && armor.getType() == ArmorItem.Type.HELMET) {
        this.getParentModel().copyPropertiesTo(armorModel);
        armorModel.setAllVisible(false);
        armorModel.head.visible = true;
        armorModel.hat.visible = true;
        //noinspection UnstableApiUsage  I am reimplementing vanilla stuff, I will call vanilla hooks
        Model model = ClientHooks.getArmorModel(entity, helmet, EquipmentSlot.HEAD, armorModel);
        boolean enchanted = helmet.hasFoil();
        List<ArmorMaterial.Layer> layers = armor.getMaterial().value().layers();
        boolean first = true;
        for (ArmorMaterial.Layer layer : layers) {
          int color;
          // 1.21: dye is a data component; the dyeable layer is the first one
          if (first && helmet.has(DataComponents.DYED_COLOR)) {
            color = DyedItemColor.getOrDefault(helmet, 0xFFFFFFFF) | 0xFF000000;
          } else {
            color = 0xFFFFFFFF;
          }
          renderModel(matrices, buffer, packedLight, enchanted, model, color, getArmorResource(layer));
          first = false;
        }
      } else {
        // block model renderer, based on custom head layer

        // skull block rendering
        if (item instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock skullBlock) {
          matrices.scale(1.1875F, -1.1875F, -1.1875F);
          ResolvableProfile profile = helmet.get(DataComponents.PROFILE);
          matrices.translate(-0.5, 0.0, -0.5);
          SkullBlock.Type type = skullBlock.getType();
          SkullModelBase skullModel = this.skullModels.get(type);
          RenderType renderType = SkullBlockRenderer.getRenderType(type, profile);
          SkullBlockRenderer.renderSkull(null, 180.0F, pLimbSwing, matrices, buffer, packedLight, skullModel, renderType);
        } else {
          // standard rendering
          CustomHeadLayer.translateToHead(matrices, false);
          Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(entity, helmet, ItemDisplayContext.HEAD, false, matrices, buffer, packedLight);
        }
      }
      matrices.popPose();
    }
  }

  private static void renderModel(PoseStack matrices, MultiBufferSource buffer, int packedLight, boolean enchanted, Model model, int color, ResourceLocation texture) {
    VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(texture), enchanted);
    model.renderToBuffer(matrices, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
  }

  /**
   * Gets the armor texture for the given material layer.
   * @param layer Armor material layer
   * @return ResourceLocation pointing at the armor's texture
   */
  public static ResourceLocation getArmorResource(ArmorMaterial.Layer layer) {
    String path = layer.texture(false).toString();
    ResourceLocation location = HumanoidArmorLayer.ARMOR_LOCATION_CACHE.get(path);
    if (location == null) {
      location = ResourceLocation.parse(path);
      HumanoidArmorLayer.ARMOR_LOCATION_CACHE.put(path, location);
    }
    return location;
  }
}
