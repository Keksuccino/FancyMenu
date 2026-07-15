#version 150

uniform sampler2D BlurSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 CornerRadii; // BL, BR, TR, TL (matches Java flipVertical)
uniform float ShapeType; // 0.0 = rounded rect, 1.0 = superellipse
uniform float Roundness;
uniform vec4 Rotation; // m00, m01, m10, m11
uniform vec4 Tint;

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

// Calculates the alpha mask (0.0 to 1.0) for a Superellipse.
// It uses the n-th root method to linearize the distance field,
// ensuring smooth AA regardless of shape.
float getShapeAlpha(vec2 pixel, vec2 pos, vec2 size, float n) {
    vec2 halfSize = size * 0.5;
    vec2 center = pos + halfSize;

    vec2 p = abs(pixel - center);
    vec2 uv = p / (halfSize + vec2(1e-6));

    float raw = pow(uv.x, n) + pow(uv.y, n);
    float d = pow(raw, 1.0 / n);

    float fw = max(fwidth(d) * 0.5, 0.0001);
    return 1.0 - smoothstep(1.0 - fw, 1.0 + fw, d);
}

void main() {
    vec2 uv = texCoord;
    vec2 pixel = uv * OutSize;

    // --- Mask Calculation ---
    vec2 halfSize = Rect.zw * 0.5;
    vec2 center = Rect.xy + halfSize;
    vec2 p = pixel - center;
    vec2 local = vec2(Rotation.x * p.x + Rotation.y * p.y, Rotation.z * p.x + Rotation.w * p.y);
    vec2 localPixel = local + center;

    float mask;
    if (ShapeType < 0.5) {
        mask = fancymenuRoundedBoxAlpha(local, halfSize, CornerRadii);
    } else {
        float n = max(0.1, Roundness);
        mask = getShapeAlpha(localPixel, Rect.xy, Rect.zw, n);
    }

    // Discard outside pixels immediately
    if (mask <= 0.0) {
        discard;
    }

    // --- Color Application ---
    vec4 blurred = texture(BlurSampler, uv);

    // Apply Tint
    float tintStrength = clamp(Tint.a, 0.0, 1.0);
    vec3 blurColor = mix(blurred.rgb, Tint.rgb, tintStrength);

    // Output final color with mask as alpha
    fragColor = vec4(blurColor, mask);
}
