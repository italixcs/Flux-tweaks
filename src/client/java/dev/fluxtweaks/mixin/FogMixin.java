package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.Ambience;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
  @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
  private void flux$color(CallbackInfoReturnable<Vector4f> cir) {
    if (Ambience.fog().equals("Custom")) cir.setReturnValue(Ambience.colorVector());
  }

  @ModifyArgs(
      method = "setupFog",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
  private void flux$range(Args args) {
    String mode = Ambience.fog();
    if (mode.equals("Vanilla")) return;
    for (int i = 3; i <= 8; i++) args.set(i, Float.MAX_VALUE);
    if (mode.equals("Custom")) {
      double[] range = FluxTweaks.config.range(Feature.AMBIENCE, "fogRange");
      args.set(3, (float) range[0]);
      args.set(4, (float) Math.max(range[0] + .1, range[1]));
    }
  }
}
