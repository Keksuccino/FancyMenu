#version 150

uniform sampler2D ImageSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform float Roundness;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec2 UvMin;
uniform vec2 UvMax;
uniform vec4 Color;

in vec2 texCoord;

out vec4 fragColor;

// Calculates the alpha mask (0.0 to 1.0) for a Superellipse.
// It uses the n-th root method to linearize the distance field,
// ensuring perfectly smooth Anti-Aliasing (AA) regardless of shape.
float getShapeAlpha(vec2 pixel, vec2 pos, vec2 size, float n) {
    vec2 halfSize = size * 0.5;
    vec2 center = pos + halfSize;

    // Position relative to center
    vec2 p = abs(pixel - center);

    // Normalize coordinates (0.0 at center, 1.0 at edge along axes)
    // Add tiny epsilon to prevent division by zero
    vec2 uv = p / (halfSize + vec2(1e-6));

    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);

    float fw = max(fwidth(d) * 0.5, 0.0001);

    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

// Clamp the selected region to valid texel centers so bilinear filtering cannot cross its edges.
// Normalized bounds preserve reversed mapping, while center-limiting safely collapses sub-texel and out-of-range regions.
vec2 clampUvToRegionTexelCenters(vec2 uv) {
    vec2 regionMin = min(UvMin, UvMax);
    vec2 regionMax = max(UvMin, UvMax);
    vec2 textureSizePixels = max(vec2(textureSize(ImageSampler, 0)), vec2(1.0));
    vec2 halfTexel = 0.5 / textureSizePixels;
    vec2 textureMin = halfTexel;
    vec2 textureMax = vec2(1.0) - halfTexel;
    vec2 regionCenter = clamp((regionMin + regionMax) * 0.5, textureMin, textureMax);
    vec2 sampleMin = min(max(regionMin + halfTexel, textureMin), regionCenter);
    vec2 sampleMax = max(min(regionMax - halfTexel, textureMax), regionCenter);
    return clamp(uv, sampleMin, sampleMax);
}

void main() {
    vec2 pixel = texCoord * OutSize;

    float n = max(0.1, Roundness);
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;
    vec2 p = pixel - center;
    vec2 local = vec2(Rotation.x * p.x + Rotation.y * p.y, Rotation.z * p.x + Rotation.w * p.y);
    vec2 localPixel = local + center;
    float mask = getShapeAlpha(localPixel, Rect.xy, Rect.zw, n);

    if (mask <= 0.0) {
        discard;
    }

    vec2 uv = (localPixel - Rect.xy) / Rect.zw;
    uv.y = 1.0 - uv.y;

    uv = mix(UvMin, UvMax, uv);
    uv = clampUvToRegionTexelCenters(uv);

    vec4 texColor = texture(ImageSampler, uv);
    vec3 rgb = texColor.rgb * Color.rgb;
    float alpha = texColor.a * Color.a * mask;

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
