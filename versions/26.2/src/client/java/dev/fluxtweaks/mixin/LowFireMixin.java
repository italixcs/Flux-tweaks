package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class LowFireMixin {
  @Inject(method = "submitFire", at = @At("HEAD"))
  private static void flux$lower(
      PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
    pose.pushPose();
    if (FluxTweaks.active(Feature.LOW_FIRE)) pose.translate(0, -.3f, 0);
  }

  @Inject(method = "submitFire", at = @At("RETURN"))
  private static void flux$restore(
      PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
    pose.popPose();
  }
}
