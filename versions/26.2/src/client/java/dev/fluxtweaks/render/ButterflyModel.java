package dev.fluxtweaks.render;

import dev.fluxtweaks.config.ButterflyMotion;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** Original curved blade and independently hinged, channelled handles. Units are model pixels. */
public final class ButterflyModel extends Model<ButterflyMotion.Pose> {
  private final ModelPart first, second;

  public ButterflyModel() {
    super(createRoot(), RenderTypes::entitySolid);
    first = root().getChild("hardware").getChild("handle_a");
    second = root().getChild("hardware").getChild("handle_b");
  }

  @Override
  public void setupAnim(ButterflyMotion.Pose pose) {
    super.setupAnim(pose);
    first.zRot = (float) Math.toRadians(pose.handleA());
    second.zRot = (float) Math.toRadians(pose.handleB());
  }

  private static LayerDefinition createLayer() {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("tang", CubeListBuilder.create().texOffs(4, 68)
        .addBox(-.75f, -.65f, -.30f, 1.5f, 1.55f, .6f), PartPose.ZERO);
    handle(root, "handle_a", -.52f, -1);
    handle(root, "handle_b", .52f, 1);
    // Paired pivot bosses remain fixed with the blade tang.
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
    return LayerDefinition.create(mesh, 128, 128);
  }

  private static ModelPart createRoot() {
    return new ModelPart(List.of(), Map.of(
        "hardware", createLayer().bakeRoot(), "blade", new ModelPart(bladeMesh(), Map.of())));
  }

  /** Joined surface rings retain native lighting/glint while avoiding stacked-cube stair steps. */
  private static List<ModelPart.Cube> bladeMesh() {
    var mesh = new ArrayList<ModelPart.Cube>();
    for (int segment = 0; segment < 32; segment++) {
      var lower = ring(segment / 32f);
      var upper = ring((segment + 1) / 32f);
      var cube = new ModelPart.Cube(0, 0, -.7f, .65f, -.2f, 2.8f, 10f, .4f,
          0, 0, 0, false, 128, 128, EnumSet.allOf(Direction.class));
      for (int edge = 0; edge < 6; edge++) {
        int next = (edge + 1) % 6;
        var vertices = new ModelPart.Vertex[] {lower[edge], upper[edge], upper[next], lower[next]};
        cube.polygons[edge] = polygon(vertices);
      }
      mesh.add(cube);
    }
    var bottom = ring(0);
    var top = ring(1);
    var caps = new ModelPart.Cube(0, 0, -.7f, .65f, -.2f, 2.8f, 10f, .4f,
        0, 0, 0, false, 128, 128, EnumSet.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH));
    caps.polygons[0] = polygon(bottom[0], bottom[1], bottom[2], bottom[3]);
    caps.polygons[1] = polygon(bottom[0], bottom[3], bottom[4], bottom[5]);
    caps.polygons[2] = polygon(top[3], top[2], top[1], top[0]);
    caps.polygons[3] = polygon(top[5], top[4], top[3], top[0]);
    mesh.add(caps);
    return mesh;
  }

  private static ModelPart.Polygon polygon(ModelPart.Vertex... vertices) {
    var a = point(vertices[1]).sub(point(vertices[0]));
    var b = point(vertices[2]).sub(point(vertices[0]));
    var normal = a.cross(b);
    if (!normal.isFinite() || normal.lengthSquared() < 1e-12f)
      throw new IllegalStateException("Degenerate Butterfly blade face");
    return new ModelPart.Polygon(vertices, normal.normalize());
  }

  private static Vector3f point(ModelPart.Vertex vertex) {
    return new Vector3f(vertex.x(), vertex.y(), vertex.z());
  }

  private static ModelPart.Vertex[] ring(float t) {
    float y = .65f + 10f * t;
    float spine = -.62f + 1.68f * t * t * t;
    float width = (.94f + .62f * (float) Math.sin(Math.PI * t)) * (1 - .995f * t * t * t);
    float thick = .19f * (1 - .96f * t * t * t);
    float[] across = {0, .10f, .67f, 1, .67f, .10f};
    float[] depth = {0, -thick, -thick, 0, thick, thick};
    var vertices = new ModelPart.Vertex[6];
    for (int i = 0; i < 6; i++) {
      vertices[i] = new ModelPart.Vertex(spine + width * across[i], y, depth[i],
          (3 + 57 * across[i]) / 128f, (3 + 57 * t) / 128f);
    }
    return vertices;
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
