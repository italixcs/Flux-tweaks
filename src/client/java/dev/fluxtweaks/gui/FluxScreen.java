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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FluxScreen extends Screen {
  private record Entry(Setting setting, JsonElement value, Consumer<JsonElement> setter) {}

  private record Zone(
      String label, int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {}

  private final List<Zone> zones = new ArrayList<>();
  private final Ui.Motion motion = new Ui.Motion();
  private Feature selected;
  private boolean theme, settingsVisible = true, binding;
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
  private String lastNarrated = "";
  private int contentTop, contentBottom;

  public FluxScreen() {
    super(Component.literal("Flux Tweaks"));
    String id = FluxTweaks.config.layout.selected;
    selected =
        Arrays.stream(Feature.values())
            .filter(f -> f.id.equals(id))
            .findFirst()
            .orElse(Feature.HAND_CHAMS);
  }

  @Override
  protected void init() {
    scale = Ui.scaleFor(width, height);
    logicalWidth = (int) (width / scale);
    logicalHeight = (int) (height / scale);
    navHeight = Math.min(460, logicalHeight - 40);
    settingsHeight = desiredSettingsHeight();
    clampPanels();
    String old = search == null ? "" : search.getValue();
    search = Ui.input(0, 0, 122, 18, "Search features");
    search.setBordered(false);
    search.setMaxLength(64);
    search.setHint(Ui.label("Search features…"));
    search.setTextColor(Ui.WHITE);
    search.setValue(old);
  }

  private String featureHint() {
    return switch (selected) {
      case SHIELD_STATUS -> "Other players' cooldowns are estimates.";
      case FREECAM -> "Camera only. Your body stays in the world.";
      case SPOTIFY -> FluxTweaks.spotify.status();
      case FULLBRIGHT -> "Brightens dark areas on your screen.";
      case LOW_FIRE -> "Lowers burning flames to keep your view clear.";
      case ZOOM -> "Enable, then hold your key. Scroll to adjust.";
      case BLOCK_OVERLAY -> "Customizes the block under your crosshair.";
      case HIT_COLOR -> "Colors damage flashes on bodies and armor.";
      default -> "";
    };
  }

  private int desiredSettingsHeight() {
    int prefix = theme ? 44 : 103 + (featureHint().isBlank() ? 0 : 23);
    int content = entries().stream().mapToInt(e -> rowHeight(e.setting())).sum();
    return Math.min(navHeight, Math.max(150, prefix + content + 14));
  }

  private void clampPanels() {
    var p = FluxTweaks.config.layout;
    p.navX = Math.clamp(p.navX, 8, logicalWidth - 178);
    p.navY = Math.clamp(p.navY, 8, logicalHeight - navHeight - 8);
    p.settingsX = Math.clamp(p.settingsX, 8, logicalWidth - 268);
    p.settingsY = Math.clamp(p.settingsY, 8, logicalHeight - settingsHeight - 8);
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
  public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
    zones.clear();
    motion.frame();
    int mx = (int) (mouseX / scale), my = (int) (mouseY / scale);
    g.pose().pushMatrix();
    g.pose().scale(scale, scale);
    settingsHeight = desiredSettingsHeight();
    clampPanels();
    var p = FluxTweaks.config.layout;
    int nx = (int) p.navX, ny = (int) p.navY, sx = (int) p.settingsX, sy = (int) p.settingsY;
    Ui.rounded(g, nx, ny, 170, navHeight, 13, Ui.panel());
    Ui.outline(g, nx, ny, 170, navHeight, 13, 0x12FFFFFF);
    Ui.title(g, "Flux Tweaks", nx + 16, ny + 18, Ui.WHITE);
    Ui.text(g, "1.21.11", nx + 16, ny + 35, Ui.MUTED);
    zone(
        "Move feature panel",
        nx,
        ny,
        170,
        49,
        () -> {
          dragPanel = 1;
          dragOffsetX = mx - nx;
          dragOffsetY = my - ny;
        });
    Ui.rounded(g, nx + 10, ny + 55, 150, 26, 7, Ui.inset());
    Ui.outline(g, nx + 18, ny + 63, 7, 7, 3.5f, Ui.MUTED);
    Ui.line(g, nx + 24, ny + 69, nx + 27, ny + 72, 1, Ui.MUTED);
    search.setX(nx + 35);
    search.setY(ny + 63);
    search.render(g, mx, my, delta);
    int listTop = ny + 91, listBottom = ny + navHeight - 67;
    List<Feature> visible =
        Arrays.stream(Feature.values())
            .filter(
                f ->
                    f.title
                        .toLowerCase(Locale.ROOT)
                        .contains(search.getValue().toLowerCase(Locale.ROOT)))
            .toList();
    navScroll = Math.clamp(navScroll, 0, Math.max(0, visible.size() * 28 - (listBottom - listTop)));
    g.enableScissor(nx + 6, listTop, nx + 164, listBottom);
    int yy = listTop - navScroll;
    for (Feature f : visible) {
      int row = yy;
      if (row + 27 > listTop && row < listBottom) {
        if (!theme && f == selected)
          Ui.rounded(g, nx + 8, row, 154, 25, 6, Ui.alpha(FluxTweaks.config.theme.primary, .17f));
        else {
          float hover =
              motion.value("nav." + f.id, Ui.inside(mx, my, nx + 8, row, 154, 25) ? 1 : 0);
          Ui.rounded(g, nx + 8, row, 154, 25, 6, Ui.alpha(0xffffff, hover * .055f));
        }
        Ui.text(g, f.title, nx + 17, row + 8, (!theme && f == selected) ? Ui.WHITE : Ui.MUTED);
        Ui.rounded(
            g, nx + 147, row + 10, 4, 4, 2, FluxTweaks.active(f) ? Ui.secondary() : 0xFF626268);
        int ztop = Math.max(row, listTop), zbottom = Math.min(row + 27, listBottom);
        zone(
            "Configure " + f.title,
            nx + 8,
            ztop,
            154,
            zbottom - ztop,
            () -> {
              selected = f;
              theme = false;
              settingsVisible = true;
              settingsScroll = 0;
              motion.snap("scroll", 0);
              binding = false;
              FluxTweaks.config.layout.selected = f.id;
              FluxTweaks.changed();
            });
      }
      yy += 28;
    }
    g.disableScissor();
    if (visible.isEmpty()) Ui.text(g, "No matching features", nx + 15, listTop + 10, Ui.MUTED);
    g.fill(nx + 15, ny + navHeight - 61, nx + 155, ny + navHeight - 60, 0x1AFFFFFF);
    textButton(
        g,
        "Theme",
        nx + 12,
        ny + navHeight - 52,
        146,
        22,
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
        ny + navHeight - 28,
        146,
        22,
        mx,
        my,
        () -> {
          FluxTweaks.save();
          minecraft.setScreen(new HudEditorScreen(this));
        });
    if (settingsVisible) renderSettings(g, sx, sy, mx, my, delta);
    if (focus >= 0 && focus < zones.size() && !popup()) {
      Zone z = zones.get(focus);
      Ui.outline(g, z.x - 1, z.y - 1, z.w + 2, z.h + 2, 5, Ui.secondary());
    }
    if (!FluxTweaks.message.isBlank()) {
      Ui.rounded(
          g,
          8,
          logicalHeight - 23,
          Math.min(logicalWidth - 16, Ui.width(FluxTweaks.message) + 18),
          18,
          6,
          0xCC1A1A20);
      Ui.text(g, Ui.fit(FluxTweaks.message, logicalWidth - 34), 17, logicalHeight - 17, Ui.MUTED);
    }
    if (popup()) {
      g.nextStratum();
      renderPopup(g, mx, my, delta);
    }
    g.pose().popMatrix();
  }

  private void renderSettings(GuiGraphics g, int x, int y, int mx, int my, float delta) {
    Ui.rounded(g, x, y, 260, settingsHeight, 13, Ui.panel());
    Ui.outline(g, x, y, 260, settingsHeight, 13, 0x12FFFFFF);
    Ui.title(g, theme ? "Theme" : selected.title, x + 16, y + 18, Ui.WHITE);
    zone(
        "Move settings panel",
        x,
        y,
        227,
        43,
        () -> {
          dragPanel = 2;
          dragOffsetX = mx - x;
          dragOffsetY = my - y;
        });
    float closeHover = motion.value("close", Ui.inside(mx, my, x + 228, y + 9, 25, 25) ? 1 : 0);
    Ui.rounded(g, x + 230, y + 11, 21, 21, 6, Ui.alpha(0xffffff, closeHover * .06f));
    Ui.close(g, x + 238, y + 19, Ui.MUTED);
    zone("Close settings panel", x + 228, y + 9, 25, 25, () -> settingsVisible = false);
    int top = y + 44;
    if (!theme) {
      Ui.text(g, "Enabled", x + 16, top + 4, Ui.MUTED);
      Ui.toggle(
          g,
          x + 217,
          top,
          motion.value(selected.id + ".enabled", FluxTweaks.active(selected) ? 1 : 0));
      zone(
          "Toggle " + selected.title,
          x + 12,
          top - 3,
          236,
          24,
          () -> FluxTweaks.setEnabled(selected, !FluxTweaks.active(selected)));
      Ui.text(g, "Keybind", x + 16, top + 35, Ui.MUTED);
      String key =
          binding
              ? "Press a key…"
              : FluxTweaks.keys.get(selected).getTranslatedKeyMessage().getString();
      Ui.right(g, Ui.fit(key, 135), x + 244, top + 35, binding ? Ui.primary() : Ui.WHITE);
      zone(
          "Set " + selected.title + " keybind",
          x + 12,
          top + 26,
          236,
          28,
          () -> {
            binding = true;
            search.setFocused(false);
          });
      top += 59;
      String hint = featureHint();
      if (!hint.isBlank()) {
        Ui.text(g, Ui.fit(hint, 228), x + 16, top, Ui.MUTED);
        top += 23;
      }
    }
    contentTop = top;
    contentBottom = y + settingsHeight - 12;
    List<Entry> entries = entries();
    int total = entries.stream().mapToInt(e -> rowHeight(e.setting)).sum();
    settingsScroll =
        Math.clamp(settingsScroll, 0, Math.max(0, total - (contentBottom - contentTop)));
    g.enableScissor(x + 9, contentTop, x + 251, contentBottom);
    int scroll = Math.round(motion.value("scroll", settingsScroll));
    int row = contentTop - scroll;
    for (Entry entry : entries) {
      int h = rowHeight(entry.setting);
      if (row + h > contentTop && row < contentBottom)
        renderEntry(g, entry, x + 16, row, 228, mx, my);
      row += h;
    }
    g.disableScissor();
    if (total > contentBottom - contentTop) {
      int track = contentBottom - contentTop;
      int thumb = Math.max(18, track * track / total);
      int offset = (int) ((track - thumb) * scroll / (double) Math.max(1, total - track));
      Ui.rounded(
          g,
          x + 253,
          contentTop + offset,
          2,
          thumb,
          1,
          Ui.alpha(FluxTweaks.config.theme.secondary, .65f));
    }
  }

  private List<Entry> entries() {
    if (theme) {
      var c = FluxTweaks.config;
      return List.of(
          new Entry(
              Setting.color("primary", "Primary Color", c.theme.primary),
              new JsonPrimitive(c.theme.primary),
              v -> {
                c.theme.primary = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.color("secondary", "Secondary Color", c.theme.secondary),
              new JsonPrimitive(c.theme.secondary),
              v -> {
                c.theme.secondary = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.color("background", "Background Color", c.theme.background),
              new JsonPrimitive(c.theme.background),
              v -> {
                c.theme.background = v.getAsInt();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.number("transparency", "Background Transparency", .08, 0, 1, .01, "%"),
              new JsonPrimitive(1 - c.theme.opacity),
              v -> {
                c.theme.opacity = 1 - v.getAsDouble();
                FluxTweaks.changed();
              }),
          new Entry(
              Setting.action("reset", "Reset Layout"), JsonNull.INSTANCE, v -> resetLayout()));
    }
    List<Entry> result = new ArrayList<>();
    Feature f = selected;
    for (Setting s : Settings.of(f)) {
      if (!visible(f, s.key())) continue;
      result.add(
          new Entry(
              s,
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
      case BOOLEAN, COLOR, ACTION -> 32;
      case NUMBER, RANGE -> 44;
      case CHOICE -> 35 + s.choices().size() * 23;
      case HEADING -> 33;
    };
  }

  private void renderEntry(GuiGraphics g, Entry e, int x, int y, int width, int mx, int my) {
    Setting s = e.setting;
    switch (s.kind()) {
      case HEADING -> {
        g.fill(x, y + 5, x + width, y + 6, 0x18FFFFFF);
        Ui.title(g, s.label(), x, y + 16, Ui.WHITE);
      }
      case BOOLEAN -> {
        Ui.text(g, s.label(), x, y + 8, Ui.MUTED);
        Ui.toggle(
            g,
            x + width - 27,
            y + 5,
            motion.value(selected.id + "." + s.key(), e.value.getAsBoolean() ? 1 : 0));
        clippedZone(
            s.label(),
            x - 3,
            y,
            width + 6,
            28,
            () -> e.setter.accept(new JsonPrimitive(!e.value.getAsBoolean())),
            null);
      }
      case COLOR -> {
        Ui.text(g, s.label(), x, y + 8, Ui.MUTED);
        Ui.circle(g, x + width - 13, y + 7, 12, e.value.getAsInt() | 0xff000000);
        Ui.outline(g, x + width - 13, y + 7, 12, 12, 6, 0x16FFFFFF);
        clippedZone(
            s.label(),
            x - 3,
            y,
            width + 6,
            28,
            () ->
                openColor(
                    s.label(),
                    e.value.getAsInt(),
                    color -> e.setter.accept(new JsonPrimitive(color))),
            null);
      }
      case NUMBER -> {
        double n = e.value.getAsDouble();
        Ui.text(g, s.label(), x, y + 5, Ui.MUTED);
        String formatted = format(n, s);
        Ui.right(g, formatted, x + width, y + 5, Ui.WHITE);
        boolean percent = theme && s.key().equals("transparency");
        clippedZone(
            "Type " + s.label(),
            x + width - 84,
            y - 1,
            84,
            18,
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
        int barY = y + 29;
        float t = (float) ((n - s.min()) / (s.max() - s.min()));
        String key = selected.id + ".slider." + s.key();
        if (slider != null) motion.snap(key, t);
        else t = motion.value(key, t);
        Ui.rounded(g, x, barY, width, 2.5f, 1.25f, Ui.inset());
        Ui.rounded(g, x, barY, Math.max(0, width * t), 2.5f, 1.25f, Ui.primary());
        thumb(g, x + width * t, barY + 1.25f, Ui.inside(mx, my, x, y + 18, width, 23));
        DoubleConsumer set =
            mouse ->
                e.setter.accept(
                    new JsonPrimitive(
                        s.bounded(
                            s.min()
                                + Math.clamp((mouse - x) / width, 0, 1) * (s.max() - s.min()))));
        clippedZone(s.label() + " slider", x, y + 20, width, 20, () -> set.accept(mx), set);
      }
      case RANGE -> {
        var a = e.value.getAsJsonArray();
        double lo = a.get(0).getAsDouble(), hi = a.get(1).getAsDouble();
        Ui.text(g, s.label(), x, y + 5, Ui.MUTED);
        Ui.right(
            g,
            String.format(Locale.ROOT, "%.0f – %.0f %s", lo, hi, s.unit()),
            x + width,
            y + 5,
            Ui.WHITE);
        clippedZone(
            "Type " + s.label() + " range",
            x + width - 112,
            y - 1,
            112,
            18,
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
        int by = y + 29;
        float l = (float) ((lo - s.min()) / (s.max() - s.min())),
            h = (float) ((hi - s.min()) / (s.max() - s.min()));
        Ui.rounded(g, x, by, width, 2.5f, 1.25f, Ui.inset());
        Ui.rounded(g, x + l * width, by, Math.max(0, (h - l) * width), 2.5f, 1.25f, Ui.primary());
        thumb(g, x + l * width, by + 1.25f, Ui.inside(mx, my, x, y + 18, width, 23));
        thumb(g, x + h * width, by + 1.25f, Ui.inside(mx, my, x, y + 18, width, 23));
        boolean left = Math.abs(mx - (x + l * width)) <= Math.abs(mx - (x + h * width));
        DoubleConsumer set =
            mouse -> {
              double n =
                  s.bounded(s.min() + Math.clamp((mouse - x) / width, 0, 1) * (s.max() - s.min()));
              JsonArray next = new JsonArray();
              next.add(left ? Math.min(n, hi) : lo);
              next.add(left ? hi : Math.max(n, lo));
              e.setter.accept(next);
            };
        clippedZone(s.label() + " range slider", x, y + 20, width, 20, () -> set.accept(mx), set);
      }
      case CHOICE -> {
        Ui.text(g, s.label(), x, y + 5, Ui.MUTED);
        Ui.rounded(g, x, y + 23, width, s.choices().size() * 23 + 4, 11, Ui.inset());
        int yy = y + 29;
        for (String option : s.choices()) {
          int row = yy;
          boolean on = option.equals(e.value.getAsString());
          if (Ui.inside(mx, my, x + 5, row - 1, width - 10, 22))
            Ui.rounded(g, x + 5, row - 1, width - 10, 22, 5, 0x0DFFFFFF);
          if (on) Ui.check(g, x + 12, row + 6, Ui.WHITE);
          Ui.text(g, option, x + 31, row + 4, on ? Ui.WHITE : Ui.MUTED);
          clippedZone(
              s.label() + ": " + option,
              x + 5,
              row - 1,
              width - 10,
              22,
              () -> e.setter.accept(new JsonPrimitive(option)),
              null);
          yy += 23;
        }
      }
      case ACTION -> {
        Ui.rounded(g, x, y + 2, width, 24, 7, Ui.inset());
        Ui.text(g, s.label(), x + 10, y + 10, Ui.WHITE);
        clippedZone(s.label(), x, y, width, 29, () -> action(s.key()), null);
      }
    }
  }

  private void thumb(GuiGraphics g, float x, float y, boolean hover) {
    float amount = hover ? 1 : 0;
    if (amount > 0)
      Ui.circle(g, x - 7, y - 7, 14, Ui.alpha(FluxTweaks.config.theme.primary, .14f * amount));
    Ui.circle(g, x - 4.1f, y - 4.1f, 8.2f, Ui.WHITE);
  }

  private String format(double n, Setting s) {
    if (theme && s.key().equals("transparency"))
      return String.format(Locale.ROOT, "%.0f%%", n * 100);
    String number =
        s.step() >= 1
            ? String.format(Locale.ROOT, "%.0f", n)
            : String.format(Locale.ROOT, "%.2f", n).replaceAll("0+$", "").replaceAll("\\.$", "");
    return number + (s.unit().isEmpty() ? "" : " " + s.unit());
  }

  private void action(String key) {
    switch (key) {
      case "hud" -> minecraft.setScreen(new HudEditorScreen(this));
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
    var p = FluxTweaks.config.layout;
    p.navX = 16;
    p.navY = 16;
    p.settingsX = 198;
    p.settingsY = 16;
    p.hudX = .03;
    p.hudY = .78;
    clampPanels();
    FluxTweaks.changed();
  }

  private void textButton(
      GuiGraphics g, String text, int x, int y, int w, int h, int mx, int my, Runnable action) {
    float hover = motion.value("button." + text, Ui.inside(mx, my, x, y, w, h) ? 1 : 0);
    Ui.rounded(g, x, y, w, h, 6, Ui.alpha(0xffffff, hover * .06f));
    Ui.text(g, text, x + 7, y + 7, Ui.MUTED);
    zone(text, x, y, w, h, action);
  }

  private boolean popup() {
    return editor != null;
  }

  private int popupX() {
    return (logicalWidth - 236) / 2;
  }

  private int popupY() {
    return (logicalHeight - (colorPopup ? 230 : 98)) / 2;
  }

  private void openEditor(String title, String initial, Consumer<String> commit) {
    colorPopup = false;
    editTitle = title;
    editCommit = commit;
    createEditor(initial, popupX() + 14, popupY() + 35, 208);
  }

  private void createEditor(String initial, int x, int y, int w) {
    search.setFocused(false);
    binding = false;
    editor = Ui.input(x, y, w, 22, editTitle);
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
        String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF), popupX() + 14, popupY() + 176, 208);
    editor.setResponder(
        text -> {
          try {
            String hex = text.trim().replace("#", "");
            if (hex.length() == 6) colorCommit.accept(Integer.parseInt(hex, 16));
          } catch (NumberFormatException ignored) {
          }
        });
  }

  private void renderPopup(GuiGraphics g, int mx, int my, float delta) {
    g.fill(0, 0, logicalWidth, logicalHeight, 0x60000000);
    int x = popupX(), y = popupY();
    Ui.rounded(g, x, y, 236, colorPopup ? 230 : 98, 12, 0xFF29292F);
    Ui.title(g, editTitle, x + 14, y + 14, Ui.WHITE);
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

    editor.render(g, mx, my, delta);
    Ui.text(
        g,
        error.isBlank() ? "Enter to apply · Esc to close" : error,
        x + 14,
        y + (colorPopup ? 211 : 76),
        error.isBlank() ? Ui.MUTED : 0xFFFF7777);
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
    FluxTweaks.keys.get(selected).setKey(key);
    KeyMapping.resetMapping();
    minecraft.options.save();
    binding = false;
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
    double mx = event.x() / scale, my = event.y() / scale;
    if (binding) {
      bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
      return true;
    }
    if (popup()) {
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
        z.click.run();
        return true;
      }
    }
    FluxTweaks.hud.click(event.x(), event.y());
    return true;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
    double mx = event.x() / scale, my = event.y() / scale;
    if (popup()) {
      picker(mx, my);
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
    double mx = x / scale, my = y / scale;
    var p = FluxTweaks.config.layout;
    if (settingsVisible && Ui.inside(mx, my, p.settingsX, p.settingsY, 260, settingsHeight))
      settingsScroll = Math.max(0, settingsScroll - (int) (vertical * 30));
    else if (Ui.inside(mx, my, p.navX, p.navY, 170, navHeight))
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
    if (popup()) {
      if (key == GLFW.GLFW_KEY_ESCAPE) {
        editor = null;
        return true;
      }
      if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
        try {
          if (colorPopup) {
            String hex = editor.getValue().trim().replace("#", "");
            if (hex.length() != 6) throw new IllegalArgumentException();
            colorCommit.accept(Integer.parseInt(hex, 16));
          } else editCommit.accept(editor.getValue());
          editor = null;
        } catch (RuntimeException ex) {
          error = colorPopup ? "Enter six hex digits" : "Enter a valid number or range";
        }
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
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(CharacterEvent event) {
    if (editor != null) return editor.charTyped(event);
    return search.charTyped(event);
  }

  @Override
  public void onFilesDrop(List<Path> files) {
    if (!files.isEmpty()) FluxTweaks.effects.importPng(files.getFirst());
  }

  @Override
  public void renderBackground(GuiGraphics g, int x, int y, float delta) {}

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    FluxTweaks.save();
    minecraft.setScreen(null);
  }

  @Override
  public void removed() {
    FluxTweaks.save();
  }
}
