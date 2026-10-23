package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.render.ShieldTint;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
  @Inject(method = "updateForTopItem", at = @At("TAIL"))
  private void flux$owner(
      ItemStackRenderState state,
      ItemStack stack,
      ItemDisplayContext display,
      Level level,
      ItemOwner owner,
      int seed,
      CallbackInfo ci) {
    boolean held =
        display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    ((ShieldTint.State) state)
        .flux$tint(
            held && owner instanceof LivingEntity living && FluxTweaks.shields != null
                ? FluxTweaks.shields.tint(
                    living,
                    stack,
                    display.leftHand()
                        != (living.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT))
                : -1);
  }
}
