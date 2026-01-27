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

    @Nonnull
    private final MaterialIcon icon;
    @Nonnull
    private final IntSupplier textureSizeSupplier;
    private boolean closed = false;

    public MaterialIconTexture(@Nonnull MaterialIcon icon, @Nonnull IntSupplier textureSizeSupplier) {
        this.icon = Objects.requireNonNull(icon, "icon");
        this.textureSizeSupplier = Objects.requireNonNull(textureSizeSupplier, "textureSizeSupplier");
    }

    private int getTextureSizePx() {
        return Math.max(1, this.textureSizeSupplier.getAsInt());
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
