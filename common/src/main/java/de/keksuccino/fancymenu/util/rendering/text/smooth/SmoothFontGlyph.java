package de.keksuccino.fancymenu.util.rendering.text.smooth;

final class SmoothFontGlyph {

    private final SmoothFontAtlas atlas;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final float xOffset;
    private final float yOffset;
    private final float advance;
    private final boolean hasTexture;
    private final boolean usesTrueSdf;

    SmoothFontGlyph(SmoothFontAtlas atlas, int x, int y, int width, int height, float xOffset, float yOffset, float advance, boolean hasTexture, boolean usesTrueSdf) {
        this.atlas = atlas;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.advance = advance;
        this.hasTexture = hasTexture;
        this.usesTrueSdf = usesTrueSdf;
    }

    SmoothFontAtlas atlas() {
        return atlas;
    }

    // UVs are derived from the current atlas size to stay valid after resizes.
    float u0() {
        return (float) x / (float) atlas.getWidth();
    }

    float v0() {
        return (float) y / (float) atlas.getHeight();
    }

    float u1() {
        return (float) (x + width) / (float) atlas.getWidth();
    }

    float v1() {
        return (float) (y + height) / (float) atlas.getHeight();
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    float xOffset() {
        return xOffset;
    }

    float yOffset() {
        return yOffset;
    }

    float advance() {
        return advance;
    }

    boolean hasTexture() {
        return hasTexture;
    }

    boolean usesTrueSdf() {
        return usesTrueSdf;
    }

}
