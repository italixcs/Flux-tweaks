package dev.fluxtweaks.mixin;

import dev.fluxtweaks.feature.HitColor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class HitStateMixin implements HitColor.State {
  @Unique private boolean flux$hitColor;

  public boolean flux$hitColor() {
    return flux$hitColor;
  }

  public void flux$hitColor(boolean selected) {
    flux$hitColor = selected;
  }
}
