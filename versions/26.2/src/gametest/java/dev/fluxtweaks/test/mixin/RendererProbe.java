package dev.fluxtweaks.test.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface RendererProbe {
  @Invoker("calculateFov")
  float flux$fov(float delta);

  @Invoker("calculateHudFov")
  float flux$hudFov(float delta);
}
