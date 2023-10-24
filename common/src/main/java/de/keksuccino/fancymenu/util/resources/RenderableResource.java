package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RenderableResource extends Resource {

    public static final ResourceLocation MISSING_TEXTURE_LOCATION = new ResourceLocation("missing_texture_location_fancymenu");
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
