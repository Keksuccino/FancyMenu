#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in vec4 RectInfo0;
layout(location = 4) in vec4 RectInfo1;
layout(location = 5) in vec4 RectInfo2;

layout(location = 0) out vec2 localPos;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec4 rectInfo0;
layout(location = 3) out vec4 cornerRadii;
layout(location = 4) out vec4 rotation;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    localPos = UV0;
    vertexColor = Color * ColorModulator;
    rectInfo0 = RectInfo0;
    cornerRadii = RectInfo1;
    rotation = RectInfo2;
}
