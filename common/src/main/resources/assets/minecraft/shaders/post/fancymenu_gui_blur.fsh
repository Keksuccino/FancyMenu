#version 330

uniform sampler2D OriginalSampler;
uniform sampler2D BlurSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 OriginalSize;
    vec2 BlurSize;
};

layout(std140) uniform GuiBlurConfig {
    vec4 Rect;
    vec4 CornerRadii;
    vec4 Rotation;
    vec4 Tint;
    vec4 ShapeInfo;
};

in vec2 texCoord;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    vec2 section = step(0.0, p);
    vec2 botTop = mix(r.xw, r.yz, section.x);
    float rad = mix(botTop.x, botTop.y, section.y);
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

float getSuperellipseAlpha(vec2 pixel, vec2 pos, vec2 size, float n) {
    vec2 halfSize = size * 0.5;
    vec2 center = pos + halfSize;
    vec2 p = abs(pixel - center);
    vec2 uv = p / (halfSize + vec2(1.0e-6));
    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);
    float fw = max(fwidth(d), 0.0001);
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

void main() {
    vec4 original = texture(OriginalSampler, texCoord);
    vec4 blurred = texture(BlurSampler, texCoord);
    vec2 pixel = texCoord * OutSize;

    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;
    vec2 p = pixel - center;
    vec2 local = vec2(
        Rotation.x * p.x + Rotation.y * p.y,
        Rotation.z * p.x + Rotation.w * p.y
    );

    float mask;
    if (ShapeInfo.x < 0.5) {
        float dist = sdRoundedBox(local, halfSize, CornerRadii);
        float aa = max(fwidth(dist), 0.0001);
        mask = 1.0 - smoothstep(-aa, aa, dist);
    } else {
        float n = max(0.1, ShapeInfo.y);
        mask = getSuperellipseAlpha(local + center, Rect.xy, Rect.zw, n);
    }

    float tintStrength = clamp(Tint.a, 0.0, 1.0);
    vec3 blurColor = mix(blurred.rgb, Tint.rgb, tintStrength);
    fragColor = mix(original, vec4(blurColor, original.a), clamp(mask, 0.0, 1.0));
}
