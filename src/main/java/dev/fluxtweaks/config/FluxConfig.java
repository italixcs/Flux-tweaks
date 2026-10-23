package dev.fluxtweaks.config;

import com.google.gson.*;
import java.util.*;

public final class FluxConfig {
  public int schemaVersion = 1;
  public Theme theme = new Theme();
  public Layout layout = new Layout();
  public Map<String, Module> modules = new LinkedHashMap<>();

  public static class Theme {
    public int primary = 0x9B3DFF, secondary = 0x00AFD8, background = 0x2B2D2D;
    public double opacity = .92;
    public double guiScale = 1;
    public boolean reducedMotion;
  }

  public static class Layout {
    public double navX = 16, navY = 20, settingsX = 218, settingsY = 20, hudX = .03, hudY = .78;
    public String selected = "hand_chams";
  }

  public static class Module {
    public boolean enabled;
    public JsonObject values = new JsonObject();
  }

  public FluxConfig() {
    sanitize();
  }

  public void sanitize() {
    if (theme == null) theme = new Theme();
    if (layout == null) layout = new Layout();
    if (modules == null) modules = new LinkedHashMap<>();
    theme.opacity = Double.isFinite(theme.opacity) ? Math.clamp(theme.opacity, 0, 1) : .92;
    theme.guiScale = dev.fluxtweaks.gui.GuiLayout.preference(theme.guiScale);
    theme.primary &= 0xFFFFFF;
    theme.secondary &= 0xFFFFFF;
    theme.background &= 0xFFFFFF;
    layout.navX = finite(layout.navX, 16);
    layout.navY = finite(layout.navY, 20);
    layout.settingsX = finite(layout.settingsX, 218);
    layout.settingsY = finite(layout.settingsY, 20);
    layout.hudX = Math.clamp(finite(layout.hudX, .03), 0, 1);
    layout.hudY = Math.clamp(finite(layout.hudY, .78), 0, 1);
    for (Feature feature : Feature.values()) {
      Module module = modules.computeIfAbsent(feature.id, k -> new Module());
      if (module == null) {
        module = new Module();
        modules.put(feature.id, module);
      }
      if (module.values == null) module.values = new JsonObject();
      for (Setting setting : Settings.of(feature))
        if (setting.kind() != Setting.Kind.HEADING && setting.kind() != Setting.Kind.ACTION)
          module.values.add(setting.key(), setting.validate(module.values.get(setting.key())));
    }
  }

  private static double finite(double n, double fallback) {
    return Double.isFinite(n) ? n : fallback;
  }

  public Module module(Feature f) {
    return modules.get(f.id);
  }

  public boolean enabled(Feature f) {
    return module(f).enabled;
  }

  public JsonElement value(Feature f, String key) {
    return module(f).values.get(key);
  }

  public double number(Feature f, String key) {
    return value(f, key).getAsDouble();
  }

  public float decimal(Feature f, String key) {
    return (float) number(f, key);
  }

  public boolean bool(Feature f, String key) {
    return value(f, key).getAsBoolean();
  }

  public String text(Feature f, String key) {
    return value(f, key).getAsString();
  }

  public int color(Feature f, String key) {
    return value(f, key).getAsInt() | 0xFF000000;
  }

  public double[] range(Feature f, String key) {
    var a = value(f, key).getAsJsonArray();
    return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble()};
  }

  public void set(Feature f, String key, JsonElement value) {
    module(f).values.add(key, Settings.find(f, key).validate(value));
  }
}
