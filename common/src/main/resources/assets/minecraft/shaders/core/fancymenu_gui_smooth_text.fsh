#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange; // e.g. 4.0
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // 1. Retrieve the distance from the texture (0.0 to 1.0)
    float dist = UseTrueSdf != 0 ? texColor.a : median(texColor.r, texColor.g, texColor.b);

    // 2. Calculate the width of the screen pixel in texture-space units.
    //    fwidth(texCoord0) gives the change in UV coordinates per screen pixel.
    //    textureSize(...) gives the dimensions of the texture.
    //    The product gives the number of Texels per Screen Pixel.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float texelsPerPixel = length(fwidth(texCoord0) * texSize);

    // 3. Convert that to Distance-Field units.
    //    The total distance range in the texture is [-SdfPixelRange, +SdfPixelRange].
    //    So the total span in texels is (2.0 * SdfPixelRange).
    //    We want to know how much the 'dist' value (0..1) changes per screen pixel.
    float distChangePerPixel = texelsPerPixel / (2.0 * SdfPixelRange);

    // 4. Calculate the Anti-Aliasing window width.
    //    We want the transition to happen over roughly 1 screen pixel.
    //    So the smoothing window is half the change per pixel.
    float w = distChangePerPixel * 0.5;

    // 5. CLAMP the width to prevent artifacts.
    //    If 'w' > 0.5, the smoothing window extends beyond the 0..1 range of the texture.
    //    This causes the background (0.0) to partially fade in, creating "translucent rectangles".
    //    We clamp it to slightly less than 0.5 to keep 0.0 firmly transparent.
    w = min(w, 0.45);

    // 6. Compute Alpha using smoothstep for a crisp edge.
    //    The edge is at 0.5. We smooth from (0.5 - w) to (0.5 + w).
    float alpha = smoothstep(0.5 - w, 0.5 + w, dist);

    // 7. Discard fully transparent pixels to save fill rate / fix depth issues
    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
