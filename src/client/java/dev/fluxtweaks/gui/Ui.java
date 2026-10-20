package dev.fluxtweaks.gui;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.VisualMath;
import dev.fluxtweaks.mixin.FontAccess;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.*;

public final class Ui {
  public static final int WHITE = 0xFFF4F4F5, MUTED = 0xFFBDBDC3;
  private static final FontDescription FONT = new FontDescription.Resource(FluxTweaks.id("ui"));
  private static final FontDescription TITLE =
      new FontDescription.Resource(FluxTweaks.id("ui_title"));
  private static Font inputFont, originalFont;

  public static boolean isFont(FontDescription font) {
    return FONT.equals(font) || TITLE.equals(font);
  }

  public static Component label(String text) {
    return Component.literal(text).withStyle(s -> s.withFont(FONT));
  }

  public static void text(GuiGraphics g, String text, int x, int y, int color) {
    g.drawString(Minecraft.getInstance().font, label(text), x, y, color, false);
  }

  public static void title(GuiGraphics g, String text, int x, int y, int color) {
    g.drawString(
        Minecraft.getInstance().font,
        Component.literal(text).withStyle(s -> s.withFont(TITLE)),
        x,
        y,
        color,
        false);
  }

  public static int width(String text) {
    return Minecraft.getInstance().font.width(label(text));
  }

  public static void right(GuiGraphics g, String text, int x, int y, int color) {
    text(g, text, x - width(text), y, color);
  }

  public static String fit(String text, int width) {
    if (text == null) return "";
    if (width(text) <= width) return text;
    String value = text;
    while (!value.isEmpty() && width(value + "…") > width)
      value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
    return value + "…";
  }

  public static float scaleFor(int width, int height) {
    return Math.min(.75f, Math.min(width / 560f, height / 480f));
  }

  public static int alpha(int rgb, float alpha) {
    return (Math.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xffffff);
  }

  public static int primary() {
    return FluxTweaks.config.theme.primary | 0xff000000;
  }

  public static int secondary() {
    return FluxTweaks.config.theme.secondary | 0xff000000;
  }

  public static int panel() {
    return alpha(FluxTweaks.config.theme.background, (float) FluxTweaks.config.theme.opacity);
  }

  public static int inset() {
    int rgb = FluxTweaks.config.theme.background;
    return alpha(
        ((rgb >> 16 & 255) * 3 / 4 << 16) | ((rgb >> 8 & 255) * 3 / 4 << 8) | ((rgb & 255) * 3 / 4),
        (float) FluxTweaks.config.theme.opacity);
  }

  public static void rounded(
      GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
    SmoothGui.box(g, SmoothGui.ROUND, x, y, w, h, radius, color);
  }

  public static void outline(
      GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
    SmoothGui.box(g, SmoothGui.OUTLINE, x, y, w, h, radius, color);
  }

  public static void circle(GuiGraphics g, float x, float y, float size, int color) {
    rounded(g, x, y, size, size, size * .5f, color);
  }

  public static boolean inside(double mx, double my, double x, double y, double w, double h) {
    return mx >= x && my >= y && mx < x + w && my < y + h;
  }

  public static void toggle(GuiGraphics g, float x, float y, float amount) {
    rounded(g, x, y, 27, 14, 7, VisualMath.blend(0xff49494f, primary(), amount));
    circle(g, x + 2 + 13 * amount, y + 2, 10, VisualMath.blend(0xffb9b9c0, WHITE, amount));
  }

  public static void line(
      GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, int color) {
    g.pose().pushMatrix();
    g.pose().translate(x1, y1);
    g.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
    rounded(
        g,
        -thickness * .5f,
        -thickness * .5f,
        (float) Math.hypot(x2 - x1, y2 - y1) + thickness,
        thickness,
        thickness * .5f,
        color);
    g.pose().popMatrix();
  }

  public static void check(GuiGraphics g, float x, float y, int color) {
    line(g, x, y + 3, x + 2.5f, y + 5.5f, 1.4f, color);
    line(g, x + 2.5f, y + 5.5f, x + 7, y, 1.4f, color);
  }

  public static void close(GuiGraphics g, float x, float y, int color) {
    line(g, x, y, x + 5, y + 5, 1.1f, color);
    line(g, x + 5, y, x, y + 5, 1.1f, color);
  }

  public static void triangle(GuiGraphics g, int x, int y, int size, boolean right, int color) {
    float w = size * .58f;
    g.pose().pushMatrix();
    g.pose().translate(right ? x : x + w, y);
    if (!right) g.pose().scale(-1, 1);
    SmoothGui.box(g, SmoothGui.TRIANGLE, 0, 0, w, size, 0, color);
    g.pose().popMatrix();
  }

  public static EditBox input(int x, int y, int w, int h, String title) {
    Font vanilla = Minecraft.getInstance().font;
    if (inputFont == null || originalFont != vanilla) {
      originalFont = vanilla;
      Font.Provider provider = ((FontAccess) (Object) vanilla).flux$provider();
      inputFont =
          new Font(
              new Font.Provider() {
                public GlyphSource glyphs(FontDescription ignored) {
                  return provider.glyphs(FONT);
                }

                public EffectGlyph effect() {
                  return provider.effect();
                }
              });
    }
    EditBox box = new EditBox(inputFont, x, y, w, h, label(title));
    box.setBordered(false);
    box.setTextShadow(false);
    box.setTextColor(WHITE);
    box.addFormatter((value, offset) -> label(value).getVisualOrderText());
    return box;
  }

  public static final class Motion {
    private final Map<String, Float> values = new HashMap<>();
    private long previous = System.nanoTime();
    private float seconds = 1 / 60f;

    public void frame() {
      long now = System.nanoTime();
      seconds = Math.clamp((now - previous) / 1_000_000_000f, 0, .05f);
      previous = now;
    }

    public float value(String key, float target) {
      float old = values.getOrDefault(key, target);
      float next = old + (target - old) * (float) (1 - Math.exp(-18 * seconds));
      if (Math.abs(next - target) < .001f) next = target;
      values.put(key, next);
      return next;
    }

    public void snap(String key, float target) {
      values.put(key, target);
    }
  }

  private Ui() {}
}
