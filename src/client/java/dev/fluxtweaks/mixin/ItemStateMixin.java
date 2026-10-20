package dev.fluxtweaks.mixin;

import dev.fluxtweaks.render.ShieldTint;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStateMixin implements ShieldTint.State {
  @Unique private int flux$color = -1;

  @Override
  public void flux$tint(int color) {
    flux$color = color;
  }

  @Inject(method = "clear", at = @At("HEAD"))
  private void flux$clear(CallbackInfo ci) {
    flux$color = -1;
  }

  @Inject(method = "submit", at = @At("HEAD"))
  private void flux$begin(CallbackInfo ci) {
    ShieldTint.push(flux$color);
  }

  @Inject(method = "submit", at = @At("RETURN"))
  private void flux$end(CallbackInfo ci) {
    ShieldTint.pop();
  }
}
