package dev.fluxtweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Inject(method = "renderItemInHand", at = @At("HEAD"))
  private void flux$knifeFrame(CallbackInfo ci) {
    dev.fluxtweaks.feature.ButterflyKnife.beginFrame();
  }

  @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
  private void flux$target(CallbackInfoReturnable<com.mojang.blaze3d.pipeline.RenderTarget> cir) {
    if (dev.fluxtweaks.render.HandChams.captureTarget != null)
      cir.setReturnValue(dev.fluxtweaks.render.HandChams.captureTarget);
  }

  @WrapOperation(
      method = "renderItemInHand",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"))
  private void flux$hands(
      ItemInHandRenderer renderer,
      float dt,
      PoseStack pose,
      SubmitNodeCollector collector,
      LocalPlayer player,
      int light,
      Operation<Void> original) {
    if (FluxTweaks.chams != null)
      FluxTweaks.chams.queue(renderer, dt, pose, collector, player, light, original);
    else original.call(renderer, dt, pose, collector, player, light);
  }

  @Inject(method = "renderItemInHand", at = @At("RETURN"))
  private void flux$finish(CallbackInfo ci) {
    if (FluxTweaks.chams != null) FluxTweaks.chams.finish();
  }
}
