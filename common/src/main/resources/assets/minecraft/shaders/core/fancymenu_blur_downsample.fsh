#version 150

uniform sampler2D InSampler;

// xy = output size, zw = input size, both in texels
uniform vec4 BlurSizes;

in vec2 texCoord;

out vec4 fragColor;

// Each output texel averages the 4x4 input texels around its center with four bilinear taps on input texel corners.
void main() {
    vec2 sourceCenter = texCoord * BlurSizes.xy * 2.0;
    vec2 inverseInputSize = 1.0 / BlurSizes.zw;
    vec4 sum = textureLod(InSampler, (sourceCenter + vec2(-1.0, -1.0)) * inverseInputSize, 0.0);
    sum += textureLod(InSampler, (sourceCenter + vec2(1.0, -1.0)) * inverseInputSize, 0.0);
    sum += textureLod(InSampler, (sourceCenter + vec2(-1.0, 1.0)) * inverseInputSize, 0.0);
    sum += textureLod(InSampler, (sourceCenter + vec2(1.0, 1.0)) * inverseInputSize, 0.0);
    fragColor = sum * 0.25;
}
