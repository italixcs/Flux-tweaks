package dev.fluxtweaks.media;

import com.google.gson.*;
import java.util.function.Predicate;

public final class WindowsMediaAcceptance {
  private static JsonObject snapshot() {
    return JsonParser.parseString(WindowsMedia.snapshot()).getAsJsonObject();
  }

  private static boolean bool(JsonObject o, String key) {
    return o.get(key).getAsBoolean();
  }

  private static JsonObject await(Predicate<JsonObject> ready) throws Exception {
    for (int i = 0; i < 30; i++) {
      Thread.sleep(200);
      var s = snapshot();
      if (bool(s, "available") && ready.test(s)) return s;
    }
    throw new AssertionError("Spotify did not report the requested change");
  }

  private static void command(int command) {
    if (!WindowsMedia.command(command))
      throw new AssertionError("Spotify rejected control " + command);
  }

  public static void main(String[] args) throws Exception {
    WindowsMedia.load();
    WindowsMedia.open();
    var initial = snapshot();
    if (!bool(initial, "available"))
      throw new AssertionError("Spotify desktop session is not available");
    boolean playing = bool(initial, "playing");
    int repeat = initial.get("repeat").getAsInt();
    try {
      if (bool(initial, "toggle")) {
        command(0);
        await(s -> bool(s, "playing") != playing);
        command(0);
        await(s -> bool(s, "playing") == playing);
        System.out.println("Play/pause and restoration: PASS");
      }
      if (bool(initial, "repeatSupported")) {
        if (WindowsMedia.command(3)) {
          await(state -> state.get("repeat").getAsInt() != repeat);
          for (int i = 0; i < 3 && snapshot().get("repeat").getAsInt() != repeat; i++) {
            int old = snapshot().get("repeat").getAsInt();
            command(3);
            await(state -> state.get("repeat").getAsInt() != old);
          }
          if (snapshot().get("repeat").getAsInt() != repeat)
            throw new AssertionError("Repeat was not restored");
          System.out.println("Repeat and restoration: PASS");
        } else {
          if (bool(snapshot(), "repeatSupported"))
            throw new AssertionError("Nonworking repeat control was not hidden");
          System.out.println(
              "Repeat: Windows session did not confirm a change; unavailable control hidden: PASS");
        }
      }
      if (bool(initial, "next")) {
        String key = snapshot().get("artworkKey").getAsString();
        command(2);
        await(s -> !s.get("artworkKey").getAsString().equals(key));
        System.out.println("Next track and metadata change: PASS");
      }
      if (bool(initial, "previous")) {
        String key = snapshot().get("artworkKey").getAsString();
        command(1);
        await(s -> !s.get("artworkKey").getAsString().equals(key));
        System.out.println("Previous track and metadata change: PASS");
      }
      var s = snapshot();
      if (s.get("duration").getAsLong() <= 0) throw new AssertionError("Track duration missing");
      if (s.get("title").getAsString().isBlank()) throw new AssertionError("Track title missing");
      System.out.println(
          "Metadata/duration/artwork bridge: PASS (artwork bytes "
              + WindowsMedia.artwork().length
              + ")");
      if (bool(s, "playing")) {
        long position = s.get("position").getAsLong();
        String key = s.get("artworkKey").getAsString();
        Thread.sleep(1800);
        var after = snapshot();
        if (after.get("artworkKey").getAsString().equals(key)
            && after.get("position").getAsLong() - position < 1200)
          throw new AssertionError("Playing progress did not advance");
        System.out.println("Live progress advances: PASS");
      }
    } finally {
      var current = snapshot();
      if (bool(current, "available")) {
        if (bool(current, "playing") != playing) WindowsMedia.command(0);
        for (int i = 0; i < 3 && snapshot().get("repeat").getAsInt() != repeat; i++) {
          WindowsMedia.command(3);
          Thread.sleep(300);
        }
      }
      WindowsMedia.close();
    }
  }
}
