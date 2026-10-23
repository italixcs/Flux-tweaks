package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class FullbrightMixin {
  @Shadow private boolean needsUpdate;
  @Unique private boolean flux$enabled;

  @Inject(method = "extract", at = @At("HEAD"))
  private void flux$refresh(LightmapRenderState state, float delta, CallbackInfo ci) {
    boolean on = FluxTweaks.active(Feature.FULLBRIGHT);
    if (on != flux$enabled) {
      needsUpdate = true;
      flux$enabled = on;
    }
  }

  @Inject(method = "extract", at = @At("RETURN"))
  private void flux$bright(LightmapRenderState state, float delta, CallbackInfo ci) {
    if (FluxTweaks.active(Feature.FULLBRIGHT)) {
      state.needsUpdate = true;
      state.ambientColor = LightmapRenderStateExtractor.WHITE;
      state.nightVisionEffectIntensity = 1;
      state.darknessEffectScale = 0;
    }
  }
}
