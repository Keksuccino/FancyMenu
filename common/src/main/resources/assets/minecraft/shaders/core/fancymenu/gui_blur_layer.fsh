#version 150

uniform sampler2D DiffuseSampler;

in vec2 v_TexCoord;

uniform vec2 ScreenSize;
uniform vec4 Rect;
uniform float BlurRadius;
uniform vec3 TintColor;
uniform float TintStrength;
uniform float Opacity;
uniform float CornerRadius;

out vec4 fragColor;

const int SAMPLE_COUNT = 9;

float roundedRectMask(vec2 pixelCoord, vec2 rectSize, float radius) {
    if (pixelCoord.x < 0.0 || pixelCoord.y < 0.0 || pixelCoord.x > rectSize.x || pixelCoord.y > rectSize.y) {
        return 0.0;
    }
    if (radius <= 0.0) {
        return 1.0;
    }
    vec2 halfSize = rectSize * 0.5;
    float clampedRadius = min(radius, min(halfSize.x, halfSize.y));
    vec2 delta = abs(pixelCoord - halfSize) - (halfSize - vec2(clampedRadius));
    float distance = length(max(delta, 0.0)) - clampedRadius;
    return clamp(1.0 - distance, 0.0, 1.0);
}

vec4 sampleBlurred(vec2 baseCoord, vec2 rectMin, vec2 rectMax, float radius) {
    if (radius <= 0.001) {
        vec2 coord = clamp(baseCoord, rectMin, rectMax);
        return texture(DiffuseSampler, coord);
    }

    float centerIndex = float(SAMPLE_COUNT - 1) * 0.5;
    float sigma = max(radius * 0.35, 0.0001);
    vec4 horizontal = vec4(0.0);
    vec4 vertical = vec4(0.0);
    float weightH = 0.0;
    float weightV = 0.0;

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        float offsetIndex = float(i) - centerIndex;
        float normalized = offsetIndex / centerIndex;
        float pixelOffset = normalized * radius;
        float weight = exp(-0.5 * (pixelOffset * pixelOffset) / (sigma * sigma));

        vec2 offsetH = vec2(pixelOffset / ScreenSize.x, 0.0);
        vec2 offsetV = vec2(0.0, pixelOffset / ScreenSize.y);

        vec2 coordH = clamp(baseCoord + offsetH, rectMin, rectMax);
        vec2 coordV = clamp(baseCoord + offsetV, rectMin, rectMax);

        horizontal += texture(DiffuseSampler, coordH) * weight;
        vertical += texture(DiffuseSampler, coordV) * weight;
        weightH += weight;
        weightV += weight;
    }

    horizontal /= weightH;
    vertical /= weightV;

    return mix(horizontal, vertical, 0.5);
}

void main() {
    vec2 rectMin = Rect.xy;
    vec2 rectSize = Rect.zw;
    vec2 rectMax = rectMin + rectSize;

    if (rectSize.x <= 0.0 || rectSize.y <= 0.0) {
        discard;
    }

    if (v_TexCoord.x < rectMin.x || v_TexCoord.y < rectMin.y || v_TexCoord.x > rectMax.x || v_TexCoord.y > rectMax.y) {
        discard;
    }

    float radius = max(BlurRadius, 0.0);
    vec4 blurred = sampleBlurred(v_TexCoord, rectMin, rectMax, radius);

    float tintStrength = clamp(TintStrength, 0.0, 1.0);
    vec3 tinted = mix(blurred.rgb, TintColor, tintStrength);

    float opacity = clamp(Opacity, 0.0, 1.0);
    vec2 pixelCoord = (v_TexCoord - rectMin) * ScreenSize;
    vec2 rectSizePx = rectSize * ScreenSize;
    float mask = roundedRectMask(pixelCoord, rectSizePx, max(CornerRadius, 0.0));

    float finalAlpha = opacity * mask;
    if (finalAlpha <= 0.0) {
        discard;
    }

    fragColor = vec4(tinted, finalAlpha);
}
