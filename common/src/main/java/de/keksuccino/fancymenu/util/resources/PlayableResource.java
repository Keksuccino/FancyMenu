package de.keksuccino.fancymenu.util.resources;

public interface PlayableResource extends Resource {

    /**
     * Starts playing the resource.
     */
    void play();

    /**
     * Pauses the resource without resetting its current play progress.
     */
    void pause();

    /**
     * Completely stops the resource and resets its play progress.
     */
    void stop();

    /**
     * If the resource is currently playing.
     */
    boolean isPlaying();

}
