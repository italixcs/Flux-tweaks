package dev.fluxtweaks.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.Items;
import org.lwjgl.system.MemoryStack;

public final class HandChams implements AutoCloseable {
  public static RenderTarget captureTarget;
  public static boolean shieldOnly;
  private static final RenderPipeline COMPOSITE =
      RenderPipeline.builder()
          .withLocation(FluxTweaks.id("pipeline/hand_chams"))
          .withVertexShader(FluxTweaks.id("core/hand_chams"))
          .withFragmentShader(FluxTweaks.id("core/hand_chams"))
          .withBindGroupLayout(
              BindGroupLayout.builder()
                  .withSampler("Hands")
                  .withSampler("ShieldMask")
                  .withUniform("Chams", UniformType.UNIFORM_BUFFER)
                  .build())
          .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
          .withDepthStencilState(java.util.Optional.empty())
          .withCull(false)
          .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
          .build();
  private TextureTarget hands, shieldMask;
  private GpuBuffer uniforms;
  private ItemInHandRenderer pendingRenderer;
  private Operation<Void> pendingOperation;
  private SubmitNodeCollector pendingCollector;
  private LocalPlayer pendingPlayer;
  private PoseStack pendingPose;
  private float delta;
  private int light;

  public void queue(
      ItemInHandRenderer renderer,
      float dt,
      PoseStack pose,
      SubmitNodeCollector collector,
      LocalPlayer player,
      int packedLight,
      Operation<Void> original) {
    if (!FluxTweaks.active(Feature.HAND_CHAMS)) {
      original.call(renderer, dt, pose, collector, player, packedLight);
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    RenderTarget real = mc.gameRenderer.mainRenderTarget();
    if (hands == null || hands.width != real.width || hands.height != real.height) {
      close();
      hands =
          new TextureTarget(
              "Flux hands", real.width, real.height, true, real.getColorTexture().getFormat());
      shieldMask =
          new TextureTarget(
              "Flux shield mask",
              real.width,
              real.height,
              true,
              real.getColorTexture().getFormat());
    }
    var commands = RenderSystem.getDevice().createCommandEncoder();
    commands.clearColorAndDepthTextures(
        hands.getColorTexture(), new org.joml.Vector4f(0, 0, 0, 0), hands.getDepthTexture(), 0);
    pendingRenderer = renderer;
    pendingOperation = original;
    pendingCollector = collector;
    pendingPlayer = player;
    delta = dt;
    light = packedLight;
    pendingPose = new PoseStack();
    pendingPose.mulPose(pose.last().pose());
    captureTarget = hands;
    try {
      original.call(renderer, dt, pose, collector, player, packedLight);
    } catch (RuntimeException ex) {
      captureTarget = null;
      pendingRenderer = null;
      pendingOperation = null;
      throw ex;
    }
  }

  public void finish() {
    if (pendingRenderer == null) return;
    Minecraft mc = Minecraft.getInstance();
    try {
      mc.gameRenderer
          .featureRenderDispatcher()
          .renderAllFeatures((SubmitNodeStorage) pendingCollector);
      captureTarget = null;
      boolean mask =
          FluxTweaks.active(Feature.SHIELD_STATUS)
              && FluxTweaks.config.number(Feature.HAND_CHAMS, "fill") > 0
              && (pendingPlayer.getMainHandItem().is(Items.SHIELD)
                  || pendingPlayer.getOffhandItem().is(Items.SHIELD));
      if (mask) {
        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(
                shieldMask.getColorTexture(),
                new org.joml.Vector4f(0, 0, 0, 0),
                shieldMask.getDepthTexture(),
                0);
        shieldOnly = true;
        captureTarget = shieldMask;
        pendingOperation.call(
            pendingRenderer, delta, pendingPose, pendingCollector, pendingPlayer, light);
        mc.gameRenderer
            .featureRenderDispatcher()
            .renderAllFeatures((SubmitNodeStorage) pendingCollector);
        captureTarget = null;
        shieldOnly = false;
      }
      composite(mc.gameRenderer.mainRenderTarget(), mask);
    } catch (RuntimeException ex) {
      captureTarget = null;
      shieldOnly = false;
      FluxTweaks.config.module(Feature.HAND_CHAMS).enabled = false;
      FluxTweaks.changed();
      hands.blitAndBlendToTexture(
          mc.gameRenderer.mainRenderTarget().getColorTextureView(),
          mc.gameRenderer.mainRenderTarget().getDepthTextureView());
      FluxTweaks.message = "Hand Chams stopped after a rendering error";
      FluxTweaks.LOGGER.error(FluxTweaks.message, ex);
    } finally {
      captureTarget = null;
      shieldOnly = false;
      pendingRenderer = null;
      pendingOperation = null;
      pendingPose = null;
      pendingPlayer = null;
      pendingCollector = null;
    }
  }

  private void composite(RenderTarget target, boolean mask) {
    if (uniforms == null)
      uniforms =
          RenderSystem.getDevice()
              .createBuffer(
                  () -> "Flux Chams uniforms",
                  GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                  64);
    var c = FluxTweaks.config;
    Feature f = Feature.HAND_CHAMS;
    try (MemoryStack stack = MemoryStack.stackPush()) {
      ByteBuffer bytes = stack.malloc(64);
      putColor(bytes, c.color(f, "first"));
      putColor(bytes, c.color(f, "second"));
      bytes
          .putFloat(c.decimal(f, "radius"))
          .putFloat(c.decimal(f, "glow"))
          .putFloat(c.decimal(f, "fill"))
          .putFloat(
              (System.nanoTime() % 1_000_000_000_000L) / 1_000_000_000f * c.decimal(f, "speed"));
      bytes
          .putFloat(target.width)
          .putFloat(target.height)
          .putFloat(c.decimal(f, "quality"))
          .putFloat(mask ? 1 : 0);
      bytes.flip();
      RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniforms.slice(), bytes);
    }
    try (var pass =
        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                () -> "Flux hand outline",
                target.getColorTextureView(),
                java.util.Optional.empty())) {
      pass.setPipeline(COMPOSITE);
      pass.setUniform("Chams", uniforms);
      var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      pass.bindTexture("Hands", hands.getColorTextureView(), sampler);
      pass.bindTexture(
          "ShieldMask",
          mask ? shieldMask.getColorTextureView() : hands.getColorTextureView(),
          sampler);
      pass.draw(3, 1, 0, 0);
    }
  }

  private static void putColor(ByteBuffer bytes, int color) {
    bytes
        .putFloat(((color >> 16) & 255) / 255f)
        .putFloat(((color >> 8) & 255) / 255f)
        .putFloat((color & 255) / 255f)
        .putFloat(1);
  }

  @Override
  public void close() {
    captureTarget = null;
    if (hands != null) {
      hands.destroyBuffers();
      hands = null;
    }
    if (shieldMask != null) {
      shieldMask.destroyBuffers();
      shieldMask = null;
    }
    if (uniforms != null) {
      uniforms.close();
      uniforms = null;
    }
  }
}
