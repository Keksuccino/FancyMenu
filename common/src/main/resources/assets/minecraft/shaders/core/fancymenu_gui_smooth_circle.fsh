#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec4 circleInfo0;
layout(location = 3) in vec4 rotation;
layout(location = 4) in vec4 circleInfo2;

layout(location = 0) out vec4 fragColor;

float getShapeAlpha(vec2 p, vec2 halfSize, float n) {
    vec2 uv = abs(p) / (halfSize + vec2(1.0E-6));
    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);
    float fw = max(fwidth(d) * 0.5, 0.0001);
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

float wrapAngle(float angle) {
    float twoPi = 6.28318530718;
    float wrapped = mod(angle, twoPi);
    if (wrapped < 0.0) {
        wrapped += twoPi;
    }
    return wrapped;
}

float getArcMask(float angle, float start, float end, float aa) {
    if (start <= end) {
        float startMask = smoothstep(start - aa, start + aa, angle);
        float endMask = 1.0 - smoothstep(end - aa, end + aa, angle);
        return startMask * endMask;
    }
    float startMask = smoothstep(start - aa, start + aa, angle);
    float endMask = 1.0 - smoothstep(end - aa, end + aa, angle);
    return max(startMask, endMask);
}

void main() {
    vec2 halfSize = circleInfo0.xy;
    float borderThickness = circleInfo0.z;
    float roundness = max(0.1, circleInfo0.w);
    float shapeMode = circleInfo2.x;

    vec2 p = vec2(
        rotation.x * localPos.x + rotation.y * localPos.y,
        rotation.z * localPos.x + rotation.w * localPos.y
    );

    float alpha = getShapeAlpha(p, halfSize, roundness);

    if (borderThickness > 0.0) {
        vec2 innerHalfSize = halfSize - vec2(borderThickness);
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerAlpha = getShapeAlpha(p, innerHalfSize, roundness);
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    if (shapeMode > 0.5) {
        float angle = wrapAngle(atan(p.y, p.x));
        float start = wrapAngle(circleInfo2.y);
        float end = wrapAngle(circleInfo2.z);
        float angleAa = max(fwidth(angle) * 0.5, 0.0001);
        alpha *= getArcMask(angle, start, end, angleAa);
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
