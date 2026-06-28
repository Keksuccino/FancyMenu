package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.io.IOException;
import java.util.concurrent.Executor;

public final class ExternalTextureUtils {

    private static final AbstractTexture DETACHED_TEXTURE_FANCYMENU = new DetachedTexture();

    private ExternalTextureUtils() {
    }

    public static void unregisterWithoutDeleting(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture) {
        unregisterWithoutDeleting(textureManager, location, texture, 0, false);
    }

    public static void unregisterWithoutDeletingIfCurrentId(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture, int expectedTextureId) {
        unregisterWithoutDeleting(textureManager, location, texture, expectedTextureId, true);
    }

    public static int resolveRenderableTextureId(@NotNull TextureManager textureManager, @NotNull ResourceLocation location) {
        int textureId = textureManager.getTexture(location).getId();
        if (isLiveTextureId(textureId)) {
            return textureId;
        }
        return resolveTransparentTextureId(textureManager);
    }

    public static int sanitizeTextureId(int textureId) {
        return isLiveTextureId(textureId) ? textureId : 0;
    }

    public static int sanitizeBufferId(int bufferId) {
        return isLiveBufferId(bufferId) ? bufferId : 0;
    }

    public static boolean isLiveTextureId(int textureId) {
        return (textureId > 0) && GL11.glIsTexture(textureId);
    }

    public static boolean isLiveBufferId(int bufferId) {
        return (bufferId > 0) && GL15.glIsBuffer(bufferId);
    }

    public static int resolveTransparentTextureId(@NotNull TextureManager textureManager) {
        return Math.max(textureManager.getTexture(RenderableResource.FULLY_TRANSPARENT_TEXTURE).getId(), 0);
    }

    private static void unregisterWithoutDeleting(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture, int expectedTextureId, boolean checkTextureId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> unregisterWithoutDeleting(textureManager, location, texture, expectedTextureId, checkTextureId));
            return;
        }
        unregisterWithoutDeletingNow(textureManager, location, texture, expectedTextureId, checkTextureId);
    }

    private static void unregisterWithoutDeletingNow(@NotNull TextureManager textureManager, @NotNull ResourceLocation location, @NotNull AbstractTexture texture, int expectedTextureId, boolean checkTextureId) {
        if (checkTextureId && (texture.getId() != expectedTextureId)) {
            return;
        }
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
            if (!RenderSystem.isOnRenderThreadOrInit()) {
                RenderSystem.recordRenderCall(() -> this.bindTransparentTexture(Minecraft.getInstance().getTextureManager()));
                return;
            }
            this.bindTransparentTexture(Minecraft.getInstance().getTextureManager());
        }

        @Override
        public int getId() {
            return resolveTransparentTextureId(Minecraft.getInstance().getTextureManager());
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

        private void bindTransparentTexture(@NotNull TextureManager textureManager) {
            int textureId = resolveTransparentTextureId(textureManager);
            if (textureId > 0) {
                GlStateManager._bindTexture(textureId);
            }
        }
    }

}
