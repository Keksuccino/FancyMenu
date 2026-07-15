#version 330

#moj_import <fancymenu:fancymenu_rounded_box.glsl>

in vec2 localPos;
in vec4 vertexColor;
in vec4 rectInfo0;
in vec4 cornerRadii;
in vec4 rotation;

out vec4 fragColor;

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
