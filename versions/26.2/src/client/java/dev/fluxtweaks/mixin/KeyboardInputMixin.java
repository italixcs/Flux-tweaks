package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.player.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
  @Inject(method = "tick", at = @At("TAIL"))
  private void flux$input(CallbackInfo ci) {
    if (FluxTweaks.suppressMovement()) {
      keyPresses = Input.EMPTY;
      moveVector = Vec2.ZERO;
    }
  }
}
