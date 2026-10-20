package dev.fluxtweaks.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

public final class ViewModels {
  public static void apply(PoseStack pose, String group) {
    var c = FluxTweaks.config;
    var f = Feature.VIEW_MODELS;
    if (!c.enabled(f) || !c.bool(f, group + ".enabled")) return;
    pose.translate(
        c.number(f, group + ".posX"), c.number(f, group + ".posY"), c.number(f, group + ".posZ"));
    pose.mulPose(Axis.XP.rotationDegrees(c.decimal(f, group + ".rotX")));
    pose.mulPose(Axis.YP.rotationDegrees(c.decimal(f, group + ".rotY")));
    pose.mulPose(Axis.ZP.rotationDegrees(c.decimal(f, group + ".rotZ")));
    float scale = c.decimal(f, group + ".scale");
    pose.scale(scale, scale, scale);
  }

  public static String armGroup(HumanoidArm arm) {
    return arm == Minecraft.getInstance().player.getMainArm() ? "mainArm" : "offArm";
  }

  private ViewModels() {}
}
