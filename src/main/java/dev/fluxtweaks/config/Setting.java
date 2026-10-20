package dev.fluxtweaks.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

public record Setting(
    String key,
    String label,
    Kind kind,
    Object initial,
    double min,
    double max,
    double step,
    String unit,
    List<String> choices) {
  public enum Kind {
    BOOLEAN,
    NUMBER,
    CHOICE,
    COLOR,
    RANGE,
    HEADING,
    ACTION
  }

  public static Setting bool(String key, String label, boolean value) {
    return new Setting(key, label, Kind.BOOLEAN, value, 0, 1, 1, "", List.of());
  }

  public static Setting number(
      String key, String label, double value, double min, double max, double step, String unit) {
    return new Setting(key, label, Kind.NUMBER, value, min, max, step, unit, List.of());
  }

  public static Setting choice(String key, String label, String value, String... choices) {
    return new Setting(key, label, Kind.CHOICE, value, 0, 0, 0, "", List.of(choices));
  }

  public static Setting color(String key, String label, int rgb) {
    return new Setting(key, label, Kind.COLOR, rgb, 0, 0, 0, "", List.of());
  }

  public static Setting range(
      String key, String label, double lo, double hi, double min, double max, String unit) {
    return new Setting(key, label, Kind.RANGE, new double[] {lo, hi}, min, max, 1, unit, List.of());
  }

  public static Setting heading(String label) {
    return new Setting("", label, Kind.HEADING, "", 0, 0, 0, "", List.of());
  }

  public static Setting action(String key, String label) {
    return new Setting(key, label, Kind.ACTION, "", 0, 0, 0, "", List.of());
  }

  public JsonElement defaultValue() {
    if (initial instanceof Boolean b) return new JsonPrimitive(b);
    if (initial instanceof Number n) return new JsonPrimitive(n);
    if (initial instanceof double[] r) {
      var a = new JsonArray();
      a.add(r[0]);
      a.add(r[1]);
      return a;
    }
    return new JsonPrimitive(initial.toString());
  }

  public JsonElement validate(JsonElement input) {
    try {
      if (input == null || input.isJsonNull()) return defaultValue();
      return switch (kind) {
        case BOOLEAN ->
            input.isJsonPrimitive() && input.getAsJsonPrimitive().isBoolean()
                ? input
                : defaultValue();
        case NUMBER -> new JsonPrimitive(bounded(input.getAsDouble()));
        case COLOR -> new JsonPrimitive(input.getAsInt() & 0xFFFFFF);
        case CHOICE -> choices.contains(input.getAsString()) ? input : defaultValue();
        case RANGE -> {
          var a = input.getAsJsonArray();
          if (a.size() != 2) yield defaultValue();
          double x = bounded(a.get(0).getAsDouble()), y = bounded(a.get(1).getAsDouble());
          var result = new JsonArray();
          result.add(Math.min(x, y));
          result.add(Math.max(x, y));
          yield result;
        }
        default -> defaultValue();
      };
    } catch (RuntimeException ex) {
      return defaultValue();
    }
  }

  public double bounded(double value) {
    return Double.isFinite(value)
        ? Math.clamp(Math.round(Math.clamp(value, min, max) / step) * step, min, max)
        : ((Number) (initial instanceof Number ? initial : min)).doubleValue();
  }
}
