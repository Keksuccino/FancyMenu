package de.keksuccino.fancymenu.util.rendering.ui.icon;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.IntSupplier;

public final class MaterialIconTexture implements ITexture {

    public static final int DEFAULT_TEXTURE_SIZE = 96;

    @Nonnull
    private final MaterialIcon icon;
    @Nonnull
    private final IntSupplier textureSizeSupplier;
    private boolean closed = false;

    public MaterialIconTexture(@Nonnull MaterialIcon icon, @Nonnull IntSupplier textureSizeSupplier) {
        this.icon = Objects.requireNonNull(icon, "icon");
        this.textureSizeSupplier = Objects.requireNonNull(textureSizeSupplier, "textureSizeSupplier");
    }

    public static int getDefaultTextureSize() {
        return DEFAULT_TEXTURE_SIZE;
    }

    @Nonnull
    public MaterialIcon getIcon() {
        return this.icon;
    }

    private int getTextureSizePx() {
        return Math.max(1, this.textureSizeSupplier.getAsInt());
    }

    public int getTextureSizeForUI(float renderWidth, float renderHeight) {
        return this.icon.getTextureSizeForUI(renderWidth, renderHeight);
    }

    public int getWidthForUI(float renderWidth, float renderHeight) {
        int size = getTextureSizeForUI(renderWidth, renderHeight);
        return this.icon.getWidth(size);
    }

    public int getHeightForUI(float renderWidth, float renderHeight) {
        int size = getTextureSizeForUI(renderWidth, renderHeight);
        return this.icon.getHeight(size);
    }

    @Nullable
    public ResourceLocation getResourceLocationForUI(float renderWidth, float renderHeight) {
        return this.icon.getTextureLocationForUI(renderWidth, renderHeight);
    }

    @Override
    public @Nullable ResourceLocation getResourceLocation() {
        if (this.closed) {
            return null;
        }
        return this.icon.getTextureLocation(getTextureSizePx());
    }

    @Override
    public int getWidth() {
        if (this.closed) {
            return 1;
        }
        return this.icon.getWidth(getTextureSizePx());
    }

    @Override
    public int getHeight() {
        if (this.closed) {
            return 1;
        }
        return this.icon.getHeight(getTextureSizePx());
    }

    @Override
    public @Nonnull AspectRatio getAspectRatio() {
        return new AspectRatio(getWidth(), getHeight());
    }

    @Override
    public @Nullable InputStream open() {
        return null;
    }

    @Override
    public boolean isReady() {
        if (this.closed) {
            return false;
        }
        int size = getTextureSizePx();
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    @Override
    public boolean isLoadingCompleted() {
        if (this.closed) {
            return false;
        }
        int size = getTextureSizePx();
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    @Override
    public boolean isLoadingFailed() {
        if (this.closed) {
            return false;
        }
        return this.icon.isFailed(getTextureSizePx());
    }

    @Override
    public void reset() {
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
