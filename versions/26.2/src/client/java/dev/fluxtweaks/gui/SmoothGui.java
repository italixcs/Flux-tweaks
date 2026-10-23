package dev.fluxtweaks.gui;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Native GUI batches, with analytic pixel coverage instead of scaled corner pictures. */
public final class SmoothGui {
  public static final RenderPipeline ROUND = shape("round", null);
  public static final RenderPipeline OUTLINE = shape("outline", "OUTLINE");
  public static final RenderPipeline SATURATION_VALUE =
      shape("saturation_value", "SATURATION_VALUE");
  public static final RenderPipeline HUE = shape("hue", "HUE");
  public static final RenderPipeline TRIANGLE = shape("triangle", "TRIANGLE");
  public static final RenderPipeline COVER = shape("cover", "COVER");
  public static final RenderPipeline TEXT = text(false);
  public static final RenderPipeline COLOR_TEXT = text(true);

  private static RenderPipeline shape(String name, String define) {
    var builder =
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(FluxTweaks.id("pipeline/ui_" + name))
            .withVertexShader(FluxTweaks.id("core/ui_shape"))
            .withFragmentShader(FluxTweaks.id("core/ui_shape"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(java.util.Optional.empty())
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
    if ("COVER".equals(define))
      builder.withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0);
    if (define != null) builder.withShaderDefine(define);
    return builder.build();
  }

  private static RenderPipeline text(boolean colored) {
    var builder =
        RenderPipeline.builder(RenderPipelines.GUI_TEXT_SNIPPET)
            .withLocation(FluxTweaks.id("pipeline/ui_text" + (colored ? "_color" : "")))
            .withVertexShader(FluxTweaks.id("core/ui_text"))
            .withFragmentShader(FluxTweaks.id("core/ui_text"))
            .withCull(false);
    if (colored) builder.withShaderDefine("COLORED");
    return builder.build();
  }

  public static void box(
      GuiGraphicsExtractor g,
      RenderPipeline pipeline,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color) {
    box(g, pipeline, x, y, width, height, radius, color, TextureSetup.noTexture());
  }

  public static void image(
      GuiGraphicsExtractor g, Identifier texture, float x, float y, float size, float radius) {
    var setup =
        TextureSetup.singleTexture(
            Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    box(g, COVER, x, y, size, size, radius, 0xffffffff, setup);
  }

  private static void box(
      GuiGraphicsExtractor g,
      RenderPipeline pipeline,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      TextureSetup texture) {
    if (width <= 0 || height <= 0 || color >>> 24 == 0) return;
    radius = Math.clamp(radius, 0, Math.min(width, height) * .5f);
    var pose = new Matrix3x2f(g.pose());
    var scissor = g.scissorStack.peek();
    var bounds =
        new ScreenRectangle(
                (int) Math.floor(x - 1),
                (int) Math.floor(y - 1),
                (int) Math.ceil(width + 3),
                (int) Math.ceil(height + 3))
            .transformMaxBounds(pose);
    if (scissor != null) bounds = scissor.intersection(bounds);
    if (bounds != null)
      g.guiRenderState.addGuiElement(
          new Shape(pipeline, pose, x, y, width, height, radius, color, scissor, bounds, texture));
  }

  private record Shape(
      RenderPipeline pipeline,
      Matrix3x2f pose,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      ScreenRectangle scissorArea,
      ScreenRectangle bounds,
      TextureSetup textureSetup)
      implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer consumer) {
      float halfW = width * .5f, halfH = height * .5f;
      vertex(consumer, x - 1, y - 1, -halfW - 1, -halfH - 1);
      vertex(consumer, x - 1, y + height + 1, -halfW - 1, halfH + 1);
      vertex(consumer, x + width + 1, y + height + 1, halfW + 1, halfH + 1);
      vertex(consumer, x + width + 1, y - 1, halfW + 1, -halfH - 1);
    }

    private void vertex(VertexConsumer consumer, float px, float py, float u, float v) {
      // UV2 holds dimensions in sixteenths; z holds radius. The UI shader uses z=0 for depth.
      consumer
          .addVertex(
              pose.m00() * px + pose.m10() * py + pose.m20(),
              pose.m01() * px + pose.m11() * py + pose.m21(),
              radius)
          .setColor(color)
          .setUv(u, v)
          .setUv2(Math.round(width * 8), Math.round(height * 8));
    }
  }

  private SmoothGui() {}
}
