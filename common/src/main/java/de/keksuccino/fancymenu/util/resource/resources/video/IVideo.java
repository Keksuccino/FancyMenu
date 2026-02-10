package de.keksuccino.fancymenu.util.resource.resources.video;

import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.PlayableResourceWithAudio;

public interface IVideo extends RenderableResource, PlayableResourceWithAudio {

    /**
     * Returns the duration in seconds.
     */
    default float getDuration() {
        return 0.0F;
    }

    /**
     * Returns the current play time in seconds.
     */
    default float getPlayTime() {
        return 0.0F;
    }

    default void setLooping(boolean looping) {
    }

    default boolean isLooping() {
        return false;
    }

}
