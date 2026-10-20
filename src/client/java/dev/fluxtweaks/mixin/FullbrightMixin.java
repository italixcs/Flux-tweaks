package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class FullbrightMixin {
  @Shadow private boolean updateLightTexture;
  @Unique private boolean flux$fullbright;

  @Inject(method = "updateLightTexture", at = @At("HEAD"))
  private void flux$refresh(float delta, CallbackInfo ci) {
    boolean enabled = FluxTweaks.active(Feature.FULLBRIGHT);
    if (enabled != flux$fullbright) {
      flux$fullbright = enabled;
      // Refresh immediately on toggles, including frames without a lighting tick.
      updateLightTexture = true;
    }
  }

  @Redirect(
      method = "updateLightTexture",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F"))
  private float flux$ambient(DimensionType dimension) {
    return FluxTweaks.active(Feature.FULLBRIGHT) ? 1.0f : dimension.ambientLight();
  }
}
