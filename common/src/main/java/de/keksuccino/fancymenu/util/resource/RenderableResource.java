package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RenderableResource extends Resource {

    public static final ResourceLocation MISSING_TEXTURE_LOCATION = TextureManager.INTENTIONAL_MISSING_TEXTURE;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = new ResourceLocation("fancymenu", "textures/fully_transparent.png");

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
