package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RenderableResource {

    public static final ResourceLocation MISSING_TEXTURE_LOCATION = new ResourceLocation("missing_texture_location_fancymenu");
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = new ResourceLocation("fancymenu", "textures/fully_transparent.png");

    @Nullable ResourceLocation getResourceLocation();

    int getWidth();

    int getHeight();

    @NotNull AspectRatio getAspectRatio();

    boolean isReady();

    void reset();

}
