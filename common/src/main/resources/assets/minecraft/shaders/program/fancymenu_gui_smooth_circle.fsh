#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform float BorderThickness;
uniform float Roundness;
uniform vec4 Color;

in vec2 texCoord;

out vec4 fragColor;

float superellipseMask(vec2 pixel, vec2 pos, vec2 size, float roundness) {
    vec2 halfSize = size * 0.5;
    if (halfSize.x <= 0.0 || halfSize.y <= 0.0) {
        return 0.0;
    }
    vec2 center = pos + halfSize;
    vec2 rel = abs(pixel - center);
    vec2 norm = rel / halfSize;
    float n = max(0.1, roundness);
    float value = pow(norm.x, n) + pow(norm.y, n);
    float aa = fwidth(value);
    return 1.0 - smoothstep(1.0 - aa, 1.0 + aa, value);
}

void main() {
    vec2 uv = texCoord;
    vec2 pixel = uv * OutSize;
    float mask = superellipseMask(pixel, Rect.xy, Rect.zw, Roundness);
    if (BorderThickness > 0.0) {
        vec2 innerPos = Rect.xy + vec2(BorderThickness);
        vec2 innerSize = Rect.zw - vec2(BorderThickness * 2.0);
        if (innerSize.x > 0.0 && innerSize.y > 0.0) {
            float innerMask = superellipseMask(pixel, innerPos, innerSize, Roundness);
            mask = max(0.0, mask - innerMask);
        }
    }
    if (mask <= 0.0001) {
        discard;
    }

    float alpha = clamp(Color.a, 0.0, 1.0) * mask;
    fragColor = vec4(Color.rgb, alpha);
}
