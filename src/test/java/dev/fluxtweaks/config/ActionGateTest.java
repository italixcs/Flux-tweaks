package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ActionGateTest {
  @Test
  void disabledActionsCannotStart() {
    assertFalse(new ActionGate().update(false, true, true, false, false));
  }

  @Test
  void toggleStartsOnceAndStopsOnTheNextPress() {
    var gate = new ActionGate();
    assertTrue(gate.update(true, true, true, false, false));
    assertTrue(gate.update(true, true, true, false, true));
    assertTrue(gate.update(true, false, false, false, true));
    assertFalse(gate.update(true, true, true, false, true));
  }

  @Test
  void holdTracksPressAndRelease() {
    var gate = new ActionGate();
    assertTrue(gate.update(true, true, true, true, false));
    assertFalse(gate.update(true, false, false, true, true));
  }

  @Test
  void restoredHeldKeyAfterScreenFocusLossNeedsFreshPress() {
    var gate = new ActionGate();
    gate.update(true, true, true, true, false);
    assertFalse(gate.update(false, true, false, true, true));
    assertFalse(gate.update(true, true, false, true, false));
    gate.update(true, false, false, true, false);
    assertTrue(gate.update(true, true, true, true, false));
  }

  @Test
  void cancelledHoldCannotFightAnotherCameraUntilReleased() {
    var gate = new ActionGate();
    gate.update(true, true, true, true, false);
    gate.block();
    assertFalse(gate.update(true, true, false, true, false));
    gate.update(true, false, false, true, false);
    assertTrue(gate.update(true, true, true, true, false));
  }

  @Test
  void fastTapBetweenTicksStillToggles() {
    assertTrue(new ActionGate().update(true, false, true, false, false));
  }
}
