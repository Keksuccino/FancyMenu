package de.keksuccino.fancymenu.util.resources.audio;

import de.keksuccino.fancymenu.util.resources.PlayableResource;

public interface PlayableResourceWithAudio extends PlayableResource {

    void setMuted(boolean muted);

    boolean isMuted();

    void setVolume(float volume);

    float getVolume();

}
