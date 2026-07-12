package de.keksuccino.fancymenu.util.rendering.text.markdown;

final class MarkdownTextPositioning {

    private MarkdownTextPositioning() {
    }

    static float normalizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0F ? scale : 0.0F;
    }

    static float calculateOrigin(float base, float contextualOffset, float scale) {
        return base + contextualOffset * normalizeScale(scale);
    }

    static float calculateRenderCoordinate(float origin, float scale) {
        float normalizedScale = normalizeScale(scale);
        return normalizedScale > 0.0F ? origin / normalizedScale : 0.0F;
    }

}
