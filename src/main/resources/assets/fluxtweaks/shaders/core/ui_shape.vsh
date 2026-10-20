#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
out vec2 local;
out vec4 vertexColor;
flat out vec2 halfSize;
flat out float radius;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    local = UV0;
    vertexColor = Color;
    halfSize = vec2(UV2) / 16.0;
    radius = Position.z;
}
