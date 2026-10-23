package dev.fluxtweaks.test;

import java.util.ArrayList;
import java.util.List;

public final class RenderCaptures {
  public static boolean enabled;
  public static final List<Entry> entries = new ArrayList<>();

  public record Entry(
      String pipeline, int overlay, int tint, boolean trim, boolean hurt, boolean selected) {}

  static void start() {
    entries.clear();
    enabled = true;
  }
}
