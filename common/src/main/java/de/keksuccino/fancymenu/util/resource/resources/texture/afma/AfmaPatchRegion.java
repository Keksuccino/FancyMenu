package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaPatchRegion {

    @Nullable
    protected String path;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public AfmaPatchRegion() {
    }

    public AfmaPatchRegion(@Nullable String path, int x, int y, int width, int height) {
        this.path = path;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Nullable
    public String getPath() {
        return this.path;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
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

    public void validate(@NotNull String context, int canvasWidth, int canvasHeight, boolean requirePath) {
        if (requirePath && ((this.path == null) || this.path.isBlank())) {
            throw new IllegalArgumentException(context + " is missing its patch path");
        }
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException(context + " has invalid patch size " + this.width + "x" + this.height);
        }
        if (this.x < 0 || this.y < 0) {
            throw new IllegalArgumentException(context + " has negative patch coordinates");
        }
        if ((this.x + this.width) > canvasWidth || (this.y + this.height) > canvasHeight) {
            throw new IllegalArgumentException(context + " patch rectangle exceeds canvas bounds");
        }
    }

}
