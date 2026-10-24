#version 330
#moj_import <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
#ifdef COLORED
    fragColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
#else
    float coverage = texture(Sampler0, texCoord0).r;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
#endif
}
