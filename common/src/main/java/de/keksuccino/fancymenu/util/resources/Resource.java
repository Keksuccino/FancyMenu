package de.keksuccino.fancymenu.util.resources;

import java.io.Closeable;

public interface Resource extends Closeable {

    /**
     * The resource is considered ready once all important variables are set to real non-placeholder values.
     */
    boolean isReady();

    /**
     * Waits for {@link Resource#isReady()} to return TRUE.
     */
    @SuppressWarnings("all")
    default void waitForReady(long timeoutMs) {
        long start = System.currentTimeMillis();
        while(!this.isReady() && ((start + timeoutMs) > System.currentTimeMillis()));
    }

    void reload();

    boolean isClosed();

}
