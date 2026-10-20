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
    click(context, 420, 110);
    context.runOnClient(mc -> mc.screen.keyPressed(key(GLFW.GLFW_KEY_B, 0)));
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.keys.get(Feature.HAND_CHAMS).matches(key(GLFW.GLFW_KEY_B, 0)),
                "GUI key assignment failed"));
    click(context, 420, 110);
    context.runOnClient(mc -> mc.screen.keyPressed(key(GLFW.GLFW_KEY_DELETE, 0)));

    // Numeric text and color hex use their real popups.
    click(context, 420, 192);
    type(context, "2.25");
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.number(Feature.HAND_CHAMS, "speed") == 2.25,
                "Typed slider value was not applied"));
    click(context, 425, 131);
    context.waitTicks(2);
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
                  py = (lh - 230) / 2 + 36;
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

    context.runOnClient(mc -> mc.screen.keyPressed(key(GLFW.GLFW_KEY_F, GLFW.GLFW_MOD_CONTROL)));
    context.runOnClient(
        mc -> "free".codePoints().forEach(c -> mc.screen.charTyped(new CharacterEvent(c, 0))));
    context.waitTicks(2);
    click(context, 65, 146);
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.layout.selected.equals("freecam"),
                "Search did not filter the feature list"));
    context.runOnClient(mc -> mc.screen.keyPressed(key(GLFW.GLFW_KEY_HOME, 0)));
    context.runOnClient(
        mc ->
            check(
                FluxTweaks.config.layout.navX == 16 && FluxTweaks.config.layout.settingsX == 198,
                "Layout reset failed"));

    for (int[] size : new int[][] {{854, 480, 1}, {1920, 1080, 3}, {1280, 800, 2}}) {
      context.getInput().resizeWindow(size[0], size[1]);
      context.runOnClient(
          mc -> {
            mc.options.guiScale().set(size[2]);
            mc.resizeDisplay();
            mc.setScreen(new FluxScreen());
          });
      context.waitTicks(2);
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
          mc.setScreen(new FluxScreen());
        });
    context.waitTicks(3);
    context.takeScreenshot("flux-gui-choices");
    context.runOnClient(
        mc -> {
          FluxTweaks.config = new dev.fluxtweaks.config.FluxConfig();
          mc.setScreen(new FluxScreen());
        });
    context.waitTicks(2);
  }

  private static void click(ClientGameTestContext context, double x, double y) {
    context.waitTicks(1);
    context.runOnClient(
        mc -> {
          double scale = scale(mc);
          var e = new MouseButtonEvent(x * scale, y * scale, new MouseButtonInfo(0, 0));
          mc.screen.mouseClicked(e, false);
          mc.screen.mouseReleased(e);
        });
  }

  private static void type(ClientGameTestContext context, String text) {
    context.runOnClient(
        mc -> {
          mc.screen.keyPressed(key(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL));
          text.codePoints().forEach(c -> mc.screen.charTyped(new CharacterEvent(c, 0)));
          mc.screen.keyPressed(key(GLFW.GLFW_KEY_ENTER, 0));
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
