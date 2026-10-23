package dev.fluxtweaks.gui;

import static org.junit.jupiter.api.Assertions.*;

import dev.fluxtweaks.config.Feature;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class NotificationQueueTest {
  @Test
  void keepsEachTransitionAndBoundsTheQueue() {
    var clock = new AtomicLong();
    var queue = new NotificationQueue(clock::get);
    for (var state : NotificationQueue.State.values()) queue.add(Feature.FREECAM, state);
    assertEquals(3, queue.visible().size());
    assertEquals(NotificationQueue.State.DISABLED, queue.visible().getFirst().state());
    assertEquals(NotificationQueue.State.DEACTIVATED, queue.visible().getLast().state());
  }

  @Test
  void expiryAndWorldClearLeaveNoBacklog() {
    var clock = new AtomicLong();
    var queue = new NotificationQueue(clock::get);
    queue.add(Feature.FREECAM, NotificationQueue.State.ENABLED);
    clock.set(2500);
    assertTrue(queue.visible().isEmpty());
    queue.add(Feature.ZOOM, NotificationQueue.State.ACTIVATED);
    queue.clear();
    assertTrue(queue.visible().isEmpty());
  }

  @Test
  void monotonicTimingsAndReducedMotionAreDeterministic() {
    var clock = new AtomicLong();
    var queue = new NotificationQueue(clock::get);
    queue.add(Feature.ZOOM, NotificationQueue.State.ACTIVATED);
    var notice = queue.visible().getFirst();
    assertEquals(0, queue.appearance(notice, false));
    assertEquals(1, queue.appearance(notice, true));
    clock.set(110);
    assertEquals(.875, queue.appearance(notice, false), .001);
    clock.set(300);
    assertEquals(1, queue.appearance(notice, false));
    clock.set(2420);
    assertEquals(.875, queue.appearance(notice, false), .001);
    clock.set(2500);
    assertEquals(0, queue.appearance(notice, false));
  }
}
