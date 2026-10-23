package dev.fluxtweaks.gui;

import com.google.gson.JsonPrimitive;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
  private record Box(double x, double y, double width, double height) {
    boolean contains(double mx, double my) {
      return Ui.inside(mx, my, x, y, width, height);
    }

    double overlap(Box other) {
      return Math.max(0, Math.min(x + width, other.x + other.width) - Math.max(x, other.x))
          * Math.max(0, Math.min(y + height, other.y + other.height) - Math.max(y, other.y));
    }
  }

  private final Screen previous;
  private boolean dragging, notifications;
  private double offsetX, offsetY;

  public HudEditorScreen(Screen previous) {
    super(Component.literal("Flux Tweaks HUD Editor"));
    this.previous = previous;
  }

  private Box bounds(boolean notification) {
    if (notification) {
      var b = FluxTweaks.notifications.bounds();
      return new Box(b.x(), b.y(), b.width(), b.height());
    }
    var b = FluxTweaks.hud.bounds();
    return new Box(b.x(), b.y(), b.width(), b.height());
  }

  private Box toolbar() {
    double w = Math.min(308, width - 12), h = 88;
    Box best = null;
    double score = Double.MAX_VALUE;
    for (Box candidate :
        new Box[] {
          new Box((width - w) / 2, 12, w, h),
          new Box((width - w) / 2, height - h - 12, w, h),
          new Box(6, 12, w, h),
          new Box(width - w - 6, height - h - 12, w, h)
        }) {
      double overlap = candidate.overlap(bounds(false)) + candidate.overlap(bounds(true));
      if (overlap < score) {
        best = candidate;
        score = overlap;
      }
    }
    return best;
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
    FluxTweaks.hud.render(g, true);
    FluxTweaks.notifications.render(g, true);
    for (boolean notification : new boolean[] {false, true}) {
      var b = bounds(notification);
      Ui.outline(
          g,
          (float) b.x - 2,
          (float) b.y - 2,
          (float) b.width + 4,
          (float) b.height + 4,
          12,
          notification == notifications ? Ui.secondary() : 0x65FFFFFF);
    }
    var bar = toolbar();
    int x = (int) bar.x, y = (int) bar.y;
    Ui.surface(g, x, y, (float) bar.width, (float) bar.height);
    Ui.titleRow(g, "HUD Editor", x + 14, y + 8, 24, Ui.WHITE);
    Ui.rounded(g, x + 174, y + 8, 54, 24, 6, Ui.inset());
    Ui.centered(g, "Reset", x + 174, y + 8, 54, 24, Ui.MUTED);
    Ui.rounded(g, x + 236, y + 8, 58, 24, 6, Ui.alpha(FluxTweaks.config.theme.primary, .28f));
    Ui.centered(g, "Done", x + 236, y + 8, 58, 24, Ui.WHITE);
    for (int i = 0; i < 2; i++) {
      int bx = x + 14 + i * 144;
      boolean selected = notifications == (i == 1);
      Ui.rounded(
          g,
          bx,
          y + 40,
          136,
          22,
          6,
          selected ? Ui.alpha(FluxTweaks.config.theme.primary, .24f) : Ui.inset());
      if (selected) Ui.outline(g, bx, y + 40, 136, 22, 6, Ui.secondary());
      Ui.centered(
          g,
          i == 0 ? "Spotify" : "Notifications",
          bx,
          y + 40,
          136,
          22,
          selected ? Ui.WHITE : Ui.MUTED);
    }
    Ui.text(
        g,
        Ui.fit("Drag · Scroll to scale · Arrows move · Tab selects", 280),
        x + 14,
        y + 71,
        Ui.MUTED);
  }

  private void reset() {
    if (notifications) {
      var c = FluxTweaks.config.notifications;
      c.x = .66;
      c.y = .06;
      c.scale = 1;
    } else {
      FluxTweaks.config.layout.hudX = .03;
      FluxTweaks.config.layout.hudY = .78;
      FluxTweaks.config.set(Feature.SPOTIFY, "scale", new JsonPrimitive(1));
    }
    FluxTweaks.changed();
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
    if (event.button() != 0) return true;
    var bar = toolbar();
    int x = (int) bar.x, y = (int) bar.y;
    if (Ui.inside(event.x(), event.y(), x + 236, y + 8, 58, 24)) {
      onClose();
      return true;
    }
    if (Ui.inside(event.x(), event.y(), x + 174, y + 8, 54, 24)) {
      reset();
      return true;
    }
    for (int i = 0; i < 2; i++) {
      if (Ui.inside(event.x(), event.y(), x + 14 + i * 144, y + 40, 136, 22)) {
        notifications = i == 1;
        return true;
      }
    }
    if (bar.contains(event.x(), event.y())) return true;
    if (bounds(true).contains(event.x(), event.y())) notifications = true;
    else if (bounds(false).contains(event.x(), event.y())) notifications = false;
    else return true;
    var b = bounds(notifications);
    if (!notifications
        && FluxTweaks.hud.controlAt(event.x(), event.y())
        && FluxTweaks.spotify.frame().track().available())
      FluxTweaks.hud.click(event.x(), event.y());
    else {
      dragging = true;
      offsetX = event.x() - b.x;
      offsetY = event.y() - b.y;
    }
    return true;
  }

  private void position(double x, double y) {
    if (notifications) {
      FluxTweaks.config.notifications.x = Math.clamp(x / width, 0, 1);
      FluxTweaks.config.notifications.y = Math.clamp(y / height, 0, 1);
    } else {
      FluxTweaks.config.layout.hudX = Math.clamp(x / width, 0, 1);
      FluxTweaks.config.layout.hudY = Math.clamp(y / height, 0, 1);
    }
    FluxTweaks.changed();
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
    if (dragging) position(event.x() - offsetX, event.y() - offsetY);
    return true;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    dragging = false;
    return true;
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    if (notifications)
      FluxTweaks.config.notifications.scale =
          Math.clamp(FluxTweaks.config.notifications.scale + vertical * .05, .5, 2);
    else
      FluxTweaks.config.set(
          Feature.SPOTIFY,
          "scale",
          new JsonPrimitive(FluxTweaks.config.number(Feature.SPOTIFY, "scale") + vertical * .05));
    FluxTweaks.changed();
    return true;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    if (event.key() == GLFW.GLFW_KEY_ESCAPE || FluxTweaks.guiKey.matches(event)) {
      onClose();
      return true;
    }
    if (event.key() == GLFW.GLFW_KEY_TAB) {
      notifications = !notifications;
      dragging = false;
      return true;
    }
    if (event.key() == GLFW.GLFW_KEY_HOME) {
      reset();
      return true;
    }
    int dx = event.key() == GLFW.GLFW_KEY_RIGHT ? 1 : event.key() == GLFW.GLFW_KEY_LEFT ? -1 : 0,
        dy = event.key() == GLFW.GLFW_KEY_DOWN ? 1 : event.key() == GLFW.GLFW_KEY_UP ? -1 : 0;
    if (dx != 0 || dy != 0) {
      var b = bounds(notifications);
      position(b.x + dx, b.y + dy);
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {}

  @Override
  public void onClose() {
    FluxTweaks.save();
    minecraft.gui.setScreen(previous);
  }
}
