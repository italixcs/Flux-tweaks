package dev.fluxtweaks.render;

import dev.fluxtweaks.config.ButterflyMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /** Custom polygons must not enter Sodium's constructor-cached cuboid compiler. */
  private static final class MeshCube extends ModelPart.Cube {
    private MeshCube(int u, int v, float x, float y, float z, float width, float height,
        float depth, float growX, float growY, float growZ, boolean mirror,
        float textureWidth, float textureHeight, Set<Direction> faces) {
      super(u, v, x, y, z, width, height, depth, growX, growY, growZ, mirror,
          textureWidth, textureHeight, faces);
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
      var scratch = new Vector3f();
      for (var polygon : polygons) {
        pose.transformNormal(polygon.normal(), scratch);
        float nx = scratch.x, ny = scratch.y, nz = scratch.z;
        for (var vertex : polygon.vertices()) {
          pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), scratch);
          buffer.addVertex(scratch.x, scratch.y, scratch.z, color, vertex.u(), vertex.v(),
              overlay, light, nx, ny, nz);
        }
      }
    }
  }

  public ButterflyModel() {
    super(createRoot(), RenderTypes::entitySolid);
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
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("tang", CubeListBuilder.create().texOffs(4, 68)
        .addBox(-.75f, -.65f, -.30f, 1.5f, 1.55f, .6f), PartPose.ZERO);
    // Paired pivot bosses remain fixed with the blade tang.
    for (int side = -1; side <= 1; side += 2) {
      for (int face = -1; face <= 1; face += 2) {
        root.addOrReplaceChild("screw_" + side + "_" + face,
            CubeListBuilder.create().texOffs(72, 72).addBox(-.15f, -.045f, -.04f, .3f, .09f, .08f),
            PartPose.offset(side * .52f, 0, face * .69f));
      }
    }
    return LayerDefinition.create(mesh, 128, 128);
  }

  private static ModelPart createRoot() {
    return new ModelPart(List.of(), Map.of(
        "hardware", createLayer().bakeRoot(), "blade", new ModelPart(bladeMesh(), Map.of()),
        "handle_a", roundedHandle(-.52f, -1), "handle_b", roundedHandle(.52f, 1),
        "pivot_rings", new ModelPart(pivotMesh(), Map.of())));
  }

  /** Joined surface rings retain native lighting/glint while avoiding stacked-cube stair steps. */
  private static List<ModelPart.Cube> bladeMesh() {
    var mesh = new ArrayList<ModelPart.Cube>();
    for (int segment = 0; segment < 32; segment++) {
      var lower = ring(segment / 32f);
      var upper = ring((segment + 1) / 32f);
      var cube = new MeshCube(0, 0, -.7f, .65f, -.2f, 2.8f, 10f, .4f,
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
    var caps = new MeshCube(0, 0, -.7f, .65f, -.2f, 2.8f, 10f, .4f,
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
    float spine = -.65f - .25f * (float) Math.sin(Math.PI * t) + 1.72f * t * t * t;
    float width = (1.12f + .72f * (float) Math.sin(Math.PI * t)) * (1 - .995f * t * t * t);
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

  private static ModelPart roundedHandle(float hinge, int side) {
    var detailMesh = new MeshDefinition();
    var handle = detailMesh.getRoot();
    for (int i = 0; i < 8; i++) {
      float y = -i * 1.12f - .25f;
      float curve = side * (.12f + .48f * (float) Math.sin(i / 7f * Math.PI));
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
    var surfaces = new ArrayList<ModelPart.Polygon>();
    for (int face : new int[] {-1, 1}) {
      for (int segment = 0; segment < 24; segment++) {
        var lower = handleRing(segment / 24f, side, face);
        var upper = handleRing((segment + 1) / 24f, side, face);
        for (int edge = 0; edge < 8; edge++) {
          int next = (edge + 1) % 8;
          surfaces.add(polygon(lower[edge], lower[next], upper[next], upper[edge]));
        }
      }
      cap(surfaces, handleRing(0, side, face), true);
      cap(surfaces, handleRing(1, side, face), false);
    }
    var part = new ModelPart(cubes(surfaces), Map.of("details", LayerDefinition.create(detailMesh, 128, 128).bakeRoot()));
    var pivot = PartPose.offset(hinge, 0, 0);
    part.setInitialPose(pivot);
    part.loadPose(pivot);
    return part;
  }

  private static ModelPart.Vertex[] handleRing(float t, int side, int face) {
    float curve = side * (.12f + .48f * (float) Math.sin(Math.PI * t));
    float roundedEnd = .72f + .28f * (float) Math.pow(Math.sin(Math.PI * t), .35);
    float[] x = {-.24f, .24f, .34f, .34f, .24f, -.24f, -.34f, -.34f};
    float[] z = {-.14f, -.14f, -.08f, .08f, .14f, .14f, .08f, -.08f};
    var ring = new ModelPart.Vertex[8];
    for (int i = 0; i < 8; i++) ring[i] = new ModelPart.Vertex(
        curve + x[i] * roundedEnd, -.28f - 9.1f * t, face * .39f + z[i],
        (68 + (x[i] + .34f) / .68f * 54) / 128f, (3 + 57 * t) / 128f);
    return ring;
  }

  private static void cap(List<ModelPart.Polygon> faces, ModelPart.Vertex[] ring, boolean reverse) {
    var center = new Vector3f();
    for (var vertex : ring) center.add(point(vertex));
    center.div(ring.length);
    var middle = new ModelPart.Vertex(center.x, center.y, center.z, ring[0].u(), ring[0].v());
    for (int i = 0; i < ring.length; i++) {
      var a = ring[i];
      var b = ring[(i + 1) % ring.length];
      faces.add(reverse ? polygon(a, middle, b, b) : polygon(a, b, middle, middle));
    }
  }

  private static List<ModelPart.Cube> pivotMesh() {
    var faces = new ArrayList<ModelPart.Polygon>();
    for (int side : new int[] {-1, 1}) for (int face : new int[] {-1, 1}) {
      var front = new ModelPart.Vertex[16];
      var back = new ModelPart.Vertex[16];
      for (int i = 0; i < 16; i++) {
        double angle = i * Math.PI / 8;
        float x = (float) Math.cos(angle), y = (float) Math.sin(angle);
        front[i] = new ModelPart.Vertex(side * .52f + x * .37f, y * .37f, face * .66f,
            (23 + x * 12) / 128f, (89 + y * 12) / 128f);
        back[i] = new ModelPart.Vertex(side * .52f + x * .37f, y * .37f, face * .52f,
            (23 + x * 12) / 128f, (89 + y * 12) / 128f);
      }
      for (int i = 0; i < 16; i++) {
        int next = (i + 1) % 16;
        faces.add(face > 0 ? polygon(front[i], back[i], back[next], front[next])
            : polygon(front[next], back[next], back[i], front[i]));
      }
      cap(faces, front, face < 0);
      cap(faces, back, face > 0);
    }
    return cubes(faces);
  }

  private static List<ModelPart.Cube> cubes(List<ModelPart.Polygon> surfaces) {
    var result = new ArrayList<ModelPart.Cube>();
    for (int index = 0; index < surfaces.size(); index += 6) {
      int count = Math.min(6, surfaces.size() - index);
      var directions = EnumSet.noneOf(Direction.class);
      for (int i = 0; i < count; i++) directions.add(Direction.values()[i]);
      var cube = new MeshCube(0, 0, -3, -10, -1, 6, 22, 2, 0, 0, 0, false, 128, 128, directions);
      for (int i = 0; i < count; i++) cube.polygons[i] = surfaces.get(index + i);
      result.add(cube);
    }
    return result;
  }
}
