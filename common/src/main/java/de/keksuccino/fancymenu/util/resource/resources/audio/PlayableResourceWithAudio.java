package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.fancymenu.util.resource.PlayableResource;

public interface PlayableResourceWithAudio extends PlayableResource {

    void setVolume(float volume);

    float getVolume();

}
