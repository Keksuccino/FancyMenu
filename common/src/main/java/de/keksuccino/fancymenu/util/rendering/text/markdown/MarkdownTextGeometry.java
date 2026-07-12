package de.keksuccino.fancymenu.util.rendering.text.markdown;

/**
 * Pure geometry shared by markdown rendering and hit-testing. Offsets stay in local text space and are scaled exactly once when converted to screen space.
 */
final class MarkdownTextGeometry {

    private MarkdownTextGeometry() {
    }

    static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0F ? scale : 0.0F;
    }

    static float horizontalOffset(boolean quoteStart, float quoteIndent, boolean bulletStart, int bulletListLevel, float bulletListIndent, float bulletSpaceAfterIndent, boolean multilineCodeStart, boolean singleLineCodeStart) {
        float offset = 0.0F;
        if (quoteStart) offset += quoteIndent;
        if (bulletStart && bulletListLevel > 0) offset += (bulletListIndent * bulletListLevel) + bulletSpaceAfterIndent;
        if (multilineCodeStart) offset += 10.0F;
        if (singleLineCodeStart) offset += 1.0F;
        return offset;
    }

    static float verticalOffset(boolean multilineCodeStart, boolean bulletItemStartLine, float bulletListSpacing) {
        float offset = 0.0F;
        if (multilineCodeStart) offset += 10.0F;
        if (bulletItemStartLine) offset += bulletListSpacing;
        return offset;
    }

    static float screenOrigin(float fragmentOrigin, float localOffset, float scale) {
        return fragmentOrigin + (localOffset * scale);
    }

    static float localCoordinate(float screenOrigin, float scale) {
        return scale > 0.0F ? screenOrigin / scale : 0.0F;
    }

    static boolean contains(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

}
