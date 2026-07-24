package de.keksuccino.fancymenu.util.resource.resources.texture;

import de.keksuccino.fancymenu.util.CloseableUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinates decoder delivery, completion, ticker snapshots, and teardown for primitive animated-image frames. A
 * generation is captured before decoding so a callback that arrives after fallback/reload teardown cannot publish old
 * state into the replacement sequence. Frames are closed after detaching them, keeping cleanup outside the state lock.
 */
final class AnimatedTextureFrameStore<F extends AutoCloseable> implements AutoCloseable {

    private final Object lifecycleLock = new Object();
    private List<F> frames = new ArrayList<>();
    @Nullable
    private F current;
    private long generation;
    private boolean complete;
    private boolean closed;

    long generation() {
        synchronized (this.lifecycleLock) {
            return this.generation;
        }
    }

    boolean add(long expectedGeneration, @NotNull F frame) {
        synchronized (this.lifecycleLock) {
            if (!this.closed && (this.generation == expectedGeneration)) {
                this.frames.add(frame);
                return true;
            }
        }
        CloseableUtils.closeQuietly(frame);
        return false;
    }

    @NotNull
    Snapshot<F> snapshot() {
        synchronized (this.lifecycleLock) {
            return new Snapshot<>(new ArrayList<>(this.frames), this.current, this.generation, this.complete);
        }
    }

    boolean markComplete(long expectedGeneration) {
        synchronized (this.lifecycleLock) {
            if (this.closed || (this.generation != expectedGeneration)) return false;
            this.complete = true;
            return true;
        }
    }

    boolean isComplete() {
        synchronized (this.lifecycleLock) {
            return this.complete;
        }
    }

    @Nullable
    F current() {
        synchronized (this.lifecycleLock) {
            return this.current;
        }
    }

    boolean setCurrent(long expectedGeneration, @Nullable F frame) {
        synchronized (this.lifecycleLock) {
            if (this.closed || (this.generation != expectedGeneration)) return false;
            this.current = frame;
            return true;
        }
    }

    void resetToFirst() {
        synchronized (this.lifecycleLock) {
            this.current = this.frames.isEmpty() ? null : this.frames.get(0);
        }
    }

    boolean isEmpty() {
        synchronized (this.lifecycleLock) {
            return this.frames.isEmpty();
        }
    }

    boolean isGenerationActive(long expectedGeneration) {
        synchronized (this.lifecycleLock) {
            return !this.closed && (this.generation == expectedGeneration);
        }
    }

    boolean runIfGenerationActive(long expectedGeneration, @NotNull Runnable task) {
        synchronized (this.lifecycleLock) {
            if (this.closed || (this.generation != expectedGeneration)) return false;
            task.run();
            return true;
        }
    }

    void clear() {
        this.detachAndClose(false);
    }

    @Override
    public void close() {
        this.detachAndClose(true);
    }

    private void detachAndClose(boolean closeStore) {
        List<F> detachedFrames;
        synchronized (this.lifecycleLock) {
            if (closeStore && this.closed) return;
            if (closeStore) this.closed = true;
            this.generation++;
            detachedFrames = this.frames;
            this.frames = new ArrayList<>();
            this.current = null;
            this.complete = false;
        }
        detachedFrames.forEach(CloseableUtils::closeQuietly);
    }

    record Snapshot<F>(@NotNull List<F> frames, @Nullable F current, long generation, boolean complete) {
    }

}
