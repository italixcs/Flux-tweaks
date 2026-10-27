package dev.fluxtweaks.test;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.PrimitiveTopology;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.ButterflyMotion;
import dev.fluxtweaks.render.ButterflyModel;
import dev.fluxtweaks.render.ButterflyRenderer;
import dev.fluxtweaks.feature.ButterflyKnife;
import java.nio.ByteOrder;
import java.util.ArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

/** Exercises the actual vertex writer, including Sodium's optimized model path. */
final class ButterflyGeometryAcceptance {
  private static final float POSITION_TOLERANCE_SQUARED = 1e-8f;

  static void run() {
    verifyGripAnchor();
    var model = new ButterflyModel();
    verify(model, ButterflyMotion.Pose.READY, "ready");
    verify(model, new ButterflyMotion.Pose(0, 0, 0, 67, -143, 0), "hinged");
  }

  private static void verifyGripAnchor() {
    for (String style : new String[] {"Flip", "Spin", "Flourish"}) {
      var motion = new ButterflyMotion();
      motion.inspect(style, 0, false, 1);
      for (double time = 0; time < 2.5; time += .025) {
        var frame = motion.sample(time);
        for (boolean left : new boolean[] {false, true}) {
          for (float scale : new float[] {.5f, 1, 2}) {
            var pose = new PoseStack();
            ButterflyRenderer.positionKnife(pose, new ButterflyKnife.Snapshot(frame, "Steel", scale, false, left));
            var contact = new Vector3f(frame.gripX() / 16, frame.gripY() / 16, 0);
            pose.last().pose().transformPosition(contact);
            var expected = new Vector3f((-.08f - frame.lift() * .035f) * (left ? -1 : 1),
                .08f + frame.lift() / 16, -.12f - frame.lift() * .018f);
            check(contact.distanceSquared(expected) < 1e-10f,
                "Knife escaped its hand anchor: " + style + ", left=" + left + ", scale=" + scale);
          }
        }
      }
    }
    FluxTweaks.LOGGER.info("Butterfly grip anchor passed: all styles, both hands and scale limits");
  }

  private static void verify(ButterflyModel model, ButterflyMotion.Pose pose, String label) {
    model.setupAnim(pose);
    var expected = new ArrayList<Vector3f>();
    model.root().getExtentsForGui(new PoseStack(), point -> expected.add(new Vector3f(point)));
    check(!expected.isEmpty(), "Authored Butterfly geometry is empty: " + label);

    try (var storage = new ByteBufferBuilder(1 << 20)) {
      var output = new BufferBuilder(storage, PrimitiveTopology.QUADS, DefaultVertexFormat.ENTITY);
      boolean intrinsics = requireSodiumWriter(output);
      FluxTweaks.LOGGER.info("Butterfly geometry check starting: pose={}, sodiumIntrinsics={}, expectedVertices={}",
          label, intrinsics, expected.size());
      model.renderToBuffer(new PoseStack(), output, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
      try (var mesh = output.buildOrThrow()) {
        int count = mesh.drawState().vertexCount();
        check(count == expected.size(), "Butterfly " + label + " emitted " + count
            + " vertices, but authored polygons contain " + expected.size()
            + "; sodiumIntrinsics=" + intrinsics);
        var format = mesh.drawState().format();
        int stride = format.getVertexSize();
        var position = format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME);
        check(position != null, "Butterfly vertex format has no position");
        int positionOffset = position.offset();
        var bytes = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
        var matched = new boolean[expected.size()];
        for (int vertex = 0; vertex < count; vertex++) {
          int offset = bytes.position() + vertex * stride + positionOffset;
          var actual = new Vector3f(bytes.getFloat(offset), bytes.getFloat(offset + 4), bytes.getFloat(offset + 8));
          check(actual.isFinite(), "Butterfly emitted a non-finite position: " + label);
          int match = -1;
          for (int candidate = 0; candidate < expected.size(); candidate++) {
            if (!matched[candidate]
                && actual.distanceSquared(expected.get(candidate)) <= POSITION_TOLERANCE_SQUARED) {
              match = candidate;
              break;
            }
          }
          check(match >= 0, "Butterfly " + label + " emitted unexpected vertex " + vertex + " at " + actual
              + "; actual BufferBuilder output differs from authored polygons; sodiumIntrinsics=" + intrinsics);
          matched[match] = true;
        }
        FluxTweaks.LOGGER.info("Butterfly geometry passed: pose={}, sodiumIntrinsics={}, matchedVertices={}",
            label, intrinsics, count);
      }
    }
  }

  private static boolean requireSodiumWriter(BufferBuilder output) {
    if (!FabricLoader.getInstance().isModLoaded("sodium")) return false;
    try {
      var writer = Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter");
      check(writer.isInstance(output), "Sodium loaded, but BufferBuilder is not a VertexBufferWriter");
      check(Boolean.TRUE.equals(writer.getMethod("canUseIntrinsics").invoke(output)),
          "Sodium loaded, but this BufferBuilder cannot use intrinsics");
      return true;
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Could not verify the loaded Sodium vertex writer", ex);
    }
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
