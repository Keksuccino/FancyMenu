#version 150

uniform vec4 Rect;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float BorderThickness;
uniform vec4 Color;

out vec4 fragColor;

// Signed Distance Field for a Box with 4 independent corner radii.
// Adapted from Inigo Quilez.
float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    // r components: x=BL, y=BR, z=TR, w=TL
    vec2 section = step(0.0, p);
    vec2 botTop = mix(r.xw, r.yz, section.x);
    float rad = mix(botTop.x, botTop.y, section.y);
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

void main() {
    vec2 pixel = gl_FragCoord.xy;
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    vec2 p = pixel - center;
    p = vec2(
        Rotation.x * p.x + Rotation.y * p.y,
        Rotation.z * p.x + Rotation.w * p.y
    );

    float dist = sdRoundedBox(p, halfSize, CornerRadii);
    float aa = fwidth(dist);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    if (BorderThickness > 0.0) {
        vec4 innerRadii = max(CornerRadii - vec4(BorderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(BorderThickness);
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerDist = sdRoundedBox(p, innerHalfSize, innerRadii);
            float innerAlpha = 1.0 - smoothstep(-aa, aa, innerDist);
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(Color.rgb, Color.a * alpha);
}
