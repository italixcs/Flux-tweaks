package dev.fluxtweaks.test.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface RendererProbe {
  @Invoker("getFov")
  float flux$fov(Camera camera, float delta, boolean world);
}
