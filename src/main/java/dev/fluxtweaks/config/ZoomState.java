package dev.fluxtweaks.config;

public final class ZoomState {
  private boolean held, blocked = true;
  private double from = 1, target = 1, started, duration = 150;

  public void update(boolean eligible, boolean down, double initial, double millis, double now) {
    if (!eligible) {
      reset();
      return;
    }
    duration = millis;
    if (!down) blocked = false;
    if (down && !blocked && !held) {
      held = true;
      transition(initial, now);
    } else if (!down && held) {
      held = false;
      transition(1, now);
    }
  }

  public void reset() {
    held = false;
    blocked = true;
    from = target = 1;
  }

  public boolean held() {
    return held;
  }

  public boolean scroll(double steps, double now) {
    if (!held) return false;
    transition(Math.clamp(target * Math.pow(1.2, Math.clamp(steps, -100, 100)), 1, 16), now);
    return true;
  }

  private void transition(double value, double now) {
    from = magnification(now);
    target = Math.clamp(value, 1, 16);
    started = now;
  }

  public double magnification(double now) {
    double t = Math.clamp((now - started) / duration, 0, 1);
    t = t * t * (3 - 2 * t);
    return Math.exp(Math.log(from) + (Math.log(target) - Math.log(from)) * t);
  }

  public float fov(float vanilla, double now) {
    double factor = magnification(now);
    if (factor == 1) return vanilla;
    return (float)
        Math.max(1, Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(vanilla) / 2) / factor)));
  }
}
