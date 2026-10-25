package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class InteractionMixin {
  @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
  private void flux$attack(CallbackInfoReturnable<Boolean> cir) {
    dev.fluxtweaks.feature.ButterflyKnife.cancel();
    if (FluxTweaks.active(Feature.FREECAM)) cir.setReturnValue(false);
  }

  @Inject(
      method = {"startUseItem", "pickBlockOrEntity"},
      at = @At("HEAD"),
      cancellable = true)
  private void flux$interact(CallbackInfo ci) {
    dev.fluxtweaks.feature.ButterflyKnife.cancel();
    if (FluxTweaks.active(Feature.FREECAM)) ci.cancel();
  }

  @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
  private void flux$breaking(boolean down, CallbackInfo ci) {
    if (down) dev.fluxtweaks.feature.ButterflyKnife.cancel();
    if (FluxTweaks.active(Feature.FREECAM)) ci.cancel();
  }
}
