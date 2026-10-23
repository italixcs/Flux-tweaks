package dev.fluxtweaks.gui;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.VisualMath;
import dev.fluxtweaks.gui.motion.*;
import dev.fluxtweaks.mixin.FontAccess;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.*;

public final class Ui {
  public static final int WHITE = 0xFFF3F5F7, MUTED = 0xFFB5BEC9;
  private static final FontDescription FONT = new FontDescription.Resource(FluxTweaks.id("ui"));
  private static final FontDescription TITLE =
      new FontDescription.Resource(FluxTweaks.id("ui_title"));
  private static Font inputFont, originalFont;

  private record Metrics(BakedGlyph glyph, float top, float height) {}

  private static Metrics bodyMetrics, titleMetrics;

  // Use a stable capital reference, not each label's ascenders/descenders, so rows share a
  // baseline.
  // The baked glyph identity changes with resource reloads; no stale GPU/font resources are held.
  private static Metrics metrics(boolean title) {
    Font font = Minecraft.getInstance().font;
    FontDescription description = title ? TITLE : FONT;
    BakedGlyph glyph =
        ((FontAccess) (Object) font).flux$provider().glyphs(description).getGlyph('H');
    Metrics cached = title ? titleMetrics : bodyMetrics;
    if (cached == null || cached.glyph != glyph) {
      var ink = glyph.createGlyph(0, 0, WHITE, 0, Style.EMPTY.withFont(description), 0, 0);
      cached =
          new Metrics(
              glyph, ink == null ? 0 : ink.top(), ink == null ? 9 : ink.bottom() - ink.top());
      if (title) titleMetrics = cached;
      else bodyMetrics = cached;
    }
    return cached;
  }

  public static int textY(int top, int height) {
    return top + Math.round((height - metrics(false).height) / 2);
  }

  public static void row(GuiGraphicsExtractor g, String text, int x, int y, int height, int color) {
    text(g, text, x, textY(y, height), color);
  }

  public static void centered(
      GuiGraphicsExtractor g, String text, int x, int y, int w, int h, int color) {
    row(g, text, x + (w - width(text)) / 2, y, h, color);
  }

  public static void titleRow(
      GuiGraphicsExtractor g, String text, int x, int y, int height, int color) {
    title(g, text, x, y + Math.round((height - metrics(true).height) / 2), color);
  }

  public static boolean isFont(FontDescription font) {
    return FONT.equals(font) || TITLE.equals(font);
  }

  public static Component label(String text) {
    return Component.literal(text).withStyle(s -> s.withFont(FONT));
  }

  public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
    g.text(
        Minecraft.getInstance().font,
        label(text),
        x,
        y - Math.round(metrics(false).top),
        color,
        false);
  }

  public static void title(GuiGraphicsExtractor g, String text, int x, int y, int color) {
    g.text(
        Minecraft.getInstance().font,
        Component.literal(text).withStyle(s -> s.withFont(TITLE)),
        x,
        y - Math.round(metrics(true).top),
        color,
        false);
  }

  public static int width(String text) {
    return Minecraft.getInstance().font.width(label(text));
  }

  public static void right(GuiGraphicsExtractor g, String text, int x, int y, int color) {
    text(g, text, x - width(text), y, color);
  }

  public static String fit(String text, int width) {
    return fit(text, width, Ui::width);
  }

  public static String fitTitle(String text, int width) {
    return fit(
        text,
        width,
        value ->
            Minecraft.getInstance()
                .font
                .width(Component.literal(value).withStyle(s -> s.withFont(TITLE))));
  }

  private static String fit(
      String text, int width, java.util.function.ToIntFunction<String> measure) {
    if (text == null) return "";
    if (measure.applyAsInt(text) <= width) return text;
    String value = text;
    while (!value.isEmpty() && measure.applyAsInt(value + "…") > width)
      value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
    return value + "…";
  }

  public static float scaleFor(int width, int height) {
    return GuiLayout.scale(width, height, FluxTweaks.config.theme.guiScale);
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
      GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int color) {
    SmoothGui.box(g, SmoothGui.ROUND, x, y, w, h, radius, color);
  }

  public static void outline(
      GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int color) {
    SmoothGui.box(g, SmoothGui.OUTLINE, x, y, w, h, radius, color);
  }

  public static void circle(GuiGraphicsExtractor g, float x, float y, float size, int color) {
    rounded(g, x, y, size, size, size * .5f, color);
  }

  public static boolean inside(double mx, double my, double x, double y, double w, double h) {
    return mx >= x && my >= y && mx < x + w && my < y + h;
  }

  public static void toggle(GuiGraphicsExtractor g, float x, float y, float amount) {
    rounded(g, x, y, 27, 14, 7, VisualMath.blend(0xff49494f, primary(), amount));
    circle(g, x + 2 + 13 * amount, y + 2, 10, VisualMath.blend(0xffb9b9c0, WHITE, amount));
  }

  public static void line(
      GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float thickness, int color) {
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

  public static void check(GuiGraphicsExtractor g, float x, float y, int color) {
    line(g, x, y + 3, x + 2.5f, y + 5.5f, 1.4f, color);
    line(g, x + 2.5f, y + 5.5f, x + 7, y, 1.4f, color);
  }

  public static void close(GuiGraphicsExtractor g, float x, float y, int color) {
    line(g, x, y, x + 5, y + 5, 1.1f, color);
    line(g, x + 5, y, x, y + 5, 1.1f, color);
  }

  public static void triangle(
      GuiGraphicsExtractor g, int x, int y, int size, boolean right, int color) {
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
    EditBox box =
        new EditBox(inputFont, x, y, w, h, label(title)) {
          @Override
          public void extractWidgetRenderState(
              GuiGraphicsExtractor graphics, int mx, int my, float delta) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, textY(0, getHeight()) - Math.round(metrics(false).top));
            super.extractWidgetRenderState(graphics, mx, my, delta);
            graphics.pose().popMatrix();
          }
        };
    box.setBordered(false);
    box.setTextShadow(false);
    box.setTextColor(WHITE);
    box.addFormatter((value, offset) -> label(value).getVisualOrderText());
    return box;
  }

  /** Shared toolkit timings; each transition retargets from its displayed value. */
  public static final class Motion {
    private final Map<String, Tween> values = new HashMap<>();

    public float value(String key, float target) {
      return value(key, target, 100);
    }

    public float value(String key, float target, int millis) {
      Tween tween = values.computeIfAbsent(key, ignored -> new Tween(target));
      tween.animateTo(
          target,
          Duration.ofMillis(millis),
          key.equals("screen") && target == 0 ? Easing.CUBIC_IN : Easing.CUBIC_OUT,
          FluxTweaks.config.theme.reducedMotion);
      return (float) tween.value();
    }

    public void snap(String key, float target) {
      values.computeIfAbsent(key, ignored -> new Tween(target)).snapTo(target);
    }
  }

  public static int raised() {
    return VisualMath.blend(panel(), 0xFFFFFFFF, .055f);
  }

  public static void surface(GuiGraphicsExtractor g, float x, float y, float w, float h) {
    rounded(g, x, y, w, h, 12, panel());
    outline(g, x, y, w, h, 12, 0x24FFFFFF);
  }

  public static void chevron(GuiGraphicsExtractor g, float x, float y, boolean up, int color) {
    float sign = up ? -1 : 1;
    line(g, x, y, x + 3, y + 3 * sign, 1.2f, color);
    line(g, x + 3, y + 3 * sign, x + 6, y, 1.2f, color);
  }

  private Ui() {}
}
