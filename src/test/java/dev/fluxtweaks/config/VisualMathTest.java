package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class VisualMathTest {
  @Test
  void realTimeAlignsMinecraftDawnNoonAndMidnight() {
    assertEquals(0, VisualMath.time("Real Time", 0, LocalTime.of(6, 0)));
    assertEquals(6000, VisualMath.time("Real Time", 0, LocalTime.NOON));
    assertEquals(18000, VisualMath.time("Real Time", 0, LocalTime.MIDNIGHT));
  }

  @Test
  void customTimeWrapsAcrossDayBoundary() {
    assertEquals(23999, VisualMath.time("Custom", -1, LocalTime.NOON));
    assertEquals(1, VisualMath.time("Custom", 24001, LocalTime.NOON));
  }

  @Test
  void realTimeRoundsDownAcrossDawn() {
    assertEquals(23999, VisualMath.time("Real Time", 0, LocalTime.of(5, 59, 59)));
  }

  @Test
  void disabledShieldWinsAndLowDurabilityRemainsVisibleWhileBlocking() {
    assertEquals("disabled", VisualMath.shield(true, true, 5, 20));
    assertEquals("low", VisualMath.shield(false, true, 20, 20));
    assertEquals("using", VisualMath.shield(false, true, 21, 20));
    assertEquals("ready", VisualMath.shield(false, false, 21, 20));
  }

  @Test
  void transitionsClampAndPreserveColorEndpoints() {
    assertEquals(0xFF000000, VisualMath.blend(0xFF000000, 0xFFFFFFFF, -2));
    assertEquals(0xFFFFFFFF, VisualMath.blend(0xFF000000, 0xFFFFFFFF, 2));
    assertEquals(0xFF7F7F7F, VisualMath.blend(0xFF000000, 0xFFFFFFFF, .5f));
  }
}
