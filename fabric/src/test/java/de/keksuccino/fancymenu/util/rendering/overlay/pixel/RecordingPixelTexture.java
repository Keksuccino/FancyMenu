package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PixelTexture} for tests that keeps its image in an array and records every write and upload.
 */
final class RecordingPixelTexture implements PixelTexture {

    final String name;
    final int width;
    final int height;
    final int[] image;
    final List<int[]> writes = new ArrayList<>();
    final List<int[]> uploads = new ArrayList<>();
    boolean closed = false;

    RecordingPixelTexture(@NotNull String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.image = new int[width * height];
    }

    static final class Factory implements PixelTexture.Factory {

        final List<RecordingPixelTexture> created = new ArrayList<>();

        @Override
        public @NotNull PixelTexture create(@NotNull String name, int width, int height) {
            RecordingPixelTexture texture = new RecordingPixelTexture(name, width, height);
            this.created.add(texture);
            return texture;
        }

        RecordingPixelTexture last() {
            return this.created.get(this.created.size() - 1);
        }

    }

    int pixel(int x, int y) {
        return this.image[y * this.width + x];
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public void write(@NotNull int[] source, int sourceOffset, int sourceStride, int x, int y, int width, int height) {
        if ((x < 0) || (y < 0) || (x + width > this.width) || (y + height > this.height)) throw new IllegalArgumentException("Write outside of the texture");
        this.writes.add(new int[]{x, y, width, height});
        for (int row = 0; row < height; row++) {
            System.arraycopy(source, sourceOffset + row * sourceStride, this.image, (y + row) * this.width + x, width);
        }
    }

    @Override
    public void upload(int firstRow, int endRow) {
        this.uploads.add(new int[]{firstRow, endRow});
    }

    @Override
    public void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, boolean flipX, boolean flipY, int color) {
        throw new UnsupportedOperationException("Tests don't draw");
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }

}
