package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfmaFrameDescriptor {

    @Nullable
    protected AfmaFrameOperationType type;
    @Nullable
    protected String path;
    @Nullable
    protected Integer x;
    @Nullable
    protected Integer y;
    @Nullable
    protected Integer width;
    @Nullable
    protected Integer height;
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
        return (this.x != null) ? this.x : 0;
    }

    public int getY() {
        return (this.y != null) ? this.y : 0;
    }

    public int getWidth() {
        return (this.width != null) ? this.width : 0;
    }

    public int getHeight() {
        return (this.height != null) ? this.height : 0;
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

    @NotNull
    public AfmaFrameDescriptor withPrimaryPath(@NotNull String newPath) {
        if (this.type == AfmaFrameOperationType.FULL) {
            return full(newPath);
        }
        if (this.type == AfmaFrameOperationType.DELTA_RECT) {
            return deltaRect(newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        throw new IllegalStateException("AFMA frame type does not support a primary payload path override: " + this.type);
    }

    @NotNull
    public AfmaFrameDescriptor withPatchPath(@NotNull String newPath) {
        if (this.type != AfmaFrameOperationType.COPY_RECT_PATCH) {
            throw new IllegalStateException("AFMA frame type does not support a patch payload path override: " + this.type);
        }
        if (this.patch == null) {
            throw new IllegalStateException("AFMA copy_rect_patch frame does not contain a patch section");
        }
        AfmaPatchRegion patchRegion = new AfmaPatchRegion(newPath, this.patch.getX(), this.patch.getY(), this.patch.getWidth(), this.patch.getHeight());
        return copyRectPatch(this.copy, patchRegion);
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
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
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
