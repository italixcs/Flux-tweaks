package dev.fluxtweaks.config;

import java.time.LocalTime;

public final class VisualMath {
  public static long time(String preset, long custom, LocalTime clock) {
    return switch (preset) {
      case "Dawn" -> 0;
      case "Morning" -> 1000;
      case "Day" -> 6000;
      case "Evening" -> 10000;
      case "Sunset" -> 12000;
      case "Night" -> 18000;
      case "Real Time" ->
          Math.floorMod(Math.floorDiv((clock.toSecondOfDay() - 21600) * 5L, 18), 24000);
      default -> Math.floorMod(custom, 24000);
    };
  }

  public static String shield(
      boolean disabled, boolean using, double durability, double threshold) {
    return disabled ? "disabled" : durability <= threshold ? "low" : using ? "using" : "ready";
  }

  public static int blend(int a, int b, float t) {
    t = Math.clamp(t, 0, 1);
    return 0xFF000000
        | ((int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t) << 16)
        | ((int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t) << 8)
        | (int) ((a & 255) * (1 - t) + (b & 255) * t);
  }

  private VisualMath() {}
}
