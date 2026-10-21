package dev.fluxtweaks.mixin;

import dev.fluxtweaks.feature.HitColor;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntityRenderer.class)
public abstract class HitColorMixin {
  @Inject(
      method =
          "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL"))
  private void flux$select(
      LivingEntity entity, LivingEntityRenderState state, float delta, CallbackInfo ci) {
    ((HitColor.State) state).flux$hitColor(HitColor.target(entity));
  }

  @Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
  private static void flux$overlay(
      LivingEntityRenderState state, float progress, CallbackInfoReturnable<Integer> cir) {
    if (HitColor.applies(state))
      cir.setReturnValue((cir.getReturnValue() & 0xFFFF) | HitColor.OVERLAY_ROW << 16);
  }
}
