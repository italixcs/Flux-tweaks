package dev.fluxtweaks.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.gui.FluxScreen;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class LowFireAcceptance {
  static void run(ClientGameTestContext context, TestSingleplayerContext world) {
    String selected = FluxTweaks.config.layout.selected;
    try {
      world
          .getServer()
          .runOnServer(
              server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(
                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0, false, false));
                player.igniteForSeconds(60);
              });
      context.runOnClient(
          mc -> {
            FluxTweaks.setEnabled(Feature.LOW_FIRE, false);
            FluxTweaks.keys
                .get(Feature.LOW_FIRE)
                .setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F9));
            KeyMapping.resetMapping();
          });
      context.waitFor(mc -> mc.player.isOnFire());
      context.waitTicks(5);
      Path normal = captureFlames(context, "flux-low-fire-off", 0, .65);
      context.getInput().pressKey(GLFW.GLFW_KEY_F9);
      context.waitTicks(5);
      context.runOnClient(
          mc -> check(FluxTweaks.active(Feature.LOW_FIRE), "Low Fire key did not enable it"));
      Path low = captureFlames(context, "flux-low-fire-on", .75, 1);
      check(
          world
              .getServer()
              .computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().isOnFire()),
          "Low Fire extinguished the player");
      context.runOnClient(
          mc -> {
            FluxTweaks.config.layout.selected = Feature.LOW_FIRE.id;
            mc.setScreen(new FluxScreen());
          });
      context.waitTicks(3);
      context.takeScreenshot("flux-low-fire-settings");
      context.setScreen(() -> null);
      context.getInput().pressKey(GLFW.GLFW_KEY_F9);
      context.waitTicks(5);
      context.runOnClient(
          mc -> check(!FluxTweaks.active(Feature.LOW_FIRE), "Low Fire key did not disable it"));
      Path restored = captureFlames(context, "flux-low-fire-restored", 0, .65);
      int before = firePixels(normal, 0, .65),
          after = firePixels(low, 0, .65),
          off = firePixels(restored, 0, .65);
      check(before > 500 && off > 500, "Normal fire overlay missing before or after toggling");
      check(after < before / 10, "Low Fire did not clear the upper view");
      check(firePixels(low, .75, 1) > 100, "Low Fire removed the burning indicator entirely");
      FluxTweaks.LOGGER.info(
          "Low Fire upper-view flame pixels: normal={}, low={}, restored={}", before, after, off);
    } finally {
      context.runOnClient(
          mc -> {
            mc.setScreen(null);
            FluxTweaks.setEnabled(Feature.LOW_FIRE, false);
            FluxTweaks.keys.get(Feature.LOW_FIRE).setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            FluxTweaks.config.layout.selected = selected;
          });
      world
          .getServer()
          .runOnServer(
              server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.clearFire();
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
                player.setGameMode(GameType.CREATIVE);
              });
      context.waitTicks(5);
    }
  }

  private static Path captureFlames(
      ClientGameTestContext context, String name, double top, double bottom) {
    Path best = null;
    int most = -1;
    // Sample the animation so a brief low flame frame cannot hide the burning indicator.
    for (int i = 0; i < 4; i++) {
      Path sample = context.takeScreenshot(name + "-sample-" + i);
      int pixels = firePixels(sample, top, bottom);
      if (pixels > most) {
        most = pixels;
        best = sample;
      }
      context.waitTicks(5);
    }
    return best;
  }

  private static int firePixels(Path path, double top, double bottom) {
    try {
      var image = ImageIO.read(path.toFile());
      int count = 0;
      for (int y = (int) (image.getHeight() * top); y < (int) (image.getHeight() * bottom); y++)
        for (int x = 0; x < image.getWidth() / 3; x++) {
          int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
          if (r > 100 && g > 40 && g < 240 && r > g * 1.25 && b < 80) count++;
        }
      return count;
    } catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
