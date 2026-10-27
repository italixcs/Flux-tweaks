package dev.fluxtweaks.config;

import static dev.fluxtweaks.config.Setting.*;

import java.util.*;

public final class Settings {
  private static final Map<Feature, List<Setting>> ALL = new EnumMap<>(Feature.class);

  static {
    ALL.put(
        Feature.HAND_CHAMS,
        List.of(
            color("first", "First Color", 0xFFFFFF),
            color("second", "Second Color", 0x00AFD8),
            number("speed", "Animation Speed", 1.5, 0, 5, .05, ""),
            number("quality", "Glow Quality", 3, 1, 5, 1, ""),
            number("radius", "Glow Radius", 5.7, 1, 20, .1, "px"),
            number("glow", "Glow Opacity", .7, 0, 2, .01, ""),
            number("fill", "Fill Opacity", 0, 0, 1, .01, "")));
    var view = new ArrayList<Setting>();
    for (String target : List.of("mainItem", "offItem", "mainArm", "offArm")) {
      String title =
          switch (target) {
            case "mainItem" -> "Main-hand Item";
            case "offItem" -> "Off-hand Item";
            case "mainArm" -> "Main Arm";
            default -> "Off Arm";
          };
      view.add(heading(title));
      view.add(bool(target + ".enabled", "Work on " + title, true));
      view.add(number(target + ".scale", "Scale", 1, .1, 3, .01, ""));
      for (String axis : List.of("X", "Y", "Z"))
        view.add(number(target + ".pos" + axis, "Position " + axis, 0, -2, 2, .01, ""));
      for (String axis : List.of("X", "Y", "Z"))
        view.add(number(target + ".rot" + axis, "Rotation " + axis, 0, -180, 180, 1, "deg"));
    }
    ALL.put(Feature.VIEW_MODELS, List.copyOf(view));
    ALL.put(
        Feature.SPOTIFY,
        List.of(number("scale", "HUD Scale", 1, .5, 3, .05, ""), action("hud", "Move HUD")));
    ALL.put(
        Feature.HIT_PARTICLES,
        List.of(
            choice(
                "shape",
                "Particles",
                "Stars",
                "Stars",
                "Snowflakes",
                "Ghosts",
                "Hearts",
                "Diamonds",
                "Thunders",
                "Moons",
                "Clouds"),
            color("color", "Color", 0xFFFFFF),
            choice("physics", "Physics", "Fall", "Fall", "Rise", "Burst", "Fly"),
            number("scale", "Scale", 3, .2, 8, .1, ""),
            number("speed", "Speed", 3, 0, 10, .1, ""),
            number("amount", "Amount", 2, 1, 40, 1, ""),
            number("lifetime", "Time Before Disappear", 3, .1, 10, .1, "s"),
            range("angle", "Rotation Angle", 0, 180, -180, 180, "deg"),
            range("rotation", "Rotation Speed", 0, 60, -360, 360, "deg/s"),
            bool("glow", "Glow", true)));
    ALL.put(
        Feature.JUMP_CIRCLE,
        List.of(
            number("size", "Circle Size", 1, .1, 5, .05, ""),
            number("duration", "Fade Duration", 1, .1, 5, .1, "s"),
            bool("self", "Only Self", true),
            color("color", "Circle Color", 0xFFFFFF),
            choice("style", "Circle Texture", "Glowing", "Default", "Glowing", "Custom"),
            action("texture", "Import Custom PNG")));
    ALL.put(
        Feature.AMBIENCE,
        List.of(
            bool("particles", "Particles", true),
            choice("shape", "Particle Type", "Triangles", "Triangles", "Cubes"),
            color("color", "Particles Color", 0xFFFFFF),
            number("amount", "Particles Amount", 40, 0, 250, 1, ""),
            number("size", "Particle Size", .12, .02, .8, .01, ""),
            number("speed", "Particle Speed", .3, 0, 3, .05, ""),
            number("distance", "Spawn Distance", 10, 2, 32, 1, "blocks"),
            bool("time", "Change Time", false),
            choice("timeMode", "Time Mode", "Presets", "Presets", "Custom"),
            choice(
                "preset",
                "Time",
                "Day",
                "Dawn",
                "Morning",
                "Day",
                "Evening",
                "Sunset",
                "Night",
                "Real Time"),
            number("timeValue", "Custom Time", 6000, 0, 23999, 1, "ticks"),
            bool("weather", "Change Weather", false),
            choice("weatherType", "Weather", "Clear", "Clear", "Rain", "Thunder", "Snow"),
            choice("fog", "Fog", "Vanilla", "Vanilla", "Removed", "Custom"),
            color("fogColor", "Fog Color", 0x9B3DFF),
            range("fogRange", "Fog Range", 64, 128, 0, 512, "blocks"),
            bool("sky", "Sync with Sky", true)));
    ALL.put(
        Feature.SHIELD_STATUS,
        List.of(
            bool("self", "Only Self", true),
            bool("transition", "Color Transition", true),
            color("ready", "Ready Color", 0x00E84A),
            color("using", "Using Color", 0x70FF80),
            color("disabled", "Disabled Color", 0xFF4040),
            color("low", "Low Durability Color", 0xFFB43B),
            number("threshold", "Low Durability Threshold", 20, 1, 99, 1, "%")));
    ALL.put(
        Feature.FREELOOK, List.of(choice("activation", "Action Mode", "Toggle", "Toggle", "Hold")));
    ALL.put(
        Feature.FREECAM,
        List.of(
            choice("activation", "Action Mode", "Toggle", "Toggle", "Hold"),
            number("speed", "Camera Speed", .5, .05, 3, .05, "blocks/tick"),
            number("sprint", "Sprint Multiplier", 3, 1, 10, .1, "")));
    ALL.put(Feature.FULLBRIGHT, List.of());
    ALL.put(Feature.LOW_FIRE, List.of());
    ALL.put(
        Feature.ZOOM,
        List.of(
            number("magnification", "Default Magnification", 4, 1, 16, .25, "x"),
            number("duration", "Transition Duration", 150, 50, 500, 10, "ms")));
    ALL.put(
        Feature.BLOCK_OVERLAY,
        List.of(
            bool("outline", "Outline", true),
            color("outlineColor", "Outline Color", 0x9B3DFF),
            number("outlineOpacity", "Outline Opacity", 100, 0, 100, 1, "%"),
            number("thickness", "Outline Thickness", 2, 1, 6, .25, "px"),
            bool("fill", "Fill", true),
            color("fillColor", "Fill Color", 0x00AFD8),
            number("fillOpacity", "Fill Opacity", 20, 0, 100, 1, "%")));
    ALL.put(
        Feature.HIT_COLOR,
        List.of(
            color("color", "Color", 0x9B3DFF),
            number("opacity", "Opacity", 35, 0, 100, 1, "%"),
            bool("players", "Players", true),
            bool("mobs", "Mobs", true),
            bool("self", "Self", false)));
    ALL.put(
        Feature.BUTTERFLY_KNIFE,
        List.of(
            choice("finish", "Finish", "Steel", "Steel", "Flux", "Sunset", "Violet Marble", "Emerald Marble", "Obsidian Red"),
            choice("inspect", "Inspect Style", "Flourish", "Flip", "Spin", "Flourish", "Random"),
            choice("activation", "Inspect Mode", "Press", "Press", "Hold", "Toggle"),
            number("speed", "Animation Speed", 1, .25, 3, .05, "x"),
            number("scale", "Knife Scale", 1, .5, 2, .01, "x"),
            bool("glint", "Shimmer / Glint", true)));
  }

  private Settings() {}

  public static List<Setting> of(Feature feature) {
    return ALL.get(feature);
  }

  public static Setting find(Feature feature, String key) {
    return of(feature).stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
  }
}
