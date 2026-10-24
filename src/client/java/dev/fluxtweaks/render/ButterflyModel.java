package dev.fluxtweaks.render;

import dev.fluxtweaks.config.ButterflyMotion;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Original curved blade and independently hinged, channelled handles. Units are model pixels. */
public final class ButterflyModel extends Model<ButterflyMotion.Pose> {
  private final ModelPart first, second;

  public ButterflyModel() {
    super(createLayer().bakeRoot(), RenderTypes::entitySolid);
    first = root().getChild("handle_a");
    second = root().getChild("handle_b");
  }

  @Override
  public void setupAnim(ButterflyMotion.Pose pose) {
    super.setupAnim(pose);
    first.zRot = (float) Math.toRadians(pose.handleA());
    second.zRot = (float) Math.toRadians(pose.handleB());
  }

  private static LayerDefinition createLayer() {
    var root = new MeshDefinition().getRoot();
    // Closely spaced sections give the recurve a crisp silhouette, with a narrow cutting bevel.
    var blade = CubeListBuilder.create();
    for (int i = 0; i < 26; i++) {
      float t = i / 25f;
      float y = .7f + i * .38f;
      float back = -.60f + 1.65f * t * t * t;
      float width = (.86f + .65f * (float) Math.sin(t * Math.PI)) * (1 - .93f * t * t * t);
      blade.texOffs(4 + i % 4 * 9, 3 + i / 4 * 7)
          .addBox(back, y, -.19f, width, .4f, .38f);
      blade.texOffs(2 + i % 4 * 9, 4 + i / 4 * 7)
          .addBox(back + width - .04f, y, -.075f, .18f * (1 - .8f * t), .4f, .15f);
    }
    root.addOrReplaceChild("blade", blade, PartPose.ZERO);
    root.addOrReplaceChild("tang", CubeListBuilder.create().texOffs(4, 68)
        .addBox(-.75f, -.65f, -.30f, 1.5f, 1.55f, .6f), PartPose.ZERO);
    handle(root, "handle_a", -.52f, -1);
    handle(root, "handle_b", .52f, 1);
    // Paired octagonal pivot bosses remain fixed with the blade tang.
    for (int side = -1; side <= 1; side += 2) {
      for (int face = -1; face <= 1; face += 2) {
        root.addOrReplaceChild("pivot_" + side + "_" + face,
            CubeListBuilder.create().texOffs(20, 70).addBox(-.34f, -.34f, -.08f, .68f, .68f, .16f),
            PartPose.offsetAndRotation(side * .52f, 0, face * .57f, 0, 0, (float) Math.PI / 4));
        root.addOrReplaceChild("screw_" + side + "_" + face,
            CubeListBuilder.create().texOffs(72, 72).addBox(-.15f, -.045f, -.04f, .3f, .09f, .08f),
            PartPose.offset(side * .52f, 0, face * .69f));
      }
    }
    // LayerDefinition needs the mesh, so return the root through a fresh mesh holder below.
    return layer(root);
  }

  private static LayerDefinition layer(PartDefinition root) {
    var mesh = new MeshDefinition();
    for (var entry : root.getChildren().entrySet()) mesh.getRoot().addOrReplaceChild(entry.getKey(), entry.getValue());
    return LayerDefinition.create(mesh, 128, 128);
  }

  private static void handle(PartDefinition root, String name, float hinge, int side) {
    var handle = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(hinge, 0, 0));
    for (int i = 0; i < 8; i++) {
      float y = -i * 1.12f - .25f;
      float curve = side * (.12f + .48f * (float) Math.sin(i / 7f * Math.PI));
      var rails = CubeListBuilder.create().texOffs(66 + i % 4 * 10, 4 + i / 4 * 16)
          .addBox(curve - .32f, y - 1.1f, -.51f, .64f, 1.15f, .25f)
          .texOffs(66 + i % 4 * 10, 4 + i / 4 * 16)
          .addBox(curve - .32f, y - 1.1f, .26f, .64f, 1.15f, .25f);
      handle.addOrReplaceChild("rail_" + i, rails, PartPose.ZERO);
      if (i % 2 == 0 || i == 7) {
        handle.addOrReplaceChild("spacer_" + i,
            CubeListBuilder.create().texOffs(8 + i * 4, 80)
                .addBox(curve - .24f, y - .6f, -.26f, .48f, .24f, .52f), PartPose.ZERO);
      }
      if (i > 0 && i < 7) {
        handle.addOrReplaceChild("inlay_" + i,
            CubeListBuilder.create().texOffs(66 + i * 6, 70)
                .addBox(curve - .12f, y - .86f, -.53f, .24f, .55f, .04f)
                .addBox(curve - .12f, y - .86f, .49f, .24f, .55f, .04f), PartPose.ZERO);
      }
    }
    handle.addOrReplaceChild("heel", CubeListBuilder.create().texOffs(18, 88)
        .addBox(-.34f + side * .12f, -9.5f, -.52f, .68f, .5f, 1.04f), PartPose.ZERO);
    if (side == 1) {
      handle.addOrReplaceChild("latch", CubeListBuilder.create().texOffs(30, 90)
          .addBox(-1.1f, -9.65f, -.12f, 1.5f, .22f, .24f), PartPose.ZERO);
    }
  }
}
