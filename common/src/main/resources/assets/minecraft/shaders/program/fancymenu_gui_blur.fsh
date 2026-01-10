#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BlurSampler;
uniform vec2 OutSize;
uniform vec4 Rect;
uniform vec4 CornerRadii; // top-left, top-right, bottom-right, bottom-left
uniform vec4 Tint;

in vec2 texCoord;

out vec4 fragColor;

float roundedRectMask(vec2 pixel, vec2 pos, vec2 size, vec4 radii) {
    // Clamp radii so they never exceed half the respective axis; prevents inverted corners when callers pass large values.
    float maxX = size.x * 0.5;
    float maxY = size.y * 0.5;
    vec4 r = clamp(radii, 0.0, min(maxX, maxY));

    vec2 rel = pixel - pos;

    // Outside fast path
    if (rel.x < 0.0 || rel.y < 0.0 || rel.x > size.x || rel.y > size.y) {
        return 0.0;
    }

    float dist;
    // Top-left corner region
    if (rel.x < r.x && rel.y < r.x) {
        dist = length(rel - vec2(r.x, r.x)) - r.x;
    }
    // Top-right corner region
    else if (rel.x > size.x - r.y && rel.y < r.y) {
        dist = length(rel - vec2(size.x - r.y, r.y)) - r.y;
    }
    // Bottom-right corner region
    else if (rel.x > size.x - r.z && rel.y > size.y - r.z) {
        dist = length(rel - vec2(size.x - r.z, size.y - r.z)) - r.z;
    }
    // Bottom-left corner region
    else if (rel.x < r.w && rel.y > size.y - r.w) {
        dist = length(rel - vec2(r.w, size.y - r.w)) - r.w;
    }
    // Edges / center: distance to rectangle
    else {
        float dx = max(-rel.x, rel.x - size.x);
        float dy = max(-rel.y, rel.y - size.y);
        dist = max(dx, dy);
    }

    return 1.0 - smoothstep(0.0, 1.0, dist);
}

void main() {
    vec2 uv = texCoord;
    vec4 original = texture(DiffuseSampler, uv);
    vec4 blurred = texture(BlurSampler, uv);

    float tintStrength = clamp(Tint.a, 0.0, 1.0);
    vec3 blurColor = mix(blurred.rgb, Tint.rgb, tintStrength);

    vec2 pixel = uv * OutSize;
    float mask = roundedRectMask(pixel, Rect.xy, Rect.zw, CornerRadii);
    // Discard everything outside the rounded rect so the post-pass never writes to untouched pixels.
    if (mask <= 0.0001) {
        discard;
    }

    fragColor = vec4(blurColor, mask);
}
