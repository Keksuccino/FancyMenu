package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public final class AfmaFastPixelBufferPool {

    protected static final int DEFAULT_MAX_CACHED_BUFFERS_FANCYMENU = 4;

    private final int maxCachedBuffers;
    @NotNull
    private final ArrayDeque<int[]> cachedPixelBuffers = new ArrayDeque<>();

    public AfmaFastPixelBufferPool() {
        this(DEFAULT_MAX_CACHED_BUFFERS_FANCYMENU);
    }

    public AfmaFastPixelBufferPool(int maxCachedBuffers) {
        if (maxCachedBuffers <= 0) {
            throw new IllegalArgumentException("AFMA pixel buffer pool size must be greater than zero");
        }
        this.maxCachedBuffers = maxCachedBuffers;
    }

    @NotNull
    public synchronized int[] acquirePixels(int minimumLength) {
        if (minimumLength <= 0) {
            throw new IllegalArgumentException("AFMA pixel buffer length must be greater than zero");
        }

        int[] bestBuffer = null;
        for (int[] candidateBuffer : this.cachedPixelBuffers) {
            if ((candidateBuffer.length < minimumLength)
                    || ((bestBuffer != null) && (candidateBuffer.length >= bestBuffer.length))) {
                continue;
            }
            bestBuffer = candidateBuffer;
        }

        if (bestBuffer != null) {
            this.cachedPixelBuffers.remove(bestBuffer);
            return bestBuffer;
        }
        return new int[minimumLength];
    }

    public synchronized void releasePixels(@NotNull int[] pixels) {
        Objects.requireNonNull(pixels);
        if (pixels.length == 0) {
            return;
        }

        if (this.cachedPixelBuffers.size() >= this.maxCachedBuffers) {
            Iterator<int[]> iterator = this.cachedPixelBuffers.descendingIterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        this.cachedPixelBuffers.addFirst(pixels);
    }

    public synchronized void clear() {
        this.cachedPixelBuffers.clear();
    }

    public synchronized int cachedBufferCount() {
        return this.cachedPixelBuffers.size();
    }

}
