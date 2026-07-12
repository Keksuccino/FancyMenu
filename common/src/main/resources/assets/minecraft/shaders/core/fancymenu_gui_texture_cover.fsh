#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Full-image cover UVs can reach 0/1, so sample texel centers to prevent repeat-wrap edge bleed.
    vec2 textureSizePixels = max(vec2(textureSize(Sampler0, 0)), vec2(1.0));
    vec2 halfTexel = 0.5 / textureSizePixels;
    vec2 sampleUv = clamp(texCoord0, halfTexel, vec2(1.0) - halfTexel);
    vec4 texColor = texture(Sampler0, sampleUv);
    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
