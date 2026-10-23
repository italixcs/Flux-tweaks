package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.fluxtweaks.gui.SmoothGui;
import dev.fluxtweaks.gui.Ui;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderState.class)
public abstract class UiGlyphMixin {
  @Shadow @Final private TextRenderable renderable;

  @Unique
  private boolean flux$font() {
    return renderable instanceof TextRenderable.Styled styled
        && Ui.isFont(styled.style().getFont());
  }

  @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
  private void flux$sampling(CallbackInfoReturnable<TextureSetup> cir) {
    if (flux$font())
      cir.setReturnValue(
          TextureSetup.singleTexture(
              renderable.textureView(),
              RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
  }

  @Inject(method = "pipeline", at = @At("HEAD"), cancellable = true)
  private void flux$coverage(CallbackInfoReturnable<RenderPipeline> cir) {
    if (flux$font())
      cir.setReturnValue(
          renderable.guiPipeline() == RenderPipelines.GUI_TEXT_GRAYSCALE
              ? SmoothGui.TEXT
              : SmoothGui.COLOR_TEXT);
  }
}
