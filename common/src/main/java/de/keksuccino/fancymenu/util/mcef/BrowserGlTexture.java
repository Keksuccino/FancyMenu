package de.keksuccino.fancymenu.util.mcef;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;

public class BrowserGlTexture extends GlTexture {

    private static final FrameBufferCache FRAME_BUFFER_CACHE_FANCYMENU = new FrameBufferCache();

    protected int width;
    protected int height;

    public BrowserGlTexture(int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels, int glId) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels, glId, FRAME_BUFFER_CACHE_FANCYMENU);
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth(int i) {
        return this.width >> i;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight(int i) {
        return this.height >> i;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int glId() {
        return this.id;
    }

    @Override
    public void addViews() {
        // do nothing
    }

    @Override
    public void removeViews() {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }

}
