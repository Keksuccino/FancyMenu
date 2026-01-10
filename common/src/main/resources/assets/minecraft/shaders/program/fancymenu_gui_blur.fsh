#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BlurSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform float CornerRadius;
uniform vec4 Tint;

in vec2 texCoord;

out vec4 fragColor;

float roundedRectMask(vec2 pixel, vec2 pos, vec2 size, float radius) {
    vec2 halfSize = size * 0.5;
    vec2 center = pos + halfSize;
    float clampedRadius = clamp(radius, 0.0, min(halfSize.x, halfSize.y));
    vec2 delta = abs(pixel - center) - (halfSize - vec2(clampedRadius));
    float dist = length(max(delta, 0.0)) - clampedRadius;
    return 1.0 - smoothstep(0.0, 1.0, dist);
}

void main() {
    vec2 uv = texCoord;
    vec4 original = texture(DiffuseSampler, uv);
    vec4 blurred = texture(BlurSampler, uv);

    float tintStrength = clamp(Tint.a, 0.0, 1.0);
    vec3 blurColor = mix(blurred.rgb, Tint.rgb, tintStrength);

    vec2 pixel = uv * OutSize;
    float mask = roundedRectMask(pixel, Rect.xy, Rect.zw, CornerRadius);
    if (mask <= 0.0001) {
        discard; // leave pixels outside the blur rect untouched
    }

    fragColor = vec4(blurColor, mask);
}
