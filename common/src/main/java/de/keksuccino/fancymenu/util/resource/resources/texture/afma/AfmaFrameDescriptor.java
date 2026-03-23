package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaFrameDescriptor {

    @Nullable
    protected AfmaFrameOperationType type;
    @Nullable
    protected String path;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    @Nullable
    protected AfmaCopyRect copy;
    @Nullable
    protected AfmaPatchRegion patch;

    public AfmaFrameDescriptor() {
    }

    @NotNull
    public static AfmaFrameDescriptor full(@NotNull String path) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.FULL;
        descriptor.path = path;
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor deltaRect(@NotNull String path, int x, int y, int width, int height) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.DELTA_RECT;
        descriptor.path = path;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor same() {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.SAME;
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor copyRectPatch(@NotNull AfmaCopyRect copyRect, @Nullable AfmaPatchRegion patch) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.COPY_RECT_PATCH;
        descriptor.copy = copyRect;
        descriptor.patch = patch;
        return descriptor;
    }

    @Nullable
    public AfmaFrameOperationType getType() {
        return this.type;
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

    @Nullable
    public AfmaCopyRect getCopy() {
        return this.copy;
    }

    @Nullable
    public AfmaPatchRegion getPatch() {
        return this.patch;
    }

    public boolean isKeyframe() {
        return this.type == AfmaFrameOperationType.FULL;
    }

    public boolean requiresPrimaryPayload() {
        return (this.type == AfmaFrameOperationType.FULL) || (this.type == AfmaFrameOperationType.DELTA_RECT);
    }

    public boolean requiresPatchPayload() {
        return (this.type == AfmaFrameOperationType.COPY_RECT_PATCH) && (this.patch != null) && (this.patch.getPath() != null) && !this.patch.getPath().isBlank();
    }

    public void validate(@NotNull String context, int canvasWidth, int canvasHeight, boolean requireFullFrame) {
        if (this.type == null) {
            throw new IllegalArgumentException(context + " is missing its operation type");
        }

        switch (this.type) {
            case FULL -> {
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " full frame is missing its payload path");
                }
            }
            case DELTA_RECT -> {
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " delta frame is missing its payload path");
                }
                new AfmaPatchRegion(this.path, this.x, this.y, this.width, this.height).validate(context, canvasWidth, canvasHeight, true);
            }
            case SAME -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
            }
            case COPY_RECT_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.copy == null) {
                    throw new IllegalArgumentException(context + " copy_rect_patch frame is missing its copy section");
                }
                this.copy.validate(context, canvasWidth, canvasHeight);
                if (this.patch != null) {
                    this.patch.validate(context, canvasWidth, canvasHeight, true);
                }
            }
        }
    }

}
