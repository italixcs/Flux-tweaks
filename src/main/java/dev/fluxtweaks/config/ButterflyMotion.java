package dev.fluxtweaks.config;

/** Pure elapsed-time poses, sampled without advancing state during rendering passes. */
public final class ButterflyMotion {
  public record Pose(float tilt, float yaw, float roll, float handleA, float handleB, float lift) {
    public static final Pose READY = new Pose(0, 0, 0, 0, 0, 0);

    private Pose mix(Pose other, double amount) {
      float t = (float) amount;
      return new Pose(
          tilt + (other.tilt - tilt) * t, yaw + (other.yaw - yaw) * t,
          roll + (other.roll - roll) * t, handleA + (other.handleA - handleA) * t,
          handleB + (other.handleB - handleB) * t, lift + (other.lift - lift) * t);
    }

    // The safe handle's contact point follows its hinge; the blade is never the spin pivot.
    public float gripX() { return -.52f + 5.4f * (float) Math.sin(Math.toRadians(handleA)); }
    public float gripY() { return -5.4f * (float) Math.cos(Math.toRadians(handleA)); }
  }

  private static final Pose[] EQUIP = {
    new Pose(-12, -18, -35, 175, -175, -2),
    new Pose(8, 12, 55, 80, -220, 1),
    new Pose(-5, 0, -15, -35, 35, .4f), Pose.READY
  };
  private double started = Double.NEGATIVE_INFINITY, duration;
  private String style;
  private Pose origin = Pose.READY;
  private boolean inspection, reduced;

  public void equip(double now, boolean reduced, double speed) {
    cancel();
    if (!reduced) start("Equip", .7, now, false, false, speed);
  }

  public boolean inspect(String style, double now, boolean reduced, double speed) {
    if (inspecting(now)) return false;
    origin = sample(now);
    start(style, reduced ? .6 : style.equals("Flip") ? 1.2 : style.equals("Spin") ? 1.8 : 2.4,
        now, true, reduced, speed);
    return true;
  }

  private void start(String style, double seconds, double now, boolean inspecting, boolean reduced, double speed) {
    this.style = style;
    this.duration = seconds / (Double.isFinite(speed) ? Math.clamp(speed, .25, 3) : 1);
    started = now;
    inspection = inspecting;
    this.reduced = reduced;
  }

  public boolean inspecting(double now) {
    return inspection && now >= started && now < started + duration;
  }

  public Pose sample(double now) {
    if (!Double.isFinite(now) || now < started || now >= started + duration) return Pose.READY;
    double t = (now - started) / duration;
    if (!inspection) {
      double frame = t * (EQUIP.length - 1);
      int index = Math.min((int) frame, EQUIP.length - 2);
      return EQUIP[index].mix(EQUIP[index + 1], ease(frame - index));
    }
    Pose pose;
    if (reduced) {
      double turn = bump(t, 0, .5, 1);
      pose = new Pose(0, (float) (18 * turn), (float) (-8 * turn), 0, 0, 0);
    } else {
      // One continuous angular sweep, with separate release/catch timing for the free handle.
      boolean flip = style.equals("Flip"), spin = style.equals("Spin");
      double turn = phase(t, .14, .86);
      double catchKick = bump(t, .86, .93, 1);
      double roll = flip ? -28 * bump(t, 0, .18, .8)
          : -28 * phase(t, 0, .14) + 748 * turn - 7 * catchKick;
      double safeHandle = flip ? -160 * bump(t, .08, .30, .76)
          : spin ? 10 * Math.sin(turn * Math.PI * 4) * Math.sin(t * Math.PI)
          : -145 * bump(t, .06, .25, .46) + 65 * bump(t, .48, .64, .83);
      double freeHandle = -360 * phase(t, .12, flip ? .84 : .49)
          - (flip ? 0 : 360 * phase(t, .51, .91));
      double envelope = Math.pow(Math.sin(t * Math.PI), 2);
      pose = new Pose((float) (-12 * envelope + 9 * Math.sin(turn * Math.PI * 2) * envelope),
          (float) (-24 * envelope), (float) roll, (float) safeHandle, (float) freeHandle,
          (float) (5.8 * envelope - .24 * catchKick));
    }
    // Starting an inspect halfway through drawing the knife must not teleport it.
    return origin.mix(pose, phase(t, 0, .14));
  }

  private static double phase(double t, double from, double to) {
    return ease(Math.clamp((t - from) / (to - from), 0, 1));
  }

  private static double bump(double t, double from, double peak, double to) {
    return t < peak ? phase(t, from, peak) : 1 - phase(t, peak, to);
  }

  private static double ease(double t) {
    return t * t * t * (10 + t * (-15 + 6 * t));
  }

  public void cancel() {
    started = Double.NEGATIVE_INFINITY;
    duration = 0;
    inspection = false;
    origin = Pose.READY;
  }
}
