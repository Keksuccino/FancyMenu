package de.keksuccino.fancymenu.util.watermedia;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
        // WaterMedia owns this GL texture ID.
    }

    @Override
    public void reset(@NotNull TextureManager textureManager, @NotNull ResourceManager resourceManager, @NotNull ResourceLocation location, @NotNull Executor executor) {
        // do nothing
    }

}
