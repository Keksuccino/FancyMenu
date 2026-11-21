package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Separates override-texture state that affects baked UV data from its render location. Texture resources can change
 * their location while they stay the same resource, like animated textures, which have none while their image loads and
 * get a new one when it gets loaded again, but that only requires rebinding the render texture, not rebuilding the model.
 */
final class ModelOverrideTextureChangeDetector {

    private ModelOverrideTextureChangeDetector() {
    }

    static boolean requiresModelRebuild(@Nullable Object resource, @Nullable Object previousResource, int width, int previousWidth, int height, int previousHeight, boolean ready, boolean previouslyReady, boolean readFailed, boolean previouslyReadFailed) {
        return resource != previousResource || width != previousWidth || height != previousHeight || ready != previouslyReady || readFailed != previouslyReadFailed;
    }

    static boolean hasRenderLocationChanged(@Nullable Object renderLocation, @Nullable Object previousRenderLocation) {
        return !Objects.equals(renderLocation, previousRenderLocation);
    }

}
