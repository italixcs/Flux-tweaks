package dev.fluxtweaks.mixin;

import dev.fluxtweaks.feature.Ambience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeProbe.class)
public abstract class SkyMixin {
  @Shadow Level level;

  @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
  @SuppressWarnings("unchecked") // The attribute identity check fixes T to Integer.
  private <T> void flux$sky(
      EnvironmentAttribute<T> attribute, float dt, CallbackInfoReturnable<T> cir) {
    if (level instanceof ClientLevel
        && attribute == EnvironmentAttributes.SKY_COLOR
        && Ambience.sky()) cir.setReturnValue((T) Integer.valueOf(Ambience.fogColor()));
  }
}
