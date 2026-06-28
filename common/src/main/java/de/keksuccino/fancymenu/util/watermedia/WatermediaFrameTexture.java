package de.keksuccino.fancymenu.util.watermedia;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.concurrent.Executor;

public class WatermediaFrameTexture extends AbstractTexture {

    public WatermediaFrameTexture(int id) {
        this.id = id;
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) throws IOException {
        // do nothing
    }

    @Override
    public void setFilter(boolean blur, boolean mipmap) {
        // do nothing
    }

    @Override
    public void bind() {
        // do nothing
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void releaseId() {
        // do nothing
    }

    @Override
    public void reset(@NotNull TextureManager textureManager, @NotNull ResourceManager resourceManager, @NotNull ResourceLocation location, @NotNull Executor executor) {
        // do nothing
    }

}
