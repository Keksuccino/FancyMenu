package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngFrameStreamTest {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 12;

    private long now = 1_000_000_000L;
    private final ManualExecutor executor = new ManualExecutor(() -> this.now);
    private final List<ApngFrameStream> streams = new ArrayList<>();
    private final List<ApngTestFiles.MemorySource> sources = new ArrayList<>();

    @AfterEach
    void closeStreams() {
        this.streams.forEach(ApngFrameStream::close);
        this.executor.runTasks();
        for (ApngFrameStream stream : this.streams) assertEquals(0L, stream.getNativeMemoryBytes());
        for (ApngTestFiles.MemorySource source : this.sources) assertTrue(source.isClosed());
    }

    @Test
    void updatesRebuildEveryComposedFrameOnTheTexture() throws IOException {
        byte[] file = ApngCompositorTest.randomAnimation(new Random(7), WIDTH, HEIGHT, 10);
        int[][] expected = ApngCompositorTest.composeAll(file);
        ApngFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];

        for (int play = 0; play < 3; play++) {
            for (int frame = 0; frame < expected.length; frame++) {
                ApngFrameUpdate update = this.next(stream);
                assertEquals(frame, update.getFrameIndex());
                assertEquals(play, update.getPlay());
                apply(update, texture);
                assertArrayEquals(expected[frame], texture, "play " + play + ", frame " + frame);
                stream.recycle(update);
            }
        }
    }

    @Test
    void keepsOnlyAFewFramesReady() throws IOException {
        ApngFrameStream stream = this.stream(ApngCompositorTest.randomAnimation(new Random(8), WIDTH, HEIGHT, 30));
        this.executor.runTasks();

        int ready = 0;
        ApngFrameUpdate update;
        while ((update = stream.poll()) != null) {
            ready++;
            stream.recycle(update);
        }

        assertTrue((ready >= ApngFrameStream.MIN_QUEUED_UPDATES) && (ready <= ApngFrameStream.MAX_QUEUED_UPDATES), "ready frames: " + ready);
    }

    @Test
    void finitePlaysEndAfterTheirLastFrame() throws IOException {
        ApngFrameStream stream = this.stream(threeFrames(2));

        List<String> shown = new ArrayList<>();
        while (true) {
            this.executor.runTasks();
            ApngFrameUpdate update = stream.poll();
            if (update == null) break;
            shown.add(update.getFrameIndex() + "/" + update.getPlay());
            stream.recycle(update);
        }

        assertEquals(List.of("0/0", "1/0", "2/0", "0/1", "1/1", "2/1"), shown);
        assertTrue(stream.isFinished());
    }

    @Test
    void laterPlaysReuseTheStoredFirstFrame() throws IOException {
        ApngFrameStream stream = this.stream(threeFrames(0));
        ApngFrameUpdate firstPlay = this.next(stream);
        stream.recycle(this.next(stream));
        stream.recycle(this.next(stream));
        ApngFrameUpdate secondPlay = this.next(stream);

        assertEquals(1, secondPlay.getPlay());
        assertSame(firstPlay.getPixels(), secondPlay.getPixels());
        assertTrue(secondPlay.hasSharedPixels());
    }

    @Test
    void restartShowsTheStoredFirstFrameAndContinuesWithTheSecondOne() throws IOException {
        byte[] file = ApngCompositorTest.randomAnimation(new Random(9), WIDTH, HEIGHT, 6);
        int[][] expected = ApngCompositorTest.composeAll(file);
        ApngFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];
        for (int i = 0; i < 4; i++) {
            ApngFrameUpdate update = this.next(stream);
            apply(update, texture);
            stream.recycle(update);
        }

        ApngFrameUpdate first = stream.restart();
        assertNotNull(first);
        apply(first, texture);
        stream.recycle(first);
        assertArrayEquals(expected[0], texture);

        for (int frame = 1; frame < expected.length; frame++) {
            ApngFrameUpdate update = this.next(stream);
            assertEquals(frame, update.getFrameIndex());
            assertEquals(0, update.getPlay());
            apply(update, texture);
            assertArrayEquals(expected[frame], texture, "frame " + frame);
            stream.recycle(update);
        }
    }

    @Test
    void restartBeforeTheFirstFrameExistsStartsFromTheBeginning() throws IOException {
        ApngFrameStream stream = this.stream(threeFrames(0));

        assertNull(stream.restart());
        ApngFrameUpdate update = this.next(stream);

        assertEquals(0, update.getFrameIndex());
        assertEquals(0, update.getPlay());
    }

    @Test
    void restartingAFinishedAnimationPlaysItAgain() throws IOException {
        ApngFrameStream stream = this.stream(threeFrames(1));
        for (int i = 0; i < 3; i++) stream.recycle(this.next(stream));
        this.executor.runTasks();
        assertNull(stream.poll());
        assertTrue(stream.isFinished());

        ApngFrameUpdate first = stream.restart();

        assertNotNull(first);
        assertFalse(stream.isFinished());
        ApngFrameUpdate second = this.next(stream);
        assertEquals(1, second.getFrameIndex());
        stream.recycle(second);
    }

    @Test
    void idleStreamFreesItsReadyFramesAndResumesWithTheWholeCanvas() throws IOException {
        byte[] file = ApngCompositorTest.randomAnimation(new Random(10), WIDTH, HEIGHT, 12);
        int[][] expected = ApngCompositorTest.composeAll(file);
        ApngFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];
        for (int i = 0; i < 3; i++) {
            ApngFrameUpdate update = this.next(stream);
            apply(update, texture);
            stream.recycle(update);
        }
        this.executor.runTasks();
        long firstFrameBytes = WIDTH * HEIGHT * 4L;
        assertTrue(stream.getNativeMemoryBytes() > firstFrameBytes);

        this.now += ApngFrameStream.INACTIVITY_TIMEOUT_NANOS + 1L;
        this.executor.runTasks();

        assertEquals(firstFrameBytes, stream.getNativeMemoryBytes());
        // Rendering the texture again starts decoding right away
        stream.markActive();
        ApngFrameUpdate resumed = this.next(stream);
        assertEquals(1, resumed.getRectangleCount());
        assertEquals((long) WIDTH * HEIGHT, resumed.getPixelCount());
        apply(resumed, texture);
        assertArrayEquals(expected[resumed.getFrameIndex()], texture);
        stream.recycle(resumed);
        // Playback continues normally after the frames that got dropped
        ApngFrameUpdate following = this.next(stream);
        apply(following, texture);
        assertArrayEquals(expected[following.getFrameIndex()], texture);
        stream.recycle(following);
    }

    @Test
    void idleStreamThatShowedNothingStartsAtItsFirstFrame() throws IOException {
        byte[] file = ApngCompositorTest.randomAnimation(new Random(15), WIDTH, HEIGHT, 12);
        int[][] expected = ApngCompositorTest.composeAll(file);
        ApngFrameStream stream = this.stream(file);
        this.executor.runTasks();

        this.now += ApngFrameStream.INACTIVITY_TIMEOUT_NANOS + 1L;
        this.executor.runTasks();
        stream.markActive();

        int[] texture = new int[WIDTH * HEIGHT];
        for (int frame = 0; frame < 4; frame++) {
            ApngFrameUpdate update = this.next(stream);
            assertEquals(frame, update.getFrameIndex());
            assertEquals(0, update.getPlay());
            apply(update, texture);
            assertArrayEquals(expected[frame], texture, "frame " + frame);
            stream.recycle(update);
        }
    }

    @Test
    void restartedStreamIsNotFinishedBeforeItDecodedAgain() throws IOException {
        ApngFrameStream stream = this.stream(threeFrames(1));
        for (int i = 0; i < 3; i++) stream.recycle(this.next(stream));
        this.executor.runTasks();
        assertTrue(stream.isFinished());

        stream.restart();

        assertFalse(stream.isFinished());
        this.executor.runTasks();
        assertFalse(stream.isFinished());
    }

    @Test
    void streamThatStaysActiveKeepsItsReadyFrames() throws IOException {
        ApngFrameStream stream = this.stream(ApngCompositorTest.randomAnimation(new Random(12), WIDTH, HEIGHT, 12));
        this.executor.runTasks();
        long bytes = stream.getNativeMemoryBytes();

        for (int i = 0; i < 5; i++) {
            this.now += ApngFrameStream.INACTIVITY_TIMEOUT_NANOS / 2L;
            stream.markActive();
            this.executor.runTasks();
        }

        assertEquals(bytes, stream.getNativeMemoryBytes());
    }

    @Test
    void corruptedFrameCutsTheAnimationBeforeIt() throws IOException {
        ApngTestFiles.Png png = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(4, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(ApngTestFiles.solid(4, 4, 255, 0, 0, 255))
                .frame(2, 2, 0, 0, 1, 10, 0, 0, ApngTestFiles.solid(2, 2, 0, 255, 0, 255))
                .frameControl(2, 2, 2, 2, 1, 10, 0, 0);
        png.chunk("fdAT", new byte[] {0, 0, 0, 4, 0x78, (byte) 0x9C, 1, 2, 3});
        png.frame(1, 1, 0, 0, 1, 10, 0, 0, ApngTestFiles.solid(1, 1, 0, 0, 255, 255));
        ApngFrameStream stream = this.stream(png.end());

        List<String> shown = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ApngFrameUpdate update = this.next(stream);
            shown.add(update.getFrameIndex() + "/" + update.getPlay());
            stream.recycle(update);
        }

        assertEquals(List.of("0/0", "1/0", "0/1", "1/1", "0/2"), shown);
        assertFalse(stream.hasFailed());
    }

    @Test
    void corruptedFirstFrameFailsTheStream() throws IOException {
        byte[] file = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().chunk("IDAT", new byte[] {0x78, (byte) 0x9C, 1, 2, 3}).end();
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(file);
        ApngFrameStream stream = this.stream(source);

        this.executor.runTasks();

        assertTrue(stream.hasFailed());
        assertNull(stream.poll());
        assertEquals(0L, stream.getNativeMemoryBytes());
        // A failed stream never decodes again, so it releases its source right away
        assertTrue(source.isClosed());
    }

    @Test
    void changedSourceFailsTheStreamInsteadOfCuttingTheAnimation() throws IOException {
        byte[] file = fourFrames();
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(file);
        ApngFrameStream stream = this.stream(source);
        this.executor.runTasks();
        // The stream decoded the first two frames ahead, the third one changes before it gets decoded
        source.data = changedFrameData(file, 2);

        List<String> shown = new ArrayList<>();
        // Bounded, because an animation that got cut instead would loop forever
        for (int i = 0; i < 20; i++) {
            this.executor.runTasks();
            ApngFrameUpdate update = stream.poll();
            if (update == null) break;
            shown.add(update.getFrameIndex() + "/" + update.getPlay());
            stream.recycle(update);
        }

        // A corrupted frame would make the animation continue with the next play instead
        assertEquals(List.of("0/0", "1/0"), shown);
        assertTrue(stream.hasFailed());
        assertFalse(stream.isFinished());
        assertNull(stream.restart());
        assertTrue(source.isClosed());
    }

    @Test
    void changedSourceFailsTheStreamInLaterPlaysToo() throws IOException {
        byte[] file = fourFrames();
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(file);
        ApngFrameStream stream = this.stream(source);
        for (int i = 0; i < 4; i++) stream.recycle(this.next(stream));
        this.executor.runTasks();

        source.data = changedFrameData(file, 3);
        int shown = 0;
        for (int i = 0; i < 20; i++) {
            this.executor.runTasks();
            ApngFrameUpdate update = stream.poll();
            if (update == null) break;
            assertEquals(1, update.getPlay());
            shown++;
            stream.recycle(update);
        }

        assertEquals(3, shown);
        assertTrue(stream.hasFailed());
        assertTrue(source.isClosed());
    }

    @Test
    void staticImageFinishesAfterItsOnlyFrame() throws IOException {
        ApngFrameStream stream = this.stream(ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end());

        ApngFrameUpdate update = this.next(stream);
        stream.recycle(update);
        this.executor.runTasks();

        assertEquals(0, update.getFrameIndex());
        assertNull(stream.poll());
        assertTrue(stream.isFinished());
    }

    @Test
    void singleFrameAnimationsOnlyChangeTheTextureOnce() throws IOException {
        byte[] file = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(1, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end();
        ApngFrameStream stream = this.stream(file);

        ApngFrameUpdate first = this.next(stream);
        assertFalse(first.isContentUnchanged());
        stream.recycle(first);
        for (int play = 1; play < 4; play++) {
            ApngFrameUpdate update = this.next(stream);
            assertEquals(play, update.getPlay());
            assertTrue(update.isContentUnchanged());
            stream.recycle(update);
        }
    }

    @Test
    void closingFreesAllNativeMemoryEvenWithAPolledUpdate() throws IOException {
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(ApngCompositorTest.randomAnimation(new Random(13), WIDTH, HEIGHT, 8));
        ApngFrameStream stream = this.stream(source);
        stream.recycle(this.next(stream));
        ApngFrameUpdate held = this.next(stream);
        this.executor.runTasks();

        stream.close();
        assertTrue(source.isClosed());
        stream.recycle(held);
        this.executor.runTasks();

        assertEquals(0L, stream.getNativeMemoryBytes());
        assertNull(stream.poll());
        assertNull(stream.restart());
    }

    @Test
    void closingBeforeThePendingTaskRunsLetsTheTaskCleanUp() throws IOException {
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(ApngCompositorTest.randomAnimation(new Random(14), WIDTH, HEIGHT, 8));
        ApngFrameStream stream = this.stream(source);

        stream.close();
        // The pending task owns the source until it noticed the close
        assertFalse(source.isClosed());
        this.executor.runTasks();

        assertTrue(source.isClosed());
        assertEquals(0L, stream.getNativeMemoryBytes());
        assertNull(stream.poll());
    }

    @Test
    void closingAStaticImageAfterItsFrameClosesTheSource() throws IOException {
        ApngTestFiles.MemorySource source = new ApngTestFiles.MemorySource(ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end());
        ApngFrameStream stream = this.stream(source);
        stream.recycle(this.next(stream));
        this.executor.runTasks();
        assertFalse(source.isClosed());

        stream.close();

        assertTrue(source.isClosed());
    }

    private ApngFrameStream stream(byte[] file) throws IOException {
        return this.stream(new ApngTestFiles.MemorySource(file));
    }

    private ApngFrameStream stream(ApngTestFiles.MemorySource source) throws IOException {
        ApngImage image = ApngReader.read(source, "test.png");
        ApngFrameStream stream = new ApngFrameStream(image, "test.png", this.executor, () -> this.now);
        this.streams.add(stream);
        this.sources.add(source);
        stream.start();
        return stream;
    }

    private ApngFrameUpdate next(ApngFrameStream stream) {
        this.executor.runTasks();
        ApngFrameUpdate update = stream.poll();
        assertNotNull(update, "no frame was ready");
        return update;
    }

    /**
     * Four 4x4 frames that loop forever, every frame shows for 100 ms.
     */
    static byte[] fourFrames() {
        return ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(4, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(ApngTestFiles.solid(4, 4, 255, 0, 0, 255))
                .frame(2, 2, 0, 0, 1, 10, 0, 0, ApngTestFiles.solid(2, 2, 0, 255, 0, 255))
                .frame(2, 2, 2, 2, 1, 10, 0, 0, ApngTestFiles.solid(2, 2, 0, 0, 255, 255))
                .frame(1, 1, 1, 1, 1, 10, 0, 0, ApngTestFiles.solid(1, 1, 9, 9, 9, 255))
                .end();
    }

    /**
     * A copy of the file with one changed byte in the middle of the data of a frame, like an edit of the file.
     */
    static byte[] changedFrameData(byte[] file, int frameIndex) throws IOException {
        ApngFrame.DataChunks chunks = ApngTestFiles.read(file).getFrame(frameIndex).dataChunks();
        byte[] changed = file.clone();
        int dataStart = (int) chunks.positions()[0] + chunks.headerLength();
        changed[dataStart + (chunks.lengths()[0] - (chunks.headerLength() - 8)) / 2] ^= 0x10;
        return changed;
    }

    /**
     * Three 4x4 frames that play the given number of times.
     */
    private static byte[] threeFrames(int numPlays) {
        return ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(3, numPlays)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(ApngTestFiles.solid(4, 4, 255, 0, 0, 255))
                .frame(2, 2, 0, 0, 1, 10, 0, 0, ApngTestFiles.solid(2, 2, 0, 255, 0, 255))
                .frame(2, 2, 2, 2, 1, 10, 0, 0, ApngTestFiles.solid(2, 2, 0, 0, 255, 255))
                .end();
    }

    /**
     * Does what the GPU texture upload does, on an array.
     */
    private static void apply(ApngFrameUpdate update, int[] texture) {
        if (update.isContentUnchanged()) return;
        IntBuffer pixels = update.getPixels().duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        int offset = 0;
        for (int i = 0; i < update.getRectangleCount(); i++) {
            for (int row = 0; row < update.getRectangleHeight(i); row++) {
                for (int column = 0; column < update.getRectangleWidth(i); column++) {
                    texture[(update.getRectangleY(i) + row) * WIDTH + update.getRectangleX(i) + column] = pixels.get(offset++);
                }
            }
        }
    }

    /**
     * Runs tasks only when the test says so, and delayed tasks once the fake clock reached their time.
     */
    static final class ManualExecutor extends AbstractExecutorService implements ScheduledExecutorService {

        private final LongSupplier clock;
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private final List<DelayedTask> delayedTasks = new ArrayList<>();

        ManualExecutor(LongSupplier clock) {
            this.clock = clock;
        }

        void runTasks() {
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
