#version 150

uniform sampler2D BlurSampler;
uniform vec4 ColorTint;
uniform vec2 UVMin;
uniform vec2 UVMax;
uniform vec2 AreaOrigin;
uniform vec2 AreaSize;
uniform float CornerRadius;
uniform float SmoothRadius;
uniform int Rounded;

in vec2 vUV;

out vec4 fragColor;

void main() {
    vec2 uv = mix(UVMin, UVMax, vUV);
    vec4 blurred = texture(BlurSampler, uv);

    float overlay = clamp(ColorTint.a, 0.0, 1.0);
    vec3 tinted = mix(blurred.rgb, ColorTint.rgb, overlay);
    vec4 color = vec4(tinted, blurred.a);

    if (Rounded == 1) {
        vec2 fragPos = gl_FragCoord.xy - AreaOrigin;
        vec2 clampedPos = clamp(fragPos, vec2(0.0), AreaSize);
        vec2 edgeDist = min(clampedPos, AreaSize - clampedPos);
        float radius = CornerRadius;
        float mask = 1.0;
        if (radius > 0.0) {
            float minDist = min(edgeDist.x, edgeDist.y);
            float delta = radius - minDist;
            float smooth = max(SmoothRadius, 0.0001);
            mask = 1.0 - smoothstep(0.0, smooth, delta);
        }
        color.a *= clamp(mask, 0.0, 1.0);
    }

    fragColor = color;
}
