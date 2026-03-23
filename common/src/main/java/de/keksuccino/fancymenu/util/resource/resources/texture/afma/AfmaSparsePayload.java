package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaSparsePayload {

    @Nullable
    protected String pixels_path;
    protected int packed_width;
    protected int packed_height;

    public AfmaSparsePayload() {
    }

    public AfmaSparsePayload(@Nullable String pixelsPath, int packedWidth, int packedHeight) {
        this.pixels_path = pixelsPath;
        this.packed_width = packedWidth;
        this.packed_height = packedHeight;
    }

    @Nullable
    public String getPixelsPath() {
        return this.pixels_path;
    }

    public int getPackedWidth() {
        return this.packed_width;
    }

    public int getPackedHeight() {
        return this.packed_height;
    }

    public long getPackedArea() {
        return (long) this.packed_width * (long) this.packed_height;
    }

    public void validate(@NotNull String context) {
        if ((this.pixels_path == null) || this.pixels_path.isBlank()) {
            throw new IllegalArgumentException(context + " is missing its packed pixel payload path");
        }
        if ((this.packed_width <= 0) || (this.packed_height <= 0)) {
            throw new IllegalArgumentException(context + " has invalid packed pixel dimensions " + this.packed_width + "x" + this.packed_height);
        }
    }

    @NotNull
    public AfmaSparsePayload withPixelsPath(@NotNull String pixelsPath) {
        return new AfmaSparsePayload(pixelsPath, this.getPackedWidth(), this.getPackedHeight());
    }

}
