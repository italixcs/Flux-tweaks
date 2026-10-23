package dev.fluxtweaks.gui;

/** One transform for panels, modal editors, clipping and input. */
public final class GuiLayout {
  public static double preference(double value) {
    return Double.isFinite(value) ? Math.clamp(value, .5, 1.5) : 1;
  }

  public static float scale(int width, int height, double preference) {
    float fit = Math.min(1f, Math.min(width / 560f, height / 560f));
    return (float) Math.min(fit * preference(preference), Math.min(width / 560f, height / 360f));
  }

  private GuiLayout() {}
}
