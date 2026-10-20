package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
  @Redirect(
      method = "turnPlayer",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
  private void flux$turn(LocalPlayer player, double dx, double dy) {
    if (FluxTweaks.cameras != null && FluxTweaks.cameras.active()) FluxTweaks.cameras.turn(dx, dy);
    else player.turn(dx, dy);
  }
}
