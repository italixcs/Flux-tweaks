package dev.fluxtweaks.gui;

import dev.fluxtweaks.config.Feature;
import net.minecraft.client.gui.GuiGraphics;

/** Small outline glyphs rendered with the shared antialiased primitives. */
public final class ModuleIcons {
  public static void draw(GuiGraphics g, Feature feature, float x, float y, float size, int color) {
    g.pose().pushMatrix();
    g.pose().translate(x, y);
    g.pose().scale(size / 16, size / 16);
    switch (feature) {
      case BUTTERFLY_KNIFE -> {
        path(g, color, 8, 8, 9, 2, 12, 0, 11, 6, 8, 8);
        path(g, color, 8, 8, 4, 15, 2, 14, 6, 7);
        path(g, color, 8, 8, 13, 14, 15, 12, 10, 6);
        Ui.circle(g, 7, 6.5f, 2.5f, color);
      }
      case HAND_CHAMS -> {
        path(
            g, color, 4, 14, 4, 11, 2, 8, 3, 6, 5, 8, 5, 3, 7, 2, 9, 3, 9, 7, 11, 5, 13, 6, 13, 10,
            11, 14, 4, 14);
        line(g, color, 5, 11, 11, 11);
      }
      case VIEW_MODELS -> {
        line(g, color, 2, 3, 14, 3);
        line(g, color, 2, 8, 14, 8);
        line(g, color, 2, 13, 14, 13);
        Ui.circle(g, 4, 1.5f, 3, color);
        Ui.circle(g, 10, 6.5f, 3, color);
        Ui.circle(g, 6, 11.5f, 3, color);
      }
      case SPOTIFY -> {
        path(g, color, 6, 12, 6, 3, 13, 2, 13, 11);
        Ui.circle(g, 2.5f, 10.5f, 4, color);
        Ui.circle(g, 9.5f, 9.5f, 4, color);
      }
      case HIT_PARTICLES -> {
        path(g, color, 8, 1, 10, 6, 15, 8, 10, 10, 8, 15, 6, 10, 1, 8, 6, 6, 8, 1);
      }
      case JUMP_CIRCLE -> {
        g.pose().pushMatrix();
        g.pose().translate(0, 8);
        g.pose().scale(1, .4f);
        Ui.outline(g, 1, 1, 14, 14, 7, color);
        g.pose().popMatrix();
        path(g, color, 5, 5, 8, 2, 11, 5);
        line(g, color, 8, 2, 8, 10);
      }
      case AMBIENCE -> {
        Ui.outline(g, 2, 1, 8, 8, 4, color);
        Ui.rounded(g, 4, 7, 10, 6, 3, color);
        Ui.circle(g, 6, 5, 6, color);
      }
      case SHIELD_STATUS -> {
        path(g, color, 8, 1, 14, 3, 13, 10, 8, 15, 3, 10, 2, 3, 8, 1);
        path(g, color, 5, 7, 7, 9, 11, 5);
      }
      case FREELOOK -> {
        path(g, color, 1, 8, 4, 4, 8, 3, 12, 4, 15, 8, 12, 12, 8, 13, 4, 12, 1, 8);
        Ui.outline(g, 5, 5, 6, 6, 3, color);
      }
      case FREECAM -> {
        Ui.outline(g, 1, 5, 10, 8, 2, color);
        path(g, color, 11, 7, 15, 5, 15, 13, 11, 11);
        path(g, color, 3, 3, 6, 1, 9, 3);
      }
      case FULLBRIGHT -> {
        Ui.outline(g, 4, 4, 8, 8, 4, color);
        line(g, color, 8, 0, 8, 2);
        line(g, color, 8, 14, 8, 16);
        line(g, color, 0, 8, 2, 8);
        line(g, color, 14, 8, 16, 8);
        line(g, color, 2, 2, 3.5f, 3.5f);
        line(g, color, 12.5f, 12.5f, 14, 14);
        line(g, color, 12.5f, 3.5f, 14, 2);
        line(g, color, 2, 14, 3.5f, 12.5f);
      }
      case LOW_FIRE -> {
        path(g, color, 4, 9, 6, 5, 7, 8, 10, 3, 13, 9, 12, 13, 8, 15, 4, 13, 4, 9);
        line(g, color, 1, 3, 15, 3);
      }
      case ZOOM -> {
        Ui.outline(g, 1, 1, 10, 10, 5, color);
        line(g, color, 10, 10, 15, 15);
        line(g, color, 4, 6, 8, 6);
        line(g, color, 6, 4, 6, 8);
      }
      case BLOCK_OVERLAY -> {
        path(g, color, 8, 1, 14, 4, 14, 12, 8, 15, 2, 12, 2, 4, 8, 1);
        path(g, color, 2, 4, 8, 7, 14, 4);
        line(g, color, 8, 7, 8, 15);
      }
      case HIT_COLOR -> {
        path(g, color, 8, 1, 13, 8, 13, 11, 11, 14, 5, 14, 3, 11, 3, 8, 8, 1);
        line(g, color, 6, 11, 7, 12);
      }
    }
    g.pose().popMatrix();
  }

  private static void line(GuiGraphics g, int color, float x, float y, float xx, float yy) {
    Ui.line(g, x, y, xx, yy, 1.35f, color);
  }

  private static void path(GuiGraphics g, int color, float... points) {
    for (int i = 2; i < points.length; i += 2)
      line(g, color, points[i - 2], points[i - 1], points[i], points[i + 1]);
  }

  private ModuleIcons() {}
}
