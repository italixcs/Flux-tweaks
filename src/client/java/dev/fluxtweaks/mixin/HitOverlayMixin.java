package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.HitColor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OverlayTexture.class)
public abstract class HitOverlayMixin {
  @Shadow @Final private DynamicTexture texture;
  @Unique private int flux$lastTint;
  @Unique private boolean flux$uploaded;

  @Inject(method = "getTextureView", at = @At("HEAD"))
  private void flux$update(CallbackInfoReturnable<GpuTextureView> cir) {
    if (!FluxTweaks.active(Feature.HIT_COLOR)) return;
    int pixel = HitColor.overlayPixel();
    if ((!flux$uploaded || pixel != flux$lastTint) && texture.getPixels() != null) {
      for (int x = 0; x < 16; x++) texture.getPixels().setPixel(x, HitColor.OVERLAY_ROW, pixel);
      texture.upload();
      flux$lastTint = pixel;
      flux$uploaded = true;
    }
  }
}
