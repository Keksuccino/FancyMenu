#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange; // e.g. 1.0 for raster
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // For raster atlas, 'dist' is simply the alpha coverage (0..1)
    float dist = UseTrueSdf != 0 ? texColor.a : median(texColor.r, texColor.g, texColor.b);

    // Calculate how many texture pixels fit in one screen pixel.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float texelsPerPixel = length(fwidth(texCoord0) * texSize);

    float alpha;

    // --- LOGIC SWITCH BASED ON SCALE ---

    if (texelsPerPixel < 1.0) {
        // MAGNIFICATION (Text is large, we are zooming in on the texture)
        // Linear filtering makes the edge blurry. We need to sharpen it.
        // We use the SDF math here to reconstruct a sharp edge.

        float distChangePerPixel = texelsPerPixel / (2.0 * SdfPixelRange);
        float w = distChangePerPixel * 0.5;
        // Clamp w to ensure we don't exceed the bounds
        w = clamp(w, 0.0, 0.49);

        alpha = smoothstep(0.5 - w, 0.5 + w, dist);

    } else {
        // MINIFICATION (Text is small, texture pixels are tiny)
        // The GPU linear filtering has already averaged the coverage for us.
        // We should NOT apply a harsh threshold (smoothstep) because that destroys the anti-aliasing
        // derived from the averaging, causing wobbling and aliasing.

        // We blend between the sharpened result (at 1.0 scale) and raw alpha (at high downscale)
        // to ensure a smooth transition.

        float blend = clamp(texelsPerPixel - 1.0, 0.0, 1.0);

        // Sharpened version (for near 1.0 scale)
        float distChange = texelsPerPixel / (2.0 * SdfPixelRange);
        float w = min(distChange * 0.5, 0.49);
        float sharpAlpha = smoothstep(0.5 - w, 0.5 + w, dist);

        // Raw version (for small text)
        // Just use the raw alpha, which is the correct area coverage.
        float rawAlpha = dist;

        alpha = mix(sharpAlpha, rawAlpha, blend);
    }

    // Discard very low alpha to help with sorting/performance
    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}