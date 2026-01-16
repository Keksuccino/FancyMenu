#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange;
uniform float SdfEdge;
uniform float SdfSharpness;
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float getDist(vec2 uv) {
    vec4 texColor = texture(Sampler0, uv);
    return UseTrueSdf != 0 ? texColor.a : median(texColor.r, texColor.g, texColor.b);
}

float getAlpha(float dist, float softness, float edge) {
    // Clamp softness to ensure the transition window fits within the [0, 1] distance range.
    float maxSoftness = 2.0 * min(edge, 1.0 - edge);
    softness = min(softness, maxSoftness);

    float lowerBound = edge - softness * 0.5;
    float upperBound = edge + softness * 0.5;

    return smoothstep(lowerBound, upperBound, dist);
}

void main() {
    // Calculate how many texture pixels fit into one screen pixel.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float pxPerScreenPx = length(fwidth(texCoord0) * texSize);

    // SdfPixelRange is the distance range in texture pixels covered by the 0..1 distance values.
    // Calculate the softness based on the screen derivative.
    float softness = pxPerScreenPx / SdfPixelRange;

    // Apply sharpness modifier.
    // Higher sharpness means a smaller transition window (harder edge).
    // Avoid division by zero.
    softness /= max(0.001, SdfSharpness);

    // Adaptive thickening for small text
    // If pxPerScreenPx is large (zoomed out), we reduce the edge to thicken the glyph.
    // Base edge is usually 0.5.
    float edge = SdfEdge;
    if (pxPerScreenPx > 1.0) {
        // Thicken factor: how much to reduce edge.
        // Cap thickening to avoid bloat.
        float thicken = clamp((pxPerScreenPx - 1.0) * 0.05, 0.0, 0.2);
        edge -= thicken;
    }

    // Supersampling (2x2 grid)
    // We can use dFdx/dFdy to find offsets for sub-pixel sampling.
    // Offsets should be +/- 0.25 of a pixel.
    vec2 dx = dFdx(texCoord0);
    vec2 dy = dFdy(texCoord0);

    float d1 = getDist(texCoord0 + dx * -0.25 + dy * -0.25);
    float d2 = getDist(texCoord0 + dx * 0.25 + dy * -0.25);
    float d3 = getDist(texCoord0 + dx * -0.25 + dy * 0.25);
    float d4 = getDist(texCoord0 + dx * 0.25 + dy * 0.25);

    float a1 = getAlpha(d1, softness, edge);
    float a2 = getAlpha(d2, softness, edge);
    float a3 = getAlpha(d3, softness, edge);
    float a4 = getAlpha(d4, softness, edge);

    float alpha = (a1 + a2 + a3 + a4) * 0.25;

    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}