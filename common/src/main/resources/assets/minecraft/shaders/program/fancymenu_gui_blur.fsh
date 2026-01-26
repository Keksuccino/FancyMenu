#version 150

uniform sampler2D BlurSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float ShapeType; // 0.0 = rounded rect, 1.0 = superellipse
uniform float Roundness;
uniform vec4 InvRotation; // m00, m01, m10, m11
uniform vec4 Tint;

in vec2 texCoord;

out vec4 fragColor;

// Signed Distance Field for a Box with 4 independent corner radii
float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    // r components: x=BL, y=BR, z=TR, w=TL
    // Select radius based on quadrant (relative to center)
    vec2 section = step(0.0, p);
    vec2 botTop = mix(r.xw, r.yz, section.x);
    float rad = mix(botTop.x, botTop.y, section.y);

    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

// Calculates the alpha mask (0.0 to 1.0) for a Superellipse.
// It uses the n-th root method to linearize the distance field,
// ensuring smooth AA regardless of shape.
float getShapeAlpha(vec2 pixel, vec2 pos, vec2 size, float n) {
    vec2 halfSize = size * 0.5;
    vec2 center = pos + halfSize;

    vec2 p = abs(pixel - center);
    vec2 uv = p / (halfSize + vec2(1e-6));

    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);

    float fw = fwidth(d);
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

void main() {
    vec2 uv = texCoord;
    vec2 pixel = uv * OutSize;

    // --- Mask Calculation ---
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;
    vec2 p = pixel - center;
    vec2 local = vec2(InvRotation.x * p.x + InvRotation.y * p.y, InvRotation.z * p.x + InvRotation.w * p.y);
    vec2 localPixel = local + center;

    float mask;
    if (ShapeType < 0.5) {
        float dist = sdRoundedBox(local, halfSize, CornerRadii);
        float aa = fwidth(dist);
        mask = 1.0 - smoothstep(-aa, aa, dist);
    } else {
        float n = max(0.1, Roundness);
        mask = getShapeAlpha(localPixel, Rect.xy, Rect.zw, n);
    }

    // Discard outside pixels immediately
    if (mask <= 0.0) {
        discard;
    }

    // --- Color Application ---
    vec4 blurred = texture(BlurSampler, uv);

    // Apply Tint
    float tintStrength = clamp(Tint.a, 0.0, 1.0);
    vec3 blurColor = mix(blurred.rgb, Tint.rgb, tintStrength);

    // Output final color with mask as alpha
    fragColor = vec4(blurColor, mask);
}
