package dev.fluxtweaks.mixin;

import dev.fluxtweaks.render.ShieldTint;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitTintMixin {
  @ModifyVariable(
      method = {"submitModelPart", "submitModel"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 2)
  private int flux$tint(int color) {
    return ShieldTint.apply(color);
  }
}
