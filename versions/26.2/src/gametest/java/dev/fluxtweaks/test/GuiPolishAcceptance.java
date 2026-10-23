package dev.fluxtweaks.test;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import dev.fluxtweaks.gui.*;
import dev.fluxtweaks.media.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Real packaged rendering/input; synthetic media frames never enter production code. */
@SuppressWarnings("UnstableApiUsage")
final class GuiPolishAcceptance {
  static void alignment(ClientGameTestContext context, Path screenshot) {
    double[] layout =
        context.computeOnClient(
            mc ->
                new double[] {
                  Ui.scaleFor(
                          mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight())
                      * mc.getWindow().getGuiScale(),
                  FluxTweaks.config.layout.navX,
                  FluxTweaks.config.layout.navY,
                  FluxTweaks.config.layout.settingsX,
                  FluxTweaks.config.layout.settingsY
                });
    double n = layout[1], ny = layout[2], s = layout[3], sy = layout[4];
    // Restrict to text, excluding icons/borders; measure the capital K in Keybind
    // so its descender (y) does not change a test of the shared capital-height baseline.
    double[][] rows = {
      {n + 28, ny + 17, 120, 20},
      {n + 40, ny + 67, 126, 32},
      {n + 21, ny + 111, 100, 30},
      {s + 28, sy + 76, 100, 38},
      {s + 20, sy + 122, 8, 30},
      {s + 230, sy + 254, 64, 26}
    };
    try {
      var image = javax.imageio.ImageIO.read(screenshot.toFile());
      for (int i = 0; i < rows.length; i++) {
        double[] r = rows[i];
        int top = image.getHeight(), bottom = -1;
        for (int y = (int) Math.ceil(r[1] * layout[0]); y < (r[1] + r[3]) * layout[0]; y++)
          for (int x = (int) Math.ceil(r[0] * layout[0]); x < (r[0] + r[2]) * layout[0]; x++) {
            int rgb = image.getRGB(x, y),
                red = rgb >> 16 & 255,
                green = rgb >> 8 & 255,
                blue = rgb & 255;
            if (Math.min(red, Math.min(green, blue)) > 155
                && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue))
                    < 45) {
              top = Math.min(top, y);
              bottom = Math.max(bottom, y);
            }
          }
        check(bottom >= top, "Missing text in alignment row " + i);
        double center = (top + bottom + 1) / 2.0 / layout[0], expected = r[1] + r[3] / 2;
        check(
            Math.abs(center - expected) <= 1.5,
            "Text is not centered in row " + i + ": " + center + " vs " + expected);
      }
    } catch (java.io.IOException ex) {
      throw new AssertionError(ex);
    }
  }

  static void scaleChecks(ClientGameTestContext context) {
    GuiAcceptance.clickControl(context, "Theme");
    GuiAcceptance.clickControl(context, "Type GUI Scale");
    GuiAcceptance.type(context, "125");
    context.waitTicks(5);
    context.runOnClient(
        mc -> check(FluxTweaks.config.theme.guiScale == 1.25, "Typed GUI percentage failed"));
    context.takeScreenshot("flux-theme-scale-125");
    // Real slider input after the transform changed, then another popup at that scale.
    GuiAcceptance.clickControlAt(context, "GUI Scale slider", 0);
    context.waitTicks(5);
    context.runOnClient(
        mc -> check(FluxTweaks.config.theme.guiScale == .5, "Minimum GUI scale failed"));
    context.takeScreenshot("flux-theme-scale-50");
    GuiAcceptance.clickControl(context, "Primary Color");
    GuiAcceptance.type(context, "#9B3DFF");
    GuiAcceptance.clickControlAt(context, "GUI Scale slider", .999);
    context.waitTicks(5);
    context.runOnClient(
        mc -> {
          check(FluxTweaks.config.theme.guiScale == 1.5, "Maximum GUI scale failed");
          FluxTweaks.save();
          try {
            var loaded =
                new ConfigStore(
                        net.fabricmc.loader.api.FabricLoader.getInstance()
                            .getConfigDir()
                            .resolve("fluxtweaks.json"))
                    .load();
            check(loaded.theme.guiScale == 1.5, "GUI scale was not persisted");
          } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
          }
        });
    context.takeScreenshot("flux-theme-scale-150");
    GuiAcceptance.clickControl(context, "Configure Hand Chams");
    GuiAcceptance.clickControl(context, "Toggle Hand Chams");
    context.runOnClient(
        mc -> check(FluxTweaks.active(Feature.HAND_CHAMS), "Scaled feature toggle missed"));
    // Search and both panels remain interactive after resizing at the largest preference.
    context.getInput().resizeWindow(854, 480);
    context.waitTicks(5);
    context.takeScreenshot("flux-gui-large-small-window");
    context.runOnClient(
        mc -> {
          mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_CONTROL));
          "zoom".codePoints().forEach(c -> mc.gui.screen().charTyped(new CharacterEvent(c)));
        });
    context.waitTicks(3);
    GuiAcceptance.clickControl(context, "Configure Zoom");
    context.runOnClient(
        mc -> {
          check(FluxTweaks.config.layout.selected.equals("zoom"), "Scaled search input missed");
          mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_HOME, 0, 0));
          check(FluxTweaks.config.theme.guiScale == 1, "Layout recovery did not restore GUI scale");
        });
    context.getInput().resizeWindow(1280, 800);
    context.runOnClient(
        mc -> {
          FluxTweaks.config = new FluxConfig();
          mc.gui.setScreen(new FluxScreen());
        });
    context.waitTicks(5);
  }

  static void spotify(ClientGameTestContext context) {
    SpotifyService real = context.computeOnClient(mc -> FluxTweaks.spotify);
    SpotifyService fixture = new SpotifyService();
    try {
      var worker = SpotifyService.class.getDeclaredField("worker");
      worker.setAccessible(true);
      var executor = (ScheduledExecutorService) worker.get(fixture);
      executor.shutdown();
      check(executor.awaitTermination(2, TimeUnit.SECONDS), "Fixture worker did not stop");
      var frameField = SpotifyService.class.getDeclaredField("frame");
      frameField.setAccessible(true);
      @SuppressWarnings("unchecked")
      var frames = (AtomicReference<SpotifyService.Frame>) frameField.get(fixture);
      var cover =
          new java.awt.image.BufferedImage(48, 48, java.awt.image.BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < 48; y++) for (int x = 0; x < 48; x++) cover.setRGB(x, y, 0xff00afd8);
      var bytes = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(cover, "png", bytes);
      frames.set(
          new SpotifyService.Frame(
              new SpotifyService.Track(
                  true,
                  "A very long song title that must stay inside the card",
                  "Björk · 宇多田ヒカル",
                  "gui-test-cover",
                  true,
                  85000,
                  240000,
                  true,
                  true,
                  true,
                  true,
                  1),
              System.nanoTime(),
              bytes.toByteArray()));
      context.runOnClient(
          mc -> {
            FluxTweaks.spotify = fixture;
            FluxTweaks.effects.clear();
            FluxTweaks.config.layout.hudX = .05;
            FluxTweaks.config.layout.hudY = .48;
            mc.gui.setScreen(new HudEditorScreen(new FluxScreen()));
          });
      context.waitTicks(8);
      Path capture = context.takeScreenshot("flux-spotify-redesign-playing");
      context.runOnClient(
          mc -> {
            try {
              var buttons = SpotifyHud.class.getDeclaredField("buttons");
              buttons.setAccessible(true);
              check(
                  ((java.util.List<?>) buttons.get(FluxTweaks.hud)).size() == 4,
                  "Playback commands missing");
              var b = FluxTweaks.hud.bounds();
              double x = b.x() + 70 * b.scale(), y = b.y() + 108 * b.scale();
              check(FluxTweaks.hud.controlAt(x, y), "Playback hit area missed redesigned button");
              FluxTweaks.config.theme.guiScale = 1.5;
              check(FluxTweaks.hud.bounds().equals(b), "Theme scale changed independent HUD scale");
              var image = javax.imageio.ImageIO.read(capture.toFile());
              int middle =
                  image.getRGB(
                      (int) ((b.x() + 38 * b.scale()) * mc.getWindow().getGuiScale()),
                      (int) ((b.y() + 38 * b.scale()) * mc.getWindow().getGuiScale()));
              // Read actual uploaded artwork, not just successful shader compilation.
              check(
                  (middle & 255) > 180 && (middle >> 16 & 255) < 50,
                  "Rounded cover did not render");
            } catch (ReflectiveOperationException | java.io.IOException ex) {
              throw new AssertionError(ex);
            }
          });
      for (int mode : new int[] {0, 1, 2}) {
        frames.set(
            new SpotifyService.Frame(
                new SpotifyService.Track(
                    true,
                    "Repeat " + (mode == 0 ? "off" : mode == 1 ? "one" : "all"),
                    "Flux Tweaks",
                    "gui-test-cover",
                    true,
                    85000,
                    240000,
                    true,
                    true,
                    true,
                    true,
                    mode),
                System.nanoTime(),
                bytes.toByteArray()));
        context.waitTicks(4);
        Path repeatCapture = context.takeScreenshot("flux-spotify-repeat-" + mode);
        context.runOnClient(
            mc -> {
              try {
                var image = javax.imageio.ImageIO.read(repeatCapture.toFile());
                var bounds = FluxTweaks.hud.bounds();
                double scale = bounds.scale() * mc.getWindow().getGuiScale();
                double ox = bounds.x() * mc.getWindow().getGuiScale(),
                    oy = bounds.y() * mc.getWindow().getGuiScale();
                int white = 0, accent = 0;
                for (int py = (int) (oy + 99 * scale); py < oy + 117 * scale; py++)
                  for (int px = (int) (ox + 181 * scale); px < ox + 199 * scale; px++) {
                    int rgb = image.getRGB(px, py),
                        red = rgb >> 16 & 255,
                        green = rgb >> 8 & 255,
                        blue = rgb & 255;
                    if (red > 180 && green > 180 && blue > 180) white++;
                    if (red < 80 && green > 100 && blue > 130) accent++;
                  }
                check(
                    mode == 0 ? white > 15 && accent == 0 : accent > 15 && white == 0,
                    "Repeat state color mismatch for mode " + mode);
              } catch (java.io.IOException ex) {
                throw new AssertionError(ex);
              }
            });
      }
      frames.set(
          new SpotifyService.Frame(
              new SpotifyService.Track(
                  true,
                  "Paused track",
                  "No artwork",
                  "gui-test-missing",
                  false,
                  30000,
                  240000,
                  false,
                  false,
                  true,
                  false,
                  0),
              System.nanoTime(),
              new byte[0]));
      context.waitTicks(6);
      context.takeScreenshot("flux-spotify-redesign-paused");
      context.runOnClient(
          mc -> {
            try {
              var buttons = SpotifyHud.class.getDeclaredField("buttons");
              buttons.setAccessible(true);
              check(
                  ((java.util.List<?>) buttons.get(FluxTweaks.hud)).size() == 1,
                  "Unsupported controls not hidden");
              var b = FluxTweaks.hud.bounds();
              var screen = mc.gui.screen();
              var from = new MouseButtonEvent(b.x() + 20, b.y() + 20, new MouseButtonInfo(0, 0));
              screen.mouseClicked(from, false);
              screen.mouseDragged(
                  new MouseButtonEvent(from.x() + 30, from.y() - 20, from.buttonInfo()), 30, -20);
              screen.mouseReleased(from);
              check(FluxTweaks.hud.bounds().x() > b.x(), "Redesigned HUD did not drag");
              screen.mouseScrolled(0, 0, 0, 1);
              check(
                  FluxTweaks.config.number(Feature.SPOTIFY, "scale") > 1,
                  "Independent HUD scale failed");
            } catch (ReflectiveOperationException ex) {
              throw new AssertionError(ex);
            }
          });
      frames.set(new SpotifyService.Frame(SpotifyService.Track.empty(), 0, new byte[0]));
      context.waitTicks(5);
      context.takeScreenshot("flux-spotify-redesign-preview");
    } catch (ReflectiveOperationException | java.io.IOException | InterruptedException ex) {
      throw new AssertionError(ex);
    } finally {
      context.runOnClient(
          mc -> {
            FluxTweaks.spotify = real;
            FluxTweaks.config.theme.guiScale = 1;
          });
      // The unopened fixture executor was already terminated before publishing its frames.
    }
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
