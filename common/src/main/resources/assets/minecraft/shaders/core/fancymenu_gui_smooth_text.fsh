#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange; // The range in texels (e.g. 4.0)
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 sampleColor = texture(Sampler0, texCoord0);

    // Calculate signed distance (0..1 range)
    float dist = UseTrueSdf != 0 ? sampleColor.a : median(sampleColor.r, sampleColor.g, sampleColor.b);

    // Convert 0..1 distance to centered -0.5..0.5
    float sd = dist - 0.5;

    // Calculate the screen-space width of the SDF range.
    // texSize: Size of texture in pixels.
    // fwidth(texCoord0): Change in UV per screen pixel.
    // fwidth(texCoord0) * texSize: Change in Texels per screen pixel.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 dTex = fwidth(texCoord0) * texSize;
    float texelsPerPixel = length(dTex); // Approximate diagonal length

    // We want the gradient to change by 1.0 unit over 1.0 screen pixel.
    // The current 'sd' changes by 1.0 unit over 'SdfPixelRange' texels.
    // So 'sd' changes by (1.0 / SdfPixelRange) per texel.
    // Therefore 'sd' changes by (texelsPerPixel / SdfPixelRange) per screen pixel.

    float sdChangePerPixel = texelsPerPixel / max(SdfPixelRange, 0.1);

    // Apply anti-aliasing
    // We divide the distance by the rate of change to normalize it to pixel units.
    float alpha = clamp(sd / sdChangePerPixel + 0.5, 0.0, 1.0);

    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
    if (color.a <= 0.01) {
        discard;
    }
    fragColor = color;
}
