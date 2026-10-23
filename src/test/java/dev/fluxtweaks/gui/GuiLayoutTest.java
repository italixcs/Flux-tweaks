package dev.fluxtweaks.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GuiLayoutTest {
  @Test
  void defaultPreservesPreviousAutomaticFit() {
    assertEquals(1, GuiLayout.scale(1280, 800, 1));
    assertEquals(400f / 560, GuiLayout.scale(640, 400, 1));
  }

  @Test
  void preferenceMakesBothPanelsSmallerOrLarger() {
    assertEquals(.5, GuiLayout.scale(1280, 800, .5));
    assertEquals(1.5, GuiLayout.scale(1280, 800, 1.5));
    assertTrue(GuiLayout.scale(640, 400, 1.5) > GuiLayout.scale(640, 400, 1));
  }

  @Test
  void largeScaleKeepsTwoPanelWidthAndMinimumScrollableHeightReachable() {
    for (int[] viewport : new int[][] {{320, 240}, {640, 400}, {854, 480}, {1920, 1080}}) {
      float scale = GuiLayout.scale(viewport[0], viewport[1], 1.5);
      assertTrue(560 * scale <= viewport[0] + .001);
      assertTrue(360 * scale <= viewport[1] + .001);
    }
  }

  @Test
  void invalidPreferencesRecoverAndBoundsClamp() {
    assertEquals(1, GuiLayout.preference(Double.NaN));
    assertEquals(1, GuiLayout.preference(Double.POSITIVE_INFINITY));
    assertEquals(.5, GuiLayout.preference(-1));
    assertEquals(1.5, GuiLayout.preference(20));
  }
}
