package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaSparsePayload {

    @Nullable
    protected String pixels_path;
    protected int pixel_count;
    protected int channels;

    public AfmaSparsePayload() {
    }

    public AfmaSparsePayload(@Nullable String pixelsPath, int pixelCount, int channels) {
        this.pixels_path = pixelsPath;
        this.pixel_count = pixelCount;
        this.channels = channels;
    }

    @Nullable
    public String getPixelsPath() {
        return this.pixels_path;
    }

    public int getChangedPixelCount() {
        return this.pixel_count;
    }

    public int getChannels() {
        return this.channels;
    }

    public void validate(@NotNull String context) {
        if ((this.pixels_path == null) || this.pixels_path.isBlank()) {
            throw new IllegalArgumentException(context + " is missing its sparse residual payload path");
        }
        if (this.pixel_count <= 0) {
            throw new IllegalArgumentException(context + " has an invalid sparse pixel count: " + this.pixel_count);
        }
        if (!AfmaResidualPayloadHelper.isValidChannelCount(this.channels)) {
            throw new IllegalArgumentException(context + " has an invalid sparse residual channel count: " + this.channels);
        }
    }

    @NotNull
    public AfmaSparsePayload withPixelsPath(@NotNull String pixelsPath) {
        return new AfmaSparsePayload(pixelsPath, this.getChangedPixelCount(), this.getChannels());
    }

}
