package dev.fluxtweaks.feature;

import com.mojang.blaze3d.platform.NativeImage;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class Effects implements AutoCloseable {
  private record Jump(boolean grounded, double y) {}

  private final List<Fx> live = new ArrayList<>();
  private final Map<UUID, Jump> jumps = new HashMap<>();
  private final Random random = new Random();
  private final ExecutorService imageWorker =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Flux PNG import");
            t.setDaemon(true);
            return t;
          });
  private static final Identifier CUSTOM = FluxTweaks.id("custom_jump_circle");
  private boolean customReady;
  private volatile boolean closed;

  public Effects() {
    Path saved = FluxTweaks.dataDirectory().resolve("jump-circle.png");
    if (Files.exists(saved)) importPng(saved);
  }

  public void clear() {
    live.forEach(Fx::remove);
    live.clear();
    jumps.clear();
  }

  public void hit(LivingEntity target) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    int amount = (int) FluxTweaks.config.number(Feature.HIT_PARTICLES, "amount");
    for (int i = 0; i < amount && live.size() < 512; i++) {
      var c = FluxTweaks.config;
      String shape = c.text(Feature.HIT_PARTICLES, "shape").toLowerCase(Locale.ROOT);
      boolean glow = c.bool(Feature.HIT_PARTICLES, "glow");
      double speed = c.number(Feature.HIT_PARTICLES, "speed") * .04;
      Fx particle =
          new Fx(
              mc.level,
              Feature.HIT_PARTICLES,
              target
                  .position()
                  .add(
                      (random.nextDouble() - .5) * target.getBbWidth(),
                      target.getBbHeight() * .55 + (random.nextDouble() - .5) * .4,
                      (random.nextDouble() - .5) * target.getBbWidth()),
              texture(shape + (glow ? "_glow" : "")),
              c.color(Feature.HIT_PARTICLES, "color"),
              c.decimal(Feature.HIT_PARTICLES, "scale") * .08f,
              (int) (c.number(Feature.HIT_PARTICLES, "lifetime") * 20),
              glow,
              false);
      particle.motion(
          (random.nextDouble() - .5) * speed,
          (random.nextDouble() - .3) * speed,
          (random.nextDouble() - .5) * speed);
      particle.physics(c.text(Feature.HIT_PARTICLES, "physics"));
      double[] angle = c.range(Feature.HIT_PARTICLES, "angle"),
          rotation = c.range(Feature.HIT_PARTICLES, "rotation");
      particle.rotation(between(angle), between(rotation));
      add(particle);
    }
  }

  private double between(double[] range) {
    return range[0] + random.nextDouble() * (range[1] - range[0]);
  }

  private static Identifier texture(String name) {
    return FluxTweaks.id("textures/particle/" + name + ".png");
  }

  private void add(Fx particle) {
    Minecraft mc = Minecraft.getInstance();
    mc.getTextureManager().getTexture(particle.layer.textureAtlasLocation());
    live.add(particle);
    mc.particleEngine.add(particle);
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    live.removeIf(p -> !p.isAlive());
    for (Player player : mc.level.players()) {
      Jump old = jumps.put(player.getUUID(), new Jump(player.onGround(), player.getY()));
      if (!FluxTweaks.active(Feature.JUMP_CIRCLE)
          || old == null
          || !old.grounded
          || player.onGround()
          || player.getY() - old.y < .025
          || player.isPassenger()
          || player.isInWater()
          || player.onClimbable()) continue;
      if (FluxTweaks.config.bool(Feature.JUMP_CIRCLE, "self") && player != mc.player) continue;
      if (mc.player == null || player.distanceToSqr(mc.player) > 48 * 48 || live.size() >= 512)
        continue;
      String style = FluxTweaks.config.text(Feature.JUMP_CIRCLE, "style");
      Identifier tex =
          style.equals("Custom") && customReady
              ? CUSTOM
              : texture(style.equals("Default") ? "circle" : "circle_glow");
      add(
          new Fx(
              mc.level,
              Feature.JUMP_CIRCLE,
              new Vec3(player.getX(), old.y + .025, player.getZ()),
              tex,
              FluxTweaks.config.color(Feature.JUMP_CIRCLE, "color"),
              FluxTweaks.config.decimal(Feature.JUMP_CIRCLE, "size"),
              (int) (FluxTweaks.config.number(Feature.JUMP_CIRCLE, "duration") * 20),
              true,
              true));
    }
    Set<UUID> present = new HashSet<>();
    mc.level.players().forEach(p -> present.add(p.getUUID()));
    jumps.keySet().retainAll(present);
    if (FluxTweaks.active(Feature.AMBIENCE)
        && FluxTweaks.config.bool(Feature.AMBIENCE, "particles")) {
      long count = live.stream().filter(p -> p.feature == Feature.AMBIENCE).count();
      int wanted = (int) FluxTweaks.config.number(Feature.AMBIENCE, "amount");
      for (int i = 0; i < Math.min(6, wanted - count) && live.size() < 512; i++) {
        double distance = FluxTweaks.config.number(Feature.AMBIENCE, "distance");
        Vec3 center = mc.gameRenderer.mainCamera().position();
        Vec3 offset =
            new Vec3(
                    random.nextDouble() * 2 - 1,
                    random.nextDouble() * 1.2 - .3,
                    random.nextDouble() * 2 - 1)
                .scale(distance);
        Fx particle =
            new Fx(
                mc.level,
                Feature.AMBIENCE,
                center.add(offset),
                texture(FluxTweaks.config.text(Feature.AMBIENCE, "shape").toLowerCase(Locale.ROOT)),
                FluxTweaks.config.color(Feature.AMBIENCE, "color"),
                FluxTweaks.config.decimal(Feature.AMBIENCE, "size"),
                100 + random.nextInt(100),
                true,
                false);
        double speed = FluxTweaks.config.number(Feature.AMBIENCE, "speed") / 20;
        particle.motion(
            (random.nextDouble() - .5) * speed, speed * .25, (random.nextDouble() - .5) * speed);
        particle.rotation = .015f;
        add(particle);
      }
    }
  }

  public void importPng(Path source) {
    if (closed) return;
    imageWorker.execute(
        () -> {
          NativeImage image = null;
          try {
            if (Files.size(source) > 4 * 1024 * 1024)
              throw new IOException("PNG must be at most 4 MB");
            byte[] bytes = Files.readAllBytes(source);
            if (bytes.length < 8
                || bytes[0] != (byte) 137
                || bytes[1] != 80
                || bytes[2] != 78
                || bytes[3] != 71) throw new IOException("Choose a PNG image");
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
              var readers = ImageIO.getImageReaders(stream);
              if (!readers.hasNext()) throw new IOException("PNG cannot be read");
              var reader = readers.next();
              try {
                reader.setInput(stream);
                if (reader.getWidth(0) > 2048 || reader.getHeight(0) > 2048)
                  throw new IOException("PNG must be at most 2048 x 2048");
              } finally {
                reader.dispose();
              }
            }
            image = NativeImage.read(new ByteArrayInputStream(bytes));
            Path destination = FluxTweaks.dataDirectory().resolve("jump-circle.png");
            Files.createDirectories(destination.getParent());
            if (!source
                .toAbsolutePath()
                .normalize()
                .equals(destination.toAbsolutePath().normalize())) {
              Path temp = Files.createTempFile(destination.getParent(), "circle-", ".png");
              try {
                Files.write(temp, bytes);
                try {
                  Files.move(
                      temp,
                      destination,
                      StandardCopyOption.ATOMIC_MOVE,
                      StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                  Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
                }
              } finally {
                Files.deleteIfExists(temp);
              }
            }
            NativeImage decoded = image;
            Minecraft.getInstance()
                .execute(
                    () -> {
                      if (closed) {
                        decoded.close();
                        return;
                      }
                      Minecraft.getInstance()
                          .getTextureManager()
                          .register(
                              CUSTOM, new DynamicTexture(() -> "Flux custom jump circle", decoded));
                      customReady = true;
                      FluxTweaks.message = "Custom jump circle imported";
                    });
            image = null;
          } catch (Exception ex) {
            FluxTweaks.LOGGER.warn("Custom circle import rejected", ex);
            Minecraft.getInstance().execute(() -> FluxTweaks.message = ex.getMessage());
          } finally {
            if (image != null) image.close();
          }
        });
  }

  @Override
  public void close() {
    closed = true;
    clear();
    imageWorker.shutdownNow();
    if (customReady) Minecraft.getInstance().getTextureManager().release(CUSTOM);
  }

  private static final class Fx extends SingleQuadParticle {
    final Feature feature;
    final Layer layer;
    final boolean glow, circle;
    float rotation;
    boolean wave;

    Fx(
        ClientLevel level,
        Feature feature,
        Vec3 at,
        Identifier texture,
        int color,
        float size,
        int ticks,
        boolean glow,
        boolean circle) {
      super(level, at.x, at.y, at.z, null);
      this.feature = feature;
      this.glow = glow;
      this.circle = circle;
      this.layer =
          new Layer(
              true, texture, net.minecraft.client.renderer.RenderPipelines.TRANSLUCENT_PARTICLE);
      this.quadSize = size;
      this.lifetime = Math.max(2, ticks);
      this.hasPhysics = !circle;
      this.friction = .97f;
      setColor(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
    }

    void motion(double x, double y, double z) {
      xd = x;
      yd = y;
      zd = z;
    }

    void physics(String mode) {
      switch (mode) {
        case "Fall" -> gravity = .35f;
        case "Rise" -> {
          gravity = -.08f;
          yd = Math.abs(yd);
        }
        case "Burst" -> {
          xd *= 2;
          yd *= 2;
          zd *= 2;
        }
        case "Fly" -> {
          yd = .02;
          wave = true;
        }
      }
    }

    void rotation(double angle, double speed) {
      roll = oRoll = (float) Math.toRadians(angle);
      rotation = (float) Math.toRadians(speed) / 20;
    }

    @Override
    public void tick() {
      if (!FluxTweaks.active(feature)
          || (feature == Feature.AMBIENCE && !FluxTweaks.config.bool(feature, "particles"))) {
        remove();
        return;
      }
      oRoll = roll;
      roll += rotation;
      if (circle) {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) remove();
      } else {
        if (wave) yd += Math.sin(age * .3) * .001;
        super.tick();
      }
      float progress = age / (float) lifetime;
      alpha = circle ? 1 - progress : Math.min(1, (1 - progress) * 3);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float delta) {
      if (circle)
        extractRotatedQuad(state, camera, new Quaternionf().rotationX(-(float) Math.PI / 2), delta);
      else super.extract(state, camera, delta);
    }

    @Override
    protected Layer getLayer() {
      return layer;
    }

    @Override
    protected float getU0() {
      return 0;
    }

    @Override
    protected float getU1() {
      return 1;
    }

    @Override
    protected float getV0() {
      return 0;
    }

    @Override
    protected float getV1() {
      return 1;
    }

    @Override
    protected int getLightCoords(float delta) {
      return glow ? 0xF000F0 : super.getLightCoords(delta);
    }
  }
}
