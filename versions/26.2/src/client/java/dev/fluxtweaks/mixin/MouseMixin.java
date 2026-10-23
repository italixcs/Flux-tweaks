package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
  @Inject(method = "onButton", at = @At("HEAD"))
  private void flux$zoomButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
    var mc = Minecraft.getInstance();
    var key = FluxTweaks.keys.get(Feature.ZOOM);
    if (window == mc.getWindow().handle()
        && key != null
        && key.matchesMouse(new MouseButtonEvent(0, 0, info)))
      Zoom.keyEvent(action == GLFW.GLFW_PRESS);
  }

  @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
  private void flux$scroll(long window, double x, double y, CallbackInfo ci) {
    var mc = Minecraft.getInstance();
    if (window == mc.getWindow().handle()) {
      double amount = y == 0 ? -x : y;
      if (mc.options.discreteMouseScroll().get()) amount = Math.signum(amount);
      if (Zoom.scroll(amount * mc.options.mouseWheelSensitivity().get())) {
        mc.getFramerateLimitTracker().onInputReceived();
        ci.cancel();
      }
    }
  }

  @Redirect(
      method = "turnPlayer",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
  private void flux$turn(LocalPlayer player, double dx, double dy) {
    double sensitivity = Zoom.sensitivity();
    dx *= sensitivity;
    dy *= sensitivity;
    if (FluxTweaks.cameras != null && FluxTweaks.cameras.active()) FluxTweaks.cameras.turn(dx, dy);
    else player.turn(dx, dy);
  }
}
