#version 150

uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float BorderThickness;
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

    // 1. Calculate Outer Distance (Negative = inside, Positive = outside)
    float dist = sdRoundedBox(p, halfSize, CornerRadii);

    // 2. Anti-Aliasing
    // fwidth gives the change in distance over one pixel.
    // This allows for perfectly smooth edges regardless of scale.
    float aa = fwidth(dist);

    // Calculate alpha (1.0 inside, 0.0 outside, smooth gradient at edge)
    // Using 0.0 to 1.0 smoothstep ensures the AA is contained within the pixel boundary
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    // 3. Border Logic
    if (BorderThickness > 0.0) {
        // Calculate inner box dimensions
        // Inner radii are Outer - Thickness (clamped to 0 by max)
        vec4 innerRadii = max(CornerRadii - vec4(BorderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(BorderThickness);

        // Only render hole if the border isn't thicker than the box itself
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
             float innerDist = sdRoundedBox(p, innerHalfSize, innerRadii);
             float innerAlpha = 1.0 - smoothstep(-aa, aa, innerDist);

             // Subtract inner alpha from outer alpha
             alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    // Optimization: discard fully transparent pixels
    if (alpha <= 0.0) discard;

    fragColor = vec4(Color.rgb, Color.a * alpha);
}