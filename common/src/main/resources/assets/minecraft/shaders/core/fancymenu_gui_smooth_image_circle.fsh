#version 330

uniform sampler2D Sampler0;

in vec2 localPos;
in vec4 vertexColor;
in vec4 circleInfo0;
in vec4 rotation;
in vec4 uvBounds;

out vec4 fragColor;

float getShapeAlpha(vec2 p, vec2 halfSize, float n) {
    vec2 uv = abs(p) / (halfSize + vec2(1.0E-6));
    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);
    float fw = max(fwidth(d), 0.0001);
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
