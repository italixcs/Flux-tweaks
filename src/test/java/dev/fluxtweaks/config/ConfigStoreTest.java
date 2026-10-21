package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ConfigStoreTest {
  @TempDir Path directory;

  @Test
  void freshInstallKeepsAllFeaturesOffAndNeutralTransforms() throws Exception {
    FluxConfig config = new ConfigStore(directory.resolve("flux.json")).load();
    for (Feature feature : Feature.values()) assertFalse(config.enabled(feature), feature.id);
    for (String group : new String[] {"mainItem", "offItem", "mainArm", "offArm"})
      assertEquals(1, config.number(Feature.VIEW_MODELS, group + ".scale"));
  }

  @Test
  void roundTripPersistsVisualsButDisarmsBothCameras() throws Exception {
    ConfigStore store = new ConfigStore(directory.resolve("flux.json"));
    FluxConfig config = new FluxConfig();
    config.module(Feature.HAND_CHAMS).enabled = true;
    config.module(Feature.FULLBRIGHT).enabled = true;
    config.module(Feature.LOW_FIRE).enabled = true;
    config.module(Feature.ZOOM).enabled = true;
    config.module(Feature.BLOCK_OVERLAY).enabled = true;
    config.module(Feature.HIT_COLOR).enabled = true;
    config.module(Feature.FREELOOK).enabled = true;
    config.module(Feature.FREECAM).enabled = true;
    config.theme.primary = 0x123456;
    config.layout.hudX = .4;
    config.set(Feature.HIT_PARTICLES, "shape", new JsonPrimitive("Ghosts"));
    store.save(config);
    FluxConfig loaded = store.load();
    assertTrue(loaded.enabled(Feature.HAND_CHAMS));
    assertTrue(loaded.enabled(Feature.FULLBRIGHT));
    assertTrue(loaded.enabled(Feature.LOW_FIRE));
    assertTrue(loaded.enabled(Feature.ZOOM));
    assertTrue(loaded.enabled(Feature.BLOCK_OVERLAY));
    assertTrue(loaded.enabled(Feature.HIT_COLOR));
    assertFalse(loaded.enabled(Feature.FREELOOK));
    assertFalse(loaded.enabled(Feature.FREECAM));
    assertEquals(0x123456, loaded.theme.primary);
    assertEquals(.4, loaded.layout.hudX);
    assertEquals("Ghosts", loaded.text(Feature.HIT_PARTICLES, "shape"));
    try (var files = Files.list(directory)) {
      assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp")));
    }
  }

  @Test
  void olderConfigAddsFullbrightDisabledAndKeepsExistingFeatures() {
    FluxConfig config = new FluxConfig();
    config.modules.remove(Feature.FULLBRIGHT.id);
    config.module(Feature.HAND_CHAMS).enabled = true;
    config.sanitize();
    assertFalse(config.enabled(Feature.FULLBRIGHT));
    assertTrue(config.enabled(Feature.HAND_CHAMS));
    assertTrue(Settings.of(Feature.FULLBRIGHT).isEmpty());
  }

  @Test
  void olderConfigAddsLowFireDisabledAndKeepsExistingFeatures() {
    FluxConfig config = new FluxConfig();
    config.modules.remove(Feature.LOW_FIRE.id);
    config.module(Feature.FULLBRIGHT).enabled = true;
    config.sanitize();
    assertFalse(config.enabled(Feature.LOW_FIRE));
    assertTrue(config.enabled(Feature.FULLBRIGHT));
    assertTrue(Settings.of(Feature.LOW_FIRE).isEmpty());
  }

  @Test
  void olderConfigAdds014ModulesAndAgreedDefaults() {
    FluxConfig config = new FluxConfig();
    for (var feature : new Feature[] {Feature.ZOOM, Feature.BLOCK_OVERLAY, Feature.HIT_COLOR})
      config.modules.remove(feature.id);
    config.module(Feature.FULLBRIGHT).enabled = true;
    config.sanitize();
    for (var feature : new Feature[] {Feature.ZOOM, Feature.BLOCK_OVERLAY, Feature.HIT_COLOR})
      assertFalse(config.enabled(feature));
    assertTrue(config.enabled(Feature.FULLBRIGHT));
    assertEquals(4, config.number(Feature.ZOOM, "magnification"));
    assertEquals(150, config.number(Feature.ZOOM, "duration"));
    assertEquals(20, config.number(Feature.BLOCK_OVERLAY, "fillOpacity"));
    assertEquals(35, config.number(Feature.HIT_COLOR, "opacity"));
    assertTrue(config.bool(Feature.HIT_COLOR, "players"));
    assertTrue(config.bool(Feature.HIT_COLOR, "mobs"));
    assertFalse(config.bool(Feature.HIT_COLOR, "self"));
    assertEquals(1, config.schemaVersion);
  }

  @Test
  void corruptFileIsPreservedAndDefaultsRecover() throws Exception {
    Path path = directory.resolve("flux.json");
    Files.writeString(path, "{ invalid settings");
    assertFalse(new ConfigStore(path).load().enabled(Feature.HAND_CHAMS));
    try (var files = Files.list(directory)) {
      assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("flux.json.invalid-")));
    }
    assertEquals("{ invalid settings", Files.readString(path));
  }

  @Test
  void invalidValuesRecoverWithoutLosingOtherSettings() {
    FluxConfig config = new FluxConfig();
    config.module(Feature.HIT_PARTICLES).values.addProperty("amount", 9000);
    config.module(Feature.HIT_PARTICLES).values.addProperty("shape", "Invalid sprite");
    config.module(Feature.HAND_CHAMS).values.addProperty("speed", Double.NaN);
    config.theme.opacity = Double.NaN;
    config.sanitize();
    assertEquals(40, config.number(Feature.HIT_PARTICLES, "amount"));
    assertEquals("Stars", config.text(Feature.HIT_PARTICLES, "shape"));
    assertEquals(1.5, config.number(Feature.HAND_CHAMS, "speed"));
    assertEquals(.92, config.theme.opacity);
  }

  @Test
  void rangeInputsAreSortedAndClamped() {
    FluxConfig config = new FluxConfig();
    JsonArray range = new JsonArray();
    range.add(1000);
    range.add(-20);
    config.set(Feature.AMBIENCE, "fogRange", range);
    assertArrayEquals(new double[] {0, 512}, config.range(Feature.AMBIENCE, "fogRange"));
  }

  @Test
  void nullSettingsAndMissingModulesRecover() {
    FluxConfig config =
        new Gson()
            .fromJson(
                "{\"theme\":null,\"layout\":null,\"modules\":{\"hand_chams\":null}}",
                FluxConfig.class);
    config.sanitize();
    for (Feature feature : Feature.values()) assertNotNull(config.module(feature));
    assertEquals("Hold", Settings.find(Feature.FREELOOK, "activation").choices().get(1));
  }
}
