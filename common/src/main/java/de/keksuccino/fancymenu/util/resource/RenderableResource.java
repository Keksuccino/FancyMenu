package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RenderableResource extends Resource {

    // Use Minecraft's generated checkerboard. The empty-path sentinel can resolve to a pack namespace
    // directory on some loaders and turn a missing asset into a failure during the next resource reload.
    public static final ResourceLocation MISSING_TEXTURE_LOCATION = MissingTextureAtlasSprite.getLocation();
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/fully_transparent.png");

    /**
     * Some resource types asynchronously update their current {@link ResourceLocation},
     * so make sure to always cache the location returned by this method before using it.
     */
    @Nullable ResourceLocation getResourceLocation();

    int getWidth();

    int getHeight();

    @NotNull AspectRatio getAspectRatio();

    void reset();

}
