package de.keksuccino.fancymenu.resources.texture;

import de.keksuccino.fancymenu.rendering.AspectRatio;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ITexture {

    public static final ResourceLocation MISSING_TEXTURE_LOCATION = new ResourceLocation("missing_texture_location_fancymenu");

    @Nullable ResourceLocation getResourceLocation();

    int getWidth();

    int getHeight();

    @NotNull AspectRatio getAspectRatio();

    boolean isReady();

}
