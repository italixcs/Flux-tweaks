package dev.fluxtweaks.media;

import com.google.gson.Gson;
import dev.fluxtweaks.FluxTweaks;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class SpotifyService implements AutoCloseable {
  public record Track(
      boolean available,
      String title,
      String artist,
      String artworkKey,
      boolean playing,
      long position,
      long duration,
      boolean previous,
      boolean next,
      boolean toggle,
      boolean repeatSupported,
      int repeat) {
    public static Track empty() {
      return new Track(false, "", "", "", false, 0, 0, false, false, false, false, 0);
    }
  }

  public record Frame(Track track, long receivedNanos, byte[] artwork) {
    public long position() {
      return Math.clamp(
          track.position()
              + (track.playing() ? (System.nanoTime() - receivedNanos) / 1_000_000 : 0),
          0,
          Math.max(0, track.duration()));
    }
  }

  private final ScheduledExecutorService worker =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "Flux Tweaks Spotify");
            t.setDaemon(true);
            return t;
          });
  private final AtomicReference<Frame> frame =
      new AtomicReference<>(new Frame(Track.empty(), 0, new byte[0]));
  private volatile boolean enabled;
  private volatile String status = "Disabled";
  private boolean loaded, opened, failed;
  private final Gson gson = new Gson();

  public SpotifyService() {
    worker.scheduleWithFixedDelay(this::update, 0, 500, TimeUnit.MILLISECONDS);
  }

  public void enabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Frame frame() {
    return frame.get();
  }

  public String status() {
    return status;
  }

  private void update() {
    try {
      if (!enabled) {
        if (opened) {
          WindowsMedia.close();
          opened = false;
        }
        frame.set(new Frame(Track.empty(), 0, new byte[0]));
        status = "Disabled";
        failed = false;
        return;
      }
      if (failed) return;
      if (!loaded) {
        WindowsMedia.load();
        loaded = true;
      }
      if (!opened) {
        WindowsMedia.open();
        opened = true;
      }
      Track track = gson.fromJson(WindowsMedia.snapshot(), Track.class);
      if (track == null || !track.available()) {
        frame.set(new Frame(Track.empty(), 0, new byte[0]));
        status = "Spotify desktop app not detected";
        return;
      }
      Frame old = frame.get();
      byte[] art =
          track.artworkKey().equals(old.track().artworkKey())
              ? old.artwork()
              : WindowsMedia.artwork();
      frame.set(new Frame(track, System.nanoTime(), art));
      status = track.playing() ? "Playing" : "Paused";
    } catch (Exception | LinkageError ex) {
      failed = true;
      status = "Media connection unavailable: " + ex.getMessage();
      FluxTweaks.LOGGER.error("Spotify bridge unavailable", ex);
    }
  }

  public void command(int command) {
    worker.execute(
        () -> {
          if (opened) {
            try {
              if (!WindowsMedia.command(command)) status = "Spotify did not accept that control";
            } catch (RuntimeException ex) {
              status = "Spotify control failed";
              FluxTweaks.LOGGER.warn("Spotify control failed", ex);
            }
          }
        });
  }

  @Override
  public void close() {
    enabled = false;
    worker.execute(
        () -> {
          if (opened) {
            WindowsMedia.close();
            opened = false;
          }
        });
    worker.shutdown();
  }
}
