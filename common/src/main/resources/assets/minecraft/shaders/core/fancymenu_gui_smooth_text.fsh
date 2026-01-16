#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange; // e.g. 1.0
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float dist = UseTrueSdf != 0 ? texColor.a : median(texColor.r, texColor.g, texColor.b);

    // Calculate ratio of texture pixels to screen pixels
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float texelsPerPixel = length(fwidth(texCoord0) * texSize);

    float alpha;

    if (texelsPerPixel < 1.0) {
        // MAGNIFICATION (Text is large)

        float distChangePerPixel = texelsPerPixel / (2.0 * SdfPixelRange);

        // [Improvement 1] Softer Anti-Aliasing
        // Increasing softness > 0.5 allows the edge to blur slightly across pixels,
        // hiding the "stair-step" effect on round edges.
        float softness = 0.7;
        float w = distChangePerPixel * softness;

        // [Improvement 2] Dilation (Thickening)
        // Instead of adding to 'dist' (which causes rectangles), we shift the center threshold down.
        // 0.5 is neutral. Lower values (e.g. 0.45) make the text bolder/smoother.
        float center = 0.45;

        // IMPORTANT: Clamp the lower bound to > 0.001.
        // This ensures that texture alpha 0.0 remains 0.0, preventing the "translucent rectangle" bug.
        float lowerBound = max(center - w, 0.001);
        float upperBound = center + w;

        alpha = smoothstep(lowerBound, upperBound, dist);

    } else {
        // MINIFICATION (Text is normal/small)

        // [Improvement 3] Gamma Correction
        // A gamma of ~1.5 makes the anti-aliased pixels slightly more opaque,
        // reducing the "bony" or "scratchy" look of small text.
        alpha = pow(dist, 1.0 / 1.5);
    }

    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}