#version 150
uniform sampler2D Hands;
uniform sampler2D ShieldMask;
layout(std140) uniform Chams {
    vec4 First;
    vec4 Second;
    vec4 Parameters;
    vec4 Screen;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 pixel = 1.0 / Screen.xy;
    vec4 base = texture(Hands, texCoord);
    float edge = 0.0;
    float glow = 0.0;
    float weight = 0.0;
    int quality = int(Screen.z);
    for (int ring = 1; ring <= 5; ring++) {
        if (ring > quality) break;
        float distance = Parameters.x * float(ring) / float(quality);
        float w = exp(-3.0 * pow(float(ring) / float(quality), 2.0));
        for (int step = 0; step < 12; step++) {
            float angle = float(step) * 0.5235987756;
            vec2 direction = vec2(cos(angle), sin(angle));
            float a = texture(Hands, clamp(texCoord + direction * pixel * distance, pixel * 0.5, vec2(1.0) - pixel * 0.5)).a;
            glow += a * w;
            weight += w;
            if (ring == 1) edge = max(edge, texture(Hands, texCoord + direction * pixel * 1.25).a);
        }
    }
    float halo = clamp(max(edge * 0.95, glow / max(weight, 0.001) * Parameters.y * 2.2), 0.0, 1.0);
    float amount = 0.5 + 0.5 * sin((texCoord.x + texCoord.y) * 11.0 - Parameters.w * 2.0);
    vec3 color = mix(First.rgb, Second.rgb, amount);
    float shield = Screen.w > 0.5 ? texture(ShieldMask, texCoord).a : 0.0;
    vec3 surface = mix(base.rgb, color, Parameters.z * (1.0 - shield));
    float alpha = base.a + halo * (1.0 - base.a);
    if (alpha < 0.001) discard;
    fragColor = vec4((surface * base.a + color * halo * (1.0 - base.a)) / alpha, alpha);
}
