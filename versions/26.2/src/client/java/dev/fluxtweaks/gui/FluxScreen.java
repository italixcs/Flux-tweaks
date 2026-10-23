package dev.fluxtweaks.gui;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import java.awt.Color;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FluxScreen extends Screen {
  private record Entry(Setting setting, Supplier<JsonElement> read, Consumer<JsonElement> setter) {
    JsonElement value() {
      return read.get();
    }
  }

  private record Zone(
      String label, int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {}

  private final List<Zone> zones = new ArrayList<>();
  private final Map<String, IntConsumer> adjustments = new HashMap<>();
  private final Ui.Motion motion = new Ui.Motion();
  private Feature selected;
  private Entry choiceEntry;
  private int choiceFocus, savedFocus;
  private boolean closing;
  private float screenOffset;
  private boolean theme, settingsVisible = true, binding, bindingAction;
  private EditBox search, editor;
  private Consumer<String> editCommit;
  private IntConsumer colorCommit;
  private String editTitle = "", error = "";
  private float hue, saturation, value;
  private boolean colorPopup;
  private float scale = 1;
  private int logicalWidth,
      logicalHeight,
      navHeight,
      settingsHeight,
      focus = -1,
      settingsScroll,
      navScroll;
  private int dragPanel;
  private double dragOffsetX, dragOffsetY;
  private Zone slider;
  private String lastNarrated = "", lastMessage = "";
  private long messageSince;
  private int contentTop, contentBottom;

  public FluxScreen() {
    super(Component.literal("Flux Tweaks"));
    motion.snap("screen", 0);
    String id = FluxTweaks.config.layout.selected;
    selected =
        Arrays.stream(Feature.values())
            .filter(f -> f.id.equals(id))
            .findFirst()
            .orElse(Feature.HAND_CHAMS);
  }

  @Override
  protected void init() {
    updateViewport();
    String old = search == null ? "" : search.getValue();
    search = Ui.input(0, 0, 138, 32, "Search features");
    search.setBordered(false);
    search.setMaxLength(64);
    search.setHint(Ui.label("Search features…"));
    search.setTextColor(Ui.WHITE);
    search.setValue(old);
    if (editor != null) {
      editor.setX(popupX() + 14);
      editor.setY(popupY() + (colorPopup ? 174 : 33));
    }
  }

  private void updateViewport() {
    scale = Ui.scaleFor(width, height);
    logicalWidth = (int) (width / scale);
    logicalHeight = (int) (height / scale);
    navHeight = Math.min(520, logicalHeight - 32);
    settingsHeight = desiredSettingsHeight();
    clampPanels();
  }

  private String featureHint() {
    return switch (selected) {
      case HAND_CHAMS -> "Animated outlines for your hands and held items.";
      case VIEW_MODELS -> "Make your hands and items feel right.";
      case HIT_PARTICLES -> "A little feedback for every connected hit.";
      case JUMP_CIRCLE -> "Leave a fading ring where you take off.";
      case AMBIENCE -> "Set the atmosphere, just on your screen.";
      case SHIELD_STATUS -> "Remote shield cooldowns are estimates.";
      case FREELOOK -> "Look around while keeping your direction.";
      case FREECAM -> "Explore loaded terrain. Your body stays put.";
      case SPOTIFY -> FluxTweaks.spotify.status();
      case FULLBRIGHT -> "Keep dark places easy to see.";
      case LOW_FIRE -> "Keep the flames out of your line of sight.";
      case ZOOM -> "Hold your key to zoom. Scroll to adjust.";
      case BLOCK_OVERLAY -> "A clear outline of the block you are targeting.";
      case HIT_COLOR -> "Your own color for the damage flash.";
    };
  }

  private int desiredSettingsHeight() {
    return navHeight;
  }

  private void clampPanels() {
    var p = FluxTweaks.config.layout;
    p.navX = Math.clamp(p.navX, 8, Math.max(8, logicalWidth - 198));
    p.navY = Math.clamp(p.navY, 8, Math.max(8, logicalHeight - navHeight - 8));
    p.settingsX = Math.clamp(p.settingsX, 8, Math.max(8, logicalWidth - 328));
    p.settingsY = Math.clamp(p.settingsY, 8, Math.max(8, logicalHeight - settingsHeight - 8));
    // Recover the original default pair after the navigation panel became wider.
    if (p.navX == 16 && p.settingsX == 198) p.settingsX = 218;
  }

  private void zone(String label, int x, int y, int w, int h, Runnable action) {
    zones.add(new Zone(label, x, y, w, h, action, null));
  }

  private void clippedZone(
      String label, int x, int y, int w, int h, Runnable action, DoubleConsumer drag) {
    int top = Math.max(y, contentTop), bottom = Math.min(y + h, contentBottom);
    if (bottom > top) zones.add(new Zone(label, x, top, w, bottom - top, action, drag));
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    if (slider == null && !popup() && scale != Ui.scaleFor(width, height)) updateViewport();
    zones.clear();
    adjustments.clear();
    float appearance = motion.value("screen", closing ? 0 : 1, closing ? 140 : 220);
    if (closing && appearance == 0) {
      minecraft.gui.setScreen(null);
      return;
    }
    screenOffset = (1 - appearance) * 8;
    int mx = (int) (mouseX / scale), my = (int) (mouseY / scale - screenOffset);
    g.pose().pushMatrix();
    g.pose().scale(scale, scale);
    g.pose().translate(0, screenOffset);
    clampPanels();
    var p = FluxTweaks.config.layout;
    int nx = (int) p.navX, ny = (int) p.navY, sx = (int) p.settingsX, sy = (int) p.settingsY;
    Ui.surface(g, nx, ny, 190, navHeight);
    Ui.rounded(g, nx + 16, ny + 17, 4, 20, 2, Ui.primary());
    Ui.titleRow(g, "Flux Tweaks", nx + 28, ny + 17, 20, Ui.WHITE);
    long enabled =
        Arrays.stream(Feature.values()).filter(f -> FluxTweaks.config.enabled(f)).count();
    Ui.text(
        g,
        Feature.values().length + " features · " + enabled + " enabled",
        nx + 16,
        ny + 44,
        Ui.MUTED);
    zone(
        "Move feature panel",
        nx,
        ny,
        158,
        60,
        () -> {
          dragPanel = 1;
          dragOffsetX = mx - nx;
          dragOffsetY = my - ny;
        });
    Ui.close(g, nx + 168, ny + 22, Ui.MUTED);
    zone("Close Flux Tweaks", nx + 157, ny + 10, 26, 30, this::onClose);
    Ui.rounded(g, nx + 12, ny + 67, 166, 32, 7, Ui.inset());
    Ui.outline(g, nx + 12, ny + 67, 166, 32, 7, search.isFocused() ? Ui.secondary() : 0x22FFFFFF);
    Ui.outline(g, nx + 22, ny + 77, 8, 8, 4, Ui.MUTED);
    Ui.line(g, nx + 29, ny + 84, nx + 32, ny + 87, 1.2f, Ui.MUTED);
    search.setX(nx + 40);
    search.setY(ny + 67);
    search.extractRenderState(g, mx, my, delta);
    int listTop = ny + 111, listBottom = ny + navHeight - 90;
    List<Feature> visible =
        Arrays.stream(Feature.values())
            .filter(
                f ->
                    f.title
                        .toLowerCase(Locale.ROOT)
                        .contains(search.getValue().toLowerCase(Locale.ROOT)))
            .toList();
    navScroll = Math.clamp(navScroll, 0, Math.max(0, visible.size() * 32 - (listBottom - listTop)));
    g.enableScissor(nx + 8, listTop, nx + 182, listBottom);
    int yy = listTop - navScroll;
    for (Feature f : visible) {
      int row = yy;
      if (row + 30 > listTop && row < listBottom) {
        boolean current = !theme && f == selected;
        float hover = motion.value("nav." + f.id, Ui.inside(mx, my, nx + 10, row, 170, 30) ? 1 : 0);
        Ui.rounded(
            g,
            nx + 10,
            row,
            170,
            30,
            7,
            current
                ? Ui.alpha(FluxTweaks.config.theme.primary, .18f)
                : Ui.alpha(0xffffff, hover * .065f));
        if (current) Ui.rounded(g, nx + 10, row + 8, 3, 14, 1.5f, Ui.primary());
        ModuleIcons.draw(g, f, nx + 20, row + 8, 14, current ? Ui.WHITE : Ui.MUTED);
        Ui.row(g, Ui.fit(f.title, 100), nx + 42, row, 30, current ? Ui.WHITE : Ui.MUTED);
        String state =
            !FluxTweaks.config.enabled(f)
                ? "Off"
                : f.hasAction() ? (FluxTweaks.active(f) ? "Active" : "Ready") : "On";
        Ui.right(
            g,
            state,
            nx + 176,
            Ui.textY(row, 30),
            FluxTweaks.active(f) ? Ui.secondary() : Ui.MUTED);
        zone(
            "Configure " + f.title,
            nx + 10,
            Math.max(row, listTop),
            170,
            Math.min(row + 30, listBottom) - Math.max(row, listTop),
            () -> select(f));
      }
      yy += 32;
    }
    g.disableScissor();
    if (visible.isEmpty()) Ui.text(g, "No features found", nx + 21, listTop + 12, Ui.MUTED);
    if (visible.size() * 32 > listBottom - listTop) {
      float track = listBottom - listTop,
          thumb = Math.max(20, track * track / (visible.size() * 32));
      Ui.rounded(
          g,
          nx + 183,
          listTop + (track - thumb) * navScroll / Math.max(1, visible.size() * 32 - track),
          2,
          thumb,
          1,
          0x65FFFFFF);
    }
    g.fill(nx + 16, ny + navHeight - 78, nx + 174, ny + navHeight - 77, 0x20FFFFFF);
    textButton(
        g,
        "Theme",
        nx + 12,
        ny + navHeight - 68,
        166,
        28,
        mx,
        my,
        () -> {
          theme = true;
          settingsVisible = true;
          settingsScroll = 0;
          motion.snap("scroll", 0);
          binding = false;
        });
    textButton(
        g,
        "HUD Editor",
        nx + 12,
        ny + navHeight - 36,
        166,
        28,
        mx,
        my,
        () -> {
          FluxTweaks.save();
          minecraft.gui.setScreen(new HudEditorScreen(this));
        });
    if (settingsVisible) renderSettings(g, sx, sy, mx, my, delta);
    if (focus >= 0 && focus < zones.size() && !popup()) {
      Zone z = zones.get(focus);
      Ui.outline(g, z.x - 1, z.y - 1, z.w + 2, z.h + 2, 6, Ui.secondary());
    }
    if (!lastMessage.equals(FluxTweaks.message)) {
      lastMessage = FluxTweaks.message;
      messageSince = System.nanoTime();
    }
    if (!FluxTweaks.message.isBlank() && System.nanoTime() - messageSince < 4_000_000_000L) {
      Ui.rounded(
          g,
          8,
          logicalHeight - 30,
          Math.min(logicalWidth - 16, Ui.width(FluxTweaks.message) + 24),
          24,
          7,
          Ui.panel());
      Ui.text(g, Ui.fit(FluxTweaks.message, logicalWidth - 40), 20, logicalHeight - 22, Ui.WHITE);
    }
    if (popup()) {
      g.nextStratum();
      renderPopup(g, mx, my, delta);
    }
    g.pose().popMatrix();
    if (FluxTweaks.notifications != null) FluxTweaks.notifications.render(g, false);
  }

  private void select(Feature f) {
    selected = f;
    theme = false;
    settingsVisible = true;
    settingsScroll = 0;
    motion.snap("scroll", 0);
    binding = false;
    focus = -1;
    FluxTweaks.config.layout.selected = f.id;
    FluxTweaks.changed();
  }

  private void renderSettings(GuiGraphicsExtractor g, int x, int y, int mx, int my, float delta) {
    Ui.surface(g, x, y, 320, settingsHeight);
    if (!theme) ModuleIcons.draw(g, selected, x + 20, y + 21, 15, Ui.secondary());
    Ui.titleRow(
        g, theme ? "Make it yours" : selected.title, x + (theme ? 20 : 44), y + 17, 22, Ui.WHITE);
    Ui.text(
        g,
        Ui.fit(theme ? "Colors, scale and your workspace." : featureHint(), 278),
        x + 20,
        y + 46,
        Ui.MUTED);
    zone(
        "Move settings panel",
        x,
        y,
        278,
        62,
        () -> {
          dragPanel = 2;
          dragOffsetX = mx - x;
          dragOffsetY = my - y;
        });
    float hover = motion.value("close", Ui.inside(mx, my, x + 280, y + 12, 28, 28) ? 1 : 0);
    Ui.rounded(g, x + 280, y + 12, 28, 28, 7, Ui.alpha(0xffffff, hover * .08f));
    Ui.close(g, x + 291, y + 23, Ui.MUTED);
    zone("Close settings panel", x + 280, y + 12, 28, 28, () -> settingsVisible = false);
    int top = y + 76;
    if (!theme) {
      boolean on = FluxTweaks.config.enabled(selected);
      Ui.rounded(g, x + 16, top, 288, 38, 8, Ui.inset());
      Ui.row(g, on ? "Enabled" : "Disabled", x + 28, top, 38, Ui.WHITE);
      Ui.toggle(g, x + 265, top + 12, motion.value(selected.id + ".enabled", on ? 1 : 0));
      zone(
          "Toggle " + selected.title,
          x + 16,
          top,
          288,
          38,
          () -> FluxTweaks.setEnabled(selected, !FluxTweaks.config.enabled(selected)));
      top += 46;
      if (selected.hasAction() && on) {
        Ui.right(
            g,
            FluxTweaks.active(selected) ? "Active" : "Ready",
            x + 255,
            Ui.textY(y + 76, 38),
            FluxTweaks.active(selected) ? Ui.secondary() : Ui.MUTED);
      }
      renderKey(g, x, top, false);
      if (selected.hasAction()) {
        top += 38;
        renderKey(g, x, top, true);
      }
      top += 48;
    }
    contentTop = top;
    contentBottom = y + settingsHeight - 16;
    List<Entry> entries = entries();
    int total = entries.stream().mapToInt(e -> rowHeight(e.setting)).sum();
    settingsScroll =
        Math.clamp(settingsScroll, 0, Math.max(0, total - (contentBottom - contentTop)));
    g.enableScissor(x + 12, contentTop, x + 308, contentBottom);
    int scroll = Math.round(motion.value("scroll", settingsScroll, 140)), row = contentTop - scroll;
    if (entries.isEmpty()) {
      Ui.text(g, "Ready when you are.", x + 20, top + 20, Ui.WHITE);
      Ui.text(g, "Enable this feature or assign a shortcut.", x + 20, top + 42, Ui.MUTED);
    }
    for (Entry entry : entries) {
      int h = rowHeight(entry.setting);
      if (row + h > contentTop && row < contentBottom)
        renderEntry(g, entry, x + 20, row, 280, mx, my);
      row += h;
    }
    g.disableScissor();
    if (total > contentBottom - contentTop) {
      int track = contentBottom - contentTop, thumb = Math.max(20, track * track / total);
      int offset = (int) ((track - thumb) * scroll / (double) Math.max(1, total - track));
      Ui.rounded(g, x + 311, contentTop + offset, 3, thumb, 1.5f, Ui.alpha(0xffffff, .4f));
    }
  }

  private void renderKey(GuiGraphicsExtractor g, int x, int top, boolean action) {
    var keyMapping = action ? FluxTweaks.keys.get(selected) : FluxTweaks.enableKeys.get(selected);
    boolean waiting = binding && bindingAction == action;
    Ui.row(g, action ? "Action Key" : "Enable Key", x + 20, top, 30, Ui.MUTED);
    String key = waiting ? "Press a key…" : keyMapping.getTranslatedKeyMessage().getString();
    Ui.rounded(g, x + 148, top, 156, 30, 7, Ui.inset());
    Ui.outline(g, x + 148, top, 156, 30, 7, waiting ? Ui.secondary() : 0x20FFFFFF);
    Ui.right(g, Ui.fit(key, 136), x + 294, Ui.textY(top, 30), waiting ? Ui.secondary() : Ui.WHITE);
    String label =
        selected.hasAction()
            ? "Set " + selected.title + (action ? " Action key" : " Enable key")
            : "Set " + selected.title + " keybind";
    zone(
        label,
        x + 148,
        top,
        156,
        30,
        () -> {
          binding = true;
          bindingAction = action;
          search.setFocused(false);
        });
  }

  private List<Entry> entries() {
    if (theme) {
      var c = FluxTweaks.config;
      return List.of(
          new Entry(
              Setting.color("primary", "Primary Color", c.theme.primary),
              () -> new JsonPrimitive(c.theme.primary),
              v -> {
                c.theme.primary = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.color("secondary", "Secondary Color", c.theme.secondary),
              () -> new JsonPrimitive(c.theme.secondary),
              v -> {
                c.theme.secondary = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.color("background", "Background Color", c.theme.background),
              () -> new JsonPrimitive(c.theme.background),
              v -> {
                c.theme.background = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.number("transparency", "Background Transparency", .08, 0, 1, .01, "%"),
              () -> new JsonPrimitive(1 - c.theme.opacity),
              v -> {
                c.theme.opacity = 1 - v.getAsDouble();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.number("guiScale", "GUI Scale", 1, .5, 1.5, .01, "%"),
              () -> new JsonPrimitive(c.theme.guiScale),
              v -> {
                c.theme.guiScale = v.getAsDouble();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.bool("reducedMotion", "Reduced Motion", false),
              () -> new JsonPrimitive(c.theme.reducedMotion),
              v -> {
                c.theme.reducedMotion = v.getAsBoolean();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.bool("notifications", "Module Notifications", true),
              () -> new JsonPrimitive(c.notifications.enabled),
              v -> {
                c.notifications.enabled = v.getAsBoolean();
                if (!c.notifications.enabled) FluxTweaks.notifications.clear();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.action("reset", "Reset Layout"),
              () -> JsonNull.INSTANCE,
              v -> resetLayout()));
    }
    List<Entry> result = new ArrayList<>();
    Feature f = selected;
    for (Setting s : Settings.of(f)) {
      if (!visible(f, s.key())) continue;
      result.add(
          new Entry(
              s,
              () ->
                  s.kind() == Setting.Kind.HEADING || s.kind() == Setting.Kind.ACTION
                      ? JsonNull.INSTANCE
                      : FluxTweaks.config.value(f, s.key()),
              v -> {
                FluxTweaks.config.set(f, s.key(), v);
                FluxTweaks.changed();
              }));
    }
    return result;
  }

  private boolean visible(Feature f, String key) {
    if (f == Feature.JUMP_CIRCLE && key.equals("texture"))
      return FluxTweaks.config.text(f, "style").equals("Custom");
    if (f != Feature.AMBIENCE) return true;
    if (List.of("shape", "color", "amount", "size", "speed", "distance").contains(key))
      return FluxTweaks.config.bool(f, "particles");
    if (List.of("timeMode", "preset", "timeValue").contains(key)) {
      if (!FluxTweaks.config.bool(f, "time")) return false;
      if (key.equals("preset")) return FluxTweaks.config.text(f, "timeMode").equals("Presets");
      if (key.equals("timeValue")) return FluxTweaks.config.text(f, "timeMode").equals("Custom");
    }
    if (key.equals("weatherType")) return FluxTweaks.config.bool(f, "weather");
    if (List.of("fogColor", "fogRange", "sky").contains(key))
      return FluxTweaks.config.text(f, "fog").equals("Custom");
    return true;
  }

  private static int rowHeight(Setting s) {
    return switch (s.kind()) {
      case BOOLEAN, COLOR, ACTION -> 42;
      case NUMBER, RANGE -> 58;
      case CHOICE -> 62;
      case HEADING -> 42;
    };
  }

  private void renderEntry(
      GuiGraphicsExtractor g, Entry e, int x, int y, int width, int mx, int my) {
    Setting s = e.setting;
    if (s.kind() == Setting.Kind.BOOLEAN || s.kind() == Setting.Kind.COLOR) {
      float hover =
          motion.value(
              "row." + (theme ? "theme" : selected.id) + s.key(),
              Ui.inside(mx, my, x - 4, y, width + 8, 36) ? 1 : 0);
      Ui.rounded(g, x - 6, y, width + 12, 36, 7, Ui.alpha(0xffffff, hover * .045f));
    }
    switch (s.kind()) {
      case HEADING -> {
        g.fill(x, y + 5, x + width, y + 6, 0x18FFFFFF);
        Ui.title(g, s.label(), x, y + 16, Ui.WHITE);
      }
      case BOOLEAN -> {
        Ui.row(g, s.label(), x, y, 36, Ui.MUTED);
        Ui.toggle(
            g,
            x + width - 27,
            y + 11,
            motion.value(selected.id + "." + s.key(), e.value().getAsBoolean() ? 1 : 0));
        clippedZone(
            s.label(),
            x - 3,
            y,
            width + 6,
            36,
            () -> e.setter.accept(new JsonPrimitive(!e.value().getAsBoolean())),
            null);
      }
      case COLOR -> {
        Ui.row(g, s.label(), x, y, 36, Ui.MUTED);
        Ui.right(
            g,
            String.format(Locale.ROOT, "#%06X", e.value().getAsInt() & 0xffffff),
            x + width - 32,
            Ui.textY(y, 36),
            Ui.MUTED);
        Ui.rounded(g, x + width - 22, y + 7, 22, 22, 6, e.value().getAsInt() | 0xff000000);
        Ui.outline(g, x + width - 22, y + 7, 22, 22, 6, 0x50FFFFFF);
        clippedZone(
            s.label(),
            x - 3,
            y,
            width + 6,
            36,
            () ->
                openColor(
                    s.label(),
                    e.value().getAsInt(),
                    color -> e.setter.accept(new JsonPrimitive(color))),
            null);
      }
      case NUMBER -> {
        double n = e.value().getAsDouble();
        Ui.text(g, Ui.fit(s.label(), width - 102), x, y + 8, Ui.MUTED);
        String formatted = format(n, s);
        Ui.rounded(g, x + width - 92, y, 92, 26, 6, Ui.inset());
        Ui.right(g, formatted, x + width - 9, y + 8, Ui.WHITE);
        boolean percent = theme && (s.key().equals("transparency") || s.key().equals("guiScale"));
        clippedZone(
            "Type " + s.label(),
            x + width - 84,
            y,
            84,
            26,
            () ->
                openEditor(
                    s.label() + (percent ? " (%)" : ""),
                    Double.toString(percent ? n * 100 : n),
                    text -> {
                      double typed = Double.parseDouble(text.trim());
                      if (!Double.isFinite(typed)) throw new IllegalArgumentException();
                      e.setter.accept(s.validate(new JsonPrimitive(percent ? typed / 100 : typed)));
                    }),
            null);
        int barY = y + 38;
        float t = (float) ((n - s.min()) / (s.max() - s.min()));
        String key = selected.id + ".slider." + s.key();
        if (slider != null) motion.snap(key, t);
        else t = motion.value(key, t);
        Ui.rounded(g, x, barY, width, 4, 2, 0xFF64707D);
        Ui.rounded(g, x, barY, Math.max(0, width * t), 4, 2, Ui.primary());
        thumb(g, x + width * t, barY + 2, Ui.inside(mx, my, x, y + 28, width, 24));
        DoubleConsumer set =
            mouse ->
                e.setter.accept(
                    new JsonPrimitive(
                        s.bounded(
                            s.min()
                                + Math.clamp((mouse - x) / width, 0, 1) * (s.max() - s.min()))));
        clippedZone(s.label() + " slider", x, y + 28, width, 24, () -> set.accept(mx), set);
        adjustments.put(
            s.label() + " slider",
            direction ->
                e.setter.accept(
                    new JsonPrimitive(s.bounded(e.value().getAsDouble() + direction * s.step()))));
      }
      case RANGE -> {
        var a = e.value().getAsJsonArray();
        double lo = a.get(0).getAsDouble(), hi = a.get(1).getAsDouble();
        Ui.text(g, Ui.fit(s.label(), width - 102), x, y + 8, Ui.MUTED);
        Ui.rounded(g, x + width - 112, y, 112, 26, 6, Ui.inset());
        Ui.right(
            g,
            String.format(Locale.ROOT, "%.0f – %.0f %s", lo, hi, s.unit()),
            x + width - 9,
            y + 8,
            Ui.WHITE);
        clippedZone(
            "Type " + s.label() + " range",
            x + width - 112,
            y,
            112,
            26,
            () ->
                openEditor(
                    s.label() + " (min, max)",
                    lo + ", " + hi,
                    text -> {
                      String[] parts = text.trim().split("[,; ]+");
                      if (parts.length != 2) throw new IllegalArgumentException();
                      JsonArray range = new JsonArray();
                      range.add(Double.parseDouble(parts[0]));
                      range.add(Double.parseDouble(parts[1]));
                      e.setter.accept(s.validate(range));
                    }),
            null);
        int by = y + 38;
        float l = (float) ((lo - s.min()) / (s.max() - s.min())),
            h = (float) ((hi - s.min()) / (s.max() - s.min()));
        Ui.rounded(g, x, by, width, 4, 2, 0xFF64707D);
        Ui.rounded(g, x + l * width, by, Math.max(0, (h - l) * width), 4, 2, Ui.primary());
        thumb(g, x + l * width, by + 2, Ui.inside(mx, my, x, y + 28, width, 24));
        thumb(g, x + h * width, by + 2, Ui.inside(mx, my, x, y + 28, width, 24));
        int[] handle = {-1};
        DoubleConsumer set =
            mouse -> {
              var current = e.value().getAsJsonArray();
              double low = current.get(0).getAsDouble(), high = current.get(1).getAsDouble();
              if (handle[0] == -1) {
                double point =
                    s.min() + Math.clamp((mouse - x) / width, 0, 1) * (s.max() - s.min());
                handle[0] = Math.abs(point - low) <= Math.abs(point - high) ? 0 : 1;
              }
              boolean left = handle[0] == 0;
              double n =
                  s.bounded(s.min() + Math.clamp((mouse - x) / width, 0, 1) * (s.max() - s.min()));
              JsonArray next = new JsonArray();
              next.add(left ? Math.min(n, high) : low);
              next.add(left ? high : Math.max(n, low));
              e.setter.accept(next);
            };
        clippedZone(s.label() + " range slider", x, y + 28, width, 24, () -> set.accept(mx), set);
      }
      case CHOICE -> {
        Ui.text(g, s.label(), x, y + 4, Ui.MUTED);
        Ui.rounded(g, x, y + 23, width, 30, 7, Ui.inset());
        Ui.outline(g, x, y + 23, width, 30, 7, 0x25FFFFFF);
        Ui.row(g, e.value().getAsString(), x + 12, y + 23, 30, Ui.WHITE);
        Ui.chevron(g, x + width - 20, y + 36, false, Ui.MUTED);
        clippedZone(
            s.label() + " choices",
            x,
            y + 23,
            width,
            30,
            () -> {
              savedFocus = focus;
              choiceEntry = e;
              choiceFocus = Math.max(0, s.choices().indexOf(e.value().getAsString()));
              search.setFocused(false);
              binding = false;
              motion.snap("popup", 0);
            },
            null);
      }
      case ACTION -> {
        Ui.rounded(g, x, y + 2, width, 32, 7, Ui.inset());
        Ui.row(g, s.label(), x + 12, y + 2, 32, Ui.WHITE);
        clippedZone(s.label(), x, y, width, 36, () -> action(s.key()), null);
      }
    }
  }

  private void thumb(GuiGraphicsExtractor g, float x, float y, boolean hover) {
    float amount = hover ? 1 : 0;
    if (amount > 0)
      Ui.circle(g, x - 7, y - 7, 14, Ui.alpha(FluxTweaks.config.theme.primary, .14f * amount));
    Ui.circle(g, x - 5, y - 5, 10, Ui.WHITE);
  }

  private String format(double n, Setting s) {
    if (theme && (s.key().equals("transparency") || s.key().equals("guiScale")))
      return String.format(Locale.ROOT, "%.0f%%", n * 100);
    String number =
        s.step() >= 1
            ? String.format(Locale.ROOT, "%.0f", n)
            : String.format(Locale.ROOT, "%.2f", n).replaceAll("0+$", "").replaceAll("\\.$", "");
    return number + (s.unit().isEmpty() ? "" : " " + s.unit());
  }

  private void action(String key) {
    switch (key) {
      case "hud" -> minecraft.gui.setScreen(new HudEditorScreen(this));
      case "reset" -> resetLayout();
      case "texture" -> {
        String file =
            TinyFileDialogs.tinyfd_openFileDialog(
                "Select a jump-circle PNG", null, null, "PNG image", false);
        if (file != null) FluxTweaks.effects.importPng(Path.of(file));
      }
      default -> {}
    }
  }

  private void resetLayout() {
    FluxTweaks.config.theme.guiScale = 1;
    updateViewport();
    var p = FluxTweaks.config.layout;
    p.navX = 16;
    p.navY = 16;
    p.settingsX = 218;
    p.settingsY = 16;
    p.hudX = .03;
    p.hudY = .78;
    FluxTweaks.config.notifications.x = .66;
    FluxTweaks.config.notifications.y = .06;
    clampPanels();
    FluxTweaks.changed();
  }

  private void textButton(
      GuiGraphicsExtractor g,
      String text,
      int x,
      int y,
      int w,
      int h,
      int mx,
      int my,
      Runnable action) {
    float hover = motion.value("button." + text, Ui.inside(mx, my, x, y, w, h) ? 1 : 0);
    Ui.rounded(g, x, y, w, h, 6, Ui.alpha(0xffffff, hover * .06f));
    Ui.row(g, text, x + 10, y, h, Ui.MUTED);
    zone(text, x, y, w, h, action);
  }

  private boolean popup() {
    return editor != null || choiceEntry != null;
  }

  private int popupX() {
    return (logicalWidth - 236) / 2;
  }

  private int popupHeight() {
    return choiceEntry != null
        ? 64 + choiceEntry.setting.choices().size() * 28
        : colorPopup ? 280 : 160;
  }

  private int popupY() {
    return (logicalHeight - popupHeight()) / 2;
  }

  private void closePopup() {
    editor = null;
    choiceEntry = null;
    focus = savedFocus;
  }

  private void applyPopup() {
    try {
      if (colorPopup) {
        String hex = editor.getValue().trim().replace("#", "");
        if (hex.length() != 6) throw new IllegalArgumentException();
        colorCommit.accept(Integer.parseInt(hex, 16));
      } else editCommit.accept(editor.getValue());
      closePopup();
    } catch (RuntimeException ex) {
      error = colorPopup ? "Enter six hex digits" : "Enter a valid number or range";
    }
  }

  private void openEditor(String title, String initial, Consumer<String> commit) {
    colorPopup = false;
    editTitle = title;
    editCommit = commit;
    createEditor(initial, popupX() + 14, popupY() + 33, 208);
  }

  private void createEditor(String initial, int x, int y, int w) {
    savedFocus = focus;
    search.setFocused(false);
    binding = false;
    motion.snap("popup", 0);
    editor = Ui.input(x, y, w, 25, editTitle);
    editor.setMaxLength(128);
    editor.setValue(initial);
    editor.setFocused(true);
    editor.setHighlightPos(0);
    error = "";
  }

  private void openColor(String title, int rgb, IntConsumer commit) {
    colorPopup = true;
    editTitle = title;
    colorCommit = commit;
    float[] hsb = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
    hue = hsb[0];
    saturation = hsb[1];
    value = hsb[2];
    createEditor(
        String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF), popupX() + 14, popupY() + 174, 208);
    editor.setResponder(
        text -> {
          try {
            String hex = text.trim().replace("#", "");
            if (hex.length() == 6) colorCommit.accept(Integer.parseInt(hex, 16));
          } catch (NumberFormatException ignored) {
          }
        });
  }

  private void renderPopup(GuiGraphicsExtractor g, int mx, int my, float delta) {
    g.fill(0, 0, logicalWidth, logicalHeight, 0x60000000);
    int x = popupX(), y = popupY();
    Ui.surface(g, x, y, 236, popupHeight());
    float arrival = motion.value("popup", 1, 160);
    Ui.outline(
        g,
        x,
        y,
        236,
        popupHeight(),
        12,
        Ui.alpha(FluxTweaks.config.theme.secondary, arrival * .4f));
    if (choiceEntry != null) {
      Ui.title(g, choiceEntry.setting.label(), x + 14, y + 16, Ui.WHITE);
      for (int i = 0; i < choiceEntry.setting.choices().size(); i++) {
        String option = choiceEntry.setting.choices().get(i);
        int row = y + 48 + i * 28;
        boolean on = option.equals(choiceEntry.value().getAsString());
        if (choiceFocus == i || Ui.inside(mx, my, x + 8, row, 220, 28))
          Ui.rounded(g, x + 8, row, 220, 28, 6, Ui.raised());
        if (on) Ui.check(g, x + 17, row + 10, Ui.secondary());
        Ui.row(g, option, x + 36, row, 28, on ? Ui.WHITE : Ui.MUTED);
      }
      return;
    }
    Ui.title(g, Ui.fitTitle(editTitle, 208), x + 14, y + 14, Ui.WHITE);
    if (colorPopup) {
      int rgb = Color.HSBtoRGB(hue, 1, 1);
      SmoothGui.box(g, SmoothGui.SATURATION_VALUE, x + 14, y + 36, 192, 120, 5, rgb);
      SmoothGui.box(g, SmoothGui.HUE, x + 214, y + 36, 10, 120, 4, 0xffffffff);
      float cx = x + 14 + saturation * 192, cy = y + 36 + (1 - value) * 120;
      Ui.circle(g, cx - 3.5f, cy - 3.5f, 7, 0x60000000);
      Ui.outline(g, cx - 3.5f, cy - 3.5f, 7, 7, 3.5f, Ui.WHITE);
      Ui.rounded(g, x + 211, y + 35 + hue * 120, 16, 3, 1.5f, Ui.WHITE);
    }
    Ui.rounded(g, x + 12, y + (colorPopup ? 174 : 33), 212, 25, 6, Ui.inset());

    editor.extractRenderState(g, mx, my, delta);
    Ui.text(
        g,
        error.isBlank() ? "Enter to apply · Esc to return" : error,
        x + 14,
        y + (colorPopup ? 210 : 76),
        error.isBlank() ? Ui.MUTED : 0xFFFFB4AB);
    int by = y + popupHeight() - 42;
    Ui.rounded(g, x + 12, by, 100, 30, 7, Ui.inset());
    Ui.centered(g, "Close", x + 12, by, 100, 30, Ui.MUTED);
    Ui.rounded(g, x + 124, by, 100, 30, 7, Ui.alpha(FluxTweaks.config.theme.primary, .28f));
    Ui.centered(g, "Apply", x + 124, by, 100, 30, Ui.WHITE);
  }

  private boolean picker(double mx, double my) {
    if (!colorPopup) return false;
    int x = popupX(), y = popupY();
    if (Ui.inside(mx, my, x + 14, y + 36, 192, 120)) {
      saturation = (float) Math.clamp((mx - x - 14) / 191, 0, 1);
      value = 1 - (float) Math.clamp((my - y - 36) / 119, 0, 1);
    } else if (Ui.inside(mx, my, x + 210, y + 36, 18, 120))
      hue = (float) Math.clamp((my - y - 36) / 119, 0, 1);
    else return false;
    int rgb = Color.HSBtoRGB(hue, saturation, value) & 0xFFFFFF;
    colorCommit.accept(rgb);
    editor.setValue(String.format(Locale.ROOT, "#%06X", rgb));
    return true;
  }

  private void bind(InputConstants.Key key) {
    (bindingAction ? FluxTweaks.keys.get(selected) : FluxTweaks.enableKeys.get(selected))
        .setKey(key);
    KeyMapping.resetMapping();
    minecraft.options.save();
    binding = false;
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
    double mx = event.x() / scale, my = event.y() / scale - screenOffset;
    if (closing) return true;
    if (binding) {
      bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
      return true;
    }
    if (popup()) {
      if (choiceEntry != null) {
        int row = (int) Math.floor((my - popupY() - 48) / 28);
        if (event.button() == 0
            && Ui.inside(
                mx,
                my,
                popupX() + 8,
                popupY() + 48,
                220,
                choiceEntry.setting.choices().size() * 28)) {
          choiceEntry.setter.accept(new JsonPrimitive(choiceEntry.setting.choices().get(row)));
          closePopup();
        }
        return true;
      }
      int by = popupY() + popupHeight() - 42;
      if (Ui.inside(mx, my, popupX() + 12, by, 100, 30)) {
        closePopup();
        return true;
      }
      if (Ui.inside(mx, my, popupX() + 124, by, 100, 30)) {
        applyPopup();
        return true;
      }
      if (event.button() == 0 && picker(mx, my)) return true;
      editor.mouseClicked(new MouseButtonEvent(mx, my, event.buttonInfo()), twice);
      return true;
    }
    if (search.mouseClicked(new MouseButtonEvent(mx, my, event.buttonInfo()), twice)) {
      search.setFocused(true);
      return true;
    }
    search.setFocused(false);
    if (event.button() != 0) return true;
    for (int i = zones.size() - 1; i >= 0; i--) {
      Zone z = zones.get(i);
      if (Ui.inside(mx, my, z.x, z.y, z.w, z.h)) {
        focus = i;
        slider = z.drag != null ? z : null;
        if (z.drag != null) z.drag.accept(mx);
        else z.click.run();
        return true;
      }
    }
    FluxTweaks.hud.click(event.x(), event.y());
    return true;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
    double mx = event.x() / scale, my = event.y() / scale - screenOffset;
    if (popup()) {
      if (choiceEntry == null) picker(mx, my);
      return true;
    }
    if (dragPanel != 0) {
      var p = FluxTweaks.config.layout;
      if (dragPanel == 1) {
        p.navX = mx - dragOffsetX;
        p.navY = my - dragOffsetY;
      } else {
        p.settingsX = mx - dragOffsetX;
        p.settingsY = my - dragOffsetY;
      }
      clampPanels();
      FluxTweaks.changed();
      return true;
    }
    if (slider != null) {
      slider.drag.accept(mx);
      return true;
    }
    return true;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    dragPanel = 0;
    slider = null;
    return true;
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    if (popup()) return true;
    double mx = x / scale, my = y / scale - screenOffset;
    var p = FluxTweaks.config.layout;
    if (settingsVisible && Ui.inside(mx, my, p.settingsX, p.settingsY, 320, settingsHeight))
      settingsScroll = Math.max(0, settingsScroll - (int) (vertical * 30));
    else if (Ui.inside(mx, my, p.navX, p.navY, 190, navHeight))
      navScroll = Math.max(0, navScroll - (int) (vertical * 25));
    return true;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    int key = event.key();
    if (binding) {
      if (key == GLFW.GLFW_KEY_ESCAPE) binding = false;
      else
        bind(
            key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE
                ? InputConstants.UNKNOWN
                : InputConstants.getKey(event));
      return true;
    }
    if (closing) return true;
    if (popup()) {
      if (key == GLFW.GLFW_KEY_ESCAPE) {
        closePopup();
        return true;
      }
      if (choiceEntry != null) {
        int direction = key == GLFW.GLFW_KEY_UP ? -1 : key == GLFW.GLFW_KEY_DOWN ? 1 : 0;
        if (direction != 0 || key == GLFW.GLFW_KEY_TAB) {
          choiceFocus =
              Math.floorMod(
                  choiceFocus + (direction == 0 ? 1 : direction),
                  choiceEntry.setting.choices().size());
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
          choiceEntry.setter.accept(
              new JsonPrimitive(choiceEntry.setting.choices().get(choiceFocus)));
          closePopup();
        }
        return true;
      }
      if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
        applyPopup();
        return true;
      }
      return editor.keyPressed(event);
    }
    if (key == GLFW.GLFW_KEY_ESCAPE || FluxTweaks.guiKey.matches(event)) {
      onClose();
      return true;
    }
    if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
      search.setFocused(true);
      return true;
    }
    if (search.isFocused() && search.keyPressed(event)) return true;
    if (key == GLFW.GLFW_KEY_HOME) {
      resetLayout();
      return true;
    }
    if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
      settingsScroll = Math.max(0, settingsScroll + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 150 : -150));
      return true;
    }
    if (key == GLFW.GLFW_KEY_TAB && !zones.isEmpty()) {
      int direction = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
      focus = Math.floorMod(focus + direction, zones.size());
      String text = zones.get(focus).label;
      if (!text.equals(lastNarrated)) {
        minecraft.getNarrator().saySystemNow(Component.literal(text));
        lastNarrated = text;
      }
      search.setFocused(false);
      return true;
    }
    if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE)
        && focus >= 0
        && focus < zones.size()) {
      zones.get(focus).click.run();
      return true;
    }
    if ((key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)
        && focus >= 0
        && focus < zones.size()) {
      IntConsumer adjust = adjustments.get(zones.get(focus).label);
      if (adjust != null) {
        adjust.accept(key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
        return true;
      }
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(CharacterEvent event) {
    if (choiceEntry != null || closing) return true;
    if (editor != null) return editor.charTyped(event);
    return search.charTyped(event);
  }

  @Override
  public void onFilesDrop(List<Path> files) {
    if (!files.isEmpty()) FluxTweaks.effects.importPng(files.getFirst());
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    FluxTweaks.save();
    closing = true;
  }

  @Override
  public void removed() {
    FluxTweaks.save();
  }
}
