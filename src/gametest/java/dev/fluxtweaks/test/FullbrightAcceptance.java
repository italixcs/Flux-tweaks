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
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class FullbrightAcceptance {
  static void run(ClientGameTestContext context, TestSingleplayerContext world) {
    double gamma = context.computeOnClient(mc -> mc.options.gamma().get());
    String selected = FluxTweaks.config.layout.selected;
    try {
      world.getServer().runCommand("gamemode creative @a");
      world.getServer().runCommand("execute at @a run fill ~-4 ~-1 ~-4 ~4 ~5 ~4 minecraft:stone");
      world.getServer().runCommand("execute at @a run fill ~-3 ~ ~-3 ~3 ~4 ~3 minecraft:air");
      context.runOnClient(
          mc -> {
            mc.options.gamma().set(0.0);
            mc.player.setYRot(0);
            mc.player.setXRot(0);
            FluxTweaks.setEnabled(Feature.FULLBRIGHT, false);
            FluxTweaks.keys
                .get(Feature.FULLBRIGHT)
                .setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8));
            KeyMapping.resetMapping();
          });
      context.waitTicks(25);
      context.waitFor(
          mc ->
              mc.level.getBrightness(
                      LightLayer.SKY, BlockPos.containing(mc.player.getEyePosition()))
                  == 0);
      Path dark = context.takeScreenshot("flux-fullbright-off");
      context.getInput().pressKey(GLFW.GLFW_KEY_F8);
      context.waitTicks(6);
      context.runOnClient(
          mc -> {
            check(FluxTweaks.active(Feature.FULLBRIGHT), "Fullbright key did not enable it");
            check(mc.options.gamma().get() == 0.0, "Fullbright changed the brightness option");
            check(
                !mc.player.hasEffect(MobEffects.NIGHT_VISION), "Fullbright added a potion effect");
          });
      Path bright = context.takeScreenshot("flux-fullbright-on");
      context.runOnClient(
          mc -> {
            FluxTweaks.config.layout.selected = Feature.FULLBRIGHT.id;
            mc.setScreen(new FluxScreen());
          });
      context.waitTicks(3);
      context.takeScreenshot("flux-fullbright-settings");
      context.setScreen(() -> null);
      context.getInput().pressKey(GLFW.GLFW_KEY_F8);
      context.waitTicks(6);
      context.runOnClient(
          mc -> check(!FluxTweaks.active(Feature.FULLBRIGHT), "Fullbright key did not disable it"));
      Path restored = context.takeScreenshot("flux-fullbright-restored");
      double before = luminance(dark), after = luminance(bright), off = luminance(restored);
      check(after > before + 20, "Fullbright did not brighten the enclosed room");
      check(
          Math.abs(off - before) < 5, "Normal lighting did not return after disabling Fullbright");
      FluxTweaks.LOGGER.info(
          "Fullbright capture checks: dark={}, bright={}, restored={}", before, after, off);
    } finally {
      context.runOnClient(
          mc -> {
            FluxTweaks.setEnabled(Feature.FULLBRIGHT, false);
            mc.options.gamma().set(gamma);
            FluxTweaks.keys.get(Feature.FULLBRIGHT).setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            FluxTweaks.config.layout.selected = selected;
          });
      world.getServer().runCommand("execute at @a run fill ~-4 ~ ~-4 ~4 ~5 ~4 minecraft:air");
      world
          .getServer()
          .runCommand("execute at @a run fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 minecraft:grass_block");
      context.waitTicks(15);
    }
  }

  private static double luminance(Path path) {
    try {
      var image = ImageIO.read(path.toFile());
      double total = 0;
      int count = 0;
      for (int y = image.getHeight() * 3 / 10; y < image.getHeight() * 4 / 10; y++)
        for (int x = image.getWidth() * 4 / 10; x < image.getWidth() * 6 / 10; x++) {
          int rgb = image.getRGB(x, y);
          total += .2126 * (rgb >> 16 & 255) + .7152 * (rgb >> 8 & 255) + .0722 * (rgb & 255);
          count++;
        }
      return total / count;
    } catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
