package dev.fluxtweaks.gui;

import com.google.gson.JsonPrimitive;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
  private final Screen previous;
  private boolean dragging;
  private double offsetX, offsetY;

  public HudEditorScreen(Screen previous) {
    super(Component.literal("Flux Tweaks HUD Editor"));
    this.previous = previous;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float delta) {
    FluxTweaks.hud.render(g, true);
    var b = FluxTweaks.hud.bounds();
    Ui.outline(
        g,
        (int) b.x() - 2,
        (int) b.y() - 2,
        (int) b.width() + 4,
        (int) b.height() + 4,
        12,
        Ui.secondary());
    int x = Math.max(6, width / 2 - 154), y = toolbarY();
    Ui.surface(g, x, y, Math.min(308, width - 12), 58);
    Ui.titleRow(g, "HUD Editor", x + 14, y + 8, 24, Ui.WHITE);
    Ui.text(g, "Drag · Scroll to scale · Arrow keys to nudge", x + 14, y + 37, Ui.MUTED);
    Ui.rounded(g, x + 174, y + 8, 54, 24, 6, Ui.inset());
    Ui.centered(g, "Reset", x + 174, y + 8, 54, 24, Ui.MUTED);
    Ui.rounded(g, x + 236, y + 8, 58, 24, 6, Ui.alpha(FluxTweaks.config.theme.primary, .28f));
    Ui.centered(g, "Done", x + 236, y + 8, 58, 24, Ui.WHITE);
  }

  private int toolbarY() {
    var b = FluxTweaks.hud.bounds();
    return b.y() + b.height() / 2 >= height / 2f ? 12 : height - 70;
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
    int x = Math.max(6, width / 2 - 154), y = toolbarY();
    if (event.button() == 0 && Ui.inside(event.x(), event.y(), x + 236, y + 8, 58, 24)) {
      onClose();
      return true;
    }
    if (event.button() == 0 && Ui.inside(event.x(), event.y(), x + 174, y + 8, 54, 24)) {
      FluxTweaks.config.layout.hudX = .03;
      FluxTweaks.config.layout.hudY = .78;
      FluxTweaks.config.set(Feature.SPOTIFY, "scale", new JsonPrimitive(1));
      FluxTweaks.changed();
      return true;
    }
    var b = FluxTweaks.hud.bounds();
    if (event.button() == 0
        && Ui.inside(event.x(), event.y(), b.x(), b.y(), b.width(), b.height())) {
      if (FluxTweaks.hud.controlAt(event.x(), event.y())
          && FluxTweaks.spotify.frame().track().available())
        FluxTweaks.hud.click(event.x(), event.y());
      else {
        dragging = true;
        offsetX = event.x() - b.x();
        offsetY = event.y() - b.y();
      }
    }
    return true;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
    if (dragging) {
      FluxTweaks.config.layout.hudX = Math.clamp((event.x() - offsetX) / width, 0, 1);
      FluxTweaks.config.layout.hudY = Math.clamp((event.y() - offsetY) / height, 0, 1);
      FluxTweaks.changed();
    }
    return true;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    dragging = false;
    return true;
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    double scale = FluxTweaks.config.number(Feature.SPOTIFY, "scale");
    FluxTweaks.config.set(Feature.SPOTIFY, "scale", new JsonPrimitive(scale + vertical * .05));
    FluxTweaks.changed();
    return true;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    if (event.key() == GLFW.GLFW_KEY_ESCAPE || FluxTweaks.guiKey.matches(event)) {
      onClose();
      return true;
    }
    int dx = event.key() == GLFW.GLFW_KEY_RIGHT ? 1 : event.key() == GLFW.GLFW_KEY_LEFT ? -1 : 0,
        dy = event.key() == GLFW.GLFW_KEY_DOWN ? 1 : event.key() == GLFW.GLFW_KEY_UP ? -1 : 0;
    if (dx != 0 || dy != 0) {
      FluxTweaks.config.layout.hudX =
          Math.clamp(FluxTweaks.config.layout.hudX + dx / (double) width, 0, 1);
      FluxTweaks.config.layout.hudY =
          Math.clamp(FluxTweaks.config.layout.hudY + dy / (double) height, 0, 1);
      FluxTweaks.changed();
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

  @Override
  public void onClose() {
    FluxTweaks.save();
    minecraft.setScreen(previous);
  }
}
