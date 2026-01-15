#version 150

uniform vec2 OutSize;
uniform vec4 Rect;
uniform float BorderThickness;
uniform float Roundness;
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

    // --- The Math Fix ---
    // Original was: pow(x, n) + pow(y, n)
    // New is: (pow(x, n) + pow(y, n)) ^ (1/n)
    // This makes 'd' behave like a linear distance (linear gradient),
    // which makes fwidth() return consistent values for AA.
    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);

    // Calculate change of 'd' per screen pixel
    float fw = fwidth(d);

    // Smoothstep creates the AA gradient.
    // We target d=1.0 as the edge.
    // The width of the gradient is determined by fw.
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

void main() {
    vec2 pixel = texCoord * OutSize;

    // Clamp roundness to be safe (matching Java)
    float n = max(0.1, Roundness);

    // Calculate Outer Shape
    float alpha = getShapeAlpha(pixel, Rect.xy, Rect.zw, n);

    // Calculate Inner Hole (Border)
    if (BorderThickness > 0.0) {
        vec2 innerPos = Rect.xy + vec2(BorderThickness);
        vec2 innerSize = Rect.zw - vec2(BorderThickness * 2.0);

        // Only cut the hole if there is space for it
        if (innerSize.x > 0.0 && innerSize.y > 0.0) {
            float innerAlpha = getShapeAlpha(pixel, innerPos, innerSize, n);
            // Cut inner from outer
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    // Optimization: Discard fully transparent pixels
    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(Color.rgb, Color.a * alpha);
}