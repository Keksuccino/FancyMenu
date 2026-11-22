package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.LogCapture;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedFrameStreamTest {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 12;

    private long now = 1_000_000_000L;
    private final AnimatedImageTestSupport.ManualExecutor executor = new AnimatedImageTestSupport.ManualExecutor(() -> this.now);
    private final List<AnimatedFrameStream> streams = new ArrayList<>();
    private final List<AnimatedImageTestSupport.MemorySource> sources = new ArrayList<>();

    @AfterEach
    void closeStreams() {
        this.streams.forEach(AnimatedFrameStream::close);
        this.executor.runTasks();
        for (AnimatedFrameStream stream : this.streams) assertEquals(0L, stream.getNativeMemoryBytes());
        for (AnimatedImageTestSupport.MemorySource source : this.sources) assertTrue(source.isClosed());
    }

    @Test
    void updatesRebuildEveryComposedFrameOnTheTexture() throws IOException {
        byte[] file = ApngTestFiles.randomAnimation(new Random(7), WIDTH, HEIGHT, 10);
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];

        for (int play = 0; play < 3; play++) {
            for (int frame = 0; frame < expected.length; frame++) {
                AnimatedFrameUpdate update = this.next(stream);
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
        AnimatedFrameStream stream = this.stream(ApngTestFiles.randomAnimation(new Random(8), WIDTH, HEIGHT, 30));
        this.executor.runTasks();

        int ready = 0;
        AnimatedFrameUpdate update;
        while ((update = stream.poll()) != null) {
            ready++;
            stream.recycle(update);
        }

        assertTrue((ready >= AnimatedFrameStream.MIN_QUEUED_UPDATES) && (ready <= AnimatedFrameStream.MAX_QUEUED_UPDATES), "ready frames: " + ready);
    }

    @Test
    void finitePlaysEndAfterTheirLastFrame() throws IOException {
        AnimatedFrameStream stream = this.stream(ApngTestFiles.threeFrames(2));

        List<String> shown = new ArrayList<>();
        while (true) {
            this.executor.runTasks();
            AnimatedFrameUpdate update = stream.poll();
            if (update == null) break;
            shown.add(update.getFrameIndex() + "/" + update.getPlay());
            stream.recycle(update);
        }

        assertEquals(List.of("0/0", "1/0", "2/0", "0/1", "1/1", "2/1"), shown);
        assertTrue(stream.isFinished());
    }

    @Test
    void laterPlaysReuseTheStoredFirstFrame() throws IOException {
        AnimatedFrameStream stream = this.stream(ApngTestFiles.threeFrames(0));
        AnimatedFrameUpdate firstPlay = this.next(stream);
        stream.recycle(this.next(stream));
        stream.recycle(this.next(stream));
        AnimatedFrameUpdate secondPlay = this.next(stream);

        assertEquals(1, secondPlay.getPlay());
        assertSame(firstPlay.getPixels(), secondPlay.getPixels());
        assertTrue(secondPlay.hasSharedPixels());
    }

    @Test
    void restartShowsTheStoredFirstFrameAndContinuesWithTheSecondOne() throws IOException {
        byte[] file = ApngTestFiles.randomAnimation(new Random(9), WIDTH, HEIGHT, 6);
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];
        for (int i = 0; i < 4; i++) {
            AnimatedFrameUpdate update = this.next(stream);
            apply(update, texture);
            stream.recycle(update);
        }

        AnimatedFrameUpdate first = stream.restart();
        assertNotNull(first);
        apply(first, texture);
        stream.recycle(first);
        assertArrayEquals(expected[0], texture);

        for (int frame = 1; frame < expected.length; frame++) {
            AnimatedFrameUpdate update = this.next(stream);
            assertEquals(frame, update.getFrameIndex());
            assertEquals(0, update.getPlay());
            apply(update, texture);
            assertArrayEquals(expected[frame], texture, "frame " + frame);
            stream.recycle(update);
        }
    }

    @Test
    void restartBeforeTheFirstFrameExistsStartsFromTheBeginning() throws IOException {
        AnimatedFrameStream stream = this.stream(ApngTestFiles.threeFrames(0));

        assertNull(stream.restart());
        AnimatedFrameUpdate update = this.next(stream);

        assertEquals(0, update.getFrameIndex());
        assertEquals(0, update.getPlay());
    }

    @Test
    void restartingAFinishedAnimationPlaysItAgain() throws IOException {
        AnimatedFrameStream stream = this.stream(ApngTestFiles.threeFrames(1));
        for (int i = 0; i < 3; i++) stream.recycle(this.next(stream));
        this.executor.runTasks();
        assertNull(stream.poll());
        assertTrue(stream.isFinished());

        AnimatedFrameUpdate first = stream.restart();

        assertNotNull(first);
        assertFalse(stream.isFinished());
        AnimatedFrameUpdate second = this.next(stream);
        assertEquals(1, second.getFrameIndex());
        stream.recycle(second);
    }

    @Test
    void idleStreamFreesItsReadyFramesAndResumesWithTheWholeCanvas() throws IOException {
        byte[] file = ApngTestFiles.randomAnimation(new Random(10), WIDTH, HEIGHT, 12);
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedFrameStream stream = this.stream(file);
        int[] texture = new int[WIDTH * HEIGHT];
        for (int i = 0; i < 3; i++) {
            AnimatedFrameUpdate update = this.next(stream);
            apply(update, texture);
            stream.recycle(update);
        }
        this.executor.runTasks();
        long firstFrameBytes = WIDTH * HEIGHT * 4L;
        assertTrue(stream.getNativeMemoryBytes() > firstFrameBytes);

        this.now += AnimatedFrameStream.INACTIVITY_TIMEOUT_NANOS + 1L;
        this.executor.runTasks();

        assertEquals(firstFrameBytes, stream.getNativeMemoryBytes());
        // Rendering the texture again starts decoding right away
        stream.markActive();
        AnimatedFrameUpdate resumed = this.next(stream);
        assertEquals(1, resumed.getRectangleCount());
        assertEquals((long) WIDTH * HEIGHT, resumed.getPixelCount());
        apply(resumed, texture);
        assertArrayEquals(expected[resumed.getFrameIndex()], texture);
        stream.recycle(resumed);
        // Playback continues normally after the frames that got dropped
        AnimatedFrameUpdate following = this.next(stream);
        apply(following, texture);
        assertArrayEquals(expected[following.getFrameIndex()], texture);
        stream.recycle(following);
    }

    @Test
    void idleStreamThatShowedNothingStartsAtItsFirstFrame() throws IOException {
        byte[] file = ApngTestFiles.randomAnimation(new Random(15), WIDTH, HEIGHT, 12);
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedFrameStream stream = this.stream(file);
        this.executor.runTasks();

        this.now += AnimatedFrameStream.INACTIVITY_TIMEOUT_NANOS + 1L;
        this.executor.runTasks();
        stream.markActive();

        int[] texture = new int[WIDTH * HEIGHT];
        for (int frame = 0; frame < 4; frame++) {
            AnimatedFrameUpdate update = this.next(stream);
            assertEquals(frame, update.getFrameIndex());
            assertEquals(0, update.getPlay());
            apply(update, texture);
            assertArrayEquals(expected[frame], texture, "frame " + frame);
            stream.recycle(update);
        }
    }

    @Test
    void streamIsNotFinishedWhileItsTaskStillComposesTheLastFrame() throws IOException {
        // Production finishes while the task composes the last frame, and the decoder gets closed before the task queues that frame
        ApngImage image = ApngReader.read(new AnimatedImageTestSupport.MemorySource(ApngTestFiles.threeFrames(1)), "test.png");
        List<Boolean> finishedWhileComposing = new ArrayList<>();
        AnimatedFrameStream[] stream = new AnimatedFrameStream[1];
        AnimatedImage hooked = new DecoderCloseHook(image, () -> finishedWhileComposing.add(stream[0].isFinished()));
        stream[0] = new AnimatedFrameStream(hooked, "test.png", this.executor, () -> this.now);
        this.streams.add(stream[0]);
        stream[0].start();
        stream[0].recycle(this.next(stream[0]));
        stream[0].recycle(stream[0].poll());

        this.executor.runTasks();

        assertEquals(List.of(false), finishedWhileComposing);
        assertFalse(stream[0].isFinished());
        stream[0].recycle(this.next(stream[0]));
        assertTrue(stream[0].isFinished());
    }

    @Test
    void restartedStreamIsNotFinishedBeforeItDecodedAgain() throws IOException {
        AnimatedFrameStream stream = this.stream(ApngTestFiles.threeFrames(1));
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
        AnimatedFrameStream stream = this.stream(ApngTestFiles.randomAnimation(new Random(12), WIDTH, HEIGHT, 12));
        this.executor.runTasks();
        long bytes = stream.getNativeMemoryBytes();

        for (int i = 0; i < 5; i++) {
            this.now += AnimatedFrameStream.INACTIVITY_TIMEOUT_NANOS / 2L;
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
        AnimatedFrameStream stream = this.stream(png.end());

        List<String> shown = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AnimatedFrameUpdate update = this.next(stream);
            shown.add(update.getFrameIndex() + "/" + update.getPlay());
            stream.recycle(update);
        }

        assertEquals(List.of("0/0", "1/0", "0/1", "1/1", "0/2"), shown);
        assertFalse(stream.hasFailed());
    }

    @Test
    void corruptedFirstFrameFailsTheStream() throws IOException {
        byte[] file = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().chunk("IDAT", new byte[] {0x78, (byte) 0x9C, 1, 2, 3}).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        AnimatedFrameStream stream = this.stream(source);

        this.executor.runTasks();

        assertTrue(stream.hasFailed());
        assertNull(stream.poll());
        assertEquals(0L, stream.getNativeMemoryBytes());
        // A failed stream never decodes again, so it releases its source right away
        assertTrue(source.isClosed());
    }

    @Test
    void changedSourceFailsTheStreamInsteadOfCuttingTheAnimation() throws IOException {
        byte[] file = ApngTestFiles.fourFrames();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        AnimatedFrameStream stream = this.stream(source);
        this.executor.runTasks();
        // The stream decoded the first two frames ahead, the third one changes before it gets decoded
        source.data = ApngTestFiles.changedFrameData(file, 2);

        List<String> shown = new ArrayList<>();
        // Bounded, because an animation that got cut instead would loop forever
        for (int i = 0; i < 20; i++) {
            this.executor.runTasks();
            AnimatedFrameUpdate update = stream.poll();
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
        byte[] file = ApngTestFiles.fourFrames();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        AnimatedFrameStream stream = this.stream(source);
        for (int i = 0; i < 4; i++) stream.recycle(this.next(stream));
        this.executor.runTasks();

        source.data = ApngTestFiles.changedFrameData(file, 3);
        int shown = 0;
        for (int i = 0; i < 20; i++) {
            this.executor.runTasks();
            AnimatedFrameUpdate update = stream.poll();
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
        AnimatedFrameStream stream = this.stream(ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end());

        AnimatedFrameUpdate update = this.next(stream);
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
        AnimatedFrameStream stream = this.stream(file);

        AnimatedFrameUpdate first = this.next(stream);
        assertFalse(first.isContentUnchanged());
        stream.recycle(first);
        for (int play = 1; play < 4; play++) {
            AnimatedFrameUpdate update = this.next(stream);
            assertEquals(play, update.getPlay());
            assertTrue(update.isContentUnchanged());
            stream.recycle(update);
        }
    }

    @Test
    void closingFreesAllNativeMemoryEvenWithAPolledUpdate() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(ApngTestFiles.randomAnimation(new Random(13), WIDTH, HEIGHT, 8));
        AnimatedFrameStream stream = this.stream(source);
        stream.recycle(this.next(stream));
        AnimatedFrameUpdate held = this.next(stream);
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
    void closingBeforeThePendingTaskRunsClosesTheSourceRightAwayAndLetsTheTaskCleanUp() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(ApngTestFiles.randomAnimation(new Random(14), WIDTH, HEIGHT, 8));
        AnimatedFrameStream stream = this.stream(source);

        stream.close();
        // The file of the image is never open anymore once the stream got closed, even though the pending task still owns the decoder
        assertTrue(source.isClosed());
        this.executor.runTasks();

        assertEquals(0L, stream.getNativeMemoryBytes());
        assertNull(stream.poll());
    }

    @Test
    void closingWhileATaskReadsTheSourceClosesItRightAwayAndTheTaskStopsWithoutAnError() throws Exception {
        byte[] file = ApngTestFiles.randomAnimation(new Random(15), WIDTH, HEIGHT, 8);
        AnimatedImageTestSupport.GatedSource source = new AnimatedImageTestSupport.GatedSource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        ReadGate gate = new ReadGate();
        source.holdNextRead(gate);
        ScheduledThreadPoolExecutor decodingThread = new ScheduledThreadPoolExecutor(1);
        try (LogCapture problems = LogCapture.problemsOf(AnimatedFrameStream.class)) {
            AnimatedFrameStream stream = new AnimatedFrameStream(image, "test.png", decodingThread, () -> this.now);
            stream.start();
            gate.awaitReached();

            stream.close();

            assertTrue(source.isClosed());
            gate.open();
            decodingThread.shutdown();
            assertTrue(decodingThread.awaitTermination(ReadGate.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(0L, stream.getNativeMemoryBytes());
            assertEquals(List.of(), problems.messages());
        } finally {
            decodingThread.shutdownNow();
        }
    }

    @Test
    void closingTheSourceFromAnotherThreadStopsDecodingWithoutAnError() throws Exception {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(ApngTestFiles.randomAnimation(new Random(16), WIDTH, HEIGHT, 8));
        AnimatedFrameStream stream = this.stream(source);
        stream.recycle(this.next(stream));

        try (LogCapture problems = LogCapture.problemsOf(AnimatedFrameStream.class)) {
            Thread closer = new Thread(stream::closeSource, "Animated image test closer");
            closer.start();
            ReadGate.join(closer);
            assertTrue(source.isClosed());
            // Polling makes the stream decode again, which fails on the closed source
            AnimatedFrameUpdate update = stream.poll();
            if (update != null) stream.recycle(update);
            this.executor.runTasks();

            assertTrue(stream.hasFailed());
            assertEquals(List.of(), problems.messages());
        }
    }

    @Test
    void closingAStaticImageAfterItsFrameClosesTheSource() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(ApngTestFiles.solid(4, 4, 1, 2, 3, 4)).end());
        AnimatedFrameStream stream = this.stream(source);
        stream.recycle(this.next(stream));
        this.executor.runTasks();
        assertFalse(source.isClosed());

        stream.close();

        assertTrue(source.isClosed());
    }

    private AnimatedFrameStream stream(byte[] file) throws IOException {
        return this.stream(new AnimatedImageTestSupport.MemorySource(file));
    }

    private AnimatedFrameStream stream(AnimatedImageTestSupport.MemorySource source) throws IOException {
        ApngImage image = ApngReader.read(source, "test.png");
        AnimatedFrameStream stream = new AnimatedFrameStream(image, "test.png", this.executor, () -> this.now);
        this.streams.add(stream);
        this.sources.add(source);
        stream.start();
        return stream;
    }

    private AnimatedFrameUpdate next(AnimatedFrameStream stream) {
        this.executor.runTasks();
        AnimatedFrameUpdate update = stream.poll();
        assertNotNull(update, "no frame was ready");
        return update;
    }

    /**
     * Does what the GPU texture upload does, on an array.
     */
    private static void apply(AnimatedFrameUpdate update, int[] texture) {
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
     * An image that runs a hook whenever one of its decoders gets closed, and otherwise is the given image.
     */
    private record DecoderCloseHook(AnimatedImage image, Runnable onClose) implements AnimatedImage {

        @Override
        public @NotNull String getFormatName() {
            return this.image.getFormatName();
        }

        @Override
        public int getWidth() {
            return this.image.getWidth();
        }

        @Override
        public int getHeight() {
            return this.image.getHeight();
        }

        @Override
        public boolean isAnimated() {
            return this.image.isAnimated();
        }

        @Override
        public int getNumPlays() {
            return this.image.getNumPlays();
        }

        @Override
        public int getFrameCount() {
            return this.image.getFrameCount();
        }

        @Override
        public @NotNull AnimatedImageFrame getFrame(int index) {
            return this.image.getFrame(index);
        }

        @Override
        public @NotNull AnimatedFrameDecoder createFrameDecoder() {
            AnimatedFrameDecoder decoder = this.image.createFrameDecoder();
            return new AnimatedFrameDecoder() {
                @Override
                public void decode(int frameIndex, @NotNull RowConsumer consumer) throws IOException {
                    decoder.decode(frameIndex, consumer);
                }

                @Override
                public void close() {
                    DecoderCloseHook.this.onClose.run();
                    decoder.close();
                }
            };
        }

        @Override
        public void close() {
            this.image.close();
        }

    }

}
