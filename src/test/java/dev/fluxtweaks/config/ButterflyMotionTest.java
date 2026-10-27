package dev.fluxtweaks.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonPrimitive;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ButterflyMotionTest {
  @TempDir Path directory;
  @Test void posesAreStableAcrossPassesAndReturnToReady() {
    for (String style : new String[] {"Flip", "Spin", "Flourish"}) {
      var motion = new ButterflyMotion();
      assertTrue(motion.inspect(style, 10, false, 1));
      assertNotEquals(ButterflyMotion.Pose.READY, motion.sample(10.4));
      assertEquals(motion.sample(10.4), motion.sample(10.4));
      assertFalse(motion.inspect(style, 10.4, false, 1), "Repeated input must not queue or restart");
      assertEquals(ButterflyMotion.Pose.READY, motion.sample(13));
      assertFalse(motion.inspecting(13));
      assertTrue(motion.inspect(style, 13, false, 1));
      motion.cancel();
      assertEquals(ButterflyMotion.Pose.READY, motion.sample(13.2));
      assertFalse(motion.inspecting(13.2));
    }
  }

  @Test void reducedMotionKeepsHandlesStillAndEquipIsImmediate() {
    var motion = new ButterflyMotion();
    motion.equip(0, true, 1);
    assertEquals(ButterflyMotion.Pose.READY, motion.sample(.2));
    motion.inspect("Flourish", 1, true, 1);
    var pose = motion.sample(1.3);
    assertEquals(0, pose.handleA());
    assertEquals(0, pose.handleB());
    assertTrue(Math.abs(pose.roll()) <= 8);
    assertEquals(ButterflyMotion.Pose.READY, motion.sample(1.7));
  }

  @Test void inspectingDuringEquipStartsFromDisplayedPose() {
    var motion = new ButterflyMotion();
    motion.equip(1, false, 1);
    var current = motion.sample(1.15);
    motion.inspect("Spin", 1.15, false, 1);
    assertEquals(current, motion.sample(1.15));
  }

  @Test void speedChangesTimeWithoutChangingTheChoreography() {
    for (String style : new String[] {"Flip", "Spin", "Flourish"}) {
      var normal = new ButterflyMotion();
      normal.inspect(style, 0, false, 1);
      for (double speed : new double[] {.25, .5, 2, 3}) {
        var adjusted = new ButterflyMotion();
        adjusted.inspect(style, 0, false, speed);
        for (double time = .05; time < 2.8; time += .05) {
          var a = normal.sample(time);
          var b = adjusted.sample(time / speed);
          assertEquals(a.roll(), b.roll(), .001);
          assertEquals(a.handleA(), b.handleA(), .001);
          assertEquals(a.handleB(), b.handleB(), .001);
          assertEquals(a.lift(), b.lift(), .001);
          assertEquals(normal.inspecting(time), adjusted.inspecting(time / speed));
        }
      }
    }
    var slow = new ButterflyMotion();
    slow.equip(0, false, .25);
    assertNotEquals(ButterflyMotion.Pose.READY, slow.sample(1));
    assertEquals(ButterflyMotion.Pose.READY, slow.sample(2.8));
  }

  @Test void spinDoesNotStopBetweenRevolutionsAndCatchesWithoutAPop() {
    var motion = new ButterflyMotion();
    motion.inspect("Spin", 0, false, 1);
    for (double fraction : new double[] {.25, .5, .75}) {
      double time = 1.8 * fraction;
      assertTrue(motion.sample(time + .001).roll() - motion.sample(time - .001).roll() > .1,
          "The knife stopped at an intermediate rotation");
    }
    var caught = motion.sample(1.8 - .00001);
    assertEquals(0, Math.IEEEremainder(caught.roll(), 360), .001);
    assertEquals(0, Math.IEEEremainder(caught.handleB(), 360), .001);
    assertEquals(0, caught.handleA(), .001);
    assertEquals(0, caught.lift(), .001);
    assertEquals(0, caught.tilt(), .001);
    assertEquals(0, caught.yaw(), .001);
    assertEquals(ButterflyMotion.Pose.READY, motion.sample(1.8));
  }

  @Test void newModuleMigratesAndScaleAndFinishAreValidated() {
    var config = new FluxConfig();
    config.modules.remove(Feature.BUTTERFLY_KNIFE.id);
    config.module(Feature.VIEW_MODELS).enabled = true;
    config.sanitize();
    assertFalse(config.enabled(Feature.BUTTERFLY_KNIFE));
    assertTrue(config.enabled(Feature.VIEW_MODELS));
    assertTrue(config.bool(Feature.BUTTERFLY_KNIFE, "glint"));
    assertEquals(1, config.number(Feature.BUTTERFLY_KNIFE, "speed"));
    assertEquals("Press", config.text(Feature.BUTTERFLY_KNIFE, "activation"));
    config.set(Feature.BUTTERFLY_KNIFE, "activation", new JsonPrimitive("invalid"));
    assertEquals("Press", config.text(Feature.BUTTERFLY_KNIFE, "activation"));
    config.set(Feature.BUTTERFLY_KNIFE, "speed", new JsonPrimitive(100));
    assertEquals(3, config.number(Feature.BUTTERFLY_KNIFE, "speed"));
    config.set(Feature.BUTTERFLY_KNIFE, "speed", new JsonPrimitive(-1));
    assertEquals(.25, config.number(Feature.BUTTERFLY_KNIFE, "speed"));
    config.set(Feature.BUTTERFLY_KNIFE, "scale", new JsonPrimitive(100));
    assertEquals(2, config.number(Feature.BUTTERFLY_KNIFE, "scale"));
    config.set(Feature.BUTTERFLY_KNIFE, "scale", new JsonPrimitive(-1));
    assertEquals(.5, config.number(Feature.BUTTERFLY_KNIFE, "scale"));
    config.set(Feature.BUTTERFLY_KNIFE, "finish", new JsonPrimitive("missing"));
    assertEquals("Steel", config.text(Feature.BUTTERFLY_KNIFE, "finish"));
  }

  @Test void preferencesPersistWithoutReplacingOtherModuleSettings() throws Exception {
    var config = new FluxConfig();
    config.module(Feature.BUTTERFLY_KNIFE).enabled = true;
    config.set(Feature.BUTTERFLY_KNIFE, "finish", new JsonPrimitive("Emerald Marble"));
    config.set(Feature.BUTTERFLY_KNIFE, "inspect", new JsonPrimitive("Spin"));
    config.set(Feature.BUTTERFLY_KNIFE, "scale", new JsonPrimitive(1.35));
    config.set(Feature.BUTTERFLY_KNIFE, "speed", new JsonPrimitive(1.75));
    config.set(Feature.BUTTERFLY_KNIFE, "activation", new JsonPrimitive("Toggle"));
    config.set(Feature.BUTTERFLY_KNIFE, "glint", new JsonPrimitive(false));
    config.set(Feature.VIEW_MODELS, "mainItem.scale", new JsonPrimitive(.85));
    var store = new ConfigStore(directory.resolve("flux.json"));
    store.save(config);
    var loaded = store.load();
    assertTrue(loaded.enabled(Feature.BUTTERFLY_KNIFE));
    assertEquals("Emerald Marble", loaded.text(Feature.BUTTERFLY_KNIFE, "finish"));
    assertEquals("Spin", loaded.text(Feature.BUTTERFLY_KNIFE, "inspect"));
    assertEquals(1.35, loaded.number(Feature.BUTTERFLY_KNIFE, "scale"));
    assertEquals(1.75, loaded.number(Feature.BUTTERFLY_KNIFE, "speed"));
    assertEquals("Toggle", loaded.text(Feature.BUTTERFLY_KNIFE, "activation"));
    assertFalse(loaded.bool(Feature.BUTTERFLY_KNIFE, "glint"));
    assertEquals(.85, loaded.number(Feature.VIEW_MODELS, "mainItem.scale"));
  }
}
