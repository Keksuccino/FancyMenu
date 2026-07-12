package de.keksuccino.fancymenu.util.rendering.text.markdown;

final class MarkdownTextPositioning {

    private MarkdownTextPositioning() {
    }

    static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0F ? scale : 0.0F;
    }

    static float resolveTextOrigin(float fragmentOrigin, float localOffset, float scale) {
        // Offsets are defined in unscaled font space, while the fragment origin is already in GUI space.
        return fragmentOrigin + (localOffset * sanitizeScale(scale));
    }

    static float toLocalCoordinate(float absoluteCoordinate, float scale) {
        float sanitizedScale = sanitizeScale(scale);
        return sanitizedScale > 0.0F ? absoluteCoordinate / sanitizedScale : 0.0F;
    }

}
