package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class MaterialIcon {

    private final String name;
    private final int codepoint;
    private volatile int width;
    private volatile int height;
    private volatile boolean loaded;
    private volatile boolean failed;
    private volatile ResourceLocation textureLocation;

    MaterialIcon(@Nonnull String name, int codepoint) {
        this.name = Objects.requireNonNull(name);
        this.codepoint = codepoint;
    }

    @Nonnull
    public String getName() {
        return this.name;
    }

    public int getCodepoint() {
        return this.codepoint;
    }

    public int getWidth() {
        ensureLoaded();
        return this.width;
    }

    public int getHeight() {
        ensureLoaded();
        return this.height;
    }

    @Nullable
    public ResourceLocation getTextureLocation() {
        ensureLoaded();
        return this.textureLocation;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isFailed() {
        return this.failed;
    }

    void assign(@Nonnull ResourceLocation location, int width, int height) {
        this.textureLocation = location;
        this.width = width;
        this.height = height;
        this.loaded = true;
        this.failed = false;
    }

    void markFailed() {
        this.failed = true;
    }

    void reset() {
        this.textureLocation = null;
        this.width = 0;
        this.height = 0;
        this.loaded = false;
        this.failed = false;
    }

    @Nullable
    ResourceLocation getTextureLocationInternal() {
        return this.textureLocation;
    }

    private void ensureLoaded() {
        if (this.loaded || this.failed) {
            return;
        }
        synchronized (this) {
            if (this.loaded || this.failed) {
                return;
            }
            MaterialIcons.loadIcon(this);
        }
    }

}
