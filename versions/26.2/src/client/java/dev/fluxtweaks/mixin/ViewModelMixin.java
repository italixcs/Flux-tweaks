package dev.fluxtweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fluxtweaks.feature.ViewModels;
import dev.fluxtweaks.render.HandChams;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ViewModelMixin {
  @Unique private InteractionHand flux$hand = InteractionHand.MAIN_HAND;

  @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
  private void flux$hand(
      AbstractClientPlayer player,
      float dt,
      float pitch,
      InteractionHand hand,
      float swing,
      ItemStack stack,
      float equip,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci) {
    flux$hand = hand;
    if (HandChams.shieldOnly && !stack.is(Items.SHIELD)) ci.cancel();
  }

  @Inject(method = "renderItem", at = @At("HEAD"))
  private void flux$item(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext display,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci) {
    pose.pushPose();
    ViewModels.apply(pose, flux$hand == InteractionHand.MAIN_HAND ? "mainItem" : "offItem");
  }

  @Inject(method = "renderItem", at = @At("RETURN"))
  private void flux$itemEnd(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext display,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci) {
    pose.popPose();
  }

  @Inject(method = "renderPlayerArm", at = @At("HEAD"))
  private void flux$arm(
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      float equip,
      float swing,
      HumanoidArm arm,
      CallbackInfo ci) {
    pose.pushPose();
    ViewModels.apply(pose, ViewModels.armGroup(arm));
  }

  @Inject(method = "renderPlayerArm", at = @At("RETURN"))
  private void flux$armEnd(
      PoseStack pose,
      SubmitNodeCollector collector,
      int light,
      float equip,
      float swing,
      HumanoidArm arm,
      CallbackInfo ci) {
    pose.popPose();
  }

  @Inject(method = "renderMapHand", at = @At("HEAD"))
  private void flux$mapArm(
      PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm, CallbackInfo ci) {
    pose.pushPose();
    ViewModels.apply(pose, ViewModels.armGroup(arm));
  }

  @Inject(method = "renderMapHand", at = @At("RETURN"))
  private void flux$mapArmEnd(
      PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm, CallbackInfo ci) {
    pose.popPose();
  }

  @Inject(method = "renderMap", at = @At("HEAD"))
  private void flux$map(
      PoseStack pose, SubmitNodeCollector collector, int light, ItemStack stack, CallbackInfo ci) {
    pose.pushPose();
    ViewModels.apply(pose, flux$hand == InteractionHand.MAIN_HAND ? "mainItem" : "offItem");
  }

  @Inject(method = "renderMap", at = @At("RETURN"))
  private void flux$mapEnd(
      PoseStack pose, SubmitNodeCollector collector, int light, ItemStack stack, CallbackInfo ci) {
    pose.popPose();
  }
}
