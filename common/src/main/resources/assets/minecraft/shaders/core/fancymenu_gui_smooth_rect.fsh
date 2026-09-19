#version 330
#extension GL_ARB_separate_shader_objects : require

#include <fancymenu:fancymenu_rounded_box.glsl>

layout(location = 0) in vec2 localPos;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec4 rectInfo0;
layout(location = 3) in vec4 cornerRadii;
layout(location = 4) in vec4 rotation;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 halfSize = rectInfo0.xy;
    float borderThickness = rectInfo0.z;

    vec2 p = vec2(
        rotation.x * localPos.x + rotation.y * localPos.y,
        rotation.z * localPos.x + rotation.w * localPos.y
    );

    float alpha = fancymenuRoundedBoxAlpha(p, halfSize, cornerRadii);

    if (borderThickness > 0.0) {
        vec4 innerRadii = max(cornerRadii - vec4(borderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(borderThickness);
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerAlpha = fancymenuRoundedBoxAlpha(p, innerHalfSize, innerRadii);
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
