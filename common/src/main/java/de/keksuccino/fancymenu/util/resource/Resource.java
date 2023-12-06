package de.keksuccino.fancymenu.util.resource;

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
     * Should only return TRUE if all asynchronous loading tasks completed successfully.<br>
     * {@link Resource#isReady()} should always return TRUE as well if this method returns TRUE.
     */
    boolean isLoadingCompleted();

    /**
     * Should only return TRUE if loading failed in some way, making it impossible for the {@link Resource} to correctly finish loading.<br>
     * It is possible that {@link Resource#isReady()} and this method both return TRUE.
     */
    boolean isLoadingFailed();

    /**
     * Waits for {@link Resource#isReady()} to return TRUE.
     */
    @SuppressWarnings("all")
    default void waitForReady(long timeoutMs) {
        long start = System.currentTimeMillis();
        while(!this.isReady() && ((start + timeoutMs) > System.currentTimeMillis()));
    }

    /**
     * Waits for {@link Resource#isLoadingCompleted()} or {@link Resource#isLoadingFailed()} to return TRUE.
     */
    @SuppressWarnings("all")
    default void waitForLoadingCompletedOrFailed(long timeoutMs) {
        long start = System.currentTimeMillis();
        while(!this.isLoadingCompleted() && !this.isLoadingFailed() && ((start + timeoutMs) > System.currentTimeMillis()));
    }

    boolean isClosed();

}
