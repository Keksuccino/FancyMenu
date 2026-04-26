#version 330

uniform samplerCube Sampler0;

in vec2 localPlane;
in vec4 vertexColor;
in vec4 panoramaInfo;

out vec4 fragColor;

vec3 rotateX(vec3 value, float angleSin, float angleCos) {
    return vec3(
        value.x,
        (angleCos * value.y) - (angleSin * value.z),
        (angleSin * value.y) + (angleCos * value.z)
    );
}

vec3 rotateY(vec3 value, float angleSin, float angleCos) {
    return vec3(
        (angleCos * value.x) + (angleSin * value.z),
        value.y,
        (-angleSin * value.x) + (angleCos * value.z)
    );
}

void main() {
    vec3 direction = normalize(vec3(localPlane.x, -localPlane.y, 1.0));
    direction = rotateX(direction, panoramaInfo.x, panoramaInfo.y);
    direction = rotateY(direction, panoramaInfo.z, panoramaInfo.w);

    vec4 texColor = texture(Sampler0, direction);
    vec4 color = vec4(texColor.rgb * vertexColor.rgb, texColor.a * vertexColor.a);

    if (color.a <= 0.0) {
        discard;
    }

    fragColor = color;
}
