package dev.fluxtweaks.feature;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.mixin.CameraAccess;
import net.minecraft.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CameraModes {
  private CameraType previousPerspective;
  private Vec3 position = Vec3.ZERO, previous = Vec3.ZERO;
  private float yaw, pitch;

  public boolean change(Feature feature, boolean enabled) {
    Minecraft mc = Minecraft.getInstance();
    if (enabled) {
      if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
        FluxTweaks.message = "Join a world before enabling camera features";
        return false;
      }
      if (FluxTweaks.active(feature)) return true;
      reset();
      previousPerspective = mc.options.getCameraType();
      yaw = mc.player.getYRot();
      pitch = mc.player.getXRot();
      position = previous = mc.player.getEyePosition();
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      if (feature == Feature.FREECAM) mc.player.stopUsingItem();
    } else if (FluxTweaks.active(feature)) reset();
    return true;
  }

  public void reset() {
    if (previousPerspective != null) {
      Minecraft.getInstance().options.setCameraType(previousPerspective);
      previousPerspective = null;
    }
    FluxTweaks.config.module(Feature.FREELOOK).enabled = false;
    FluxTweaks.config.module(Feature.FREECAM).enabled = false;
  }

  public boolean active() {
    return FluxTweaks.active(Feature.FREECAM) || FluxTweaks.active(Feature.FREELOOK);
  }

  public void turn(double dx, double dy) {
    yaw = Mth.wrapDegrees(yaw + (float) dx * .15f);
    pitch = Math.clamp(pitch + (float) dy * .15f, -90, 90);
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || !mc.player.isAlive()) {
      if (active()) reset();
      return;
    }
    previous = position;
    if (!FluxTweaks.active(Feature.FREECAM) || mc.screen != null) return;
    double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
    double right = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
    double up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
    double angle = Math.toRadians(yaw), tilt = Math.toRadians(pitch);
    Vec3 direction =
        new Vec3(
            -Math.sin(angle) * Math.cos(tilt), -Math.sin(tilt), Math.cos(angle) * Math.cos(tilt));
    Vec3 movement =
        direction.scale(forward).add(Math.cos(angle) * -right, up, -Math.sin(angle) * right);
    if (movement.lengthSqr() > 1) movement = movement.normalize();
    double speed =
        FluxTweaks.config.number(Feature.FREECAM, "speed")
            * (mc.options.keySprint.isDown()
                ? FluxTweaks.config.number(Feature.FREECAM, "sprint")
                : 1);
    position = position.add(movement.scale(speed));
  }

  public void apply(Camera camera, float delta) {
    if (!active()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;
    CameraAccess access = (CameraAccess) camera;
    access.flux$rotation(yaw, pitch);
    access.flux$detached(true);
    if (FluxTweaks.active(Feature.FREECAM)) access.flux$position(previous.lerp(position, delta));
    else {
      access.flux$position(mc.player.getEyePosition(delta));
      access.flux$move(-access.flux$zoom(4), 0, 0);
    }
  }
}
