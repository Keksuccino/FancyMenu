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

    // Ratio of texture pixels to screen pixels.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float texelsPerPixel = length(fwidth(texCoord0) * texSize);

    float alpha;

    if (texelsPerPixel < 1.0) {
        // Magnification path for large text.

        float distChangePerPixel = texelsPerPixel / (2.0 * SdfPixelRange);

        // Edge softness controls the transition width.
        float softness = 0.7;
        float w = distChangePerPixel * softness;

        // Shift the center threshold to adjust thickness.
        float center = 0.45;

        // Keep the lower bound above zero so fully transparent texels stay transparent.
        float lowerBound = max(center - w, 0.001);
        float upperBound = center + w;

        alpha = smoothstep(lowerBound, upperBound, dist);

    } else {
        // Minification path for normal/small text.

        // Gamma adjust to keep small text legible.
        alpha = pow(dist, 1.0 / 1.5);
    }

    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
