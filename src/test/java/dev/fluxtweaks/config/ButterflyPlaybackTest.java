package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ButterflyPlaybackTest {
  @Test void pressRequestsOnePlaybackAndIgnoresHeldRepeats() {
    var input = new ButterflyPlayback();
    assertTrue(input.update("Press", true, true, true));
    assertFalse(input.repeating());
    assertFalse(input.update("Press", true, true, true));
    assertFalse(input.update("Press", true, false, false));
    assertTrue(input.update("Press", true, true, true));
  }

  @Test void holdRepeatsUntilRelease() {
    var input = new ButterflyPlayback();
    assertTrue(input.update("Hold", true, true, true));
    assertTrue(input.repeating());
    assertTrue(input.update("Hold", true, true, false));
    assertFalse(input.update("Hold", true, false, false));
    assertFalse(input.repeating());
  }

  @Test void toggleSurvivesReleaseAndStopsOnTheNextFreshPress() {
    var input = new ButterflyPlayback();
    assertTrue(input.update("Toggle", true, true, true));
    assertTrue(input.update("Toggle", true, true, true));
    assertTrue(input.update("Toggle", true, false, false));
    assertTrue(input.repeating());
    assertFalse(input.update("Toggle", true, true, true));
    assertFalse(input.repeating());
  }

  @Test void cancellationDoesNotRestartFromAnAlreadyHeldKey() {
    for (String mode : new String[] {"Hold", "Toggle"}) {
      var input = new ButterflyPlayback();
      input.update(mode, true, true, true);
      input.cancel();
      assertFalse(input.repeating());
      assertFalse(input.update(mode, true, true, true));
      assertFalse(input.update(mode, true, false, false));
      assertTrue(input.update(mode, true, true, true));
    }
  }

  @Test void unavailableInputStopsBothKindsOfLoop() {
    for (String mode : new String[] {"Hold", "Toggle"}) {
      var input = new ButterflyPlayback();
      input.update(mode, true, true, true);
      assertFalse(input.update(mode, false, true, false));
      assertFalse(input.repeating());
      assertFalse(input.update(mode, true, true, false));
    }
  }
}
