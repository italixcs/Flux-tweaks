package dev.fluxtweaks.test;

import com.google.gson.JsonPrimitive;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import dev.fluxtweaks.gui.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.input.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
public final class FluxClientTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    context.getInput().resizeWindow(1280, 800);
    context.runOnClient(
        mc -> {
          FluxTweaks.config = new FluxConfig();
          FluxTweaks.message = "";
          mc.options.guiScale().set(2);
          mc.resizeDisplay();
        });
    context.setScreen(FluxScreen::new);
    context.waitForScreen(FluxScreen.class);
    context.waitTicks(5);
    GuiPolishAcceptance.alignment(context, context.takeScreenshot("flux-gui-default"));
    GuiAcceptance.run(context);
    GuiPolishAcceptance.scaleChecks(context);
    context.runOnClient(
        mc -> {
          float scale = scale(mc);
          double old = FluxTweaks.config.layout.navX;
          var screen = mc.screen;
          screen.mouseClicked(
              mouse((old + 35) * scale, (FluxTweaks.config.layout.navY + 20) * scale), false);
          screen.mouseDragged(
              mouse((old + 55) * scale, (FluxTweaks.config.layout.navY + 35) * scale),
              20 * scale,
              15 * scale);
          screen.mouseReleased(mouse(0, 0));
          check(FluxTweaks.config.layout.navX > old, "Navigation panel drag did not move it");
          FluxTweaks.config.layout.navX = 16;
          FluxTweaks.config.layout.navY = 20;
          mc.setScreen(null);
        });
    try (TestSingleplayerContext world = context.worldBuilder().create()) {
      world.getClientWorld().waitForChunksRender();
      FullbrightAcceptance.run(context, world);
      LowFireAcceptance.run(context, world);
      ModulesAcceptance.run(context, world);
      world
          .getServer()
          .runOnServer(
              server -> {
                for (var p : server.getPlayerList().getPlayers()) {
                  p.setGameMode(GameType.CREATIVE);
                  p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                  p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                }
              });
      context.waitTicks(20);
      context.runOnClient(
          mc -> {
            mc.player.setXRot(0);
            mc.player.setYRot(0);
            FluxTweaks.setEnabled(Feature.HAND_CHAMS, true);
            FluxTweaks.setEnabled(Feature.SHIELD_STATUS, true);
            FluxTweaks.setEnabled(Feature.VIEW_MODELS, true);
            FluxTweaks.config.set(Feature.HAND_CHAMS, "fill", new JsonPrimitive(.18));
          });
      context.waitTicks(20);
      context.runOnClient(
          mc ->
              check(
                  FluxTweaks.active(Feature.HAND_CHAMS),
                  "Hand Chams disabled after a rendering error"));
      context.runOnClient(mc -> FluxTweaks.setEnabled(Feature.HAND_CHAMS, false));
      context.waitTicks(3);
      Path vanillaHands = context.takeScreenshot("flux-hands-vanilla");
      context.runOnClient(mc -> FluxTweaks.setEnabled(Feature.HAND_CHAMS, true));
      context.waitTicks(3);
      Path chamsHands = context.takeScreenshot("flux-hands-shield");
      checkChamsCapture(vanillaHands, chamsHands);
      context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
      context.waitForScreen(FluxScreen.class);
      context.waitTicks(5);
      context.takeScreenshot("flux-gui-in-world");
      context.setScreen(() -> null);
      cameraChecks(context);
      itemChecks(context, world);
      context.runOnClient(
          mc -> {
            FluxTweaks.setEnabled(Feature.AMBIENCE, true);
            FluxTweaks.config.set(Feature.AMBIENCE, "time", new JsonPrimitive(true));
            FluxTweaks.config.set(Feature.AMBIENCE, "preset", new JsonPrimitive("Night"));
            FluxTweaks.config.set(Feature.AMBIENCE, "weather", new JsonPrimitive(true));
            FluxTweaks.config.set(Feature.AMBIENCE, "weatherType", new JsonPrimitive("Snow"));
            FluxTweaks.config.set(Feature.AMBIENCE, "fog", new JsonPrimitive("Custom"));
            FluxTweaks.setEnabled(Feature.JUMP_CIRCLE, true);
            FluxTweaks.setEnabled(Feature.HIT_PARTICLES, true);
          });
      world
          .getServer()
          .runCommand("execute at @a run summon minecraft:cow ~ ~ ~3 {NoAI:1b,Silent:1b}");
      context.waitTicks(10);
      for (String shape :
          List.of(
              "Stars",
              "Snowflakes",
              "Ghosts",
              "Hearts",
              "Diamonds",
              "Thunders",
              "Moons",
              "Clouds")) {
        context.runOnClient(
            mc -> {
              FluxTweaks.config.set(Feature.HIT_PARTICLES, "shape", new JsonPrimitive(shape));
              LivingEntity target =
                  mc
                      .level
                      .getEntitiesOfClass(
                          LivingEntity.class,
                          mc.player.getBoundingBox().inflate(8),
                          e -> e != mc.player)
                      .stream()
                      .findFirst()
                      .orElseThrow();
              mc.gameMode.attack(mc.player, target);
            });
        context.waitTicks(2);
      }
      for (String physics : List.of("Fall", "Rise", "Burst", "Fly")) {
        context.runOnClient(
            mc -> {
              FluxTweaks.config.set(Feature.HIT_PARTICLES, "physics", new JsonPrimitive(physics));
              var target =
                  mc.level
                      .getEntitiesOfClass(
                          LivingEntity.class,
                          mc.player.getBoundingBox().inflate(8),
                          e -> e != mc.player)
                      .getFirst();
              mc.gameMode.attack(mc.player, target);
            });
        context.waitTicks(3);
      }
      context.getInput().holdKeyFor(options -> options.keyJump, 2);
      context.waitTicks(2);
      context.takeScreenshot("flux-ambience-effects");
      context.runOnClient(
          mc -> {
            FluxTweaks.setEnabled(Feature.AMBIENCE, false);
            FluxTweaks.setEnabled(Feature.SPOTIFY, true);
          });
      context.waitTicks(30);
      context.takeScreenshot("flux-spotify");
      context.setScreen(() -> new HudEditorScreen(new FluxScreen()));
      context.waitTicks(4);
      context.takeScreenshot("flux-hud-editor");
      GuiPolishAcceptance.spotify(context);
      context.setScreen(() -> null);
      pngChecks(context);
      for (String style : List.of("Default", "Glowing", "Custom")) {
        context.runOnClient(
            mc -> FluxTweaks.config.set(Feature.JUMP_CIRCLE, "style", new JsonPrimitive(style)));
        context.getInput().holdKeyFor(options -> options.keyJump, 2);
        context.waitTicks(18);
      }
      context.runOnClient(
          mc -> {
            FluxTweaks.config.set(Feature.SHIELD_STATUS, "transition", new JsonPrimitive(false));
            ItemStack shield = mc.player.getOffhandItem();
            mc.player.getCooldowns().addCooldown(shield, 40);
            check(
                FluxTweaks.shields.tint(mc.player, shield)
                    == FluxTweaks.config.color(Feature.SHIELD_STATUS, "disabled"),
                "Exact own shield cooldown did not select disabled color");
          });
      context.runOnClient(mc -> FluxTweaks.setEnabled(Feature.FREECAM, true));
    }
    context.waitTicks(3);
    context.runOnClient(
        mc -> {
          check(
              !FluxTweaks.active(Feature.FREECAM) && !FluxTweaks.active(Feature.FREELOOK),
              "Camera state leaked across world exit");
          FluxTweaks.config = new FluxConfig();
          FluxTweaks.save();
        });
  }

  private static void checkChamsCapture(Path before, Path after) {
    try {
      var a = javax.imageio.ImageIO.read(before.toFile());
      var b = javax.imageio.ImageIO.read(after.toFile());
      int x = a.getWidth() / 2, y = a.getHeight() / 4;
      int skyA = a.getRGB(x, y), skyB = b.getRGB(x, y);
      int difference =
          Math.abs((skyA >> 16 & 255) - (skyB >> 16 & 255))
              + Math.abs((skyA >> 8 & 255) - (skyB >> 8 & 255))
              + Math.abs((skyA & 255) - (skyB & 255));
      check(difference < 30, "Hand Chams replaced the world background");
      int outline = 0;
      for (int py = a.getHeight() / 3; py < a.getHeight() - 40; py++)
        for (int px = 0; px < a.getWidth(); px++) {
          int old = a.getRGB(px, py), color = b.getRGB(px, py);
          int gain =
              (color >> 16 & 255)
                  + (color >> 8 & 255)
                  + (color & 255)
                  - (old >> 16 & 255)
                  - (old >> 8 & 255)
                  - (old & 255);
          if (gain > 120 && (color >> 8 & 255) > 160 && (color & 255) > 170) outline++;
        }
      check(outline > 100, "Hand Chams did not add a visible outline");
    } catch (java.io.IOException ex) {
      throw new AssertionError(ex);
    }
  }

  private static void itemChecks(ClientGameTestContext context, TestSingleplayerContext world) {
    for (HumanoidArm arm : HumanoidArm.values()) {
      context.runOnClient(
          mc -> {
            mc.options.mainHand().set(arm);
            mc.options.broadcastOptions();
          });
      context.waitTicks(5);
      for (Item item : List.of(Items.BOW, Items.APPLE, Items.SHIELD, Items.FILLED_MAP, Items.AIR)) {
        world
            .getServer()
            .runOnServer(
                server -> {
                  for (var p : server.getPlayerList().getPlayers()) {
                    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                    p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                  }
                });
        context.waitTicks(8);
        context.runOnClient(
            mc -> {
              FluxTweaks.config.set(Feature.VIEW_MODELS, "mainItem.posX", new JsonPrimitive(.08));
              FluxTweaks.config.set(Feature.VIEW_MODELS, "offItem.rotZ", new JsonPrimitive(8));
              FluxTweaks.config.set(Feature.VIEW_MODELS, "mainArm.scale", new JsonPrimitive(.9));
            });
        context.getInput().holdKeyFor(options -> options.keyUse, 8);
        context.waitTicks(3);
        context.runOnClient(
            mc ->
                check(
                    FluxTweaks.active(Feature.HAND_CHAMS),
                    "Chams failed during held-item animation"));
      }
    }
    context.runOnClient(
        mc -> {
          mc.options.mainHand().set(HumanoidArm.RIGHT);
          mc.options.broadcastOptions();
          for (String group : List.of("mainItem", "offItem", "mainArm", "offArm")) {
            FluxTweaks.config.set(Feature.VIEW_MODELS, group + ".scale", new JsonPrimitive(1));
            FluxTweaks.config.set(Feature.VIEW_MODELS, group + ".posX", new JsonPrimitive(0));
            FluxTweaks.config.set(Feature.VIEW_MODELS, group + ".rotZ", new JsonPrimitive(0));
          }
        });
    world
        .getServer()
        .runOnServer(
            server -> {
              for (var p : server.getPlayerList().getPlayers()) {
                p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
              }
            });
    context.waitTicks(10);
  }

  private static void cameraChecks(ClientGameTestContext context) {
    context.runOnClient(
        mc -> {
          FluxTweaks.keys
              .get(Feature.HAND_CHAMS)
              .setKey(
                  com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
                      GLFW.GLFW_KEY_B));
          KeyMapping.resetMapping();
        });
    context.getInput().pressKey(GLFW.GLFW_KEY_B);
    context.runOnClient(
        mc -> check(!FluxTweaks.active(Feature.HAND_CHAMS), "Feature key did not disable Chams"));
    context.getInput().pressKey(GLFW.GLFW_KEY_B);
    context.runOnClient(
        mc -> {
          check(FluxTweaks.active(Feature.HAND_CHAMS), "Feature key did not enable Chams");
          FluxTweaks.keys
              .get(Feature.HAND_CHAMS)
              .setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
          FluxTweaks.config.set(Feature.FREELOOK, "activation", new JsonPrimitive("Hold"));
          FluxTweaks.keys
              .get(Feature.FREELOOK)
              .setKey(
                  com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(
                      GLFW.GLFW_KEY_R));
          KeyMapping.resetMapping();
        });
    context.getInput().holdKey(GLFW.GLFW_KEY_R);
    context.waitTicks(2);
    context.runOnClient(
        mc -> check(FluxTweaks.active(Feature.FREELOOK), "Hold Freelook did not activate"));
    context.getInput().releaseKey(GLFW.GLFW_KEY_R);
    context.waitTicks(2);
    context.runOnClient(
        mc -> {
          check(!FluxTweaks.active(Feature.FREELOOK), "Hold Freelook did not release");
          FluxTweaks.config.set(Feature.FREELOOK, "activation", new JsonPrimitive("Toggle"));
          FluxTweaks.keys
              .get(Feature.FREELOOK)
              .setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
          KeyMapping.resetMapping();
        });
    CameraType original = context.computeOnClient(mc -> mc.options.getCameraType());
    float facing = context.computeOnClient(mc -> mc.player.getYRot());
    context.runOnClient(
        mc -> {
          FluxTweaks.setEnabled(Feature.FREELOOK, true);
          FluxTweaks.cameras.turn(400, 120);
          check(mc.player.getYRot() == facing, "Freelook changed player facing");
        });
    context.waitTicks(3);
    context.runOnClient(
        mc -> {
          check(
              mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK,
              "Freelook perspective incorrect");
          FluxTweaks.setEnabled(Feature.FREELOOK, false);
          check(mc.options.getCameraType() == original, "Freelook did not restore perspective");
          FluxTweaks.setEnabled(Feature.FREECAM, true);
        });
    Vec3 body = context.computeOnClient(mc -> mc.player.position());
    Vec3 camera = context.computeOnClient(mc -> mc.gameRenderer.getMainCamera().position());
    context.getInput().holdKeyFor(options -> options.keyUp, 10);
    context.waitTicks(2);
    context.runOnClient(
        mc -> {
          check(mc.player.position().distanceToSqr(body) < .02, "Freecam moved player body");
          check(
              mc.gameRenderer.getMainCamera().position().distanceToSqr(camera) > .1,
              "Freecam camera did not move");
          check(mc.player.getYRot() == facing, "Freecam changed player facing");
          FluxTweaks.setEnabled(Feature.FREELOOK, true);
          check(!FluxTweaks.active(Feature.FREECAM), "Camera modes are not mutually exclusive");
          FluxTweaks.setEnabled(Feature.FREELOOK, false);
          check(
              mc.options.getCameraType() == original, "Camera switch did not restore perspective");
        });
  }

  private static void pngChecks(ClientGameTestContext context) {
    context.runOnClient(
        mc -> {
          try {
            Path good = FluxTweaks.dataDirectory().resolve("test-valid.png");
            Files.createDirectories(good.getParent());
            try (var stream =
                FluxClientTest.class.getResourceAsStream(
                    "/assets/fluxtweaks/textures/particle/circle.png")) {
              Files.copy(Objects.requireNonNull(stream), good, StandardCopyOption.REPLACE_EXISTING);
            }
            FluxTweaks.message = "";
            FluxTweaks.effects.importPng(good);
          } catch (Exception ex) {
            throw new AssertionError(ex);
          }
        });
    context.waitFor(mc -> FluxTweaks.message.equals("Custom jump circle imported"));
    context.runOnClient(
        mc -> {
          try {
            Path bad = FluxTweaks.dataDirectory().resolve("test-invalid.png");
            Files.writeString(bad, "invalid image");
            FluxTweaks.message = "";
            FluxTweaks.effects.importPng(bad);
          } catch (Exception ex) {
            throw new AssertionError(ex);
          }
        });
    context.waitFor(mc -> FluxTweaks.message.equals("Choose a PNG image"));
    context.runOnClient(
        mc -> {
          try {
            check(
                Files.size(FluxTweaks.dataDirectory().resolve("jump-circle.png")) > 100,
                "Invalid import destroyed valid image");
          } catch (Exception ex) {
            throw new AssertionError(ex);
          }
        });
  }

  private static float scale(Minecraft mc) {
    return dev.fluxtweaks.gui.Ui.scaleFor(
        mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
  }

  private static MouseButtonEvent mouse(double x, double y) {
    return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
