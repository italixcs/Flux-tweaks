package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.Ambience;
import net.minecraft.client.renderer.fog.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
  @Inject(method = "setupFog", at = @At("RETURN"))
  private void flux$fog(CallbackInfoReturnable<FogData> cir) {
    String mode = Ambience.fog();
    if (mode.equals("Vanilla")) return;
    FogData fog = cir.getReturnValue();
    fog.environmentalStart =
        fog.environmentalEnd =
            fog.renderDistanceStart =
                fog.renderDistanceEnd = fog.skyEnd = fog.cloudEnd = Float.MAX_VALUE;
    if (mode.equals("Custom")) {
      double[] range = FluxTweaks.config.range(Feature.AMBIENCE, "fogRange");
      fog.environmentalStart = (float) range[0];
      fog.environmentalEnd = (float) Math.max(range[0] + .1, range[1]);
      fog.color.set(Ambience.colorVector());
    }
  }
}
