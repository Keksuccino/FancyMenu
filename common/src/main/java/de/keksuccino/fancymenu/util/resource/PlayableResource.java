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

    /**
     * How many times the resource finished playing its full sequence since it got last stopped or reset.<br>
     * Loop restarts count as finished sequences, so this keeps increasing while a looping resource plays.<br>
     * Resources that don't track their play cycles always return 0.
     */
    default int getCompletedPlayCycles() {
        return 0;
    }

}
