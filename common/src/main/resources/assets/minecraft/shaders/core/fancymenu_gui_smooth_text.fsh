#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfSharpness;
uniform float SdfEdge;
uniform float SdfPixelRange;
uniform int DebugMode;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float screenPxRange(vec2 uv) {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 unitRange = vec2(max(SdfPixelRange, 0.5)) / texSize;
    vec2 screenTexSize = vec2(1.0) / fwidth(uv);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    float dist = texture(Sampler0, texCoord0).r;
    if (DebugMode == 1) {
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }
    if (DebugMode == 2) {
        fragColor = vec4(dist, dist, dist, 1.0);
        return;
    }
    float sigDist = dist - SdfEdge;
    float range = screenPxRange(texCoord0) * SdfSharpness;
    float alpha = clamp(sigDist * range + 0.5, 0.0, 1.0);
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
    if (color.a <= 0.001) {
        discard;
    }
    fragColor = color;
}
