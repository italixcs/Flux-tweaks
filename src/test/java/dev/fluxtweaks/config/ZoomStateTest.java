package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ZoomStateTest {
  @Test
  void startsInactiveAndRequiresFreshPressAfterCancellation() {
    var zoom = new ZoomState();
    zoom.update(true, true, 4, 150, 0);
    assertFalse(zoom.held());
    zoom.update(true, false, 4, 150, 0);
    zoom.update(true, true, 4, 150, 1);
    assertTrue(zoom.held());
    zoom.update(false, true, 4, 150, 200);
    assertEquals(1, zoom.magnification(200));
    zoom.update(true, true, 4, 150, 300);
    assertFalse(zoom.held());
    zoom.update(true, false, 4, 150, 301);
    zoom.update(true, true, 4, 150, 302);
    assertTrue(zoom.held());
  }

  @Test
  void smoothlyChangesFovAndRestoresExactly() {
    var zoom = activated();
    assertEquals(1, zoom.magnification(1));
    assertEquals(2, zoom.magnification(76), 1e-9);
    assertEquals(4, zoom.magnification(151), 1e-9);
    assertEquals(19.858, zoom.fov(70, 151), .01);
    zoom.update(true, false, 4, 150, 151);
    assertEquals(4, zoom.magnification(151), 1e-9);
    assertEquals(70, zoom.fov(70, 301));
  }

  @Test
  void scrollClampsAndEachActivationReturnsToConfiguredDefault() {
    var zoom = activated();
    assertTrue(zoom.scroll(1, 151));
    assertEquals(4.8, zoom.magnification(301), 1e-9);
    zoom.scroll(1000, 301);
    assertEquals(16, zoom.magnification(451), 1e-9);
    assertEquals(1, zoom.fov(7, 451));
    zoom.scroll(-1000, 451);
    assertEquals(1, zoom.magnification(601), 1e-9);
    zoom.update(true, false, 4, 150, 601);
    assertFalse(zoom.scroll(1, 602));
    zoom.update(true, true, 4, 150, 752);
    assertEquals(4, zoom.magnification(902), 1e-9);
  }

  @Test
  void resetDropsTransientZoomAndRequiresRelease() {
    var zoom = activated();
    zoom.reset();
    zoom.update(true, true, 4, 150, 300);
    assertFalse(zoom.held());
    assertEquals(1, zoom.magnification(500));
  }

  private ZoomState activated() {
    var zoom = new ZoomState();
    zoom.update(true, false, 4, 150, 0);
    zoom.update(true, true, 4, 150, 1);
    return zoom;
  }
}
