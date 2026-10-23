package dev.fluxtweaks.render;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import dev.fluxtweaks.mixin.VoxelShapeAccess;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public final class BlockOverlay {
  private static final RenderType FILL =
      RenderType.create(
          "flux_block_fill",
          RenderSetup.builder(
                  RenderPipelines.register(
                      RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                          .withLocation(FluxTweaks.id("pipeline/block_fill"))
                          .withDepthStencilState(
                              new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                          .build()))
              .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
              .createRenderSetup());

  public static boolean render(LevelRenderContext context, BlockOutlineRenderState outline) {
    if (!FluxTweaks.active(Feature.BLOCK_OVERLAY)) return true;
    var cfg = FluxTweaks.config;
    var pose = context.poseStack();
    var camera = context.levelState().cameraRenderState.pos;
    pose.pushPose();
    pose.translate(
        outline.pos().getX() - camera.x,
        outline.pos().getY() - camera.y,
        outline.pos().getZ() - camera.z);
    try {
      if (cfg.bool(Feature.BLOCK_OVERLAY, "fill")
          && cfg.number(Feature.BLOCK_OVERLAY, "fillOpacity") > 0) {
        int color = color("fill");
        var shape = outline.shape();
        var xs = shape.getCoords(Direction.Axis.X);
        var ys = shape.getCoords(Direction.Axis.Y);
        var zs = shape.getCoords(Direction.Axis.Z);
        context
            .submitNodeCollector()
            .submitCustomGeometry(
                pose,
                FILL,
                (transform, vertices) ->
                    ((VoxelShapeAccess) shape)
                        .flux$voxels()
                        .forAllFaces(
                            (dir, x, y, z) ->
                                face(
                                    vertices,
                                    transform.pose(),
                                    dir,
                                    xs.getDouble(x),
                                    ys.getDouble(y),
                                    zs.getDouble(z),
                                    xs.getDouble(x + 1),
                                    ys.getDouble(y + 1),
                                    zs.getDouble(z + 1),
                                    color)));
      }
      if (cfg.bool(Feature.BLOCK_OVERLAY, "outline")
          && cfg.number(Feature.BLOCK_OVERLAY, "outlineOpacity") > 0) {
        int color = color("outline");
        float thickness = cfg.decimal(Feature.BLOCK_OVERLAY, "thickness");
        context
            .submitNodeCollector()
            .submitCustomGeometry(
                pose,
                RenderTypes.lines(),
                (transform, v) ->
                    outline
                        .shape()
                        .forAllEdges(
                            (x0, y0, z0, x1, y1, z1) -> {
                              var normal =
                                  new org.joml.Vector3f(
                                          (float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0))
                                      .normalize();
                              v.addVertex(transform.pose(), (float) x0, (float) y0, (float) z0)
                                  .setColor(color)
                                  .setNormal(transform, normal)
                                  .setLineWidth(thickness);
                              v.addVertex(transform.pose(), (float) x1, (float) y1, (float) z1)
                                  .setColor(color)
                                  .setNormal(transform, normal)
                                  .setLineWidth(thickness);
                            }));
      }
    } finally {
      pose.popPose();
    }
    return false;
  }

  private static int color(String prefix) {
    return ((int)
                Math.round(
                    FluxTweaks.config.number(Feature.BLOCK_OVERLAY, prefix + "Opacity") * 2.55)
            << 24)
        | (FluxTweaks.config.color(Feature.BLOCK_OVERLAY, prefix + "Color") & 0xFFFFFF);
  }

  private static void face(
      VertexConsumer v,
      Matrix4f m,
      Direction face,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      int color) {
    switch (face) {
      case WEST -> quad(v, m, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      case EAST -> quad(v, m, color, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
      case DOWN -> quad(v, m, color, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
      case UP -> quad(v, m, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      case NORTH -> quad(v, m, color, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
      case SOUTH -> quad(v, m, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
    }
  }

  private static void quad(VertexConsumer v, Matrix4f m, int color, double... coords) {
    for (int i = 0; i < coords.length; i += 3)
      v.addVertex(m, (float) coords[i], (float) coords[i + 1], (float) coords[i + 2])
          .setColor(color);
  }
}
