package dev.fluxtweaks.test;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.gui.FluxScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class GuiAcceptance {
  static void run(ClientGameTestContext context) {
    // Bind through the actual settings row, then restore the unbound default.
    clickControl(context, "Set Hand Chams keybind");
    context.runOnClient(mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_B, 0)));
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.keys.get(Feature.HAND_CHAMS).matches(key(GLFW.GLFW_KEY_B, 0)),
                "GUI key assignment failed"));
    clickControl(context, "Set Hand Chams keybind");
    context.runOnClient(mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_DELETE, 0)));

    // Numeric text and color hex use their real popups.
    clickControl(context, "Type Animation Speed");
    type(context, "2.25");
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.number(Feature.HAND_CHAMS, "speed") == 2.25,
                "Typed slider value was not applied"));
    clickControl(context, "First Color");
    context.waitTicks(5);
    var pickerImage = context.takeScreenshot("flux-gui-color-picker");
    int[] pixels =
        context.computeOnClient(
            mc -> {
              float zoom =
                  dev.fluxtweaks.gui.Ui.scaleFor(
                      mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
              int lw = (int) (mc.getWindow().getGuiScaledWidth() / zoom),
                  lh = (int) (mc.getWindow().getGuiScaledHeight() / zoom);
              double physical = zoom * mc.getWindow().getGuiScale();
              int px = (int) Math.round(((lw - 236) / 2 + 219) * physical),
                  py = (lh - 280) / 2 + 36;
              return new int[] {
                px, (int) Math.round((py + 40) * physical), (int) Math.round((py + 80) * physical)
              };
            });
    try {
      var image = javax.imageio.ImageIO.read(pickerImage.toFile());
      int green = image.getRGB(pixels[0], pixels[1]), blue = image.getRGB(pixels[0], pixels[2]);
      check(
          (green >> 8 & 255) > 220 && (green >> 16 & 255) < 35 && (green & 255) < 35,
          "Hue strip lost its green range");
      check(
          (blue & 255) > 220 && (blue >> 16 & 255) < 35 && (blue >> 8 & 255) < 35,
          "Hue strip lost its blue range");
    } catch (java.io.IOException ex) {
      throw new AssertionError(ex);
    }
    type(context, "#11AAEE");
    context.runOnClient(
        mc ->
            check(
                (FluxTweaks.config.color(Feature.HAND_CHAMS, "first") & 0xffffff) == 0x11aaee,
                "Hex color was not applied"));

    context.runOnClient(
        mc -> {
          FluxTweaks.config.layout.selected = Feature.HIT_PARTICLES.id;
          mc.gui.setScreen(new FluxScreen());
        });
    clickControl(context, "Particles choices");
    context.waitTicks(5);
    context.takeScreenshot("flux-gui-choice-popup");
    context.runOnClient(
        mc -> {
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_DOWN, 0));
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ENTER, 0));
          check(
              FluxTweaks.config.text(Feature.HIT_PARTICLES, "shape").equals("Snowflakes"),
              "Keyboard choice did not commit");
        });
    clickControl(context, "Particles choices");
    click(context, 40, 140);
    context.runOnClient(
        mc -> {
          check(
              FluxTweaks.config.layout.selected.equals(Feature.HIT_PARTICLES.id),
              "Modal leaked a background click");
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ESCAPE, 0));
          check(mc.gui.screen() instanceof FluxScreen, "Escape closed the screen behind a popup");
        });
    context.runOnClient(
        mc ->
            mc.gui
                .screen()
                .mouseScrolled(
                    (FluxTweaks.config.layout.settingsX + 100) * scale(mc),
                    (FluxTweaks.config.layout.settingsY + 220) * scale(mc),
                    0,
                    -20));
    context.waitTicks(6);
    clickControlAt(context, "Rotation Angle range slider", .9);
    context.runOnClient(
        mc -> {
          double[] range = FluxTweaks.config.range(Feature.HIT_PARTICLES, "angle");
          check(
              range[0] == 0 && range[1] > 140 && range[1] < 150,
              "Range click selected the wrong handle");
        });
    clickControl(context, "Theme");
    clickControl(context, "Reduced Motion");
    context.runOnClient(
        mc -> {
          check(FluxTweaks.config.theme.reducedMotion, "Reduced Motion did not toggle");
          FluxTweaks.save();
        });
    context.waitTicks(3);
    context.runOnClient(
        mc -> {
          try {
            var field = FluxScreen.class.getDeclaredField("zones");
            field.setAccessible(true);
            for (Object zone : (java.util.List<?>) field.get(mc.gui.screen())) {
              var label = zone.getClass().getDeclaredMethod("label");
              label.setAccessible(true);
              if (!label.invoke(zone).equals("Reduced Motion")) continue;
              var click = zone.getClass().getDeclaredMethod("click");
              click.setAccessible(true);
              Runnable action = (Runnable) click.invoke(zone);
              action.run();
              action.run();
              check(FluxTweaks.config.theme.reducedMotion, "Rapid toggles used stale values");
              return;
            }
            throw new AssertionError("Reduced Motion control missing");
          } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
          }
        });
    context.runOnClient(
        mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_CONTROL)));
    context.runOnClient(
        mc -> "free".codePoints().forEach(c -> mc.gui.screen().charTyped(new CharacterEvent(c))));
    context.waitTicks(5);
    clickControl(context, "Configure Freecam");
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.layout.selected.equals("freecam"),
                "Search did not filter the feature list"));
    context.runOnClient(mc -> mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_HOME, 0)));
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.layout.navX == 16 && FluxTweaks.config.layout.settingsX == 218,
                "Layout reset failed"));

    for (int[] size : new int[][] {{854, 480, 1}, {1920, 1080, 3}, {1280, 800, 2}}) {
      context.getInput().resizeWindow(size[0], size[1]);
      context.runOnClient(
          mc -> {
            mc.options.guiScale().set(size[2]);
            mc.resizeGui();
            mc.gui.setScreen(new FluxScreen());
          });
      context.waitTicks(5);
      context.takeScreenshot("flux-gui-scale-" + size[2]);
      context.runOnClient(
          mc ->
              check(
                  FluxTweaks.config.layout.navX >= 8 && FluxTweaks.config.layout.settingsY >= 8,
                  "Panels escaped the resized screen"));
    }
    context.runOnClient(
        mc -> {
          FluxTweaks.config.layout.selected = "hit_particles";
          mc.gui.setScreen(new FluxScreen());
        });
    context.waitTicks(6);
    context.takeScreenshot("flux-gui-choices");
    int previousCap = context.computeOnClient(mc -> mc.options.framerateLimit().get());
    for (int cap : new int[] {30, 60, 144}) {
      context.runOnClient(
          mc -> {
            mc.options.framerateLimit().set(cap);
            FluxTweaks.config.theme.reducedMotion = false;
            mc.gui.setScreen(new FluxScreen());
          });
      context.waitTicks(6);
      context.takeScreenshot("flux-gui-fps-cap-" + cap);
      context.runOnClient(mc -> mc.gui.screen().onClose());
      context.waitTicks(5);
      context.runOnClient(
          mc ->
              check(
                  !(mc.gui.screen() instanceof FluxScreen), "Screen closing failed at cap " + cap));
    }
    context.runOnClient(mc -> mc.options.framerateLimit().set(previousCap));
    context.runOnClient(
        mc -> {
          FluxTweaks.config = new dev.fluxtweaks.config.FluxConfig();
          mc.gui.setScreen(new FluxScreen());
        });
    context.waitTicks(5);
  }

  static void clickControl(ClientGameTestContext context, String label) {
    clickControlAt(context, label, .5);
  }

  static void clickControlAt(ClientGameTestContext context, String label, double fraction) {
    context.waitTicks(6);
    double[] point =
        context.computeOnClient(
            mc -> {
              try {
                var field = FluxScreen.class.getDeclaredField("zones");
                field.setAccessible(true);
                for (Object zone : (java.util.List<?>) field.get(mc.gui.screen())) {
                  Class<?> type = zone.getClass();
                  var name = type.getDeclaredMethod("label");
                  name.setAccessible(true);
                  if (!label.equals(name.invoke(zone))) continue;
                  double[] values = new double[4];
                  String[] keys = {"x", "y", "w", "h"};
                  for (int i = 0; i < keys.length; i++) {
                    var method = type.getDeclaredMethod(keys[i]);
                    method.setAccessible(true);
                    values[i] = ((Number) method.invoke(zone)).doubleValue();
                  }
                  return new double[] {values[0] + values[2] * fraction, values[1] + values[3] / 2};
                }
                throw new AssertionError("Missing visible GUI control: " + label);
              } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
              }
            });
    click(context, point[0], point[1]);
  }

  private static void click(ClientGameTestContext context, double x, double y) {
    context.waitTicks(1);
    context.runOnClient(
        mc -> {
          double scale = scale(mc);
          var e = new MouseButtonEvent(x * scale, y * scale, new MouseButtonInfo(0, 0));
          mc.gui.screen().mouseClicked(e, false);
          mc.gui.screen().mouseReleased(e);
        });
  }

  static void type(ClientGameTestContext context, String text) {
    context.runOnClient(
        mc -> {
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL));
          text.codePoints().forEach(c -> mc.gui.screen().charTyped(new CharacterEvent(c)));
          mc.gui.screen().keyPressed(key(GLFW.GLFW_KEY_ENTER, 0));
        });
  }

  private static KeyEvent key(int code, int modifiers) {
    return new KeyEvent(code, 0, modifiers);
  }

  private static double scale(Minecraft mc) {
    return dev.fluxtweaks.gui.Ui.scaleFor(
        mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
