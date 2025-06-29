package de.keksuccino.fancymenu.util.mcef;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.concurrent.Executor;

public class BrowserFrameTexture extends AbstractTexture {

    public BrowserFrameTexture(int id) {
        this.id = id;
    }

    @Override
    public void load(@NotNull ResourceManager var1) throws IOException {
        // do nothing
    }

    @Override
    public void setFilter(boolean $$0, boolean $$1) {
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
    public void reset(@NotNull TextureManager $$0, @NotNull ResourceManager $$1, @NotNull ResourceLocation $$2, @NotNull Executor $$3) {
        // do nothing
    }

}
