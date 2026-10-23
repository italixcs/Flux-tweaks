package dev.fluxtweaks.config;

/** Keeps held/restored keys from reactivating an action after input cancellation. */
public final class ActionGate {
  private boolean blocked = true, wasDown;

  public void block() {
    blocked = true;
  }

  public boolean update(
      boolean eligible, boolean down, boolean pressed, boolean hold, boolean active) {
    boolean fresh = pressed && (!wasDown || !down);
    wasDown = down;
    if (!eligible) {
      blocked = true;
      return false;
    }
    if (!down || fresh) blocked = false;
    if (blocked) return !hold && active;
    return hold ? down : fresh ? !active : active;
  }
}
