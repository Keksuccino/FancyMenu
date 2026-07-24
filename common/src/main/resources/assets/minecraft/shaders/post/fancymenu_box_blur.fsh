#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

// Minecraft 26.1 post chains cannot inject pipeline defines, so keep this value synchronized with GuiBlurRadius.MAX_RADIUS.
const float FANCYMENU_MAX_BLUR_RADIUS = 16.0;

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;
    // Defined from GuiBlurRadius.MAX_RADIUS so malformed uniforms cannot create unbounded GPU work.
    float actualRadius = clamp(round(Radius), 0.0, FANCYMENU_MAX_BLUR_RADIUS);

    vec4 blurred = vec4(0.0);
    for (float a = -actualRadius + 0.5; a <= actualRadius; a += 2.0) {
        blurred += texture(InSampler, texCoord + sampleStep * a);
    }
    blurred += texture(InSampler, texCoord + sampleStep * actualRadius) / 2.0;
    fragColor = blurred / (actualRadius + 0.5);
}
