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
        // Sharpen the blurred edge created by linear interpolation
        float distChangePerPixel = texelsPerPixel / (2.0 * SdfPixelRange);
        float w = clamp(distChangePerPixel * 0.5, 0.0, 0.49);
        alpha = smoothstep(0.5 - w, 0.5 + w, dist);
    } else {
        // MINIFICATION (Text is normal/small)
        // The 2x texture already provides 4-sample anti-aliasing via linear filtering.
        // We apply gamma correction to fix the "thin" or "step-y" look of linear alpha.
        // A gamma of ~1.45 broadens the semi-transparent regions slightly.
        alpha = pow(dist, 1.0 / 1.45);
    }

    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}