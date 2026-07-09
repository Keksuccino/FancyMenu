package de.keksuccino.fancymenu.util.rendering.text.markdown;

final class MarkdownTextRenderMath {

    private MarkdownTextRenderMath() {}

    static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0F ? scale : 0.0F;
    }

    static float resolveOrigin(float fragmentOrigin, float unscaledOffset, float scale) {
        return fragmentOrigin + (unscaledOffset * scale);
    }

    static float toLocalCoordinate(float textOrigin, float scale) {
        if (scale <= 0.0F) return 0.0F;
        return textOrigin / scale;
    }

}
