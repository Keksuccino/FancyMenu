package de.keksuccino.fancymenu.util.resource;

public interface PlayableResource extends Resource {

    /**
     * Starts playing the resource.
     */
    void play();

    /**
     * If the resource is currently playing.
     */
    boolean isPlaying();

    /**
     * Pauses the resource without resetting its current play progress.
     */
    void pause();

    /**
     * If the resource is currently paused.
     */
    boolean isPaused();

    /**
     * Completely stops the resource and resets its play progress.
     */
    void stop();

}
