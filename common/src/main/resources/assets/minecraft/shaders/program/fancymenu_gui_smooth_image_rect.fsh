#version 150

uniform sampler2D ImageSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 InvRotation; // m00, m01, m10, m11
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

void main() {
    // Convert 0..1 UV to actual Screen Pixel coordinates
    vec2 pixel = texCoord * OutSize;

    // Calculate Center and Half-Size of the rectangle
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    // Position relative to center
    vec2 p = pixel - center;
    vec2 local = vec2(InvRotation.x * p.x + InvRotation.y * p.y, InvRotation.z * p.x + InvRotation.w * p.y);

    // 1. Calculate Outer Distance (Negative = inside, Positive = outside)
    float dist = sdRoundedBox(local, halfSize, CornerRadii);

    // 2. Anti-Aliasing
    float aa = fwidth(dist);
    float mask = 1.0 - smoothstep(-aa, aa, dist);

    if (mask <= 0.0) {
        discard;
    }

    vec2 localPixel = local + center;
    vec2 uv = (localPixel - Rect.xy) / Rect.zw;
    uv.y = 1.0 - uv.y;

    vec2 uvMin = min(UvMin, UvMax);
    vec2 uvMax = max(UvMin, UvMax);
    uv = mix(UvMin, UvMax, uv);
    uv = clamp(uv, uvMin, uvMax);

    vec4 texColor = texture(ImageSampler, uv);
    vec3 rgb = texColor.rgb * Color.rgb;
    float alpha = texColor.a * Color.a * mask;

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
