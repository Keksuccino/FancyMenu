package de.keksuccino.fancymenu.util.resources.audio;

import java.io.Closeable;

public interface AudioClip extends Closeable {

    void play();

    void pause();

    void resume();

    void stop();

    boolean isPlaying();

    void setVolume(float volume);

    float getVolume();

    boolean isClosed();

}
