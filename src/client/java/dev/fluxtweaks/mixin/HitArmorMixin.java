package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.feature.HitColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EquipmentLayerRenderer.class)
public abstract class HitArmorMixin {
  private static final String LAYERS =
      "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

  @Redirect(
      method = LAYERS,
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
  private RenderType flux$armor(
      Identifier texture,
      EquipmentClientInfo.LayerType type,
      ResourceKey<EquipmentAsset> asset,
      Model<?> model,
      Object state,
      ItemStack stack,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      Identifier playerTexture,
      int outline,
      int order) {
    return HitColor.armor(type, state)
        ? HitColor.armorType(texture, false)
        : RenderTypes.armorCutoutNoCull(texture);
  }

  @Redirect(
      method = LAYERS,
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
  private RenderType flux$trim(
      boolean decal,
      EquipmentClientInfo.LayerType type,
      ResourceKey<EquipmentAsset> asset,
      Model<?> model,
      Object state,
      ItemStack stack,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      Identifier playerTexture,
      int outline,
      int order) {
    return HitColor.armor(type, state)
        ? HitColor.armorType(Sheets.ARMOR_TRIMS_SHEET, decal)
        : Sheets.armorTrimsSheet(decal);
  }

  @ModifyArgs(
      method = LAYERS,
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
  private void flux$coords(Args args) {
    if (HitColor.customArmor(args.get(3))) args.set(5, HitColor.OVERLAY_ROW << 16);
  }
}
