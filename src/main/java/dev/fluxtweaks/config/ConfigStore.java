package dev.fluxtweaks.config;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;

public final class ConfigStore {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private final Path path;

  public ConfigStore(Path path) {
    this.path = path;
  }

  public FluxConfig load() throws IOException {
    if (!Files.exists(path)) return new FluxConfig();
    try (Reader reader = Files.newBufferedReader(path)) {
      FluxConfig config = GSON.fromJson(reader, FluxConfig.class);
      if (config == null || config.schemaVersion != 1)
        throw new JsonParseException("Unsupported configuration version");
      config.sanitize();
      for (Feature f : Feature.values()) if (f.camera()) config.module(f).enabled = false;
      return config;
    } catch (JsonParseException | IllegalStateException ex) {
      Files.copy(
          path,
          path.resolveSibling(path.getFileName() + ".invalid-" + Instant.now().toEpochMilli()));
      return new FluxConfig();
    }
  }

  public void save(FluxConfig config) throws IOException {
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), "fluxtweaks-", ".tmp");
    try {
      try (Writer writer = Files.newBufferedWriter(temp)) {
        GSON.toJson(config, writer);
      }
      try {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
