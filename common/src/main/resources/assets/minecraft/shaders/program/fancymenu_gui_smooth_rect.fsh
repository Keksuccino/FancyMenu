#version 150

uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float BorderThickness;
uniform vec4 Color;

in vec2 texCoord;

out vec4 fragColor;

// Minecraft 1.19.2's EffectProgram rejects shader imports, so keep this block identical to the shared include.
float fancymenuResolveCornerRadius(vec2 position, vec4 cornerRadii) {
    vec2 section = step(0.0, position);
    vec2 bottomTop = mix(cornerRadii.xw, cornerRadii.yz, section.x);
    return mix(bottomTop.x, bottomTop.y, section.y);
}

float fancymenuRoundedBoxDistance(vec2 position, vec2 halfSize, float cornerRadius) {
    vec2 cornerDistance = abs(position) - halfSize + cornerRadius;
    return min(max(cornerDistance.x, cornerDistance.y), 0.0) + length(max(cornerDistance, 0.0)) - cornerRadius;
}

float fancymenuSharpBoxAlpha(vec2 position, vec2 halfSize, vec2 coordinateDerivativeWidth) {
    vec2 edgeDistance = abs(position) - halfSize;
    vec2 antialiasWidth = max(coordinateDerivativeWidth * 0.5, vec2(0.0001));
    vec2 axisAlpha = 1.0 - smoothstep(-antialiasWidth, antialiasWidth, edgeDistance);
    return axisAlpha.x * axisAlpha.y;
}

float fancymenuRoundedBoxAlpha(vec2 position, vec2 halfSize, vec4 cornerRadii) {
    float cornerRadius = fancymenuResolveCornerRadius(position, cornerRadii);
    float distance = fancymenuRoundedBoxDistance(position, halfSize, cornerRadius);
    float antialiasWidth = max(fwidth(distance) * 0.5, 0.0001);
    float roundedAlpha = 1.0 - smoothstep(-antialiasWidth, antialiasWidth, distance);
    vec2 coordinateDerivativeWidth = fwidth(position);

    // Zero and subpixel-radius corners remain near the non-differentiable cusp of the box distance field. Derivatives
    // there depend on the GPU's 2x2 fragment-quad alignment and can dim covered pixels based on screen X/Y parity.
    // Blend from stable independent-axis coverage over one local pixel footprint so tiny radii stay artifact-free.
    float sharpAlpha = fancymenuSharpBoxAlpha(position, halfSize, coordinateDerivativeWidth);
    float cornerTransitionWidth = max(max(coordinateDerivativeWidth.x, coordinateDerivativeWidth.y), 0.0001);
    float roundedWeight = smoothstep(0.0, cornerTransitionWidth, max(cornerRadius, 0.0));
    return mix(sharpAlpha, roundedAlpha, roundedWeight);
}

void main() {
    // Convert 0..1 UV to actual Screen Pixel coordinates
    vec2 pixel = texCoord * OutSize;

    // Calculate Center and Half-Size of the rectangle
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    // Position relative to center
    vec2 p = pixel - center;
    p = vec2(Rotation.x * p.x + Rotation.y * p.y, Rotation.z * p.x + Rotation.w * p.y);

    float alpha = fancymenuRoundedBoxAlpha(p, halfSize, CornerRadii);

    // 3. Border Logic
    if (BorderThickness > 0.0) {
        // Calculate inner box dimensions
        // Inner radii are Outer - Thickness (clamped to 0 by max)
        vec4 innerRadii = max(CornerRadii - vec4(BorderThickness), vec4(0.0));
        vec2 innerHalfSize = halfSize - vec2(BorderThickness);

        // Only render hole if the border isn't thicker than the box itself
        if (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) {
            float innerAlpha = fancymenuRoundedBoxAlpha(p, innerHalfSize, innerRadii);

            // Subtract inner alpha from outer alpha
            alpha = clamp(alpha - innerAlpha, 0.0, 1.0);
        }
    }

    // Optimization: discard fully transparent pixels
    if (alpha <= 0.0) discard;

    fragColor = vec4(Color.rgb, Color.a * alpha);
}
