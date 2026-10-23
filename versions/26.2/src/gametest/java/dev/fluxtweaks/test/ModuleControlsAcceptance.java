package dev.fluxtweaks.test;

import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import dev.fluxtweaks.gui.*;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class ModuleControlsAcceptance {
  static void run(ClientGameTestContext ctx) {
    ctx.runOnClient(
        mc -> {
          FluxTweaks.config.layout.selected = Feature.FREECAM.id;
          FluxTweaks.setEnabled(Feature.FREECAM, false);
          FluxTweaks.notifications.clear();
          mc.gui.setScreen(new FluxScreen());
        });
    GuiAcceptance.clickControl(ctx, "Set Freecam Enable key");
    ctx.runOnClient(mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_F6)));
    GuiAcceptance.clickControl(ctx, "Set Freecam Action key");
    ctx.runOnClient(mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_F7)));
    ctx.waitTicks(5);
    ctx.takeScreenshot("flux-module-two-keys");
    ctx.runOnClient(
        mc -> {
          check(
              FluxTweaks.enableKeys.get(Feature.FREECAM).matches(key(GLFW.GLFW_KEY_F6)),
              "Enable key assignment failed");
          check(
              FluxTweaks.keys.get(Feature.FREECAM).matches(key(GLFW.GLFW_KEY_F7)),
              "Action key assignment failed");
          mc.gui.setScreen(null);
        });
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F7);
    ctx.runOnClient(
        mc -> {
          check(
              !FluxTweaks.active(Feature.FREECAM) && !FluxTweaks.config.enabled(Feature.FREECAM),
              "Disabled action activated a camera");
          check(
              FluxTweaks.notifications.queue.visible().isEmpty(),
              "Ignored action generated a notification");
        });
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F6);
    ctx.runOnClient(
        mc -> {
          check(
              FluxTweaks.config.enabled(Feature.FREECAM) && !FluxTweaks.active(Feature.FREECAM),
              "Enable key activated the camera");
          last(NotificationQueue.State.ENABLED);
          int count = FluxTweaks.notifications.queue.visible().size();
          FluxTweaks.setEnabled(Feature.FREECAM, true);
          check(
              FluxTweaks.notifications.queue.visible().size() == count,
              "Unchanged state produced duplicate notifications");
          FluxTweaks.notifications.clear();
        });
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F7);
    ctx.runOnClient(
        mc -> {
          check(FluxTweaks.active(Feature.FREECAM), "Action key did not activate Freecam");
          last(NotificationQueue.State.ACTIVATED);
        });
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F7);
    ctx.runOnClient(
        mc -> {
          check(
              !FluxTweaks.active(Feature.FREECAM) && FluxTweaks.config.enabled(Feature.FREECAM),
              "Action key did not leave Freecam ready");
          last(NotificationQueue.State.DEACTIVATED);
        });
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F7);
    ctx.runOnClient(mc -> FluxTweaks.notifications.clear());
    ctx.getInput().pressKey(GLFW.GLFW_KEY_F6);
    ctx.runOnClient(
        mc -> {
          check(
              !FluxTweaks.active(Feature.FREECAM) && !FluxTweaks.config.enabled(Feature.FREECAM),
              "Disabling did not stop active Freecam");
          check(
              FluxTweaks.notifications.queue.visible().stream()
                  .map(NotificationQueue.Notice::state)
                  .toList()
                  .equals(
                      List.of(
                          NotificationQueue.State.DEACTIVATED, NotificationQueue.State.DISABLED)),
              "Disable transition notifications are wrong");
          FluxTweaks.setEnabled(Feature.FREECAM, true);
          FluxTweaks.config.set(Feature.FREECAM, "activation", new JsonPrimitive("Hold"));
        });
    ctx.waitTicks(3);
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F7);
    ctx.waitTicks(2);
    ctx.runOnClient(mc -> check(FluxTweaks.active(Feature.FREECAM), "Hold Freecam did not start"));
    ctx.setScreen(FluxScreen::new);
    ctx.waitTicks(2);
    ctx.runOnClient(
        mc ->
            check(
                !FluxTweaks.active(Feature.FREECAM), "Hold action survived editing cancellation"));
    ctx.setScreen(() -> null);
    ctx.waitTicks(2);
    ctx.runOnClient(
        mc ->
            check(
                !FluxTweaks.active(Feature.FREECAM),
                "Restored held key bypassed the fresh-press gate"));
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F7);
    ctx.waitTicks(2);
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F7);
    ctx.waitTicks(2);
    ctx.runOnClient(
        mc -> check(FluxTweaks.active(Feature.FREECAM), "Fresh hold press was not accepted"));
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F7);
    ctx.waitTicks(2);
    ctx.runOnClient(
        mc -> {
          check(!FluxTweaks.active(Feature.FREECAM), "Hold release did not stop Freecam");
          FluxTweaks.config.set(Feature.FREECAM, "activation", new JsonPrimitive("Toggle"));
          FluxTweaks.notifications.clear();
          FluxTweaks.setEnabled(Feature.HAND_CHAMS, true);
          FluxTweaks.setActive(Feature.FREECAM, true);
          FluxTweaks.setActive(Feature.FREECAM, false);
        });
    ctx.waitTicks(5);
    ctx.takeScreenshot("flux-module-notifications");
    ctx.waitTicks(51);
    ctx.runOnClient(
        mc ->
            check(
                FluxTweaks.notifications.queue.visible().isEmpty(),
                "Notifications did not expire"));
    ctx.setScreen(() -> new HudEditorScreen(new FluxScreen()));
    ctx.waitTicks(5);
    ctx.takeScreenshot("flux-hud-notification-editor");
    ctx.runOnClient(
        mc -> {
          var spotify = FluxTweaks.hud.bounds();
          var b = FluxTweaks.notifications.bounds();
          var from = mouse(b.x() + 12, b.y() + 20);
          mc.gui.screen().mouseClicked(from, false);
          mc.gui.screen().mouseDragged(mouse(from.x() - 40, from.y() + 60), -40, 60);
          mc.gui.screen().mouseReleased(from);
          check(FluxTweaks.notifications.bounds().x() < b.x(), "Notification HUD did not drag");
          check(FluxTweaks.hud.bounds().equals(spotify), "Dragging notifications moved Spotify");
          mc.gui.screen().mouseScrolled(0, 0, 0, 1);
          check(FluxTweaks.config.notifications.scale > 1, "Notification HUD did not scale");
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_RIGHT));
          FluxTweaks.save();
          try {
            var loaded =
                new ConfigStore(
                        net.fabricmc.loader.api.FabricLoader.getInstance()
                            .getConfigDir()
                            .resolve("fluxtweaks.json"))
                    .load();
            check(
                loaded.notifications.x == FluxTweaks.config.notifications.x
                    && loaded.notifications.scale == FluxTweaks.config.notifications.scale,
                "Notification HUD placement/scale did not persist");
            check(loaded.enabled(Feature.FREECAM), "Camera availability did not persist");
          } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
          }
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_HOME));
          check(
              FluxTweaks.config.notifications.scale == 1
                  && FluxTweaks.config.notifications.x == .66,
              "Selected notification reset failed");
          check(FluxTweaks.hud.bounds().equals(spotify), "Notification reset changed Spotify");
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_TAB));
          mc.gui.screen().mouseScrolled(0, 0, 0, 1);
          check(
              FluxTweaks.config.number(Feature.SPOTIFY, "scale") > 1
                  && FluxTweaks.config.notifications.scale == 1,
              "HUD element scales are not independent");
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_HOME));
          mc.gui.setScreen(null);
          FluxTweaks.setEnabled(Feature.HAND_CHAMS, false);
          FluxTweaks.setEnabled(Feature.FREECAM, false);
          FluxTweaks.enableKeys.get(Feature.FREECAM).setKey(InputConstants.UNKNOWN);
          FluxTweaks.keys.get(Feature.FREECAM).setKey(InputConstants.UNKNOWN);
          KeyMapping.resetMapping();
          FluxTweaks.notifications.clear();
        });
  }

  private static void last(NotificationQueue.State state) {
    check(
        !FluxTweaks.notifications.queue.visible().isEmpty()
            && FluxTweaks.notifications.queue.visible().getLast().state() == state,
        "Expected notification " + state);
  }

  private static KeyEvent key(int key) {
    return new KeyEvent(key, 0, 0);
  }

  private static MouseButtonEvent mouse(double x, double y) {
    return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
  }

  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
}
