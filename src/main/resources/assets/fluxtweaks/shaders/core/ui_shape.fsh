#version 330
#moj_import <minecraft:dynamictransforms.glsl>
in vec2 local;
in vec4 vertexColor;
flat in vec2 halfSize;
flat in float radius;
out vec4 fragColor;
void main() {
    vec2 q = abs(local) - halfSize + radius;
    float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
#ifdef TRIANGLE
    distance = max(-(local.x + halfSize.x), max(
        dot(local + halfSize, normalize(vec2(halfSize.y, -2.0*halfSize.x))),
        dot(local - vec2(halfSize.x, 0.0), normalize(vec2(halfSize.y, 2.0*halfSize.x)))));
#endif
    float aa = max(fwidth(distance) * 0.5, 0.0001);
    float coverage = 1.0 - smoothstep(-aa, aa, distance);
#ifdef OUTLINE
    coverage -= 1.0 - smoothstep(-aa, aa, distance + 1.0);
#endif
    vec4 color = vertexColor;
    // Independent axes keep thin hue strips spanning their full vertical colour range.
    float horizontal = clamp((local.x + halfSize.x) / (2.0*halfSize.x), 0.0, 1.0);
    float vertical = clamp((local.y + halfSize.y) / (2.0*halfSize.y), 0.0, 1.0);
#ifdef SATURATION_VALUE
    color.rgb = mix(vec3(1.0), color.rgb, horizontal) * (1.0 - vertical);
#endif
#ifdef HUE
    color.rgb = clamp(abs(fract(vertical + vec3(0.0, 2.0/3.0, 1.0/3.0))*6.0-3.0)-1.0, 0.0, 1.0);
#endif
    fragColor = vec4(color.rgb, color.a * coverage) * ColorModulator;
}
