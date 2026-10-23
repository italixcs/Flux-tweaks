package dev.fluxtweaks.gui;

import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.gui.motion.Easing;
import java.util.*;
import java.util.function.LongSupplier;

public final class NotificationQueue {
  public enum State {
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    ACTIVATED("Activated"),
    DEACTIVATED("Deactivated");
    public final String label;

    State(String label) {
      this.label = label;
    }

    public boolean positive() {
      return this == ENABLED || this == ACTIVATED;
    }
  }

  public record Notice(Feature feature, State state, long created) {}

  public static final int LIMIT = 3;
  public static final long LIFETIME_MS = 2500;
  private final LongSupplier clock;
  private final Deque<Notice> queue = new ArrayDeque<>();

  public NotificationQueue() {
    this(() -> System.nanoTime() / 1_000_000);
  }

  public NotificationQueue(LongSupplier clock) {
    this.clock = clock;
  }

  public void add(Feature feature, State state) {
    queue.removeIf(n -> age(n) >= LIFETIME_MS);
    if (queue.size() == LIMIT) queue.removeFirst();
    queue.addLast(new Notice(feature, state, clock.getAsLong()));
  }

  public List<Notice> visible() {
    queue.removeIf(n -> age(n) >= LIFETIME_MS);
    return List.copyOf(queue);
  }

  public double age(Notice notice) {
    return Math.max(0, clock.getAsLong() - notice.created());
  }

  public float appearance(Notice notice, boolean reduced) {
    if (reduced) return 1;
    double age = age(notice);
    return (float)
        Math.min(
            Easing.CUBIC_OUT.apply(age / 220),
            1 - Easing.CUBIC_IN.apply((age - (LIFETIME_MS - 160)) / 160));
  }

  public void clear() {
    queue.clear();
  }
}
