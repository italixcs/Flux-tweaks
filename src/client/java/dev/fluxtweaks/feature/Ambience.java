package dev.fluxtweaks.feature;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import java.time.LocalTime;
import org.joml.Vector4f;

public final class Ambience {
  public static boolean time() {
    return FluxTweaks.active(Feature.AMBIENCE) && FluxTweaks.config.bool(Feature.AMBIENCE, "time");
  }

  public static long timeValue() {
    var c = FluxTweaks.config;
    return VisualMath.time(
        c.text(Feature.AMBIENCE, "timeMode").equals("Custom")
            ? "Custom"
            : c.text(Feature.AMBIENCE, "preset"),
        (long) c.number(Feature.AMBIENCE, "timeValue"),
        LocalTime.now());
  }

  public static boolean weather() {
    return FluxTweaks.active(Feature.AMBIENCE)
        && FluxTweaks.config.bool(Feature.AMBIENCE, "weather");
  }

  public static String weatherType() {
    return FluxTweaks.config.text(Feature.AMBIENCE, "weatherType");
  }

  public static String fog() {
    return FluxTweaks.active(Feature.AMBIENCE)
        ? FluxTweaks.config.text(Feature.AMBIENCE, "fog")
        : "Vanilla";
  }

  public static boolean sky() {
    return fog().equals("Custom") && FluxTweaks.config.bool(Feature.AMBIENCE, "sky");
  }

  public static int fogColor() {
    return FluxTweaks.config.color(Feature.AMBIENCE, "fogColor");
  }

  public static Vector4f colorVector() {
    int rgb = fogColor();
    return new Vector4f(
        ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
  }

  private Ambience() {}
}
