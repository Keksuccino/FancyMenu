#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec4 SdfInfo;

out vec4 vertexColor;
out vec2 texCoord0;
out vec4 sdfInfo;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
    sdfInfo = SdfInfo;
}
