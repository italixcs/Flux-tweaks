package dev.fluxtweaks.feature;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.config.ZoomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class Zoom {
  private static final ZoomState STATE = new ZoomState();
  private static ClientLevel level;
  private static boolean down;
  private static String binding = "";

  private static double now() {
    return System.nanoTime() / 1_000_000.0;
  }

  public static void refresh() {
    var mc = Minecraft.getInstance();
    if (level != mc.level) {
      level = mc.level;
      STATE.reset();
    }
    var key = FluxTweaks.keys.get(Feature.ZOOM);
    String bound = key == null ? "" : key.saveString();
    if (!binding.equals(bound)) {
      binding = bound;
      down = false;
      STATE.reset();
    }
    STATE.update(
        FluxTweaks.active(Feature.ZOOM)
            && mc.player != null
            && mc.player.isAlive()
            && mc.gui.screen() == null
            && mc.gui.overlay() == null
            && mc.isWindowActive(),
        down,
        FluxTweaks.config.number(Feature.ZOOM, "magnification"),
        FluxTweaks.config.number(Feature.ZOOM, "duration"),
        now());
  }

  public static void keyEvent(boolean pressed) {
    refresh();
    down = pressed;
    refresh();
  }

  public static float fov(float vanilla) {
    refresh();
    return STATE.fov(vanilla, now());
  }

  public static double sensitivity() {
    refresh();
    return 1 / STATE.magnification(now());
  }

  public static boolean scroll(double steps) {
    refresh();
    return STATE.scroll(steps, now());
  }

  public static boolean held() {
    refresh();
    return STATE.held();
  }
}
