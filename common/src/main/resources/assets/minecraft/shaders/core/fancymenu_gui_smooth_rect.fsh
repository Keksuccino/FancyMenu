#version 330

in vec2 localPos;
in vec4 vertexColor;
in vec4 rectInfo0;
in vec4 cornerRadii;
in vec4 rotation;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    vec2 section = step(0.0, p);
    vec2 botTop = mix(r.xw, r.yz, section.x);
    float rad = mix(botTop.x, botTop.y, section.y);
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

void main() {
    vec2 halfSize = rectInfo0.xy;
    float borderThickness = rectInfo0.z;

    vec2 p = vec2(
        rotation.x * localPos.x + rotation.y * localPos.y,
        rotation.z * localPos.x + rotation.w * localPos.y
    );

    float dist = sdRoundedBox(p, halfSize, cornerRadii);
    float aa = max(fwidth(dist), 0.0001);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    if (borderThickness > 0.0) {
        vec4 innerRadii = max(cornerRadii - vec4(borderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(borderThickness);
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerDist = sdRoundedBox(p, innerHalfSize, innerRadii);
            float innerAlpha = 1.0 - smoothstep(-aa, aa, innerDist);
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
