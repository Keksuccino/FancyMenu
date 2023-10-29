package de.keksuccino.fancymenu.util.resources;

import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface Resource extends Closeable {

    /**
     * Tries to open an {@link InputStream} of the {@link Resource}.<br>
     * Some types of {@link Resource}s don't support that, in which case this method returns NULL.
     */
    @Nullable
    InputStream open() throws IOException;

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

    boolean isClosed();

}
