package dev.fluxtweaks.media;

import java.io.*;
import java.nio.file.*;

final class WindowsMedia {
  static void load() throws IOException {
    if (!System.getProperty("os.name").toLowerCase().contains("windows")
        || !System.getProperty("os.arch").equals("amd64"))
      throw new IOException("Spotify HUD requires Windows x64");
    Path directory = Files.createTempDirectory("fluxtweaks-media-");
    Path dll = directory.resolve("fluxmedia.dll");
    directory.toFile().deleteOnExit();
    dll.toFile().deleteOnExit();
    try (InputStream source =
        WindowsMedia.class.getResourceAsStream(
            "/assets/fluxtweaks/native/windows-x64/fluxmedia.dll")) {
      if (source == null) throw new IOException("Bundled Windows media bridge is missing");
      Files.copy(source, dll);
    }
    System.load(dll.toAbsolutePath().toString());
  }

  static native void open();

  static native String snapshot();

  static native byte[] artwork();

  static native boolean command(int command);

  static native void close();
}
