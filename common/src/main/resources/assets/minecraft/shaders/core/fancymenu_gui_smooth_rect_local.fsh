#version 150

#moj_import <fancymenu_rounded_box.glsl>

uniform vec4 Rect;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float BorderThickness;
uniform vec4 Color;

out vec4 fragColor;

void main() {
    vec2 pixel = gl_FragCoord.xy;
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    vec2 p = pixel - center;
    p = vec2(
        Rotation.x * p.x + Rotation.y * p.y,
        Rotation.z * p.x + Rotation.w * p.y
    );

    float alpha = fancymenuRoundedBoxAlpha(p, halfSize, CornerRadii);

    if (BorderThickness > 0.0) {
        vec4 innerRadii = max(CornerRadii - vec4(BorderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(BorderThickness);
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerAlpha = fancymenuRoundedBoxAlpha(p, innerHalfSize, innerRadii);
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(Color.rgb, Color.a * alpha);
}
