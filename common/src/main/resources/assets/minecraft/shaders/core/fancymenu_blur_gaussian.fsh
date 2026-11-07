#version 150

// Keep this synchronized with GuiBlurKernel.MAX_TAPS; core shaders of this Minecraft version can't receive defines. GuiBlurShaderResourceTest checks it.
#define FANCYMENU_BLUR_MAX_TAPS 24

uniform sampler2D InSampler;

// xy = output size, zw = input size, both in texels
uniform vec4 BlurSizes;
// xy = direction, z = sigma in texels, w = taps per side
uniform vec4 BlurKernel;

in vec2 texCoord;

out vec4 fragColor;

// One axis of a separable Gaussian. Two neighboring taps share one bilinear sample, placed at their weighted center.
void main() {
    vec2 inverseInputSize = 1.0 / BlurSizes.zw;
    vec2 uv = texCoord * BlurSizes.xy * inverseInputSize;
    vec4 sum = textureLod(InSampler, uv, 0.0);
    float sigma = BlurKernel.z;
    int taps = int(BlurKernel.w + 0.5);
    if (sigma > 0.0 && taps > 0) {
        float falloff = -0.5 / (sigma * sigma);
        vec2 texelStep = BlurKernel.xy * inverseInputSize;
        float weightSum = 1.0;
        // FANCYMENU_BLUR_MAX_TAPS bounds the loop, so malformed uniforms can't create unbounded GPU work.
        for (int tap = 1; tap <= FANCYMENU_BLUR_MAX_TAPS; tap += 2) {
            if (tap > taps) {
                break;
            }
            float firstOffset = float(tap);
            float secondOffset = firstOffset + 1.0;
            float firstWeight = exp(firstOffset * firstOffset * falloff);
            float secondWeight = (tap < taps) ? exp(secondOffset * secondOffset * falloff) : 0.0;
            float pairWeight = firstWeight + secondWeight;
            if (pairWeight < 1.0e-8) {
                break;
            }
            vec2 offset = texelStep * ((firstOffset * firstWeight + secondOffset * secondWeight) / pairWeight);
            sum += (textureLod(InSampler, uv + offset, 0.0) + textureLod(InSampler, uv - offset, 0.0)) * pairWeight;
            weightSum += 2.0 * pairWeight;
        }
        sum /= weightSum;
    }
    fragColor = sum;
}
