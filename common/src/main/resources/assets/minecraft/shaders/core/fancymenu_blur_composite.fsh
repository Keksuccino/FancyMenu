#version 150

#moj_import <fancymenu_rounded_box.glsl>

uniform sampler2D BlurSampler;

// Framebuffer rect: xy = bottom-left corner, zw = size
uniform vec4 Rect;
// Bottom-left, bottom-right, top-right, top-left
uniform vec4 CornerRadii;
// Row-major 2x2 matrix from framebuffer space into the mask's local space
uniform vec4 Rotation;
uniform vec4 Tint;
// Replaces Tint in the header band at the top of the mask
uniform vec4 HeaderTint;
// x = shape type (0 = rounded rect, 1 = superellipse), y = superellipse roundness, z = header height, w = opacity
uniform vec4 ShapeInfo;
// xy = blur level size in texels, z = framebuffer pixels per blur level texel, w = blur strength
uniform vec4 SampleInfo;
// xy = output size, z = 1 if BlurSampler holds a blur
uniform vec4 TargetInfo;

in vec2 texCoord;

out vec4 fragColor;

float superellipseAlpha(vec2 local, vec2 halfSize, float n) {
    vec2 uv = abs(local) / (halfSize + vec2(1.0e-6));
    float d = pow(pow(uv.x, n) + pow(uv.y, n), 1.0 / n);
    float antialiasWidth = max(fwidth(d) * 0.5, 0.0001);
    return 1.0 - smoothstep(1.0 - antialiasWidth, 1.0 + antialiasWidth, d);
}

// Cubic B-spline upsample with four bilinear taps. It is smooth, so the upscaled blur shows no blocky or diamond-shaped steps.
vec3 sampleBlur(vec2 texel) {
    vec2 inverseSize = 1.0 / SampleInfo.xy;
    vec2 position = texel - 0.5;
    vec2 base = floor(position);
    vec2 f = position - base;
    vec2 f2 = f * f;
    vec2 f3 = f2 * f;
    vec2 w0 = (1.0 - 3.0 * f + 3.0 * f2 - f3) / 6.0;
    vec2 w1 = (4.0 - 6.0 * f2 + 3.0 * f3) / 6.0;
    vec2 w2 = (1.0 + 3.0 * f + 3.0 * f2 - 3.0 * f3) / 6.0;
    vec2 w3 = f3 / 6.0;
    vec2 g0 = w0 + w1;
    vec2 g1 = w2 + w3;
    vec2 h0 = (base - 0.5 + w1 / g0) * inverseSize;
    vec2 h1 = (base + 1.5 + w3 / g1) * inverseSize;
    vec3 s00 = textureLod(BlurSampler, h0, 0.0).rgb;
    vec3 s10 = textureLod(BlurSampler, vec2(h1.x, h0.y), 0.0).rgb;
    vec3 s01 = textureLod(BlurSampler, vec2(h0.x, h1.y), 0.0).rgb;
    vec3 s11 = textureLod(BlurSampler, h1, 0.0).rgb;
    return g0.y * (g0.x * s00 + g1.x * s10) + g1.y * (g0.x * s01 + g1.x * s11);
}

float interleavedGradientNoise(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}

// Outputs premultiplied color for ONE, ONE_MINUS_SRC_ALPHA blending, so the result is the tinted, blurred backdrop over the
// untouched framebuffer, weighted by the mask.
void main() {
    vec2 pixel = texCoord * TargetInfo.xy;
    vec2 halfSize = Rect.zw * 0.5;
    vec2 offset = pixel - (Rect.xy + halfSize);
    vec2 local = vec2(Rotation.x * offset.x + Rotation.y * offset.y, Rotation.z * offset.x + Rotation.w * offset.y);

    float shapeAlpha;
    if (ShapeInfo.x < 0.5) {
        shapeAlpha = fancymenuRoundedBoxAlpha(local, halfSize, CornerRadii);
    } else {
        shapeAlpha = superellipseAlpha(local, halfSize, max(0.1, ShapeInfo.y));
    }
    float headerAntialiasWidth = max(fwidth(local.y) * 0.5, 0.0001);
    float headerCoverage = (ShapeInfo.z > 0.0) ? smoothstep(-headerAntialiasWidth, headerAntialiasWidth, local.y - (halfSize.y - ShapeInfo.z)) : 0.0;

    float mask = clamp(shapeAlpha, 0.0, 1.0) * clamp(ShapeInfo.w, 0.0, 1.0);
    if (mask <= 0.0) {
        discard;
    }

    vec4 tint = mix(Tint, HeaderTint, headerCoverage);
    float tintStrength = clamp(tint.a, 0.0, 1.0);
    float blurWeight = (TargetInfo.z > 0.5) ? (1.0 - tintStrength) * clamp(SampleInfo.w, 0.0, 1.0) : 0.0;
    vec3 color = tintStrength * tint.rgb;
    if (blurWeight > 0.0) {
        color += blurWeight * sampleBlur(pixel / SampleInfo.z);
    }
    // Dither by half an 8-bit step so smooth blurred gradients don't band.
    color = mask * color + (interleavedGradientNoise(pixel) - 0.5) * (mask / 255.0);
    fragColor = vec4(color, mask * (tintStrength + blurWeight));
}
