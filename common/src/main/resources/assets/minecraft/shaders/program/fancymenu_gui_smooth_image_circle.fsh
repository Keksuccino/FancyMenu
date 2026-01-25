#version 150

uniform sampler2D ImageSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform float Roundness;
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

    float fw = fwidth(d);

    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

void main() {
    vec2 pixel = texCoord * OutSize;

    float n = max(0.1, Roundness);
    float mask = getShapeAlpha(pixel, Rect.xy, Rect.zw, n);

    if (mask <= 0.0) {
        discard;
    }

    vec2 uv = (pixel - Rect.xy) / Rect.zw;
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
