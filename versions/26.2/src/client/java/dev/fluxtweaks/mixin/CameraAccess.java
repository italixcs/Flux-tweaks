package dev.fluxtweaks.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(Camera.class)
public interface CameraAccess {
  @Invoker("setRotation")
  void flux$rotation(float yaw, float pitch);

  @Invoker("setPosition")
  void flux$position(Vec3 position);

  @Invoker("getMaxZoom")
  float flux$zoom(float distance);

  @Invoker("move")
  void flux$move(float forward, float up, float side);

  @Accessor("detached")
  void flux$detached(boolean value);
}
