package dev.fluxtweaks.mixin;

import dev.fluxtweaks.feature.Ambience;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherMixin {
  @Redirect(
      method = "extractRenderState",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
  private float flux$rain(ClientLevel level, float dt) {
    return Ambience.weather()
        ? (Ambience.weatherType().equals("Clear") ? 0 : 1)
        : level.getRainLevel(dt);
  }

  @Redirect(
      method = "extractRenderState",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
  private Biome.Precipitation flux$type(ClientLevel level, BlockPos pos) {
    if (Ambience.weather())
      return Ambience.weatherType().equals("Clear")
          ? Biome.Precipitation.NONE
          : Ambience.weatherType().equals("Snow")
              ? Biome.Precipitation.SNOW
              : Biome.Precipitation.RAIN;
    return level.getPrecipitationAt(pos);
  }
}
