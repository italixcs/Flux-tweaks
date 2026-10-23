package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
  @Inject(method = "alignWithEntity", at = @At("TAIL"))
  private void flux$camera(float delta, CallbackInfo ci) {
    if (FluxTweaks.cameras != null) FluxTweaks.cameras.apply((Camera) (Object) this, delta);
  }

  @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
  private void flux$zoom(
      float delta,
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
    cir.setReturnValue(dev.fluxtweaks.feature.Zoom.fov(cir.getReturnValue()));
  }
}
