package dev.fluxtweaks.feature;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.Feature;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class HitColor {
  public static final int OVERLAY_ROW = 1;

  public interface State {
    boolean flux$hitColor();

    void flux$hitColor(boolean selected);
  }

  private static final RenderPipeline ARMOR = pipeline(false);
  private static final RenderPipeline TRIM = pipeline(true);

  private record ArmorKey(Identifier texture, boolean decal) {}

  private static final Map<ArmorKey, RenderType> ARMOR_TYPES = new HashMap<>();

  private static RenderPipeline pipeline(boolean decal) {
    return RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(FluxTweaks.id("pipeline/hit_armor" + (decal ? "_decal" : "")))
            .withShaderDefine("ALPHA_CUTOUT", .1f)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withDepthStencilState(
                new DepthStencilState(
                    decal ? CompareOp.EQUAL : CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build());
  }

  public static boolean target(LivingEntity entity) {
    var config = FluxTweaks.config;
    if (entity == Minecraft.getInstance().player) return config.bool(Feature.HIT_COLOR, "self");
    if (entity instanceof Player) return config.bool(Feature.HIT_COLOR, "players");
    return entity instanceof Mob && config.bool(Feature.HIT_COLOR, "mobs");
  }

  public static boolean applies(Object state) {
    return FluxTweaks.active(Feature.HIT_COLOR)
        && state instanceof LivingEntityRenderState living
        && living.hasRedOverlay
        && ((State) living).flux$hitColor();
  }

  public static boolean armor(EquipmentClientInfo.LayerType type, Object state) {
    return applies(state)
        && switch (type) {
          case HUMANOID, HUMANOID_LEGGINGS, HORSE_BODY, WOLF_BODY -> true;
          default -> false;
        };
  }

  public static int overlayPixel() {
    // Vanilla mixes overlay RGB toward the base using overlay alpha, so opacity is inverted.
    return ((int)
                Math.round(255 * (1 - FluxTweaks.config.number(Feature.HIT_COLOR, "opacity") / 100))
            << 24)
        | (FluxTweaks.config.color(Feature.HIT_COLOR, "color") & 0xFFFFFF);
  }

  public static RenderType armorType(Identifier texture, boolean decal) {
    return ARMOR_TYPES.computeIfAbsent(
        new ArmorKey(texture, decal),
        key ->
            RenderType.create(
                "flux_hit_armor",
                RenderSetup.builder(key.decal() ? TRIM : ARMOR)
                    .withTexture("Sampler0", key.texture())
                    .useLightmap()
                    .useOverlay()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));
  }

  public static boolean customArmor(RenderType type) {
    return type.pipeline() == ARMOR || type.pipeline() == TRIM;
  }

  public static void clearCache() {
    ARMOR_TYPES.clear();
  }
}
