#version 150

uniform sampler2D BlurSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
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

void main() {
    vec2 uv = texCoord;
    vec2 pixel = uv * OutSize;

    // --- Mask Calculation (SDF Method) ---
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;
    vec2 p = pixel - center;

    // Calculate distance (negative = inside, positive = outside)
    float dist = sdRoundedBox(p, halfSize, CornerRadii);

    // Smooth Anti-Aliasing
    float aa = fwidth(dist);
    float mask = 1.0 - smoothstep(-aa, aa, dist);

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