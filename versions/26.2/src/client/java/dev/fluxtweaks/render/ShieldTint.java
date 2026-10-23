package dev.fluxtweaks.render;

import java.util.ArrayDeque;

public final class ShieldTint {
  public interface State {
    void flux$tint(int color);
  }

  private static final ArrayDeque<Integer> COLORS = new ArrayDeque<>();

  public static void push(int color) {
    COLORS.push(color);
  }

  public static void pop() {
    if (!COLORS.isEmpty()) COLORS.pop();
  }

  public static int apply(int original) {
    if (COLORS.isEmpty() || COLORS.peek() == -1) return original;
    int tint = COLORS.peek();
    return (original & 0xFF000000)
        | ((((original >> 16) & 255) * ((tint >> 16) & 255) / 255) << 16)
        | ((((original >> 8) & 255) * ((tint >> 8) & 255) / 255) << 8)
        | ((original & 255) * (tint & 255) / 255);
  }

  private ShieldTint() {}
}
