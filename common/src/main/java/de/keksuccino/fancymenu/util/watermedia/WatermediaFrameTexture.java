package de.keksuccino.fancymenu.util.watermedia;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.concurrent.Executor;

public class WatermediaFrameTexture extends AbstractTexture {

    public WatermediaFrameTexture(long textureHandle) {
        this.setHandle(textureHandle);
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

    public void setHandle(long textureHandle) {
        // WaterMedia uses a backend-neutral long handle. Minecraft 1.19.2 only supports OpenGL texture names, so the
        // narrowing conversion stays at this renderer-specific boundary and rejects opaque/non-GL handles safely.
        this.id = WatermediaReflectionBridge.openGlTextureId(textureHandle);
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
