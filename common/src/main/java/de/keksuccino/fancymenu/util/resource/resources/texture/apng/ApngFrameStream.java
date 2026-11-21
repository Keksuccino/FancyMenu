package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Decodes and composes the frames of one APNG image in the background, a few frames ahead of playback, and hands them out as {@link ApngFrameUpdate}s
 * that only hold the pixels that changed since the frame before.<br>
 * Frames get decoded again for every play instead of being cached, reading their compressed data from the source of the image, so an animation only needs one canvas
 * and a few small updates in memory, no matter how many frames it has. Only the first frame gets stored, so restarting the animation can show it instantly and every later play starts without decoding it.<br>
 * The stream owns the image and with it its source, which gets closed when the stream gets closed or fails.<br><br>
 *
 * <b>Threading:</b> {@link #poll()}, {@link #recycle(ApngFrameUpdate)}, {@link #restart()} and {@link #close()} belong to one consumer thread (the render thread),
 * because the consumer reads the pixel buffers of the updates it got without holding the lock. Decoding runs on a shared executor, with at most one task per stream
 * at a time, so the decoder and the compositor never get used concurrently. Every task decodes a single frame and then gets queued again, so streams sharing the executor take turns.<br><br>
 *
 * <b>Memory:</b> Pixel buffers are native memory that gets freed explicitly. Queued and pooled buffers belong to the stream, the buffer of a polled update belongs
 * to the consumer until it recycles the update, and the buffer being filled belongs to the decoding task. When the consumer stops polling for a while,
 * queued updates and pooled buffers get freed. Once it polls again, decoding continues with an update of the whole canvas, which skips the few frames that got dropped.
 * Streams that never had a frame polled start again at their first frame instead.
 */
final class ApngFrameStream implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Shortest time a frame stays visible. Shorter delays (including 0, which means "as fast as possible") would mostly waste uploads of frames nobody sees. **/
    static final long MIN_FRAME_DELAY_NANOS = 10_000_000L;
    /** The stream keeps at least this many frames ready, so short hiccups of the decoding thread don't stall playback. **/
    static final int MIN_QUEUED_UPDATES = 2;
    static final int MAX_QUEUED_UPDATES = 6;
    /** Ready frames should cover this much playback time, so fast animations still play at full speed when the game renders slower than the animation. **/
    static final long QUEUED_PLAYBACK_NANOS = 80_000_000L;
    /** Ready frames of big canvases get limited to about this many bytes, as if every update covered the whole canvas, but never to fewer than {@link #MIN_QUEUED_UPDATES}. **/
    static final long QUEUED_BYTES_LIMIT = 16L * 1024L * 1024L;
    /** Streams that don't get polled for this long free their ready frames and pooled buffers. **/
    static final long INACTIVITY_TIMEOUT_NANOS = 10_000_000_000L;

    @NotNull
    private final ApngImage image;
    @NotNull
    private final String sourceName;
    @NotNull
    private final ScheduledExecutorService executor;
    @NotNull
    private final LongSupplier nanoClock;
    private final int targetQueuedUpdates;
    private final int canvasBytes;

    private final Object lock = new Object();
    // Guarded by the lock
    @NotNull
    private final ArrayDeque<ApngFrameUpdate> readyUpdates = new ArrayDeque<>();
    @NotNull
    private final ArrayDeque<ByteBuffer> freeBuffers = new ArrayDeque<>();
    /** Increases whenever ready updates get dropped, so updates of a task that started before can't get queued anymore. **/
    private long generation;
    private boolean taskScheduled;
    private boolean trimCheckScheduled;
    private boolean restartPending;
    /** If the consumer got the stored first frame from {@link #restart()}, so decoding continues after it instead of delivering it again. **/
    private boolean restartAfterFirstFrame;
    /** If the consumer got a frame since the stream started or restarted. **/
    private boolean frameDelivered;
    private boolean fullCanvasPending;
    private boolean productionFinished;
    private boolean failed;
    private boolean closed;
    /** The whole canvas right after the first frame. Shared by all updates of the first frame, never pooled. **/
    @Nullable
    private ByteBuffer firstFrame;
    private volatile long lastActiveNanos;
    /** Set when decoding stopped because nobody polled for a while, so the next sign of activity starts it again. **/
    private volatile boolean waitingForActivity;

    // Only used by the decoding task
    @Nullable
    private ApngFrameDecoder decoder;
    @Nullable
    private ApngCompositor compositor;
    private int nextFrameIndex;
    private int nextPlay;
    private int playableFrameCount;

    /**
     * Takes ownership of the image.
     */
    ApngFrameStream(@NotNull ApngImage image, @NotNull String sourceName) {
        this(image, sourceName, SharedExecutor.INSTANCE, System::nanoTime);
    }

    ApngFrameStream(@NotNull ApngImage image, @NotNull String sourceName, @NotNull ScheduledExecutorService executor, @NotNull LongSupplier nanoClock) {
        this.image = image;
        this.sourceName = sourceName;
        this.executor = executor;
        this.nanoClock = nanoClock;
        this.canvasBytes = image.getWidth() * image.getHeight() * 4;
        this.playableFrameCount = image.getFrameCount();
        this.targetQueuedUpdates = resolveTargetQueuedUpdates(image, this.canvasBytes);
        this.lastActiveNanos = nanoClock.getAsLong();
    }

    int getWidth() {
        return this.image.getWidth();
    }

    int getHeight() {
        return this.image.getHeight();
    }

    /**
     * Starts decoding the first frames.
     */
    void start() {
        synchronized (this.lock) {
            this.scheduleTaskLocked();
        }
    }

    /**
     * Returns the next frame update, or NULL if no frame is ready yet or all frames got played. Also tells the stream that its frames are still wanted.
     */
    @Nullable
    ApngFrameUpdate poll() {
        this.lastActiveNanos = this.nanoClock.getAsLong();
        synchronized (this.lock) {
            ApngFrameUpdate update = this.readyUpdates.pollFirst();
            if (update != null) this.frameDelivered = true;
            this.waitingForActivity = false;
            this.scheduleTaskLocked();
            return update;
        }
    }

    /**
     * Tells the stream that its frames are still wanted, without taking one. Keeps it from freeing its ready frames while playback waits, like while paused.
     */
    void markActive() {
        this.lastActiveNanos = this.nanoClock.getAsLong();
        if (this.waitingForActivity) {
            synchronized (this.lock) {
                this.waitingForActivity = false;
                this.scheduleTaskLocked();
            }
        }
    }

    /**
     * Gives the pixel buffer of a polled update back, after its pixels got uploaded.
     */
    void recycle(@NotNull ApngFrameUpdate update) {
        ByteBuffer buffer = update.takeOwnedPixels();
        if (buffer == null) return;
        synchronized (this.lock) {
            if (!this.closed && !this.productionFinished && this.canPoolBufferLocked()) {
                this.freeBuffers.addLast(buffer);
                return;
            }
        }
        MemoryUtil.memFree(buffer);
    }

    /**
     * Restarts the animation at its first frame. Drops all ready frames.
     *
     * @return The stored first frame, so it can get shown right away, or NULL if it didn't get decoded yet. Then the next polled update is the first frame.
     */
    @Nullable
    ApngFrameUpdate restart() {
        synchronized (this.lock) {
            if (this.closed || this.failed) return null;
            this.generation++;
            this.dropReadyUpdatesLocked();
            ByteBuffer first = this.firstFrame;
            this.restartPending = true;
            // Decided here and not by the task, because a task that is decoding the first frame right now could store it before the next task starts
            this.restartAfterFirstFrame = (first != null);
            this.frameDelivered = (first != null);
            this.fullCanvasPending = false;
            this.productionFinished = false;
            this.lastActiveNanos = this.nanoClock.getAsLong();
            this.scheduleTaskLocked();
            if (first == null) return null;
            ApngFrameUpdate update = new ApngFrameUpdate(0, 0, displayDelayNanos(this.image.getFrame(0)));
            update.setFullCanvas(this.image.getWidth(), this.image.getHeight());
            update.setPixels(first, true);
            return update;
        }
    }

    /**
     * If all frames of all plays got polled, so the animation ended.
     */
    boolean isFinished() {
        synchronized (this.lock) {
            return this.productionFinished && this.readyUpdates.isEmpty() && !this.restartPending;
        }
    }

    /**
     * If the stream can't decode any more frames, because its first frame could not get decoded or its source changed after it got parsed.
     * Frames the consumer polled before stay valid.
     */
    boolean hasFailed() {
        synchronized (this.lock) {
            return this.failed;
        }
    }

    /**
     * How many bytes of native memory the stream holds right now, for tests and debugging.
     */
    long getNativeMemoryBytes() {
        synchronized (this.lock) {
            long bytes = (this.firstFrame != null) ? this.firstFrame.capacity() : 0L;
            for (ApngFrameUpdate update : this.readyUpdates) bytes += ownedBytes(update);
            for (ByteBuffer buffer : this.freeBuffers) bytes += buffer.capacity();
            return bytes;
        }
    }

    @Override
    public void close() {
        ByteBuffer first;
        synchronized (this.lock) {
            if (this.closed) return;
            this.closed = true;
            this.generation++;
            this.dropReadyUpdatesLocked();
            this.freePooledBuffersLocked();
            // A running task may still read the first frame or use the decoder, so it frees them itself when it notices the stream got closed
            if (this.taskScheduled) return;
            first = this.firstFrame;
            this.firstFrame = null;
        }
        if (first != null) MemoryUtil.memFree(first);
        this.releaseDecodingState();
        this.image.close();
    }

    private void runTask() {
        long taskGeneration;
        boolean restart;
        boolean restartAfterFirst;
        boolean fullCanvas;
        ByteBuffer first;
        synchronized (this.lock) {
            if (this.closed) {
                this.finishClosedTaskLocked();
                return;
            }
            if (!this.wantsMoreUpdatesLocked()) {
                this.stopTaskLocked();
                return;
            }
            taskGeneration = this.generation;
            restart = this.restartPending;
            restartAfterFirst = this.restartAfterFirstFrame;
            fullCanvas = this.fullCanvasPending;
            this.restartPending = false;
            this.fullCanvasPending = false;
            first = this.firstFrame;
        }
        ApngFrameUpdate update = null;
        boolean fatal = false;
        try {
            if (restart) this.applyRestart(restartAfterFirst ? first : null);
            if (!this.isProductionFinished()) update = this.produceNextUpdate(fullCanvas, first);
        } catch (ApngSourceChangedException ex) {
            fatal = true;
            LOGGER.warn("[FANCYMENU] Stopped playing an APNG image, because its file changed after it got loaded or can't get read anymore ({}): {}", ex.getMessage(), this.sourceName);
        } catch (IOException ex) {
            fatal = true;
            LOGGER.error("[FANCYMENU] Failed to decode the first frame of an APNG image: {}", this.sourceName, ex);
        } catch (Throwable throwable) {
            fatal = true;
            LOGGER.error("[FANCYMENU] Failed to decode an APNG image: {}", this.sourceName, throwable);
        }
        boolean closedNow;
        synchronized (this.lock) {
            if (fatal) this.failed = true;
            if (update != null) {
                if (!this.closed && (taskGeneration == this.generation)) {
                    this.readyUpdates.addLast(update);
                } else {
                    this.discardLocked(update);
                }
            }
            closedNow = this.closed;
            if (closedNow) {
                this.finishClosedTaskLocked();
            } else if (this.wantsMoreUpdatesLocked()) {
                this.executor.execute(this::runTask);
            } else {
                this.stopTaskLocked();
            }
        }
        if (fatal) {
            // A failed stream never decodes again, so its source gets closed right away, which also releases its file
            this.releaseDecodingState();
            this.image.close();
        }
    }

    /**
     * Decodes and composes the next frame and returns its update, or NULL if the frame turned out broken and got cut from the animation.
     */
    @Nullable
    private ApngFrameUpdate produceNextUpdate(boolean fullCanvas, @Nullable ByteBuffer first) throws IOException {
        int index = this.nextFrameIndex;
        int play = this.nextPlay;
        ApngFrame frame = this.image.getFrame(index);
        ApngFrameUpdate update = new ApngFrameUpdate(index, play, displayDelayNanos(frame));
        if ((index == 0) && (first != null)) {
            // Every play starts with the same canvas, so later plays reuse the stored first frame instead of decoding it again
            if (this.playableFrameCount > 1) this.ensureCompositor().restoreFirstFrame(first);
            update.setFullCanvas(this.image.getWidth(), this.image.getHeight());
            update.setPixels(first, true);
            update.setContentUnchanged((this.playableFrameCount == 1) && !fullCanvas);
            this.advance(index, play);
            return update;
        }
        ApngCompositor activeCompositor = this.ensureCompositor();
        try {
            activeCompositor.composeFrame(index, this.ensureDecoder(), update);
        } catch (ApngSourceChangedException ex) {
            // No problem of this frame, so cutting the animation before it would not help
            throw ex;
        } catch (IOException ex) {
            if (index == 0) throw ex;
            // Like browsers, keep playing all frames before the broken one
            LOGGER.warn("[FANCYMENU] Frame {} of an APNG image is corrupted, so the animation only plays the frames before it: {}", index, this.sourceName, ex);
            this.playableFrameCount = index;
            this.nextFrameIndex = 0;
            this.nextPlay = play + 1;
            this.updateProductionFinished();
            return null;
        }
        if (fullCanvas) update.setFullCanvas(this.image.getWidth(), this.image.getHeight());
        if (index == 0) {
            ByteBuffer snapshot = MemoryUtil.memAlloc(this.canvasBytes);
            activeCompositor.writePixels(update, snapshot);
            if (!this.storeFirstFrame(snapshot)) return null;
            update.setPixels(snapshot, true);
            // The stored first frame is all a single-frame animation ever shows, so the canvas is not needed anymore
            if (this.playableFrameCount == 1) this.releaseDecodingState();
        } else {
            ByteBuffer buffer = this.takeBuffer((int) (update.getPixelCount() * 4L));
            activeCompositor.writePixels(update, buffer);
            update.setPixels(buffer, false);
        }
        this.advance(index, play);
        return update;
    }

    /**
     * @param shownFirstFrame The stored first frame if the consumer already shows it, so decoding continues with the frame after it.
     */
    private void applyRestart(@Nullable ByteBuffer shownFirstFrame) {
        this.nextFrameIndex = 0;
        this.nextPlay = 0;
        if (shownFirstFrame != null) {
            if (this.playableFrameCount > 1) this.ensureCompositor().restoreFirstFrame(shownFirstFrame);
            this.advance(0, 0);
        } else {
            this.updateProductionFinished();
        }
    }

    private void advance(int index, int play) {
        int next = index + 1;
        if (next >= this.playableFrameCount) {
            this.nextFrameIndex = 0;
            this.nextPlay = play + 1;
        } else {
            this.nextFrameIndex = next;
            this.nextPlay = play;
        }
        this.updateProductionFinished();
    }

    private void updateProductionFinished() {
        int numPlays = this.image.isAnimated() ? this.image.getNumPlays() : 1;
        boolean finished = (numPlays != ApngImage.INFINITE_PLAYS) && (this.nextPlay >= numPlays);
        synchronized (this.lock) {
            this.productionFinished = finished;
        }
        // Nothing gets composed anymore until a restart, which starts from the stored first frame or a fresh canvas
        if (finished) this.releaseDecodingState();
    }

    private boolean isProductionFinished() {
        synchronized (this.lock) {
            return this.productionFinished;
        }
    }

    private boolean storeFirstFrame(@NotNull ByteBuffer snapshot) {
        synchronized (this.lock) {
            if (!this.closed && (this.firstFrame == null)) {
                this.firstFrame = snapshot;
                return true;
            }
        }
        MemoryUtil.memFree(snapshot);
        return false;
    }

    @NotNull
    private ByteBuffer takeBuffer(int bytes) {
        ByteBuffer buffer = null;
        synchronized (this.lock) {
            // Prefer a pooled buffer that is big enough, otherwise grow the biggest one
            ByteBuffer biggest = null;
            for (Iterator<ByteBuffer> iterator = this.freeBuffers.iterator(); iterator.hasNext(); ) {
                ByteBuffer candidate = iterator.next();
                if (candidate.capacity() >= bytes) {
                    iterator.remove();
                    buffer = candidate;
                    break;
                }
                if ((biggest == null) || (candidate.capacity() > biggest.capacity())) biggest = candidate;
            }
            if ((buffer == null) && (biggest != null)) {
                this.freeBuffers.remove(biggest);
                buffer = biggest;
            }
        }
        if (buffer == null) return MemoryUtil.memAlloc(bytes);
        if (buffer.capacity() < bytes) return MemoryUtil.memRealloc(buffer, bytes);
        return buffer;
    }

    @NotNull
    private ApngCompositor ensureCompositor() {
        ApngCompositor active = this.compositor;
        if (active == null) {
            active = new ApngCompositor(this.image);
            this.compositor = active;
        }
        return active;
    }

    @NotNull
    private ApngFrameDecoder ensureDecoder() {
        ApngFrameDecoder active = this.decoder;
        if (active == null) {
            active = new ApngFrameDecoder(this.image);
            this.decoder = active;
        }
        return active;
    }

    private void releaseDecodingState() {
        ApngFrameDecoder activeDecoder = this.decoder;
        this.decoder = null;
        if (activeDecoder != null) activeDecoder.close();
        ApngCompositor activeCompositor = this.compositor;
        this.compositor = null;
        if (activeCompositor != null) activeCompositor.release();
    }

    private void stopTaskLocked() {
        this.taskScheduled = false;
        if (this.isInactiveLocked()) this.waitingForActivity = true;
        this.scheduleTrimCheckLocked();
    }

    private boolean isInactiveLocked() {
        return (this.nanoClock.getAsLong() - this.lastActiveNanos) >= INACTIVITY_TIMEOUT_NANOS;
    }

    private boolean wantsMoreUpdatesLocked() {
        if (this.closed || this.failed || (this.productionFinished && !this.restartPending)) return false;
        // Nobody watches the animation, so decoding waits until it gets wanted again
        if (this.isInactiveLocked()) return false;
        return this.readyUpdates.size() < this.targetQueuedUpdates;
    }

    /**
     * Ready and pooled buffers together never exceed the number of ready frames, so pooling never holds more memory than a full queue.
     */
    private boolean canPoolBufferLocked() {
        return (this.freeBuffers.size() + this.readyUpdates.size()) < this.targetQueuedUpdates;
    }

    private void scheduleTaskLocked() {
        if (this.taskScheduled || !this.wantsMoreUpdatesLocked()) return;
        this.taskScheduled = true;
        this.executor.execute(this::runTask);
    }

    private void scheduleTrimCheckLocked() {
        if (this.trimCheckScheduled || this.closed) return;
        if (this.readyUpdates.isEmpty() && this.freeBuffers.isEmpty()) return;
        this.trimCheckScheduled = true;
        long delay = Math.max(0L, (this.lastActiveNanos + INACTIVITY_TIMEOUT_NANOS) - this.nanoClock.getAsLong());
        this.executor.schedule(this::runTrimCheck, delay, TimeUnit.NANOSECONDS);
    }

    void runTrimCheck() {
        synchronized (this.lock) {
            this.trimCheckScheduled = false;
            if (this.closed) return;
            if (!this.isInactiveLocked()) {
                this.scheduleTrimCheckLocked();
                return;
            }
            // A running task would compete with dropping its updates, so the check runs again once it stopped
            if (this.taskScheduled) return;
            this.waitingForActivity = true;
            // The last frames of an animation without frames left to decode can't get decoded again, so they stay
            if (!this.productionFinished && !this.readyUpdates.isEmpty()) {
                this.generation++;
                this.dropReadyUpdatesLocked();
                if (this.frameDelivered) {
                    // The texture still shows an older frame than the canvas, so the next update has to cover all of it
                    this.fullCanvasPending = true;
                } else {
                    // Nothing got shown yet, so the animation starts at its first frame like it would have without the pause
                    this.restartPending = true;
                    this.restartAfterFirstFrame = false;
                }
            }
            this.freePooledBuffersLocked();
        }
    }

    private void finishClosedTaskLocked() {
        this.taskScheduled = false;
        ByteBuffer first = this.firstFrame;
        this.firstFrame = null;
        if (first != null) MemoryUtil.memFree(first);
        // The decoding state and the source are only used by tasks, and no other task can run anymore
        this.releaseDecodingState();
        this.image.close();
    }

    private void dropReadyUpdatesLocked() {
        ApngFrameUpdate update;
        while ((update = this.readyUpdates.pollFirst()) != null) {
            this.discardLocked(update);
        }
    }

    private void discardLocked(@NotNull ApngFrameUpdate update) {
        ByteBuffer buffer = update.takeOwnedPixels();
        if (buffer == null) return;
        if (!this.closed && this.canPoolBufferLocked()) {
            this.freeBuffers.addLast(buffer);
        } else {
            MemoryUtil.memFree(buffer);
        }
    }

    private void freePooledBuffersLocked() {
        ByteBuffer buffer;
        while ((buffer = this.freeBuffers.pollFirst()) != null) {
            MemoryUtil.memFree(buffer);
        }
    }

    private static long ownedBytes(@NotNull ApngFrameUpdate update) {
        ByteBuffer pixels = update.getPixels();
        return ((pixels != null) && !update.hasSharedPixels()) ? pixels.capacity() : 0L;
    }

    static long displayDelayNanos(@NotNull ApngFrame frame) {
        return Math.max(MIN_FRAME_DELAY_NANOS, frame.delayNanos());
    }

    private static int resolveTargetQueuedUpdates(@NotNull ApngImage image, int canvasBytes) {
        long totalDelay = 0L;
        for (int i = 0; i < image.getFrameCount(); i++) {
            totalDelay += displayDelayNanos(image.getFrame(i));
        }
        long averageDelay = Math.max(MIN_FRAME_DELAY_NANOS, totalDelay / image.getFrameCount());
        long wanted = (QUEUED_PLAYBACK_NANOS + averageDelay - 1L) / averageDelay;
        long affordable = QUEUED_BYTES_LIMIT / canvasBytes;
        return (int) Math.max(MIN_QUEUED_UPDATES, Math.min(Math.min(MAX_QUEUED_UPDATES, wanted), affordable));
    }

    /**
     * The decoding threads shared by all APNG streams. They have a slightly lower priority than normal threads, so decoding never competes with the render thread.
     */
    private static final class SharedExecutor {

        private static final ScheduledExecutorService INSTANCE = create();

        @NotNull
        private static ScheduledExecutorService create() {
            int threads = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 4));
            AtomicInteger counter = new AtomicInteger();
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threads, runnable -> {
                Thread thread = new Thread(runnable, "FancyMenu-ApngDecoder-" + counter.incrementAndGet());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                thread.setUncaughtExceptionHandler((failedThread, throwable) -> LOGGER.error("[FANCYMENU] Uncaught exception in APNG decoder thread '{}'.", failedThread.getName(), throwable));
                return thread;
            });
            executor.setRemoveOnCancelPolicy(true);
            return executor;
        }

    }

}
