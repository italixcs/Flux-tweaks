package dev.fluxtweaks.test;

import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.config.FluxConfig;
import dev.fluxtweaks.feature.ButterflyKnife;
import dev.fluxtweaks.gui.FluxScreen;
import dev.fluxtweaks.gui.Ui;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class ButterflyAcceptance {
  private static final Feature KNIFE = Feature.BUTTERFLY_KNIFE;
  private static final int ENABLE = GLFW.GLFW_KEY_F6;
  private static final int INSPECT = GLFW.GLFW_KEY_F7;

  static void run(ClientGameTestContext ctx, TestSingleplayerContext world) {
    var previous = ctx.computeOnClient(mc -> FluxTweaks.config);
    var arm = ctx.computeOnClient(mc -> mc.options.mainHand().get());
    var perspective = ctx.computeOnClient(mc -> mc.options.getCameraType());
    String enableKey = ctx.computeOnClient(mc -> FluxTweaks.enableKeys.get(KNIFE).saveString());
    String actionKey = ctx.computeOnClient(mc -> FluxTweaks.keys.get(KNIFE).saveString());
    try {
      world.getServer().runCommand("fill -16 64 8 16 85 8 minecraft:white_concrete");
      world.getServer().runCommand("tp @a 0.5 65 0.5 0 0");
      ctx.runOnClient(
          mc -> {
            FluxTweaks.config = new FluxConfig();
            FluxTweaks.config.notifications.enabled = false;
            FluxTweaks.notifications.clear();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.options.broadcastOptions();
            FluxTweaks.enableKeys.get(KNIFE).setKey(InputConstants.Type.KEYSYM.getOrCreate(ENABLE));
            FluxTweaks.keys.get(KNIFE).setKey(InputConstants.Type.KEYSYM.getOrCreate(INSPECT));
            KeyMapping.resetMapping();
            set("glint", false);
          });
      equip(ctx, world, Items.NETHERITE_SWORD, Items.SHIELD, false);
      Path vanilla = ctx.takeScreenshot("flux-butterfly-disabled");
      ctx.getInput().pressKey(INSPECT);
      ctx.runOnClient(mc -> check(!ButterflyKnife.inspecting(), "Disabled knife accepted inspect"));
      ctx.getInput().pressKey(ENABLE);
      ctx.runOnClient(mc -> check(FluxTweaks.active(KNIFE), "Knife enable binding did not enable it"));
      ctx.waitTicks(30);
      ctx.runOnClient(
          mc -> check(ButterflyKnife.snapshot(mc.player.getMainHandItem(),
              ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, mc.player) != null,
              "Equipped sword did not produce a Butterfly render snapshot"));
      Path steel = ctx.takeScreenshot("flux-butterfly-steel");
      check(differentPixels(vanilla, steel) > 100, "Enabling Butterfly did not replace the sword");

      for (String finish :
          List.of("Flux", "Sunset", "Violet Marble", "Emerald Marble", "Obsidian Red")) {
        ctx.runOnClient(mc -> set("finish", finish));
        ctx.waitTicks(3);
        Path image = ctx.takeScreenshot("flux-butterfly-" + slug(finish));
        check(differentPixels(steel, image) > 30, "Finish did not visibly change: " + finish);
      }
      ctx.runOnClient(mc -> set("finish", "Flux"));
      for (String style : List.of("Flip", "Spin", "Flourish")) {
        ctx.runOnClient(mc -> set("inspect", style));
        inspect(ctx);
        ctx.waitTicks(2);
        Path first = ctx.takeScreenshot("flux-butterfly-inspect-" + slug(style) + "-start");
        ctx.waitTicks(4);
        Path second = ctx.takeScreenshot("flux-butterfly-inspect-" + slug(style) + "-moving");
        check(differentPixels(first, second) > 15, "Inspect did not visibly move: " + style);
        ctx.waitFor(mc -> !ButterflyKnife.inspecting());
      }

      inspect(ctx);
      ctx.getInput().holdKeyFor(options -> options.keyAttack, 2);
      ctx.runOnClient(mc -> check(!ButterflyKnife.inspecting(), "Attack did not interrupt inspect"));
      ctx.waitTicks(15);
      inspect(ctx);
      ctx.getInput().holdKeyFor(options -> options.keyUse, 3);
      ctx.runOnClient(mc -> check(!ButterflyKnife.inspecting(), "Shield use did not interrupt inspect"));
      ctx.waitTicks(10);
      inspect(ctx);
      ctx.runOnClient(
          mc -> {
            FluxTweaks.config.layout.selected = KNIFE.id;
            mc.gui.setScreen(new FluxScreen());
          });
      ctx.waitTicks(3);
      ctx.runOnClient(mc -> check(!ButterflyKnife.inspecting(), "Opening settings did not cancel inspect"));
      ctx.takeScreenshot("flux-butterfly-settings");
      ctx.runOnClient(
          mc -> {
            float scale = Ui.scaleFor(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            mc.gui.screen().mouseScrolled((FluxTweaks.config.layout.settingsX + 100) * scale,
                (FluxTweaks.config.layout.settingsY + 220) * scale, 0, -20);
          });
      ctx.waitTicks(6);
      ctx.takeScreenshot("flux-butterfly-settings-scale-glint");
      ctx.setScreen(() -> null);
      ctx.waitTicks(3);

      for (double scale : new double[] {.5, 2}) {
        ctx.runOnClient(mc -> set("scale", scale));
        ctx.waitTicks(3);
        ctx.takeScreenshot("flux-butterfly-scale-" + (scale == .5 ? "minimum" : "maximum"));
      }
      ctx.runOnClient(
          mc -> {
            set("scale", 1);
            set("inspect", "Random");
            FluxTweaks.setEnabled(Feature.VIEW_MODELS, true);
            FluxTweaks.setEnabled(Feature.HAND_CHAMS, true);
            FluxTweaks.setEnabled(Feature.SHIELD_STATUS, true);
            FluxTweaks.config.set(Feature.VIEW_MODELS, "mainItem.posX", new JsonPrimitive(.08));
            FluxTweaks.config.set(Feature.VIEW_MODELS, "offItem.rotZ", new JsonPrimitive(8));
          });
      for (HumanoidArm mainArm : HumanoidArm.values()) {
        ctx.runOnClient(
            mc -> {
              mc.options.mainHand().set(mainArm);
              mc.options.broadcastOptions();
            });
        ctx.waitTicks(5);
        inspect(ctx);
        ctx.waitTicks(4);
        ctx.takeScreenshot("flux-butterfly-combined-" + slug(mainArm.name()));
        ctx.runOnClient(
            mc -> check(FluxTweaks.active(Feature.HAND_CHAMS), "Chams failed while rendering knife"));
        ctx.waitFor(mc -> !ButterflyKnife.inspecting());
      }
      equip(ctx, world, Items.SHIELD, Items.NETHERITE_SWORD, false);
      inspect(ctx);
      ctx.waitTicks(4);
      ctx.takeScreenshot("flux-butterfly-offhand-inspect");
      ctx.waitFor(mc -> !ButterflyKnife.inspecting());
      ctx.runOnClient(
          mc -> {
            FluxTweaks.setEnabled(Feature.VIEW_MODELS, false);
            FluxTweaks.setEnabled(Feature.HAND_CHAMS, false);
            FluxTweaks.setEnabled(Feature.SHIELD_STATUS, false);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.options.broadcastOptions();
          });
      equip(ctx, world, Items.NETHERITE_SWORD, Items.SHIELD, false);
      ctx.runOnClient(mc -> RenderCaptures.start());
      ctx.waitTicks(3);
      Path noGlint = ctx.takeScreenshot("flux-butterfly-shimmer-off");
      ctx.runOnClient(
          mc -> {
            check(RenderCaptures.entries.stream().noneMatch(e -> e.pipeline().contains("glint")),
                "Unenchanted knife shimmer remained enabled");
            RenderCaptures.start();
            set("glint", true);
          });
      ctx.waitTicks(3);
      Path glint = ctx.takeScreenshot("flux-butterfly-shimmer-on");
      check(differentPixels(noGlint, glint) > 10, "Optional knife shimmer was not visible");
      ctx.runOnClient(
          mc -> {
            check(RenderCaptures.entries.stream().anyMatch(e -> e.pipeline().contains("glint")),
                "Optional shimmer did not submit a glint pass");
            set("glint", false);
            RenderCaptures.enabled = false;
          });
      equip(ctx, world, Items.NETHERITE_SWORD, Items.SHIELD, true);
      ctx.runOnClient(mc -> RenderCaptures.start());
      ctx.waitTicks(3);
      ctx.takeScreenshot("flux-butterfly-enchanted-preserved-glint");
      ctx.runOnClient(
          mc -> {
            check(RenderCaptures.entries.stream().anyMatch(e -> e.pipeline().contains("glint")),
                "Disabling optional shimmer removed enchantment glint");
            RenderCaptures.enabled = false;
          });
      check(
          world.getServer().computeOnServer(
              server -> {
                var stack = server.getPlayerList().getPlayers().getFirst().getMainHandItem();
                return stack.is(Items.NETHERITE_SWORD) && stack.hasFoil() && stack.getDamageValue() == 7;
              }),
          "Cosmetic knife changed the server sword, enchantment, or durability");
      inspect(ctx);
      ctx.getInput().pressKey(ENABLE);
      ctx.runOnClient(
          mc -> check(!FluxTweaks.active(KNIFE) && !ButterflyKnife.inspecting(),
              "Disabling knife did not cancel inspect"));
      ctx.waitTicks(10);
      ctx.takeScreenshot("flux-butterfly-restored-sword");
      ctx.getInput().pressKey(ENABLE);
      equip(ctx, world, Items.APPLE, Items.SHIELD, false);
      ctx.getInput().pressKey(INSPECT);
      ctx.runOnClient(mc -> check(!ButterflyKnife.inspecting(), "Non-sword accepted inspect"));
      ctx.takeScreenshot("flux-butterfly-non-sword");
      FluxTweaks.LOGGER.info(
          "Butterfly checks passed: enable/inspect input, six finishes, motion/cancellation,"
              + " scale captures, handedness/offhand, combined modules, glint and unchanged server sword");
    } finally {
      ctx.getInput().releaseKey(ENABLE);
      ctx.getInput().releaseKey(INSPECT);
      ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
      ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
      ctx.runOnClient(
          mc -> {
            RenderCaptures.enabled = false;
            mc.gui.setScreen(null);
            FluxTweaks.setEnabled(KNIFE, false);
            FluxTweaks.setEnabled(Feature.HAND_CHAMS, false);
            FluxTweaks.config = previous;
            FluxTweaks.enableKeys.get(KNIFE).setKey(InputConstants.getKey(enableKey));
            FluxTweaks.keys.get(KNIFE).setKey(InputConstants.getKey(actionKey));
            KeyMapping.resetMapping();
            mc.options.mainHand().set(arm);
            mc.options.broadcastOptions();
            mc.options.setCameraType(perspective);
            FluxTweaks.notifications.clear();
          });
      world.getServer().runCommand("fill -16 64 8 16 85 8 minecraft:air");
      equip(ctx, world, Items.NETHERITE_SWORD, Items.SHIELD, false);
    }
  }

  private static void inspect(ClientGameTestContext ctx) {
    ctx.getInput().pressKey(INSPECT);
    ctx.runOnClient(mc -> check(ButterflyKnife.inspecting(), "Inspect key did not start animation"));
  }

  private static void equip(
      ClientGameTestContext ctx, TestSingleplayerContext world, Item main, Item off, boolean enchanted) {
    world.getServer().runOnServer(
        server -> {
          var player = server.getPlayerList().getPlayers().getFirst();
          player.setGameMode(GameType.CREATIVE);
          var stack = new ItemStack(main);
          if (enchanted) {
            stack.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS), 1);
            stack.setDamageValue(7);
          }
          player.setItemSlot(EquipmentSlot.MAINHAND, stack);
          player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(off));
        });
    ctx.waitFor(mc -> mc.player.getMainHandItem().is(main) && mc.player.getOffhandItem().is(off)
        && (!enchanted || mc.player.getMainHandItem().hasFoil()));
    ctx.waitTicks(30);
  }

  private static void set(String key, String value) {
    FluxTweaks.config.set(KNIFE, key, new JsonPrimitive(value));
  }

  private static void set(String key, double value) {
    FluxTweaks.config.set(KNIFE, key, new JsonPrimitive(value));
  }

  private static void set(String key, boolean value) {
    FluxTweaks.config.set(KNIFE, key, new JsonPrimitive(value));
  }

  private static String slug(String value) {
    return value.toLowerCase(Locale.ROOT).replace(' ', '-');
  }

  private static int differentPixels(Path before, Path after) {
    try {
      var a = ImageIO.read(before.toFile());
      var b = ImageIO.read(after.toFile());
      check(a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight(), "Capture size changed");
      int count = 0;
      // Ignore the hotbar and upper HUD; the wall keeps the item comparison background fixed.
      for (int y = a.getHeight() / 3; y < a.getHeight() - 70; y++)
        for (int x = a.getWidth() / 2; x < a.getWidth(); x++) {
          int old = a.getRGB(x, y), now = b.getRGB(x, y);
          int delta = Math.abs((old >> 16 & 255) - (now >> 16 & 255))
              + Math.abs((old >> 8 & 255) - (now >> 8 & 255))
              + Math.abs((old & 255) - (now & 255));
          if (delta > 35) count++;
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
