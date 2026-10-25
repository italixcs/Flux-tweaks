package dev.fluxtweaks.config;

/** Pure elapsed-time poses, sampled without advancing state during rendering passes. */
public final class ButterflyMotion {
  public record Pose(float tilt, float yaw, float roll, float handleA, float handleB, float lift) {
    public static final Pose READY = new Pose(0, 0, 0, 0, 0, 0);

    private Pose mix(Pose other, double amount) {
      float t = (float) (amount * amount * (3 - 2 * amount));
      return new Pose(
          tilt + (other.tilt - tilt) * t, yaw + (other.yaw - yaw) * t,
          roll + (other.roll - roll) * t, handleA + (other.handleA - handleA) * t,
          handleB + (other.handleB - handleB) * t, lift + (other.lift - lift) * t);
    }
  }

  private static final Pose[] EQUIP = {
    new Pose(-12, -18, -35, 175, -175, -2),
    new Pose(8, 12, 55, 80, -220, 1),
    new Pose(-5, 0, -15, -35, 35, .4f), Pose.READY
  };
  private static final Pose[] FLIP = {
    Pose.READY, new Pose(12, -15, -25, 20, -180, .4f),
    new Pose(-10, 10, 65, -35, -345, 1.1f),
    new Pose(5, 0, -10, 10, -370, .2f), new Pose(0, 0, 0, 0, -360, 0)
  };
  private static final Pose[] SPIN = {
    Pose.READY, new Pose(-10, -15, 65, 30, -150, .7f),
    new Pose(12, 20, 190, -40, -310, 1.2f),
    new Pose(-8, -10, 300, 20, -390, .5f), new Pose(0, 0, 360, 0, -360, 0)
  };
  private static final Pose[] FLOURISH = {
    Pose.READY, new Pose(15, -25, -45, 160, -150, .7f),
    new Pose(-15, 35, 80, 340, -300, 1.5f),
    new Pose(12, -20, 220, 210, -500, .8f),
    new Pose(-8, 10, 325, 390, -690, .3f), new Pose(0, 0, 360, 360, -720, 0)
  };
  private static final Pose[] REDUCED = {
    Pose.READY, new Pose(0, 18, -8, 0, 0, 0), Pose.READY
  };
  private double started = Double.NEGATIVE_INFINITY, duration;
  private Pose[] frames = REDUCED;
  private boolean inspection;

  public void equip(double now, boolean reduced) {
    cancel();
    if (!reduced) start(EQUIP, .7, now, false);
  }

  public boolean inspect(String style, double now, boolean reduced) {
    if (inspecting(now)) return false;
    Pose current = sample(now);
    Pose[] selected = (reduced ? REDUCED : switch (style) {
      case "Flip" -> FLIP;
      case "Spin" -> SPIN;
      default -> FLOURISH;
    }).clone();
    selected[0] = current;
    start(selected, reduced ? .6 : style.equals("Flip") ? 1.1 : style.equals("Spin") ? 1.6 : 2.2, now, true);
    return true;
  }

  private void start(Pose[] poses, double seconds, double now, boolean inspecting) {
    frames = poses;
    duration = seconds;
    started = now;
    inspection = inspecting;
  }

  public boolean inspecting(double now) {
    return inspection && now >= started && now < started + duration;
  }

  public Pose sample(double now) {
    if (!Double.isFinite(now) || now < started || now >= started + duration) return Pose.READY;
    double position = (now - started) / duration * (frames.length - 1);
    int index = Math.min((int) position, frames.length - 2);
    return frames[index].mix(frames[index + 1], position - index);
  }

  public void cancel() {
    started = Double.NEGATIVE_INFINITY;
    duration = 0;
    inspection = false;
  }
}
