float fancymenuResolveCornerRadius(vec2 position, vec4 cornerRadii) {
    vec2 section = step(0.0, position);
    vec2 bottomTop = mix(cornerRadii.xw, cornerRadii.yz, section.x);
    return mix(bottomTop.x, bottomTop.y, section.y);
}

float fancymenuRoundedBoxDistance(vec2 position, vec2 halfSize, float cornerRadius) {
    vec2 cornerDistance = abs(position) - halfSize + cornerRadius;
    return min(max(cornerDistance.x, cornerDistance.y), 0.0) + length(max(cornerDistance, 0.0)) - cornerRadius;
}

float fancymenuSharpBoxAlpha(vec2 position, vec2 halfSize, vec2 coordinateDerivativeWidth) {
    vec2 edgeDistance = abs(position) - halfSize;
    vec2 antialiasWidth = max(coordinateDerivativeWidth * 0.5, vec2(0.0001));
    vec2 axisAlpha = 1.0 - smoothstep(-antialiasWidth, antialiasWidth, edgeDistance);
    return axisAlpha.x * axisAlpha.y;
}

float fancymenuRoundedBoxAlpha(vec2 position, vec2 halfSize, vec4 cornerRadii) {
    float cornerRadius = fancymenuResolveCornerRadius(position, cornerRadii);
    float distance = fancymenuRoundedBoxDistance(position, halfSize, cornerRadius);
    float antialiasWidth = max(fwidth(distance) * 0.5, 0.0001);
    float roundedAlpha = 1.0 - smoothstep(-antialiasWidth, antialiasWidth, distance);
    vec2 coordinateDerivativeWidth = fwidth(position);

    // Zero and subpixel-radius corners remain near the non-differentiable cusp of the box distance field. Derivatives
    // there depend on the GPU's 2x2 fragment-quad alignment and can dim covered pixels based on screen X/Y parity.
    // Blend from stable independent-axis coverage over one local pixel footprint so tiny radii stay artifact-free.
    float sharpAlpha = fancymenuSharpBoxAlpha(position, halfSize, coordinateDerivativeWidth);
    float cornerTransitionWidth = max(max(coordinateDerivativeWidth.x, coordinateDerivativeWidth.y), 0.0001);
    float roundedWeight = smoothstep(0.0, cornerTransitionWidth, max(cornerRadius, 0.0));
    return mix(sharpAlpha, roundedAlpha, roundedWeight);
}
