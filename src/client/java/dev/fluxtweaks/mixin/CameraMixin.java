package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
  @Inject(method = "setup", at = @At("TAIL"))
  private void flux$camera(
      Level level, Entity entity, boolean third, boolean front, float delta, CallbackInfo ci) {
    if (FluxTweaks.cameras != null) FluxTweaks.cameras.apply((Camera) (Object) this, delta);
  }
}
