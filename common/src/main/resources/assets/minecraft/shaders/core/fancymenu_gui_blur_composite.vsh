#version 150

in vec4 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vUV;

void main() {
    vUV = UV0;
    vec4 worldPos = ModelViewMat * vec4(Position.xyz, 1.0);
    gl_Position = ProjMat * worldPos;
}
