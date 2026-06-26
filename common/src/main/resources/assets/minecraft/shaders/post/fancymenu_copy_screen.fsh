#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord * (OutSize / max(InSize, vec2(1.0)));
    fragColor = texture(InSampler, uv);
}
