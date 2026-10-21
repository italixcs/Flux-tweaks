package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.Zoom;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class ZoomKeyboardMixin {
  @Inject(method = "keyPress", at = @At("HEAD"))
  private void flux$zoomKey(long window, int action, KeyEvent event, CallbackInfo ci) {
    var key = FluxTweaks.keys.get(Feature.ZOOM);
    // Repeat events and Minecraft's restored key state must never bypass the fresh-press gate.
    if (window == Minecraft.getInstance().getWindow().handle()
        && key != null
        && key.matches(event)
        && action != GLFW.GLFW_REPEAT) Zoom.keyEvent(action == GLFW.GLFW_PRESS);
  }
}
