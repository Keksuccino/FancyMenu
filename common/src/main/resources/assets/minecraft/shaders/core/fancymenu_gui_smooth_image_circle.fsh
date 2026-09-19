#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec4 circleInfo0;
layout(location = 3) in vec4 rotation;
layout(location = 4) in vec4 uvBounds;

layout(location = 0) out vec4 fragColor;

float getShapeAlpha(vec2 p, vec2 halfSize, float n) {
    vec2 uv = abs(p) / (halfSize + vec2(1.0E-6));
    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);
    float fw = max(fwidth(d) * 0.5, 0.0001);
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

vec2 resolveImageUv(vec2 p, vec2 halfSize) {
    vec2 uv = vec2(
        (p.x + halfSize.x) / max(halfSize.x * 2.0, 1.0E-6),
        1.0 - ((p.y + halfSize.y) / max(halfSize.y * 2.0, 1.0E-6))
    );
    vec2 uvMin = min(uvBounds.xy, uvBounds.zw);
    vec2 uvMax = max(uvBounds.xy, uvBounds.zw);
    uv = mix(uvBounds.xy, uvBounds.zw, uv);
    return clamp(uv, uvMin, uvMax);
}

void main() {
    vec2 halfSize = circleInfo0.xy;
    float roundness = max(0.1, circleInfo0.z);
    vec2 p = vec2(
        rotation.x * localPos.x + rotation.y * localPos.y,
        rotation.z * localPos.x + rotation.w * localPos.y
    );

    float mask = getShapeAlpha(p, halfSize, roundness);

    if (mask <= 0.0) {
        discard;
    }

    vec4 texColor = texture(Sampler0, resolveImageUv(p, halfSize));
    vec4 color = vec4(texColor.rgb * vertexColor.rgb, texColor.a * vertexColor.a * mask);

    if (color.a <= 0.0) {
        discard;
    }

    fragColor = color;
}
