package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

public class AfmaCopyRect {

    protected int src_x;
    protected int src_y;
    protected int dst_x;
    protected int dst_y;
    protected int width;
    protected int height;

    public AfmaCopyRect() {
    }

    public AfmaCopyRect(int srcX, int srcY, int dstX, int dstY, int width, int height) {
        this.src_x = srcX;
        this.src_y = srcY;
        this.dst_x = dstX;
        this.dst_y = dstY;
        this.width = width;
        this.height = height;
    }

    public int getSrcX() {
        return this.src_x;
    }

    public int getSrcY() {
        return this.src_y;
    }

    public int getDstX() {
        return this.dst_x;
    }

    public int getDstY() {
        return this.dst_y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public long getArea() {
        return (long)this.width * (long)this.height;
    }

    public void validate(@NotNull String context, int canvasWidth, int canvasHeight) {
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException(context + " has invalid copy size " + this.width + "x" + this.height);
        }
        if (this.src_x < 0 || this.src_y < 0 || this.dst_x < 0 || this.dst_y < 0) {
            throw new IllegalArgumentException(context + " has negative copy coordinates");
        }
        if ((this.src_x + this.width) > canvasWidth || (this.dst_x + this.width) > canvasWidth) {
            throw new IllegalArgumentException(context + " copy rectangle exceeds canvas width");
        }
        if ((this.src_y + this.height) > canvasHeight || (this.dst_y + this.height) > canvasHeight) {
            throw new IllegalArgumentException(context + " copy rectangle exceeds canvas height");
        }
    }

}
