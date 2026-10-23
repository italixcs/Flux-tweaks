package dev.fluxtweaks.gui;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class NotificationHud {
  public record Bounds(float x, float y, float width, float height, float scale) {}

  public static final int WIDTH = 190, HEIGHT = 46, GAP = 6;
  public static final int STACK_HEIGHT =
      HEIGHT * NotificationQueue.LIMIT + GAP * (NotificationQueue.LIMIT - 1);
  public final NotificationQueue queue = new NotificationQueue();

  public void push(Feature feature, NotificationQueue.State state) {
    if (FluxTweaks.config.notifications.enabled) queue.add(feature, state);
  }

  public void clear() {
    queue.clear();
  }

  public Bounds bounds() {
    var mc = Minecraft.getInstance();
    float w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
    var config = FluxTweaks.config.notifications;
    float scale = (float) Math.min(config.scale, Math.min(w / WIDTH, h / STACK_HEIGHT));
    return new Bounds(
        (float) Math.clamp(config.x * w, 0, Math.max(0, w - WIDTH * scale)),
        (float) Math.clamp(config.y * h, 0, Math.max(0, h - STACK_HEIGHT * scale)),
        WIDTH * scale,
        STACK_HEIGHT * scale,
        scale);
  }

  public void render(GuiGraphicsExtractor g, boolean preview) {
    if (!preview
        && (Minecraft.getInstance().gui.screen() instanceof HudEditorScreen
            || !FluxTweaks.config.notifications.enabled)) return;
    var notices = queue.visible();
    if (!preview && notices.isEmpty()) return;
    var b = bounds();
    g.pose().pushMatrix();
    g.pose().translate(b.x, b.y);
    g.pose().scale(b.scale, b.scale);
    if (preview) {
      card(g, Feature.FREECAM, NotificationQueue.State.ACTIVATED, 0, 1, .7f);
      Ui.right(g, "Preview", WIDTH - 12, 10, Ui.MUTED);
      card(g, Feature.HAND_CHAMS, NotificationQueue.State.ENABLED, HEIGHT + GAP, 1, .85f);
      card(g, Feature.ZOOM, NotificationQueue.State.DEACTIVATED, (HEIGHT + GAP) * 2, 1, .95f);
    } else {
      for (int i = 0; i < notices.size(); i++) {
        var notice = notices.get(i);
        float amount = queue.appearance(notice, FluxTweaks.config.theme.reducedMotion);
        float remaining = (float) (1 - queue.age(notice) / NotificationQueue.LIFETIME_MS);
        card(g, notice.feature(), notice.state(), i * (HEIGHT + GAP), amount, remaining);
      }
    }
    g.pose().popMatrix();
  }

  private void card(
      GuiGraphicsExtractor g,
      Feature feature,
      NotificationQueue.State state,
      int y,
      float amount,
      float remaining) {
    if (amount <= 0) return;
    g.pose().pushMatrix();
    g.pose().translate(FluxTweaks.config.theme.reducedMotion ? 0 : 8 * (1 - amount), y);
    Ui.rounded(
        g,
        0,
        0,
        WIDTH,
        HEIGHT,
        10,
        Ui.alpha(
            FluxTweaks.config.theme.background, (float) FluxTweaks.config.theme.opacity * amount));
    Ui.outline(g, 0, 0, WIDTH, HEIGHT, 10, Ui.alpha(0xffffff, .14f * amount));
    int accent = state.positive() ? Ui.secondary() : Ui.MUTED;
    ModuleIcons.draw(g, feature, 12, 15, 16, Ui.alpha(accent, amount));
    Ui.text(g, Ui.fit(feature.title, WIDTH - 52), 40, 10, Ui.alpha(Ui.WHITE, amount));
    Ui.text(g, state.label, 40, 26, Ui.alpha(accent, amount));
    Ui.rounded(
        g,
        12,
        HEIGHT - 4,
        (WIDTH - 24) * Math.clamp(remaining, 0, 1),
        2,
        1,
        Ui.alpha(FluxTweaks.config.theme.primary, amount * .8f));
    g.pose().popMatrix();
  }
}
