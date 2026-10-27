package dev.fluxtweaks;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.config.*;
import dev.fluxtweaks.feature.*;
import dev.fluxtweaks.gui.*;
import dev.fluxtweaks.media.*;
import dev.fluxtweaks.render.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;
import org.slf4j.*;

public final class FluxTweaks implements ClientModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("Flux Tweaks");
  public static FluxConfig config = new FluxConfig();
  public static final Map<Feature, KeyMapping> keys = new EnumMap<>(Feature.class);
  public static final Map<Feature, KeyMapping> enableKeys = new EnumMap<>(Feature.class);
  private static final Map<Feature, ActionGate> actionGates = new EnumMap<>(Feature.class);
  private static final Map<Feature, String> actionBindings = new EnumMap<>(Feature.class);
  public static KeyMapping guiKey;
  public static CameraModes cameras;
  public static Effects effects;
  public static ShieldStatus shields;
  public static SpotifyService spotify;
  public static SpotifyHud hud;
  public static HandChams chams;
  public static NotificationHud notifications;
  public static String message = "";
  private static ConfigStore store;
  private static boolean dirty;
  private static int ticks;
  private ClientLevel lastLevel;

  public static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath("fluxtweaks", path);
  }

  public static Path dataDirectory() {
    return FabricLoader.getInstance().getConfigDir().resolve("fluxtweaks");
  }

  public static boolean active(Feature feature) {
    if (!config.enabled(feature)) return false;
    if (feature.camera()) return cameras != null && cameras.isActive(feature);
    return feature != Feature.ZOOM || Zoom.active();
  }

  public static boolean suppressMovement() {
    Minecraft mc = Minecraft.getInstance();
    return active(Feature.FREECAM)
        || mc.gui.screen() instanceof FluxScreen
        || mc.gui.screen() instanceof HudEditorScreen;
  }

  @Override
  public void onInitializeClient() {
    store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("fluxtweaks.json"));
    try {
      config = store.load();
    } catch (IOException ex) {
      LOGGER.error("Cannot read Flux Tweaks settings", ex);
      message = "Could not read saved settings";
    }
    KeyMapping.Category category = KeyMapping.Category.register(id("controls"));
    guiKey =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.fluxtweaks.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category));
    for (Feature f : Feature.values())
      keys.put(
          f,
          KeyMappingHelper.registerKeyMapping(
              new KeyMapping(
                  "key.fluxtweaks." + f.id, InputConstants.UNKNOWN.getValue(), category)));
    for (Feature f : Feature.values()) {
      enableKeys.put(
          f,
          f.hasAction()
              ? KeyMappingHelper.registerKeyMapping(
                  new KeyMapping(
                      "key.fluxtweaks.enable." + f.id, InputConstants.UNKNOWN.getValue(), category))
              : keys.get(f));
    }
    notifications = new NotificationHud();
    cameras = new CameraModes();
    effects = new Effects();
    shields = new ShieldStatus();
    spotify = new SpotifyService();
    hud = new SpotifyHud();
    chams = new HandChams();
    LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(BlockOverlay::render);
    ResourceLoader.get(PackType.CLIENT_RESOURCES)
        .registerReloadListener(
            id("hit_color"), (ResourceManagerReloadListener) manager -> HitColor.clearCache());
    ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
        id("butterfly"), (ResourceManagerReloadListener) manager -> ButterflyRenderer.reload());
    ClientTickEvents.START_CLIENT_TICK.register(
        mc -> {
          if (mc.level != lastLevel) {
            cameras.reset();
            Zoom.cancel();
            ButterflyKnife.reset();
            notifications.clear();
            effects.clear();
            shields.clear();
            lastLevel = mc.level;
          }
        });
    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    AttackEntityCallback.EVENT.register(
        (player, level, hand, target, hit) -> {
          if (level.isClientSide()
              && player == Minecraft.getInstance().player
              && target instanceof LivingEntity living
              && active(Feature.HIT_PARTICLES)
              && !active(Feature.FREECAM)) effects.hit(living);
          return InteractionResult.PASS;
        });
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.CHAT, id("spotify"), (graphics, delta) -> hud.render(graphics, false));
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.CHAT,
        id("notifications"),
        (graphics, delta) -> {
          if (!(Minecraft.getInstance().gui.screen() instanceof FluxScreen))
            notifications.render(graphics, false);
        });
    ClientLifecycleEvents.CLIENT_STOPPING.register(
        mc -> {
          save();
          spotify.close();
          hud.close();
          chams.close();
          effects.close();
          HitColor.clearCache();
          ButterflyKnife.reset();
          ButterflyRenderer.reload();
        });
    LOGGER.info(
        "Flux Tweaks initialized: Minecraft 26.2, all modules configurable with Right Shift");
  }

  private void tick(Minecraft mc) {
    while (guiKey.consumeClick()) {
      if (mc.gui.screen() instanceof FluxScreen || mc.gui.screen() instanceof HudEditorScreen)
        mc.gui.screen().onClose();
      else if (mc.gui.screen() == null) mc.gui.setScreen(new FluxScreen());
    }
    ButterflyKnife.tick();
    boolean editing = mc.gui.screen() != null || mc.gui.overlay() != null || !mc.isWindowActive();
    for (Feature f : Feature.values()) {
      KeyMapping enable = enableKeys.get(f);
      boolean toggled = false;
      while (enable.consumeClick()) toggled = true;
      if (toggled && !editing) setEnabled(f, !config.enabled(f));
      if (!f.hasAction()) continue;
      KeyMapping action = keys.get(f);
      boolean pressed = false;
      while (action.consumeClick()) pressed = true;
      if (f == Feature.BUTTERFLY_KNIFE) {
        ButterflyKnife.input(!toggled && !editing, action.isDown(), pressed, action.saveString());
        continue;
      }
      if (f == Feature.ZOOM) continue;
      var gate = actionGates.computeIfAbsent(f, ignored -> new ActionGate());
      String binding = action.saveString() + ":" + config.text(f, "activation");
      if (!binding.equals(actionBindings.put(f, binding))) {
        gate.block();
        setActive(f, false);
      }
      boolean available = config.enabled(f) && mc.player != null && mc.player.isAlive();
      boolean hold = config.text(f, "activation").equals("Hold");
      boolean wanted =
          gate.update(available && !toggled && !editing, action.isDown(), pressed, hold, active(f));
      if (available && editing && !hold) wanted = active(f);
      if (wanted != active(f)) setActive(f, wanted);
    }
    cameras.tick();
    Zoom.refresh();
    effects.tick();
    shields.tick();
    spotify.enabled(active(Feature.SPOTIFY));
    if (dirty && ++ticks % 20 == 0) save();
  }

  public static void setEnabled(Feature feature, boolean enabled) {
    if (config.enabled(feature) == enabled) return;
    if (!enabled && feature.camera() && cameras != null) cameras.change(feature, false);
    config.module(feature).enabled = enabled;
    if (feature.hasAction())
      actionGates.computeIfAbsent(feature, ignored -> new ActionGate()).block();
    if (feature == Feature.BUTTERFLY_KNIFE) ButterflyKnife.reset();
    if (feature == Feature.ZOOM) Zoom.refresh();
    if (feature == Feature.HAND_CHAMS && !enabled && chams != null) chams.close();
    if (notifications != null)
      notifications.push(
          feature, enabled ? NotificationQueue.State.ENABLED : NotificationQueue.State.DISABLED);
    changed();
  }

  public static void setActive(Feature feature, boolean active) {
    if (feature.camera() && cameras != null && (!active || config.enabled(feature)))
      cameras.change(feature, active);
  }

  public static void actionChanged(Feature feature, boolean active) {
    if (!active) actionGates.computeIfAbsent(feature, ignored -> new ActionGate()).block();
    if (notifications != null)
      notifications.push(
          feature,
          active ? NotificationQueue.State.ACTIVATED : NotificationQueue.State.DEACTIVATED);
  }

  public static void changed() {
    dirty = true;
  }

  public static void save() {
    if (store == null) return;
    try {
      store.save(config);
      dirty = false;
    } catch (IOException ex) {
      message = "Could not save settings";
      LOGGER.error(message, ex);
    }
  }
}
