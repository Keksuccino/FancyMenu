#version 150

uniform sampler2D ImageSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform vec2 UvMin;
uniform vec2 UvMax;
uniform vec4 Color;

in vec2 texCoord;

out vec4 fragColor;

// EffectProgram rejects #moj_import in this Minecraft version. Keep this compatibility copy byte-for-byte synchronized
// with shaders/include/fancymenu_rounded_box.glsl; SmoothRoundedBoxShaderTest enforces the resource contract.
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

// Clamp the selected region to valid texel centers so bilinear filtering cannot cross its edges.
// Normalized bounds preserve reversed mapping, while center-limiting safely collapses sub-texel and out-of-range regions.
vec2 clampUvToRegionTexelCenters(vec2 uv) {
    vec2 regionMin = min(UvMin, UvMax);
    vec2 regionMax = max(UvMin, UvMax);
    vec2 textureSizePixels = max(vec2(textureSize(ImageSampler, 0)), vec2(1.0));
    vec2 halfTexel = 0.5 / textureSizePixels;
    vec2 textureMin = halfTexel;
    vec2 textureMax = vec2(1.0) - halfTexel;
    vec2 regionCenter = clamp((regionMin + regionMax) * 0.5, textureMin, textureMax);
    vec2 sampleMin = min(max(regionMin + halfTexel, textureMin), regionCenter);
    vec2 sampleMax = max(min(regionMax - halfTexel, textureMax), regionCenter);
    return clamp(uv, sampleMin, sampleMax);
}

void main() {
    // Convert 0..1 UV to actual Screen Pixel coordinates
    vec2 pixel = texCoord * OutSize;

    // Calculate Center and Half-Size of the rectangle
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;

    // Position relative to center
    vec2 p = pixel - center;
    vec2 local = vec2(Rotation.x * p.x + Rotation.y * p.y, Rotation.z * p.x + Rotation.w * p.y);

    float mask = fancymenuRoundedBoxAlpha(local, halfSize, CornerRadii);

    if (mask <= 0.0) {
        discard;
    }

    vec2 localPixel = local + center;
    vec2 uv = (localPixel - Rect.xy) / Rect.zw;
    uv.y = 1.0 - uv.y;

    uv = mix(UvMin, UvMax, uv);
    uv = clampUvToRegionTexelCenters(uv);

    vec4 texColor = texture(ImageSampler, uv);
    vec3 rgb = texColor.rgb * Color.rgb;
    float alpha = texColor.a * Color.a * mask;

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
