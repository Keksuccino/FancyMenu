package de.keksuccino.fancymenu.util.resource.resources.texture;

import de.keksuccino.fancymenu.mixin.mixins.common.client.AccessorMixinTextureManager;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Releases texture manager entries the way {@code TextureManager#release} does in newer Minecraft versions such as 1.20.1: it removes the entry and closes its texture once.<br>
 * FancyMenu releases every texture it registers through this class, because the {@link TextureManager#release(ResourceLocation)} of 1.19.2 only deletes the GL texture of the entry.
 * It keeps the entry and never closes the texture, so a {@link net.minecraft.client.renderer.texture.DynamicTexture} would keep the native memory of its pixels forever.
 * The texture also keeps the deleted GL texture id, which closing it later deletes a second time, even after OpenGL gave that id to another texture.<br>
 * Vanilla's release stays as it is, because other mods rely on how it works in 1.19.2.
 */
public final class TextureManagerReleaser {

    private static final Logger LOGGER = LogManager.getLogger();

    private TextureManagerReleaser() {}

    /**
     * Removes the texture manager entry of the location and closes its texture on the client thread, which is also the render thread in 1.19.2:
     * right away when called on it, otherwise before its next tick, through {@link TextureManagerReleaseDispatcher}.<br>
     * A location that gets released off the client thread must not get registered again before that, because the release removes whatever is registered under it then.
     */
    public static void release(@NotNull ResourceLocation location) {
        Objects.requireNonNull(location);
        TextureManagerReleaseDispatcher.dispatch(() -> {
            AccessorMixinTextureManager textureManager = (AccessorMixinTextureManager) Minecraft.getInstance().getTextureManager();
            release(textureManager.get_byPath_FancyMenu(), textureManager.get_tickableTextures_FancyMenu(), location, MissingTextureAtlasSprite.getTexture());
        });
    }

    /**
     * Does what the release of 1.20.1 and its {@code safeClose} do with the maps of the texture manager, except for the shared missing texture.
     * Its entry only gets removed, because vanilla would also delete its GL texture, which every other location that failed to load still shows.
     */
    static void release(@NotNull Map<ResourceLocation, AbstractTexture> byPath, @NotNull Set<Tickable> tickableTextures, @NotNull ResourceLocation location, @NotNull AbstractTexture missingTexture) {
        AbstractTexture texture = byPath.remove(location);
        if ((texture == null) || (texture == missingTexture)) return;
        tickableTextures.remove(texture);
        try {
            texture.close();
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to close texture: {}", location, ex);
        }
        // Frees the GL texture of textures that don't free it when they get closed. Textures that already freed it in close() ignore this.
        texture.releaseId();
    }

}
