package dev.fluxtweaks.config;

/** Inspect input policy; active loops are transient and never saved with settings. */
public final class ButterflyPlayback {
  private final ActionGate gate = new ActionGate();
  private boolean repeating;

  public boolean update(String mode, boolean eligible, boolean down, boolean pressed) {
    boolean press = mode.equals("Press");
    boolean wanted = gate.update(eligible, down, pressed, mode.equals("Hold"), !press && repeating);
    repeating = !press && wanted;
    return wanted;
  }

  public boolean repeating() {
    return repeating;
  }

  public void cancel() {
    repeating = false;
    gate.block();
  }
}
