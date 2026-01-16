#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float SdfPixelRange;
uniform float SdfEdge;
uniform float SdfSharpness;
uniform int UseTrueSdf;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float dist = UseTrueSdf != 0 ? texColor.a : median(texColor.r, texColor.g, texColor.b);

    // Calculate how many texture pixels fit into one screen pixel.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float pxPerScreenPx = length(fwidth(texCoord0) * texSize);

    // SdfPixelRange is the distance range in texture pixels covered by the 0..1 distance values.
    // Calculate the softness based on the screen derivative.
    float softness = pxPerScreenPx / SdfPixelRange;
    
    // Apply sharpness modifier. 
    // Higher sharpness means a smaller transition window (harder edge).
    // Avoid division by zero.
    softness /= max(0.001, SdfSharpness);
    
    // Clamp softness to ensure the transition window fits within the [0, 1] distance range.
    // This prevents the "translucent text" issue at small scales by ensuring the curve
    // reaches 0.0 at dist=0 and 1.0 at dist=1 (if edge allows).
    // effectively: softness <= 2 * min(distance_to_0, distance_to_1)
    float maxSoftness = 2.0 * min(SdfEdge, 1.0 - SdfEdge);
    softness = min(softness, maxSoftness);

    // Center the smoothstep at SdfEdge with the calculated softness.
    float lowerBound = SdfEdge - softness * 0.5;
    float upperBound = SdfEdge + softness * 0.5;
    
    float alpha = smoothstep(lowerBound, upperBound, dist);

    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
