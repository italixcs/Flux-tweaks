package dev.fluxtweaks.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.feature.ButterflyKnife;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Cosmetic render-state replacement; gameplay stacks and the existing hand pass stay intact. */
public final class ButterflyRenderer implements SpecialModelRenderer<ButterflyKnife.Snapshot> {
  private static final Map<String, Identifier> TEXTURES = Map.of(
      "Steel", texture("steel"), "Flux", texture("flux"), "Sunset", texture("sunset"),
      "Violet Marble", texture("violet_marble"), "Emerald Marble", texture("emerald_marble"),
      "Obsidian Red", texture("obsidian_red"));
  private static ButterflyRenderer instance;
  private static boolean unavailable;
  private static final Vector3fc[] EXTENTS = {
      new Vector3f(-.8f, -.8f, -.15f), new Vector3f(.8f, .8f, .15f)};
  private final ButterflyModel model = new ButterflyModel();

  private static Identifier texture(String finish) {
    return FluxTweaks.id("textures/item/butterfly/" + finish + ".png");
  }

  public static void reload() {
    instance = null;
    unavailable = false;
  }

  public static void replace(
      ItemStackRenderState state, ItemStack stack, ItemDisplayContext display, ItemOwner owner) {
    var snapshot = ButterflyKnife.snapshot(stack, display, owner);
    if (snapshot == null || unavailable) return;
    if (instance == null) {
      if (TEXTURES.values().stream().anyMatch(
          id -> Minecraft.getInstance().getResourceManager().getResource(id).isEmpty())) {
        unavailable = true;
        FluxTweaks.LOGGER.warn("Butterfly Knife textures unavailable; retaining vanilla swords until resource reload");
        return;
      }
      instance = new ButterflyRenderer();
    }
    // The snapshot owns handedness. There is no vanilla display transform on this custom layer.
    state.clear();
    state.setAnimated();
    var layer = state.newLayer();
    layer.setUsesBlockLight(true);
    layer.setExtents(() -> EXTENTS);
    layer.setFoilType(stack.hasFoil() || snapshot.glint()
        ? ItemStackRenderState.FoilType.STANDARD : ItemStackRenderState.FoilType.NONE);
    layer.setupSpecialModel(instance, snapshot);
  }

  @Override
  public ButterflyKnife.Snapshot extractArgument(ItemStack stack) {
    // This renderer is populated only by replace(), which has the owner and hand context.
    return null;
  }

  @Override
  public void submit(ButterflyKnife.Snapshot snapshot, ItemDisplayContext display,
      PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
      boolean foil, int outline) {
    if (snapshot == null) return;
    var motion = snapshot.pose();
    float side = snapshot.left() ? -1 : 1;
    pose.pushPose();
    try {
      // Even NO_TRANSFORM centers item coordinates by -.5; our hinge is authored at zero.
      pose.translate(.5f, .5f, .5f);
      // Vanilla arm/equip/swing and Flux View Models have already positioned this pose.
      pose.translate(-.16f * side, .32f + motion.lift() / 16f, -.12f);
      pose.mulPose(Axis.YP.rotationDegrees((-18 + motion.yaw()) * side));
      pose.mulPose(Axis.XP.rotationDegrees(motion.tilt()));
      pose.mulPose(Axis.ZP.rotationDegrees((24 + motion.roll()) * side));
      float scale = .75f * snapshot.scale();
      pose.scale(scale, scale, scale);
      // A 180-degree Y rotation mirrors the silhouette without reversing face winding.
      if (snapshot.left()) pose.mulPose(Axis.YP.rotationDegrees(180));
      Identifier texture = TEXTURES.getOrDefault(snapshot.finish(), TEXTURES.get("Steel"));
      collector.order(0).submitModel(model, motion, pose, model.renderType(texture),
          light, overlay, outline, null);
      if (foil) collector.order(1).submitModel(model, motion, pose, RenderTypes.entityGlint(),
          light, overlay, outline, null);
    } finally {
      pose.popPose();
    }
  }

  @Override
  public void getExtents(Consumer<Vector3fc> output) {
    for (var extent : EXTENTS) output.accept(extent);
  }
}
