package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
    protected AfmaMultiCopy multi_copy;
    @Nullable
    protected AfmaPatchRegion patch;
    @Nullable
    protected AfmaSparsePayload sparse;
    @Nullable
    protected AfmaResidualPayload residual;
    @Nullable
    protected AfmaBlockInter block_inter;

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
    public static AfmaFrameDescriptor residualDeltaRect(@NotNull String path, int x, int y, int width, int height,
                                                        @NotNull AfmaResidualPayload residualPayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.RESIDUAL_DELTA_RECT;
        descriptor.path = path;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.residual = Objects.requireNonNull(residualPayload);
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor sparseDeltaRect(@NotNull String maskPath, int x, int y, int width, int height, @NotNull AfmaSparsePayload sparsePayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.SPARSE_DELTA_RECT;
        descriptor.path = maskPath;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.sparse = Objects.requireNonNull(sparsePayload);
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor copyRectSparsePatch(@NotNull AfmaCopyRect copyRect, @NotNull String maskPath,
                                                          int x, int y, int width, int height, @NotNull AfmaSparsePayload sparsePayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH;
        descriptor.copy = copyRect;
        descriptor.path = maskPath;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.sparse = Objects.requireNonNull(sparsePayload);
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor multiCopyPatch(@NotNull AfmaMultiCopy multiCopy, @Nullable AfmaPatchRegion patch) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.MULTI_COPY_PATCH;
        descriptor.multi_copy = Objects.requireNonNull(multiCopy);
        descriptor.patch = patch;
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor multiCopyResidualPatch(@NotNull AfmaMultiCopy multiCopy, @NotNull String path,
                                                             int x, int y, int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.MULTI_COPY_RESIDUAL_PATCH;
        descriptor.multi_copy = Objects.requireNonNull(multiCopy);
        descriptor.path = path;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.residual = Objects.requireNonNull(residualPayload);
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor multiCopySparsePatch(@NotNull AfmaMultiCopy multiCopy, @NotNull String maskPath,
                                                           int x, int y, int width, int height, @NotNull AfmaSparsePayload sparsePayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH;
        descriptor.multi_copy = Objects.requireNonNull(multiCopy);
        descriptor.path = maskPath;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.sparse = Objects.requireNonNull(sparsePayload);
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

    @NotNull
    public static AfmaFrameDescriptor copyRectResidualPatch(@NotNull AfmaCopyRect copyRect, @NotNull String path,
                                                            int x, int y, int width, int height, @NotNull AfmaResidualPayload residualPayload) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH;
        descriptor.copy = copyRect;
        descriptor.path = path;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.residual = Objects.requireNonNull(residualPayload);
        return descriptor;
    }

    @NotNull
    public static AfmaFrameDescriptor blockInter(@NotNull String path, int x, int y, int width, int height, @NotNull AfmaBlockInter blockInter) {
        AfmaFrameDescriptor descriptor = new AfmaFrameDescriptor();
        descriptor.type = AfmaFrameOperationType.BLOCK_INTER;
        descriptor.path = path;
        descriptor.x = x;
        descriptor.y = y;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.block_inter = Objects.requireNonNull(blockInter);
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
    public AfmaMultiCopy getMultiCopy() {
        return this.multi_copy;
    }

    @Nullable
    public AfmaPatchRegion getPatch() {
        return this.patch;
    }

    @Nullable
    public AfmaSparsePayload getSparse() {
        return this.sparse;
    }

    @Nullable
    public AfmaResidualPayload getResidual() {
        return this.residual;
    }

    @Nullable
    public AfmaBlockInter getBlockInter() {
        return this.block_inter;
    }

    public boolean isKeyframe() {
        return this.type == AfmaFrameOperationType.FULL;
    }

    public boolean requiresPrimaryPayload() {
        return (this.type == AfmaFrameOperationType.FULL)
                || (this.type == AfmaFrameOperationType.DELTA_RECT)
                || (this.type == AfmaFrameOperationType.RESIDUAL_DELTA_RECT)
                || (this.type == AfmaFrameOperationType.SPARSE_DELTA_RECT)
                || (this.type == AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH)
                || (this.type == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH)
                || (this.type == AfmaFrameOperationType.MULTI_COPY_RESIDUAL_PATCH)
                || (this.type == AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH)
                || (this.type == AfmaFrameOperationType.BLOCK_INTER);
    }

    public boolean requiresPatchPayload() {
        if (((this.type == AfmaFrameOperationType.COPY_RECT_PATCH) || (this.type == AfmaFrameOperationType.MULTI_COPY_PATCH))
                && (this.patch != null)) {
            return (this.patch.getPath() != null) && !this.patch.getPath().isBlank();
        }
        if (((this.type == AfmaFrameOperationType.SPARSE_DELTA_RECT)
                || (this.type == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH)
                || (this.type == AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH))
                && (this.sparse != null)) {
            return (this.sparse.getPixelsPath() != null) && !this.sparse.getPixelsPath().isBlank();
        }
        return false;
    }

    @Nullable
    public String getPrimaryPayloadPath() {
        return this.requiresPrimaryPayload() ? this.path : null;
    }

    @Nullable
    public String getSecondaryPayloadPath() {
        if (((this.type == AfmaFrameOperationType.COPY_RECT_PATCH) || (this.type == AfmaFrameOperationType.MULTI_COPY_PATCH))
                && (this.patch != null)) {
            return this.patch.getPath();
        }
        if (((this.type == AfmaFrameOperationType.SPARSE_DELTA_RECT)
                || (this.type == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH)
                || (this.type == AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH))
                && (this.sparse != null)) {
            return this.sparse.getPixelsPath();
        }
        return null;
    }

    @NotNull
    public AfmaFrameDescriptor withPrimaryPath(@NotNull String newPath) {
        if (this.type == AfmaFrameOperationType.FULL) {
            return full(newPath);
        }
        if (this.type == AfmaFrameOperationType.DELTA_RECT) {
            return deltaRect(newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        if (this.type == AfmaFrameOperationType.RESIDUAL_DELTA_RECT) {
            return residualDeltaRect(newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.residual));
        }
        if (this.type == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
            return sparseDeltaRect(newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.sparse));
        }
        if (this.type == AfmaFrameOperationType.COPY_RECT_RESIDUAL_PATCH) {
            return copyRectResidualPatch(Objects.requireNonNull(this.copy), newPath,
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.residual));
        }
        if (this.type == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
            return copyRectSparsePatch(Objects.requireNonNull(this.copy), newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.sparse));
        }
        if (this.type == AfmaFrameOperationType.MULTI_COPY_RESIDUAL_PATCH) {
            return multiCopyResidualPatch(Objects.requireNonNull(this.multi_copy), newPath,
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.residual));
        }
        if (this.type == AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH) {
            return multiCopySparsePatch(Objects.requireNonNull(this.multi_copy), newPath,
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.sparse));
        }
        if (this.type == AfmaFrameOperationType.BLOCK_INTER) {
            return blockInter(newPath, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Objects.requireNonNull(this.block_inter));
        }
        throw new IllegalStateException("AFMA frame type does not support a primary payload path override: " + this.type);
    }

    @NotNull
    public AfmaFrameDescriptor withPatchPath(@NotNull String newPath) {
        if (this.type == AfmaFrameOperationType.COPY_RECT_PATCH) {
            if (this.patch == null) {
                throw new IllegalStateException("AFMA copy_rect_patch frame does not contain a patch section");
            }
            AfmaPatchRegion patchRegion = new AfmaPatchRegion(newPath, this.patch.getX(), this.patch.getY(), this.patch.getWidth(), this.patch.getHeight());
            return copyRectPatch(this.copy, patchRegion);
        }
        if (this.type == AfmaFrameOperationType.SPARSE_DELTA_RECT) {
            if (this.sparse == null) {
                throw new IllegalStateException("AFMA sparse_delta_rect frame does not contain sparse payload metadata");
            }
            return sparseDeltaRect(Objects.requireNonNull(this.path), this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.sparse.withPixelsPath(newPath));
        }
        if (this.type == AfmaFrameOperationType.COPY_RECT_SPARSE_PATCH) {
            if (this.sparse == null) {
                throw new IllegalStateException("AFMA copy_rect_sparse_patch frame does not contain sparse payload metadata");
            }
            return copyRectSparsePatch(Objects.requireNonNull(this.copy), Objects.requireNonNull(this.path),
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.sparse.withPixelsPath(newPath));
        }
        if (this.type == AfmaFrameOperationType.MULTI_COPY_PATCH) {
            if (this.patch == null) {
                throw new IllegalStateException("AFMA multi_copy_patch frame does not contain a patch section");
            }
            AfmaPatchRegion patchRegion = new AfmaPatchRegion(newPath, this.patch.getX(), this.patch.getY(), this.patch.getWidth(), this.patch.getHeight());
            return multiCopyPatch(Objects.requireNonNull(this.multi_copy), patchRegion);
        }
        if (this.type == AfmaFrameOperationType.MULTI_COPY_SPARSE_PATCH) {
            if (this.sparse == null) {
                throw new IllegalStateException("AFMA multi_copy_sparse_patch frame does not contain sparse payload metadata");
            }
            return multiCopySparsePatch(Objects.requireNonNull(this.multi_copy), Objects.requireNonNull(this.path),
                    this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.sparse.withPixelsPath(newPath));
        }
        throw new IllegalStateException("AFMA frame type does not support a secondary payload path override: " + this.type);
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
            case SPARSE_DELTA_RECT -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " sparse delta frame is missing its mask payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.sparse == null) {
                    throw new IllegalArgumentException(context + " sparse delta frame is missing its sparse payload metadata");
                }
                this.sparse.validate(context + " sparse payload");
            }
            case RESIDUAL_DELTA_RECT -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " residual delta frame is missing its payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.residual == null) {
                    throw new IllegalArgumentException(context + " residual delta frame is missing its residual payload metadata");
                }
                this.residual.validate(context + " residual payload");
            }
            case COPY_RECT_SPARSE_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.copy == null) {
                    throw new IllegalArgumentException(context + " copy_rect_sparse_patch frame is missing its copy section");
                }
                this.copy.validate(context, canvasWidth, canvasHeight);
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " copy_rect_sparse_patch frame is missing its mask payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.sparse == null) {
                    throw new IllegalArgumentException(context + " copy_rect_sparse_patch frame is missing its sparse payload metadata");
                }
                this.sparse.validate(context + " sparse payload");
            }
            case MULTI_COPY_SPARSE_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.multi_copy == null) {
                    throw new IllegalArgumentException(context + " multi_copy_sparse_patch frame is missing its multi-copy section");
                }
                this.multi_copy.validate(context, canvasWidth, canvasHeight);
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " multi_copy_sparse_patch frame is missing its mask payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.sparse == null) {
                    throw new IllegalArgumentException(context + " multi_copy_sparse_patch frame is missing its sparse payload metadata");
                }
                this.sparse.validate(context + " sparse payload");
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
            case MULTI_COPY_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.multi_copy == null) {
                    throw new IllegalArgumentException(context + " multi_copy_patch frame is missing its multi-copy section");
                }
                this.multi_copy.validate(context, canvasWidth, canvasHeight);
                if (this.patch != null) {
                    this.patch.validate(context, canvasWidth, canvasHeight, true);
                }
            }
            case COPY_RECT_RESIDUAL_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.copy == null) {
                    throw new IllegalArgumentException(context + " copy_rect_residual_patch frame is missing its copy section");
                }
                this.copy.validate(context, canvasWidth, canvasHeight);
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " copy_rect_residual_patch frame is missing its residual payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.residual == null) {
                    throw new IllegalArgumentException(context + " copy_rect_residual_patch frame is missing its residual payload metadata");
                }
                this.residual.validate(context + " residual payload");
            }
            case MULTI_COPY_RESIDUAL_PATCH -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if (this.multi_copy == null) {
                    throw new IllegalArgumentException(context + " multi_copy_residual_patch frame is missing its multi-copy section");
                }
                this.multi_copy.validate(context, canvasWidth, canvasHeight);
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " multi_copy_residual_patch frame is missing its residual payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.residual == null) {
                    throw new IllegalArgumentException(context + " multi_copy_residual_patch frame is missing its residual payload metadata");
                }
                this.residual.validate(context + " residual payload");
            }
            case BLOCK_INTER -> {
                if (requireFullFrame) {
                    throw new IllegalArgumentException(context + " must be a full frame");
                }
                if ((this.path == null) || this.path.isBlank()) {
                    throw new IllegalArgumentException(context + " block_inter frame is missing its payload path");
                }
                new AfmaPatchRegion(this.path, this.getX(), this.getY(), this.getWidth(), this.getHeight()).validate(context, canvasWidth, canvasHeight, true);
                if (this.block_inter == null) {
                    throw new IllegalArgumentException(context + " block_inter frame is missing its block metadata");
                }
                this.block_inter.validate(context + " block metadata", this.getWidth(), this.getHeight());
            }
        }
    }

}
