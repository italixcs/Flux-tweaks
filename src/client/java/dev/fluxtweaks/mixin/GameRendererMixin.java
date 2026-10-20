package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Redirect(
      method = "renderItemInHand",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"))
  private void flux$hands(
      ItemInHandRenderer renderer,
      float dt,
      PoseStack pose,
      SubmitNodeCollector collector,
      LocalPlayer player,
      int light) {
    if (FluxTweaks.chams != null)
      FluxTweaks.chams.queue(renderer, dt, pose, collector, player, light);
    else renderer.renderHandsWithItems(dt, pose, collector, player, light);
  }

  @Inject(method = "renderItemInHand", at = @At("RETURN"))
  private void flux$finish(CallbackInfo ci) {
    if (FluxTweaks.chams != null) FluxTweaks.chams.finish();
  }
}
