package dev.fluxtweaks.media;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.gui.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SpotifyHud implements AutoCloseable {
  public record Bounds(float x, float y, float width, float height, float scale) {}

  private record Button(int command, float x, String label) {}

  public static final int WIDTH = 242, HEIGHT = 98;
  private static final Identifier COVER = FluxTweaks.id("spotify_cover");
  private final List<Button> buttons = new ArrayList<>();
  private final ExecutorService artworkWorker =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Flux cover artwork");
            t.setDaemon(true);
            return t;
          });
  private volatile String requestedKey = "";
  private boolean hasArtwork;
  private volatile boolean closed;
  private int artworkWidth = 1, artworkHeight = 1;

  public Bounds bounds() {
    Minecraft mc = Minecraft.getInstance();
    float scale = FluxTweaks.config.decimal(Feature.SPOTIFY, "scale"),
        w = mc.getWindow().getGuiScaledWidth(),
        h = mc.getWindow().getGuiScaledHeight();
    scale = Math.min(scale, Math.min(w / WIDTH, h / HEIGHT));
    return new Bounds(
        (float) Math.clamp(FluxTweaks.config.layout.hudX * w, 0, Math.max(0, w - WIDTH * scale)),
        (float) Math.clamp(FluxTweaks.config.layout.hudY * h, 0, Math.max(0, h - HEIGHT * scale)),
        WIDTH * scale,
        HEIGHT * scale,
        scale);
  }

  public void render(GuiGraphics g, boolean preview) {
    Minecraft mc = Minecraft.getInstance();
    if (!preview && mc.screen instanceof HudEditorScreen) return;
    SpotifyService.Frame frame = FluxTweaks.spotify.frame();
    SpotifyService.Track track = frame.track();
    if (!preview && (!FluxTweaks.active(Feature.SPOTIFY) || !track.available())) {
      if (hasArtwork && !FluxTweaks.active(Feature.SPOTIFY)) {
        mc.getTextureManager().release(COVER);
        hasArtwork = false;
        requestedKey = "";
      }
      return;
    }
    if (track.available()) artwork(frame);
    Bounds box = bounds();
    g.pose().pushMatrix();
    g.pose().translate(box.x, box.y);
    g.pose().scale(box.scale, box.scale);
    Ui.rounded(g, 0, 0, WIDTH, HEIGHT, 12, Ui.panel());
    Ui.rounded(g, 10, 10, 43, 43, 6, Ui.inset());
    if (hasArtwork && track.available())
      g.blit(
          RenderPipelines.GUI_TEXTURED,
          COVER,
          10,
          10,
          0,
          0,
          43,
          43,
          artworkWidth,
          artworkHeight,
          artworkWidth,
          artworkHeight);
    else {
      Ui.text(g, "♫", 24, 27, Ui.secondary());
    }
    Ui.text(
        g,
        Ui.fit(
            track.available() ? safe(track.title(), "Unknown track") : "Spotify HUD preview", 160),
        65,
        15,
        Ui.WHITE);
    Ui.text(
        g,
        Ui.fit(
            track.available() ? safe(track.artist(), "Unknown artist") : "Spotify desktop app",
            160),
        65,
        30,
        Ui.MUTED);
    Ui.text(
        g,
        track.available() ? (track.playing() ? "Playing" : "Paused") : "Drag to reposition",
        65,
        44,
        Ui.MUTED);
    long duration = track.available() ? track.duration() : 240000,
        position = track.available() ? frame.position() : 85000;
    Ui.text(g, time(position), 12, 61, Ui.MUTED);
    Ui.right(g, time(duration), WIDTH - 12, 61, Ui.MUTED);
    int barX = 43, barWidth = WIDTH - 86;
    Ui.rounded(g, barX, 64, barWidth, 3, 2, Ui.inset());
    Ui.rounded(
        g,
        barX,
        64,
        Math.max(2, (int) (barWidth * Math.clamp(position / (double) Math.max(1, duration), 0, 1))),
        3,
        2,
        Ui.WHITE);
    buttons.clear();
    List<Integer> commands = new ArrayList<>();
    if (!track.available() || track.previous()) commands.add(1);
    if (!track.available() || track.toggle()) commands.add(0);
    if (!track.available() || track.next()) commands.add(2);
    if (!track.available() || track.repeatSupported()) commands.add(3);
    float x = WIDTH / 2f - commands.size() * 13;
    for (int command : commands) {
      buttons.add(
          new Button(
              command,
              x,
              switch (command) {
                case 0 -> track.playing() ? "Pause" : "Play";
                case 1 -> "Previous track";
                case 2 -> "Next track";
                default -> "Repeat";
              }));
      drawButton(g, command, (int) x, 77, !track.available() || track.playing(), track.repeat());
      x += 26;
    }
    g.pose().popMatrix();
  }

  private static String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String time(long millis) {
    long seconds = Math.max(0, millis / 1000);
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
  }

  private static void drawButton(
      GuiGraphics g, int command, int x, int y, boolean playing, int repeat) {
    int color = command == 3 && repeat > 0 ? Ui.secondary() : Ui.WHITE;
    switch (command) {
      case 0 -> {
        if (playing) {
          g.fill(x + 6, y + 1, x + 9, y + 12, color);
          g.fill(x + 12, y + 1, x + 15, y + 12, color);
        } else Ui.triangle(g, x + 8, y + 1, 13, true, color);
      }
      case 1 -> {
        g.fill(x + 5, y + 2, x + 7, y + 11, color);
        Ui.triangle(g, x + 8, y + 1, 13, false, color);
      }
      case 2 -> {
        Ui.triangle(g, x + 5, y + 1, 13, true, color);
        g.fill(x + 14, y + 2, x + 16, y + 11, color);
      }
      case 3 -> {
        g.fill(x + 5, y + 2, x + 16, y + 3, color);
        g.fill(x + 4, y + 3, x + 5, y + 7, color);
        g.fill(x + 6, y + 10, x + 17, y + 11, color);
        g.fill(x + 17, y + 6, x + 18, y + 10, color);
        Ui.triangle(g, x + 13, y, 7, true, color);
        Ui.triangle(g, x + 4, y + 7, 7, false, color);
        if (repeat == 1) Ui.text(g, "1", x + 10, y + 4, color);
      }
      default -> {}
    }
  }

  public boolean click(double mx, double my) {
    if (!(Minecraft.getInstance().screen instanceof FluxScreen
        || Minecraft.getInstance().screen instanceof HudEditorScreen)) return false;
    Bounds b = bounds();
    if (!Ui.inside(mx, my, b.x, b.y, b.width, b.height)) return false;
    var track = FluxTweaks.spotify.frame().track();
    if (!FluxTweaks.active(Feature.SPOTIFY) || !track.available()) return true;
    double x = (mx - b.x) / b.scale, y = (my - b.y) / b.scale;
    for (Button button : buttons)
      if (Ui.inside(x, y, button.x, 73, 23, 22)) {
        FluxTweaks.spotify.command(button.command);
        return true;
      }
    return true;
  }

  public boolean controlAt(double mx, double my) {
    Bounds b = bounds();
    double x = (mx - b.x) / b.scale, y = (my - b.y) / b.scale;
    return buttons.stream().anyMatch(button -> Ui.inside(x, y, button.x, 73, 23, 22));
  }

  private void artwork(SpotifyService.Frame frame) {
    String key = frame.track().artworkKey();
    if (key.equals(requestedKey)) return;
    requestedKey = key;
    hasArtwork = false;
    if (frame.artwork().length == 0) {
      Minecraft.getInstance().getTextureManager().release(COVER);
      return;
    }
    artworkWorker.execute(
        () -> {
          NativeImage decoded = null;
          try {
            decoded = NativeImage.read(new ByteArrayInputStream(frame.artwork()));
            if (decoded.getWidth() > 2048 || decoded.getHeight() > 2048)
              throw new IOException("Spotify artwork exceeds size limit");
            NativeImage image = decoded;
            decoded = null;
            Minecraft.getInstance()
                .execute(
                    () -> {
                      if (closed || !key.equals(requestedKey)) {
                        image.close();
                        return;
                      }
                      artworkWidth = image.getWidth();
                      artworkHeight = image.getHeight();
                      Minecraft.getInstance()
                          .getTextureManager()
                          .register(COVER, new DynamicTexture(() -> "Flux Spotify cover", image));
                      hasArtwork = true;
                    });
          } catch (IOException ex) {
            FluxTweaks.LOGGER.debug("Spotify artwork unavailable", ex);
          } finally {
            if (decoded != null) decoded.close();
          }
        });
  }

  @Override
  public void close() {
    closed = true;
    artworkWorker.shutdown();
    Minecraft.getInstance().getTextureManager().release(COVER);
  }
}
