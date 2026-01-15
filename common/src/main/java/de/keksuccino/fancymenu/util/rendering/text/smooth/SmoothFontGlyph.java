package de.keksuccino.fancymenu.util.rendering.text.smooth;

final class SmoothFontGlyph {

    private final SmoothFontAtlas atlas;
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;
    private final int width;
    private final int height;
    private final float xOffset;
    private final float yOffset;
    private final float advance;
    private final boolean hasTexture;
    private final boolean usesTrueSdf;

    SmoothFontGlyph(SmoothFontAtlas atlas, float u0, float v0, float u1, float v1, int width, int height, float xOffset, float yOffset, float advance, boolean hasTexture, boolean usesTrueSdf) {
        this.atlas = atlas;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
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

    float u0() {
        return u0;
    }

    float v0() {
        return v0;
    }

    float u1() {
        return u1;
    }

    float v1() {
        return v1;
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
