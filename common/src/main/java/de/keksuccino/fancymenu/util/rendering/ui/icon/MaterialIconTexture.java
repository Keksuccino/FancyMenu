package de.keksuccino.fancymenu.util.rendering.ui.icon;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.Objects;

public class MaterialIconTexture implements ITexture {

    @NotNull
    protected final MaterialIcon icon;
    protected float renderWidth;
    protected float renderHeight;
    protected float renderScale;
    protected boolean closed = false;

    public MaterialIconTexture(@NotNull MaterialIcon icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
    }

    /**
     * It's important to call this before getting anything from the texture (width, height, resource location, etc.)!
     */
    public MaterialIconTexture updateRenderContext(float renderWidth, float renderHeight, float renderScale) {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.renderScale = renderScale;
        return this;
    }

    @NotNull
    public MaterialIcon getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        int size = calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.getWidth(size);
    }

    @Override
    public int getHeight() {
        int size = calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.getHeight(size);
    }

    public int calculateBestTextureSize(float renderWidth, float renderHeight, float renderScale) {
        return this.icon.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
    }

    @Override
    public @Nullable ResourceLocation getResourceLocation() {
        if (this.closed) {
            return null;
        }
        return this.icon.getTextureLocation(this.renderWidth, this.renderHeight, this.renderScale);
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
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
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    @Override
    public boolean isLoadingCompleted() {
        if (this.closed) {
            return false;
        }
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    @Override
    public boolean isLoadingFailed() {
        if (this.closed) {
            return false;
        }
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isFailed(size);
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
