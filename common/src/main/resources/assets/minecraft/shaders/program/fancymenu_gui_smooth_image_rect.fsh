#version 150

uniform sampler2D ImageSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform vec2 UvMin;
uniform vec2 UvMax;
uniform vec4 Color;

in vec2 texCoord;

out vec4 fragColor;

// Signed Distance Field for a Box with 4 independent corner radii
// Adapted from Inigo Quilez
float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    // Select radius based on quadrant (relative to center)
    // r components: x=BL, y=BR, z=TR, w=TL

    // Step returns 0.0 if negative, 1.0 if positive
    vec2 section = step(0.0, p);

    // Select between Left (x/w) and Right (y/z)
    vec2 botTop = mix(r.xw, r.yz, section.x);
    // Select between Bottom (x/y) and Top (w/z)
    float rad = mix(botTop.x, botTop.y, section.y);

    // Standard rounded box SDF calculation
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
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
    // Convert 0..1 UV to actual Screen Pixel coordinates
    vec2 pixel = texCoord * OutSize;

    // Calculate Center and Half-Size of the rectangle
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    // Position relative to center
    vec2 p = pixel - center;
    vec2 local = vec2(Rotation.x * p.x + Rotation.y * p.y, Rotation.z * p.x + Rotation.w * p.y);

    // 1. Calculate Outer Distance (Negative = inside, Positive = outside)
    float dist = sdRoundedBox(local, halfSize, CornerRadii);

    // 2. Anti-Aliasing
    float aa = max(fwidth(dist) * 0.5, 0.0001);
    float mask = 1.0 - smoothstep(-aa, aa, dist);

    if (mask <= 0.0) {
        discard;
    }

    vec2 localPixel = local + center;
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
