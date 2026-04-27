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
in vec4 Color;
in vec2 UV0;
in vec4 ImageCircleInfo0;
in vec4 ImageCircleInfo1;
in vec4 ImageCircleInfo2;

out vec2 localPos;
out vec4 vertexColor;
out vec4 circleInfo0;
out vec4 rotation;
out vec4 uvBounds;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    localPos = UV0;
    vertexColor = Color * ColorModulator;
    circleInfo0 = ImageCircleInfo0;
    rotation = ImageCircleInfo1;
    uvBounds = ImageCircleInfo2;
}
