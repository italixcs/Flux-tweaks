package dev.fluxtweaks.test;

import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.feature.HitColor;
import dev.fluxtweaks.feature.Zoom;
import dev.fluxtweaks.gui.FluxScreen;
import dev.fluxtweaks.gui.Ui;
import dev.fluxtweaks.render.BlockOverlay;
import dev.fluxtweaks.test.mixin.KeyboardProbe;
import dev.fluxtweaks.test.mixin.RendererProbe;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.*;
import net.minecraft.client.input.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
final class ModulesAcceptance {
  private static final BlockPos TARGET = new BlockPos(0, 66, 3);
  private static final int ZOMBIE_ID = 991234;

  static void run(ClientGameTestContext ctx, TestSingleplayerContext world) {
    String selected = FluxTweaks.config.layout.selected;
    var perspective = ctx.computeOnClient(mc -> mc.options.getCameraType());
    try {
      world.getServer().runCommand("fill -10 64 -10 10 64 12 minecraft:stone");
      world.getServer().runCommand("tp @a 0.5 65 0.5 0 0");
      world.getServer().runCommand("setblock 0 66 8 minecraft:white_concrete");
      ctx.waitTicks(8);
      zoom(ctx, world);
      blocks(ctx, world);
      hits(ctx, world);
      for (Feature feature : List.of(Feature.ZOOM, Feature.BLOCK_OVERLAY, Feature.HIT_COLOR)) {
        ctx.runOnClient(
            mc -> {
              FluxTweaks.config.layout.selected = Feature.HAND_CHAMS.id;
              mc.setScreen(new FluxScreen());
              mc.screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_CONTROL));
              feature
                  .title
                  .codePoints()
                  .forEach(c -> mc.screen.charTyped(new CharacterEvent(c, 0)));
            });
        ctx.waitTicks(3);
        ctx.runOnClient(
            mc -> {
              double scale =
                  Ui.scaleFor(
                      mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
              var e =
                  new MouseButtonEvent(
                      (FluxTweaks.config.layout.navX + 40) * scale,
                      (FluxTweaks.config.layout.navY + 103) * scale,
                      new MouseButtonInfo(0, 0));
              mc.screen.mouseClicked(e, false);
              mc.screen.mouseReleased(e);
              check(
                  FluxTweaks.config.layout.selected.equals(feature.id),
                  "Search cannot select " + feature.title);
            });
        ctx.waitTicks(2);
        ctx.takeScreenshot("flux-014-settings-" + feature.id);
      }
      ctx.setScreen(() -> null);
      for (var feature : List.of(Feature.BLOCK_OVERLAY, Feature.HIT_COLOR)) {
        ctx.runOnClient(
            mc -> {
              FluxTweaks.setEnabled(feature, false);
              FluxTweaks.keys
                  .get(feature)
                  .setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K));
              KeyMapping.resetMapping();
            });
        ctx.getInput().pressKey(GLFW.GLFW_KEY_K);
        ctx.runOnClient(
            mc -> check(FluxTweaks.active(feature), "Module key did not enable " + feature.title));
        ctx.getInput().pressKey(GLFW.GLFW_KEY_K);
        ctx.runOnClient(
            mc -> {
              check(!FluxTweaks.active(feature), "Module key did not disable " + feature.title);
              FluxTweaks.keys.get(feature).setKey(InputConstants.UNKNOWN);
              KeyMapping.resetMapping();
            });
      }
      FluxTweaks.LOGGER.info(
          "0.1.4 checks passed: zoom FOV/input/cleanup, target shapes/colors/depth hooks, damage"
              + " body/armor/trim/glint/filtering");
    } finally {
      ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
      ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
      ctx.runOnClient(
          mc -> {
            RenderCaptures.enabled = false;
            mc.setWindowActive(true);
            mc.setScreen(null);
            mc.options.setCameraType(perspective);
            mc.level.removeEntity(ZOMBIE_ID, Entity.RemovalReason.DISCARDED);
            for (var feature : List.of(Feature.ZOOM, Feature.BLOCK_OVERLAY, Feature.HIT_COLOR))
              FluxTweaks.setEnabled(feature, false);
            FluxTweaks.keys.get(Feature.ZOOM).setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            FluxTweaks.config.layout.selected = selected;
            mc.player.hurtTime = 0;
            mc.player.setHealth(20);
            mc.player.setXRot(0);
            mc.player.setYRot(0);
            Zoom.refresh();
          });
      world.getServer().runCommand("setblock 0 66 3 minecraft:air");
      world.getServer().runCommand("setblock 0 66 8 minecraft:air");
      ctx.waitTicks(4);
    }
  }

  private static float fov(Minecraft mc, boolean world) {
    return ((RendererProbe) mc.gameRenderer).flux$fov(mc.gameRenderer.getMainCamera(), 1, world);
  }

  private static void zoom(ClientGameTestContext ctx, TestSingleplayerContext world) {
    int initialSlot = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
    double[] options =
        ctx.computeOnClient(
            mc -> new double[] {mc.options.fov().get(), mc.options.sensitivity().get()});
    ctx.runOnClient(
        mc -> {
          FluxTweaks.keys
              .get(Feature.ZOOM)
              .setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10));
          KeyMapping.resetMapping();
          FluxTweaks.setEnabled(Feature.ZOOM, true);
        });
    ctx.waitTicks(2);
    float original = ctx.computeOnClient(mc -> fov(mc, true));
    float hands = ctx.computeOnClient(mc -> fov(mc, false));
    ctx.takeScreenshot("flux-014-zoom-off");
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(6);
    ctx.runOnClient(
        mc -> {
          check(
              Zoom.held() && FluxTweaks.active(Feature.ZOOM),
              "Zoom key changed Enabled instead of holding zoom");
          check(fov(mc, true) < original / 3, "World FOV did not zoom");
          check(fov(mc, false) == hands, "Zoom changed hand FOV");
          check(Math.abs(Zoom.sensitivity() - .25) < .001, "Zoom sensitivity not scaled");
        });
    ctx.takeScreenshot("flux-014-zoom-on");
    ctx.getInput().scroll(1);
    ctx.waitTicks(5);
    ctx.runOnClient(
        mc -> {
          check(
              Math.abs(Zoom.sensitivity() - 1 / 4.8) < .002, "Wheel did not adjust magnification");
          check(
              mc.player.getInventory().getSelectedSlot() == initialSlot,
              "Zoom scroll changed hotbar");
          check(
              mc.options.fov().get() == options[0] && mc.options.sensitivity().get() == options[1],
              "Zoom overwrote Minecraft options");
          mc.setScreen(new FluxScreen());
          check(fov(mc, true) == original, "GUI did not cancel zoom immediately");
          mc.setScreen(null);
          check(!Zoom.held(), "Zoom restarted before a fresh press");
          FluxTweaks.keys.get(Feature.ZOOM).setDown(true);
          ((KeyboardProbe) mc.keyboardHandler)
              .flux$key(
                  mc.getWindow().handle(), GLFW.GLFW_REPEAT, new KeyEvent(GLFW.GLFW_KEY_F10, 0, 0));
          check(!Zoom.held(), "Restored key state or repeat bypassed the fresh-press gate");
        });
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(2);
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(5);
    ctx.runOnClient(
        mc -> {
          check(
              Math.abs(Zoom.sensitivity() - .25) < .001, "Zoom did not return to default strength");
          mc.setWindowActive(false);
          check(fov(mc, true) == original, "Focus loss did not cancel zoom");
          mc.setWindowActive(true);
          check(!Zoom.held(), "Focus regain reactivated held key");
        });
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(2);
    for (var camera : List.of(Feature.FREELOOK, Feature.FREECAM)) {
      ctx.runOnClient(mc -> FluxTweaks.setEnabled(camera, true));
      ctx.getInput().holdKey(GLFW.GLFW_KEY_F10);
      ctx.waitTicks(5);
      ctx.runOnClient(mc -> check(Zoom.held(), "Zoom did not work with " + camera.title));
      ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
      ctx.runOnClient(mc -> FluxTweaks.setEnabled(camera, false));
      ctx.waitTicks(5);
    }
    ctx.runOnClient(mc -> check(fov(mc, true) == original, "Zoom release did not restore FOV"));
    ctx.takeScreenshot("flux-014-zoom-restored");
    ctx.runOnClient(
        mc -> {
          FluxTweaks.keys
              .get(Feature.ZOOM)
              .setKey(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_4));
          KeyMapping.resetMapping();
        });
    ctx.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_4);
    ctx.waitTicks(5);
    ctx.runOnClient(mc -> check(Zoom.held(), "Mouse Zoom binding did not activate"));
    ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_4);
    ctx.waitTicks(5);
    ctx.runOnClient(
        mc -> {
          check(!Zoom.held() && Zoom.sensitivity() == 1, "Mouse release did not restore zoom");
          FluxTweaks.keys
              .get(Feature.ZOOM)
              .setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10));
          KeyMapping.resetMapping();
        });
    world
        .getServer()
        .runOnServer(
            server ->
                server
                    .getPlayerList()
                    .getPlayers()
                    .getFirst()
                    .setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS)));
    ctx.waitFor(mc -> mc.player.getMainHandItem().is(Items.SPYGLASS));
    ctx.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    ctx.waitFor(mc -> mc.player.isScoping());
    ctx.waitTicks(25);
    float scoped = ctx.computeOnClient(mc -> fov(mc, true));
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(6);
    ctx.runOnClient(
        mc ->
            check(
                fov(mc, true) >= 1 && fov(mc, true) < scoped / 2,
                "Zoom did not compose with spyglass"));
    ctx.takeScreenshot("flux-014-zoom-spyglass");
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(6);
    ctx.runOnClient(
        mc -> check(Math.abs(fov(mc, true) - scoped) < .02, "Spyglass FOV was not restored"));
    ctx.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    world
        .getServer()
        .runOnServer(
            server ->
                server
                    .getPlayerList()
                    .getPlayers()
                    .getFirst()
                    .setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
    ctx.waitTicks(25);
    ctx.getInput().holdKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(5);
    ctx.runOnClient(
        mc -> {
          mc.player.setHealth(0);
          check(!Zoom.held() && Zoom.sensitivity() == 1, "Death did not cancel zoom");
          mc.player.setHealth(20);
          check(!Zoom.held(), "Zoom restarted after death without fresh press");
        });
    ctx.getInput().releaseKey(GLFW.GLFW_KEY_F10);
    ctx.waitTicks(5);
    ctx.getInput().scroll(1);
    ctx.waitTicks(1);
    ctx.runOnClient(
        mc -> {
          check(
              mc.player.getInventory().getSelectedSlot() != initialSlot,
              "Normal hotbar scrolling did not return");
          mc.player.getInventory().setSelectedSlot(initialSlot);
          FluxTweaks.setEnabled(Feature.ZOOM, false);
        });
  }

  private static void blocks(ClientGameTestContext ctx, TestSingleplayerContext world) {
    ctx.runOnClient(
        mc -> {
          FluxTweaks.setEnabled(Feature.BLOCK_OVERLAY, true);
          set(Feature.BLOCK_OVERLAY, "outlineColor", 0xFF00FF);
          set(Feature.BLOCK_OVERLAY, "fillColor", 0x00FFFF);
          set(Feature.BLOCK_OVERLAY, "fillOpacity", 100);
        });
    world.getServer().runCommand("setblock 0 66 3 minecraft:stone");
    aim(ctx, .5);
    Path both = ctx.takeScreenshot("flux-014-block-both");
    check(
        pixels(both, true) > 1000 && pixels(both, false) > 100, "Block fill/outline not rendered");
    ctx.runOnClient(mc -> set(Feature.BLOCK_OVERLAY, "fill", false));
    ctx.waitTicks(2);
    Path outline = ctx.takeScreenshot("flux-014-block-outline");
    check(pixels(outline, true) == 0 && pixels(outline, false) > 100, "Outline-only switch failed");
    ctx.runOnClient(mc -> set(Feature.BLOCK_OVERLAY, "thickness", 6));
    ctx.waitTicks(2);
    Path thick = ctx.takeScreenshot("flux-014-block-thick");
    check(pixels(thick, false) > pixels(outline, false), "Outline thickness did not increase");
    ctx.runOnClient(
        mc -> {
          set(Feature.BLOCK_OVERLAY, "outline", false);
          set(Feature.BLOCK_OVERLAY, "fill", true);
        });
    ctx.waitTicks(2);
    Path fill = ctx.takeScreenshot("flux-014-block-fill");
    check(pixels(fill, true) > 1000 && pixels(fill, false) == 0, "Fill-only switch failed");
    ctx.runOnClient(mc -> set(Feature.BLOCK_OVERLAY, "fill", false));
    ctx.waitTicks(2);
    Path none = ctx.takeScreenshot("flux-014-block-hidden");
    check(pixels(none, true) == 0 && pixels(none, false) == 0, "Disabled parts still render");
    ctx.runOnClient(
        mc -> {
          set(Feature.BLOCK_OVERLAY, "fill", true);
          set(Feature.BLOCK_OVERLAY, "outline", true);
          set(Feature.BLOCK_OVERLAY, "thickness", 2);
        });
    for (var shape : List.of("oak_slab", "oak_stairs[facing=north]", "oak_fence", "dandelion")) {
      world.getServer().runCommand("setblock 0 66 3 minecraft:" + shape);
      aim(ctx, shape.contains("slab") ? .25 : .5);
      ctx.runOnClient(
          mc -> {
            var selection = mc.level.getBlockState(TARGET).getShape(mc.level, TARGET);
            check(!selection.isEmpty(), "Missing real selection shape");
            if (shape.contains("slab"))
              check(selection.bounds().getYsize() < 1, "Slab became full cube");
          });
      ctx.takeScreenshot("flux-014-block-shape-" + shape.split("\\[")[0]);
    }
    world.getServer().runCommand("setblock 0 66 3 minecraft:stone");
    aim(ctx, .5);
    ctx.runOnClient(mc -> FluxTweaks.setEnabled(Feature.BLOCK_OVERLAY, false));
    ctx.waitTicks(2);
    Path vanilla = ctx.takeScreenshot("flux-014-block-restored");
    check(
        pixels(vanilla, true) == 0 && pixels(vanilla, false) == 0, "Vanilla outline not restored");
    BlockOutlineRenderState[] forced = {null};
    WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(
        (context, state) -> {
          if (forced[0] == null) return true;
          FluxTweaks.config.module(Feature.BLOCK_OVERLAY).enabled = true;
          try {
            return BlockOverlay.render(context, forced[0]);
          } finally {
            FluxTweaks.config.module(Feature.BLOCK_OVERLAY).enabled = false;
          }
        });
    world.getServer().runCommand("setblock 0 66 2 minecraft:stone");
    ctx.waitFor(
        mc ->
            mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(TARGET.north()));
    ctx.runOnClient(
        mc -> forced[0] = new BlockOutlineRenderState(TARGET, false, false, Shapes.block()));
    try {
      ctx.waitTicks(3);
      Path occluded = ctx.takeScreenshot("flux-014-block-occluded");
      check(
          pixels(occluded, true) == 0 && pixels(occluded, false) == 0,
          "Overlay is visible through terrain");
    } finally {
      ctx.runOnClient(mc -> forced[0] = null);
    }
    world.getServer().runCommand("setblock 0 66 2 minecraft:air");
    ctx.runOnClient(
        mc -> {
          FluxTweaks.setEnabled(Feature.BLOCK_OVERLAY, true);
          set(Feature.BLOCK_OVERLAY, "outlineColor", 0x9B3DFF);
          set(Feature.BLOCK_OVERLAY, "fillColor", 0x00AFD8);
          set(Feature.BLOCK_OVERLAY, "fillOpacity", 20);
        });
    aim(ctx, .5);
    ctx.takeScreenshot("flux-014-block-defaults");
    ctx.runOnClient(mc -> mc.player.setXRot(-80));
    ctx.waitTicks(3);
    Path air = ctx.takeScreenshot("flux-014-block-air");
    check(pixels(air, true) == 0 && pixels(air, false) == 0, "Highlight did not clear on air");
    ctx.runOnClient(mc -> FluxTweaks.setEnabled(Feature.BLOCK_OVERLAY, false));
    world.getServer().runCommand("setblock 0 66 3 minecraft:air");
  }

  private static void aim(ClientGameTestContext ctx, double height) {
    ctx.waitTicks(4);
    ctx.runOnClient(
        mc -> {
          double dy = 66 + height - mc.player.getEyeY();
          mc.player.setYRot(0);
          mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, 3)));
        });
    ctx.waitFor(
        mc -> mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(TARGET));
    ctx.waitTicks(2);
  }

  private static void hits(ClientGameTestContext ctx, TestSingleplayerContext world) {
    ctx.runOnClient(
        mc -> {
          mc.player.setXRot(0);
          mc.player.setYRot(0);
          FluxTweaks.setEnabled(Feature.HIT_COLOR, true);
          var zombie = new Zombie(EntityType.ZOMBIE, mc.level);
          zombie.setId(ZOMBIE_ID);
          zombie.setNoAi(true);
          zombie.setNoGravity(true);
          zombie.snapTo(.5, 65, 3.5, 180, 0);
          zombie.hurtTime = 200;
          var chest = new ItemStack(Items.LEATHER_CHESTPLATE);
          chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x2244AA));
          chest.set(
              DataComponents.TRIM,
              new ArmorTrim(
                  mc.level
                      .registryAccess()
                      .lookupOrThrow(Registries.TRIM_MATERIAL)
                      .getOrThrow(TrimMaterials.GOLD),
                  mc.level
                      .registryAccess()
                      .lookupOrThrow(Registries.TRIM_PATTERN)
                      .getOrThrow(TrimPatterns.SENTRY)));
          chest.enchant(
              mc.level
                  .registryAccess()
                  .lookupOrThrow(Registries.ENCHANTMENT)
                  .getOrThrow(Enchantments.PROTECTION),
              1);
          zombie.setItemSlot(EquipmentSlot.CHEST, chest);
          zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
          zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
          zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
          zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
          zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
          mc.level.addEntity(zombie);
          RenderCaptures.start();
        });
    ctx.waitTicks(5);
    ctx.takeScreenshot("flux-014-hit-body-armor");
    ctx.runOnClient(
        mc -> {
          var entries = RenderCaptures.entries;
          check(
              entries.stream()
                  .anyMatch(
                      e -> e.hurt() && e.selected() && e.overlay() >> 16 == HitColor.OVERLAY_ROW),
              "Body did not use custom damage overlay");
          check(
              entries.stream()
                  .anyMatch(
                      e ->
                          e.pipeline().contains("hit_armor")
                              && e.overlay() >> 16 == HitColor.OVERLAY_ROW
                              && !e.trim()),
              "Armor not tinted");
          check(
              entries.stream().anyMatch(e -> e.pipeline().contains("hit_armor") && e.trim()),
              "Armor trim not tinted");
          check(
              entries.stream()
                  .anyMatch(e -> e.pipeline().contains("glint") && e.overlay() >> 16 == 10),
              "Enchantment glint changed");
          check(
              entries.stream()
                  .anyMatch(
                      e -> e.pipeline().contains("hit_armor") && (e.tint() & 0xffffff) == 0x2244AA),
              "Leather dye lost");
          check(
              entries.stream()
                  .noneMatch(e -> e.overlay() >> 16 == HitColor.OVERLAY_ROW && !e.hurt()),
              "Damage tint leaked into held items");
          var remote = new RemotePlayer(mc.level, mc.player.getGameProfile());
          remote.hurtTime = 10;
          var state =
              (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(remote, 1);
          check(HitColor.applies(state), "Players filter failed");
          set(Feature.HIT_COLOR, "players", false);
          state = (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(remote, 1);
          check(
              !HitColor.applies(state)
                  && LivingEntityRenderer.getOverlayCoords(state, 0) >> 16 == 3,
              "Excluded player lost vanilla red");
          set(Feature.HIT_COLOR, "players", true);
          mc.player.hurtTime = 10;
          state =
              (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(mc.player, 1);
          check(!HitColor.applies(state), "Self should default off");
          set(Feature.HIT_COLOR, "self", true);
          state =
              (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(mc.player, 1);
          check(HitColor.applies(state), "Self filter failed");
          mc.player.hurtTime = 0;
          set(Feature.HIT_COLOR, "self", false);
          set(Feature.HIT_COLOR, "mobs", false);
          RenderCaptures.start();
        });
    ctx.waitTicks(3);
    ctx.takeScreenshot("flux-014-hit-excluded");
    ctx.runOnClient(
        mc -> {
          check(
              RenderCaptures.entries.stream().noneMatch(e -> e.pipeline().contains("hit_armor")),
              "Excluded armor still tinted");
          check(
              RenderCaptures.entries.stream().anyMatch(e -> e.hurt() && e.overlay() >> 16 == 3),
              "Excluded mob lost vanilla red flash");
          set(Feature.HIT_COLOR, "mobs", true);
          var zombie = (Zombie) mc.level.getEntity(ZOMBIE_ID);
          zombie.hurtTime = 0;
          var state =
              (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(zombie, 1);
          check(!HitColor.applies(state), "Undamaged target acquired a flash");
          zombie.deathTime = 1;
          state = (LivingEntityRenderState) mc.getEntityRenderDispatcher().extractEntity(zombie, 1);
          check(HitColor.applies(state), "Vanilla death flash ignored");
          zombie.deathTime = 0;
          zombie.hurtTime = 200;
          FluxTweaks.setEnabled(Feature.HIT_COLOR, false);
          RenderCaptures.start();
        });
    ctx.waitTicks(3);
    ctx.takeScreenshot("flux-014-hit-restored");
    ctx.runOnClient(
        mc -> {
          check(
              RenderCaptures.entries.stream().noneMatch(e -> e.pipeline().contains("hit_armor")),
              "Disabled module still changes armor");
          check(
              RenderCaptures.entries.stream()
                  .noneMatch(e -> e.overlay() >> 16 == HitColor.OVERLAY_ROW),
              "Disabled module still changes overlays");
          FluxTweaks.setEnabled(Feature.HIT_COLOR, true);
          set(Feature.HIT_COLOR, "color", 0x00FF00);
          set(Feature.HIT_COLOR, "opacity", 100);
        });
    ctx.waitTicks(3);
    Path green = ctx.takeScreenshot("flux-014-hit-live-color");
    check(greenPixels(green) > 1000, "Live color/opacity did not update overlay texture");
    ctx.runOnClient(mc -> set(Feature.HIT_COLOR, "opacity", 0));
    ctx.waitTicks(3);
    Path transparent = ctx.takeScreenshot("flux-014-hit-transparent");
    check(
        greenPixels(transparent) < greenPixels(green) / 5,
        "Zero opacity did not remove custom tint");
    ctx.runOnClient(
        mc -> {
          set(Feature.HIT_COLOR, "color", 0x9B3DFF);
          set(Feature.HIT_COLOR, "opacity", 35);
          RenderCaptures.enabled = false;
          mc.level.removeEntity(ZOMBIE_ID, Entity.RemovalReason.DISCARDED);
        });
  }

  private static int greenPixels(Path path) {
    try {
      var image = ImageIO.read(path.toFile());
      int count = 0;
      for (int y = 0; y < image.getHeight(); y++)
        for (int x = 0; x < image.getWidth(); x++) {
          int rgb = image.getRGB(x, y);
          if ((rgb >> 16 & 255) < 30 && (rgb >> 8 & 255) > 120 && (rgb & 255) < 30) count++;
        }
      return count;
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  private static void set(Feature f, String key, Number value) {
    FluxTweaks.config.set(f, key, new JsonPrimitive(value));
  }

  private static void set(Feature f, String key, boolean value) {
    FluxTweaks.config.set(f, key, new JsonPrimitive(value));
  }

  private static int pixels(Path path, boolean cyan) {
    try {
      var image = ImageIO.read(path.toFile());
      int result = 0;
      for (int y = 0; y < image.getHeight(); y++)
        for (int x = 0; x < image.getWidth(); x++) {
          int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
          if (cyan ? r < 30 && g > 210 && b > 210 : r > 210 && g < 30 && b > 210) result++;
        }
      return result;
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
