package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Executor;

public final class ExternalTextureUtils {

    private static final AbstractTexture DETACHED_TEXTURE_FANCYMENU = new DetachedTexture();

    private ExternalTextureUtils() {
    }

    public static void unregisterWithoutDeleting(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> unregisterWithoutDeleting(textureManager, location, texture));
            return;
        }
        unregisterWithoutDeletingNow(textureManager, location, texture);
    }

    private static void unregisterWithoutDeletingNow(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture) {
        if (textureManager.getTexture(location, DETACHED_TEXTURE_FANCYMENU) == texture) {
            textureManager.register(location, DETACHED_TEXTURE_FANCYMENU);
        }
    }

    private static final class DetachedTexture extends AbstractTexture {

        @Override
        public void load(@NotNull ResourceManager resourceManager) throws IOException {
            // no texture data
        }

        @Override
        public void bind() {
            // intentionally unbound
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public void releaseId() {
            // no GL texture owned here
        }

        @Override
        public void close() {
            // no GL texture owned here
        }

        @Override
        public void reset(@NotNull TextureManager textureManager, @NotNull ResourceManager resourceManager, @NotNull ResourceLocation location, @NotNull Executor executor) {
            textureManager.register(location, this);
        }
    }

}
