#version 330

#moj_import <fancymenu:fancymenu_rounded_box.glsl>

uniform sampler2D Sampler0;

in vec2 localPos;
in vec4 vertexColor;
in vec4 rectInfo0;
in vec4 cornerRadii;
in vec4 rotation;
in vec4 uvBounds;

out vec4 fragColor;

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
    vec2 halfSize = rectInfo0.xy;
    vec2 p = vec2(
        rotation.x * localPos.x + rotation.y * localPos.y,
        rotation.z * localPos.x + rotation.w * localPos.y
    );

    float mask = fancymenuRoundedBoxAlpha(p, halfSize, cornerRadii);

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
