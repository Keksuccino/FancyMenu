package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RenderableResource extends Resource {

    public static final Identifier MISSING_TEXTURE_LOCATION = TextureManager.INTENTIONAL_MISSING_TEXTURE;
    public static final Identifier FULLY_TRANSPARENT_TEXTURE = Identifier.fromNamespaceAndPath("fancymenu", "textures/fully_transparent.png");

    /**
     * Some resource types asynchronously update their current {@link Identifier},
     * so make sure to always cache the location returned by this method before using it.
     */
    @Nullable Identifier getResourceLocation();

    int getWidth();

    int getHeight();

    @NotNull AspectRatio getAspectRatio();

    void reset();

}
