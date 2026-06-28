package de.keksuccino.fancymenu.util.mcef;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
        int textureId = this.id;
        if (textureId <= 0) {
            return;
        }
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> GlStateManager._bindTexture(textureId));
        } else {
            GlStateManager._bindTexture(textureId);
        }
    }

    @Override
    public int getId() {
        return Math.max(this.id, 0);
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void releaseId() {
        // do nothing
    }

    @Override
    public void close() {
        // The browser owns this GL texture ID.
    }

    @Override
    public void reset(@NotNull TextureManager $$0, @NotNull ResourceManager $$1, @NotNull ResourceLocation $$2, @NotNull Executor $$3) {
        // do nothing
    }

}
