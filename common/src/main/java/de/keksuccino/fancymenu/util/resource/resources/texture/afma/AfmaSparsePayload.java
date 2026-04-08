package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaSparsePayload {

    @Nullable
    protected String pixels_path;
    protected int pixel_count;
    protected int channels;
    protected AfmaSparseLayoutCodec layout_codec;
    protected AfmaResidualCodec residual_codec;
    protected AfmaAlphaResidualMode alpha_mode;
    protected int alpha_changed_pixel_count;

    public AfmaSparsePayload() {
    }

    public AfmaSparsePayload(@Nullable String pixelsPath, int pixelCount, int channels) {
        this(pixelsPath, pixelCount, channels, AfmaSparseLayoutCodec.BITMASK, AfmaResidualCodec.INTERLEAVED,
                (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE, 0);
    }

    public AfmaSparsePayload(@Nullable String pixelsPath, int pixelCount, int channels,
                             @NotNull AfmaSparseLayoutCodec layoutCodec, @NotNull AfmaResidualCodec residualCodec,
                             @NotNull AfmaAlphaResidualMode alphaMode, int alphaChangedPixelCount) {
        this.pixels_path = pixelsPath;
        this.pixel_count = pixelCount;
        this.channels = channels;
        this.layout_codec = layoutCodec;
        this.residual_codec = residualCodec;
        this.alpha_mode = alphaMode;
        this.alpha_changed_pixel_count = alphaChangedPixelCount;
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

    @NotNull
    public AfmaSparseLayoutCodec getLayoutCodec() {
        return (this.layout_codec != null) ? this.layout_codec : AfmaSparseLayoutCodec.BITMASK;
    }

    @NotNull
    public AfmaResidualCodec getResidualCodec() {
        return (this.residual_codec != null) ? this.residual_codec : AfmaResidualCodec.INTERLEAVED;
    }

    @NotNull
    public AfmaAlphaResidualMode getAlphaMode() {
        if (this.alpha_mode != null) {
            return this.alpha_mode;
        }
        return (this.channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? AfmaAlphaResidualMode.FULL : AfmaAlphaResidualMode.NONE;
    }

    public int getAlphaChangedPixelCount() {
        return this.alpha_changed_pixel_count;
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
        if (this.layout_codec == null) {
            throw new IllegalArgumentException(context + " is missing its sparse layout codec");
        }
        if (this.residual_codec == null) {
            throw new IllegalArgumentException(context + " is missing its sparse residual codec");
        }
        if (this.alpha_mode == null) {
            throw new IllegalArgumentException(context + " is missing its sparse alpha residual mode");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGB_CHANNELS) && (this.alpha_mode != AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " cannot use alpha residual metadata with RGB-only sparse payloads");
        }
        if ((this.channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) && (this.alpha_mode == AfmaAlphaResidualMode.NONE)) {
            throw new IllegalArgumentException(context + " is missing alpha residual data despite using RGBA sparse channels");
        }
        if ((this.alpha_mode != AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count != 0)) {
            throw new IllegalArgumentException(context + " has an unexpected sparse alpha-change count");
        }
        if ((this.alpha_mode == AfmaAlphaResidualMode.SPARSE) && (this.alpha_changed_pixel_count <= 0)) {
            throw new IllegalArgumentException(context + " has an invalid sparse alpha-change count: " + this.alpha_changed_pixel_count);
        }
    }

    @NotNull
    public AfmaSparsePayload withPixelsPath(@NotNull String pixelsPath) {
        return new AfmaSparsePayload(pixelsPath, this.getChangedPixelCount(), this.getChannels(),
                this.getLayoutCodec(), this.getResidualCodec(), this.getAlphaMode(), this.getAlphaChangedPixelCount());
    }

}
