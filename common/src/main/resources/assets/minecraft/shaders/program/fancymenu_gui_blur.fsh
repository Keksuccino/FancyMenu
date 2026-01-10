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
    // Work in premultiplied space to keep soft edges of translucent textures intact.
    float blurAlpha = blurred.a;
    vec3 blurStraight = (blurAlpha > 0.0001) ? blurred.rgb / blurAlpha : vec3(0.0);
    vec3 blurTintStraight = mix(blurStraight, Tint.rgb, tintStrength);
    vec3 blurTintPremul = blurTintStraight * blurAlpha;

    float originalAlpha = original.a;
    vec3 originalPremul = original.rgb * originalAlpha;

    vec2 pixel = uv * OutSize;
    float mask = roundedRectMask(pixel, Rect.xy, Rect.zw, CornerRadius);
    float finalAlpha = mix(originalAlpha, blurAlpha, mask);
    vec3 finalPremul = mix(originalPremul, blurTintPremul, mask);
    vec3 finalColor = (finalAlpha > 0.0001) ? finalPremul / finalAlpha : vec3(0.0);

    fragColor = vec4(finalColor, finalAlpha);
}
