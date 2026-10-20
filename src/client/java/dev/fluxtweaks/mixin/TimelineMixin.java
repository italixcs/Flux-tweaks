package dev.fluxtweaks.mixin;

import dev.fluxtweaks.feature.Ambience;
import java.util.function.LongSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.WeatherAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class TimelineMixin {
  @Redirect(
      method = "addDefaultLayers",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/attribute/WeatherAttributes$WeatherAccess;from(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/attribute/WeatherAttributes$WeatherAccess;"))
  private static WeatherAttributes.WeatherAccess flux$weather(Level level) {
    WeatherAttributes.WeatherAccess original = WeatherAttributes.WeatherAccess.from(level);
    if (!(level instanceof ClientLevel)) return original;
    return new WeatherAttributes.WeatherAccess() {
      public float rainLevel() {
        return Ambience.weather()
            ? (Ambience.weatherType().equals("Clear") ? 0 : 1)
            : original.rainLevel();
      }

      public float thunderLevel() {
        return Ambience.weather()
            ? (Ambience.weatherType().equals("Thunder") ? 1 : 0)
            : original.thunderLevel();
      }
    };
  }

  @ModifyVariable(method = "addDefaultLayers", at = @At("STORE"), ordinal = 0)
  private static LongSupplier flux$time(
      LongSupplier original, EnvironmentAttributeSystem.Builder builder, Level level) {
    return level instanceof ClientLevel
        ? () -> Ambience.time() ? Ambience.timeValue() : original.getAsLong()
        : original;
  }
}
