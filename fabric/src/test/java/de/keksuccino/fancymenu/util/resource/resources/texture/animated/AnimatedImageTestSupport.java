package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Composes the frames of parsed animated images with the real compositor, holds images in memory and runs decoding tasks on a fake clock, for the tests of all animated image formats.
 */
public final class AnimatedImageTestSupport {

    private AnimatedImageTestSupport() {
    }

    /**
     * Composes every frame of the first play and returns the canvas after each frame.
     */
    @NotNull
    public static int[][] composeAll(@NotNull AnimatedImage image) throws IOException {
        AnimatedFrameCompositor compositor = new AnimatedFrameCompositor(image);
        int[][] canvases = new int[image.getFrameCount()][];
        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            for (int i = 0; i < image.getFrameCount(); i++) {
                AnimatedFrameUpdate update = new AnimatedFrameUpdate(i, 0, 0L);
                compositor.composeFrame(i, decoder, update);
                AnimatedImageFrame frame = image.getFrame(i);
                // Frames with visible pixels always change the canvas region they cover
                if ((frame.width() > 0) && (frame.height() > 0)) assertTrue(update.getPixelCount() > 0, "frame " + i);
                canvases[i] = readCanvas(compositor, image.getWidth(), image.getHeight());
            }
        }
        return canvases;
    }

    /**
     * Composes only the given frame on a transparent canvas, without the frames before it, and returns the canvas.
     */
    @NotNull
    public static int[] composeSingleFrame(@NotNull AnimatedImage image, int frameIndex) throws IOException {
        AnimatedFrameCompositor compositor = new AnimatedFrameCompositor(image);
        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            compositor.composeFrame(frameIndex, decoder, new AnimatedFrameUpdate(frameIndex, 0, 0L));
        }
        return readCanvas(compositor, image.getWidth(), image.getHeight());
    }

    @NotNull
    static int[] readCanvas(@NotNull AnimatedFrameCompositor compositor, int width, int height) {
        AnimatedFrameUpdate fullCanvas = new AnimatedFrameUpdate(0, 0, 0L);
        fullCanvas.setFullCanvas(width, height);
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        compositor.writePixels(fullCanvas, buffer);
        int[] pixels = new int[width * height];
        buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);
        return pixels;
    }

    /**
     * Packs 8-bit channels into the ABGR pixel layout of canvases.
     */
    public static int abgr(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    /**
     * An image file in memory, which tests can change or shorten while it plays.
     */
    public static final class MemorySource implements AnimatedImageSource {

        @NotNull
        public byte[] data;
        /** Position and length of every read that returned bytes. **/
        public final List<long[]> reads = new ArrayList<>();
        public int closeCount;

        public MemorySource(@NotNull byte[] data) {
            this.data = data;
        }

        @Override
        public int read(@NotNull ByteBuffer target, long position) throws IOException {
            if (this.closeCount > 0) throw new IOException("The source is closed");
            if (position >= this.data.length) return -1;
            int count = (int) Math.min(target.remaining(), this.data.length - position);
            target.put(this.data, (int) position, count);
            this.reads.add(new long[] {position, count});
            return count;
        }

        @Override
        public long size() {
            return this.data.length;
        }

        @Override
        public void close() {
            this.closeCount++;
        }

        public boolean isClosed() {
            return this.closeCount > 0;
        }

    }

    /**
     * An image file in memory whose next read can wait at a gate, like a read of a slow disk, so another thread can close the source while it gets read.
     * Reads and closing can happen on different threads. Reads of a closed source fail.
     */
    public static final class GatedSource implements AnimatedImageSource {

        @NotNull
        private final byte[] data;
        @Nullable
        private volatile ReadGate readGate;
        private volatile boolean closed;

        public GatedSource(@NotNull byte[] data) {
            this.data = data;
        }

        /**
         * Makes the next read wait at the gate.
         */
        public void holdNextRead(@NotNull ReadGate gate) {
            this.readGate = gate;
        }

        @Override
        public int read(@NotNull ByteBuffer target, long position) throws IOException {
            ReadGate gate = this.readGate;
            if (gate != null) {
                this.readGate = null;
                gate.pass();
            }
            if (this.closed) throw new ClosedChannelException();
            if (position >= this.data.length) return -1;
            int count = (int) Math.min(target.remaining(), this.data.length - position);
            target.put(this.data, (int) position, count);
            return count;
        }

        @Override
        public long size() throws IOException {
            if (this.closed) throw new ClosedChannelException();
            return this.data.length;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return this.closed;
        }

    }

    /**
     * Runs tasks only when the test says so, and delayed tasks once the fake clock reached their time.
     */
    public static final class ManualExecutor extends AbstractExecutorService implements ScheduledExecutorService {

        private final LongSupplier clock;
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private final List<DelayedTask> delayedTasks = new ArrayList<>();

        public ManualExecutor(@NotNull LongSupplier clock) {
            this.clock = clock;
        }

        /**
         * Runs all queued tasks, the tasks they queue and the delayed tasks whose time came, until nothing is left to run.
         */
        public void runTasks() {
            boolean ranSomething = true;
            while (ranSomething) {
                ranSomething = false;
                Runnable task;
                while ((task = this.tasks.pollFirst()) != null) {
                    task.run();
                    ranSomething = true;
                }
                for (Iterator<DelayedTask> iterator = this.delayedTasks.iterator(); iterator.hasNext(); ) {
                    DelayedTask delayed = iterator.next();
                    if (delayed.time <= this.clock.getAsLong()) {
                        iterator.remove();
                        this.tasks.addLast(delayed.task);
                        ranSomething = true;
                        break;
                    }
                }
            }
        }

        @Override
        public void execute(Runnable command) {
            this.tasks.addLast(command);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            DelayedTask task = new DelayedTask(command, this.clock.getAsLong() + unit.toNanos(delay));
            this.delayedTasks.add(task);
            return task;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        private record DelayedTask(Runnable task, long time) implements ScheduledFuture<Object> {

            @Override
            public long getDelay(TimeUnit unit) {
                return 0L;
            }

            @Override
            public int compareTo(Delayed other) {
                return 0;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() {
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) {
                return null;
            }

        }

    }

}
