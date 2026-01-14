#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfSharpness;
uniform float SdfEdge;
uniform int DebugMode;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

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
    float smoothness = fwidth(dist) * SdfSharpness;
    float alpha = smoothstep(SdfEdge - smoothness, SdfEdge + smoothness, dist);
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
    if (color.a <= 0.001) {
        discard;
    }
    fragColor = color;
}
