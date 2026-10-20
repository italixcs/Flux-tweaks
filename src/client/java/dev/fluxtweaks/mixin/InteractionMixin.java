package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.render.HandChams;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class InteractionMixin {
  @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
  private void flux$attack(CallbackInfoReturnable<Boolean> cir) {
    if (FluxTweaks.active(Feature.FREECAM)) cir.setReturnValue(false);
  }

  @Inject(
      method = {"startUseItem", "pickBlock"},
      at = @At("HEAD"),
      cancellable = true)
  private void flux$interact(CallbackInfo ci) {
    if (FluxTweaks.active(Feature.FREECAM)) ci.cancel();
  }

  @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
  private void flux$breaking(boolean down, CallbackInfo ci) {
    if (FluxTweaks.active(Feature.FREECAM)) ci.cancel();
  }

  @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
  private void flux$target(CallbackInfoReturnable<RenderTarget> cir) {
    if (HandChams.captureTarget != null) cir.setReturnValue(HandChams.captureTarget);
  }
}
