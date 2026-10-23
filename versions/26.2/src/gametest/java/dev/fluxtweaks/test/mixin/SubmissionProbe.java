package dev.fluxtweaks.test.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.feature.HitColor;
import dev.fluxtweaks.test.RenderCaptures;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmissionProbe {
  @Inject(method = "submitModel", at = @At("HEAD"))
  private void flux$capture(
      Model<?> model,
      Object state,
      PoseStack pose,
      RenderType type,
      int light,
      int overlay,
      int tint,
      TextureAtlasSprite sprite,
      int outline,
      ModelFeatureRenderer.CrumblingOverlay crumbling,
      CallbackInfo ci) {
    if (RenderCaptures.enabled && RenderCaptures.entries.size() < 10000)
      RenderCaptures.entries.add(
          new RenderCaptures.Entry(
              type.pipeline().getLocation().toString(),
              overlay,
              tint,
              sprite != null,
              state instanceof LivingEntityRenderState living && living.hasRedOverlay,
              state instanceof HitColor.State selected && selected.flux$hitColor()));
  }
}
