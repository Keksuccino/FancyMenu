#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform BlurPassInfo {
    // xy = output size, zw = input size, both in texels
    vec4 BlurSizes;
    // Unused here; the Gaussian pass shares this block layout
    vec4 BlurKernel;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

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
