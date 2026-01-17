#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange;
uniform float SdfEdge;
uniform float SdfSharpness;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float getDist(vec2 uv) {
    vec4 texColor = texture(Sampler0, uv);
    return texColor.a;
}

float getAlpha(float dist, float softness, float edge) {
    float maxSoftness = 2.0 * min(edge, 1.0 - edge);
    softness = min(softness, maxSoftness);

    float lowerBound = edge - softness * 0.5;
    float upperBound = edge + softness * 0.5;

    return smoothstep(lowerBound, upperBound, dist);
}

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float pxPerScreenPx = length(fwidth(texCoord0) * texSize);

    float softness = pxPerScreenPx / SdfPixelRange;

    softness /= max(0.001, SdfSharpness);

    float edge = SdfEdge;
    if (pxPerScreenPx > 1.0) {
        float thicken = clamp((pxPerScreenPx - 1.0) * 0.05, 0.0, 0.2);
        edge -= thicken;
    }

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
