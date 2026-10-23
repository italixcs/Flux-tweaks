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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SpotifyHud implements AutoCloseable {
  public record Bounds(float x, float y, float width, float height, float scale) {}

  private record Button(int command, float x, String label) {}

  public static final int WIDTH = 260, HEIGHT = 136;
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
  private final Ui.Motion motion = new Ui.Motion();
  private long pressedUntil;
  private int pressedCommand = -1;
  private static final int BUTTON_Y = 94, BUTTON_W = 32, BUTTON_H = 28;

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
    Ui.surface(g, 0, 0, WIDTH, HEIGHT);
    Ui.rounded(g, 14, 14, 48, 48, 8, Ui.inset());
    if (hasArtwork && track.available()) SmoothGui.image(g, COVER, 14, 14, 48, 8);
    else Ui.centered(g, "♫", 14, 14, 48, 48, Ui.secondary());
    Ui.outline(g, 14, 14, 48, 48, 8, 0x20FFFFFF);
    Ui.text(
        g,
        Ui.fit(
            track.available() ? safe(track.title(), "Unknown track") : "Spotify HUD preview", 170),
        76,
        18,
        Ui.WHITE);
    Ui.text(
        g,
        Ui.fit(
            track.available() ? safe(track.artist(), "Unknown artist") : "Spotify desktop app",
            170),
        76,
        35,
        Ui.MUTED);
    Ui.circle(g, 76, 54, 4, track.available() && track.playing() ? Ui.secondary() : Ui.MUTED);
    Ui.text(
        g,
        track.available() ? (track.playing() ? "Playing" : "Paused") : "Preview",
        86,
        51,
        Ui.MUTED);
    long duration = track.available() ? track.duration() : 240000,
        position = track.available() ? frame.position() : 85000;
    Ui.text(g, time(position), 14, 70, Ui.MUTED);
    Ui.right(g, time(duration), WIDTH - 14, 70, Ui.MUTED);
    int barX = 14, barWidth = WIDTH - 28;
    Ui.rounded(g, barX, 84, barWidth, 4, 2, Ui.inset());
    Ui.rounded(
        g,
        barX,
        84,
        (float) (barWidth * Math.clamp(position / (double) Math.max(1, duration), 0, 1)),
        4,
        2,
        Ui.primary());
    buttons.clear();
    List<Integer> commands = new ArrayList<>();
    if (!track.available() || track.previous()) commands.add(1);
    if (!track.available() || track.toggle()) commands.add(0);
    if (!track.available() || track.next()) commands.add(2);
    if (!track.available() || track.repeatSupported()) commands.add(3);
    float x = (WIDTH - commands.size() * BUTTON_W - Math.max(0, commands.size() - 1) * 8) / 2f;
    boolean interactive = mc.screen instanceof FluxScreen || mc.screen instanceof HudEditorScreen;
    double mx =
        (mc.mouseHandler.xpos()
                    * mc.getWindow().getGuiScaledWidth()
                    / mc.getWindow().getScreenWidth()
                - box.x)
            / box.scale;
    double my =
        (mc.mouseHandler.ypos()
                    * mc.getWindow().getGuiScaledHeight()
                    / mc.getWindow().getScreenHeight()
                - box.y)
            / box.scale;
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
      boolean hover = interactive && Ui.inside(mx, my, x, BUTTON_Y, BUTTON_W, BUTTON_H);
      float amount = motion.value("hover." + command, hover ? 1 : 0);
      Ui.rounded(
          g,
          x,
          BUTTON_Y,
          BUTTON_W,
          BUTTON_H,
          8,
          command == 0
              ? Ui.alpha(FluxTweaks.config.theme.primary, .24f + amount * .12f)
              : Ui.inset());
      if (amount > 0)
        Ui.rounded(g, x, BUTTON_Y, BUTTON_W, BUTTON_H, 8, Ui.alpha(0xffffff, amount * .07f));
      if (hover || (pressedCommand == command && System.nanoTime() < pressedUntil))
        Ui.outline(g, x, BUTTON_Y, BUTTON_W, BUTTON_H, 8, Ui.secondary());
      drawButton(
          g,
          command,
          (int) x + 5,
          BUTTON_Y + 7,
          track.available() && track.playing(),
          track.repeat());
      x += BUTTON_W + 8;
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
          Ui.rounded(g, x + 6, y + 1, 3, 11, 1, color);
          Ui.rounded(g, x + 12, y + 1, 3, 11, 1, color);
        } else Ui.triangle(g, x + 8, y + 1, 13, true, color);
      }
      case 1 -> {
        Ui.rounded(g, x + 5, y + 2, 2, 9, 1, color);
        Ui.triangle(g, x + 8, y + 1, 13, false, color);
      }
      case 2 -> {
        Ui.triangle(g, x + 5, y + 1, 13, true, color);
        Ui.rounded(g, x + 14, y + 2, 2, 9, 1, color);
      }
      case 3 -> {
        // Rounded vector strokes use the same analytic coverage as the other HUD controls.
        Ui.line(g, x + 4.5f, y + 6, x + 4.5f, y + 5, 1.4f, color);
        Ui.line(g, x + 4.5f, y + 5, x + 4.9f, y + 3.5f, 1.4f, color);
        Ui.line(g, x + 4.9f, y + 3.5f, x + 6, y + 2.4f, 1.4f, color);
        Ui.line(g, x + 6, y + 2.4f, x + 7.5f, y + 2, 1.4f, color);
        Ui.line(g, x + 7.5f, y + 2, x + 18, y + 2, 1.4f, color);
        Ui.line(g, x + 15, y - 1, x + 18, y + 2, 1.4f, color);
        Ui.line(g, x + 18, y + 2, x + 15, y + 5, 1.4f, color);
        Ui.line(g, x + 17.5f, y + 7, x + 17.5f, y + 8, 1.4f, color);
        Ui.line(g, x + 17.5f, y + 8, x + 17.1f, y + 9.5f, 1.4f, color);
        Ui.line(g, x + 17.1f, y + 9.5f, x + 16, y + 10.6f, 1.4f, color);
        Ui.line(g, x + 16, y + 10.6f, x + 14.5f, y + 11, 1.4f, color);
        Ui.line(g, x + 14.5f, y + 11, x + 4, y + 11, 1.4f, color);
        Ui.line(g, x + 7, y + 8, x + 4, y + 11, 1.4f, color);
        Ui.line(g, x + 4, y + 11, x + 7, y + 14, 1.4f, color);
        if (repeat == 1) {
          Ui.line(g, x + 9, y + 5, x + 11, y + 4, 1, color);
          Ui.line(g, x + 11, y + 4, x + 11, y + 8, 1, color);
        }
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
      if (Ui.inside(x, y, button.x, BUTTON_Y, BUTTON_W, BUTTON_H)) {
        pressedCommand = button.command;
        pressedUntil = System.nanoTime() + 140_000_000L;
        FluxTweaks.spotify.command(button.command);
        return true;
      }
    return true;
  }

  public boolean controlAt(double mx, double my) {
    Bounds b = bounds();
    double x = (mx - b.x) / b.scale, y = (my - b.y) / b.scale;
    return buttons.stream()
        .anyMatch(button -> Ui.inside(x, y, button.x, BUTTON_Y, BUTTON_W, BUTTON_H));
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
