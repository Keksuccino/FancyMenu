package de.keksuccino.fancymenu.customization.global;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeamlessWorldFrameCaptureTest {

    private static final RenderTarget SOURCE = new RenderTarget(false) {};

    private final AtomicLong now = new AtomicLong();
    private final List<FakeFrameCopy> copies = new ArrayList<>();
    private final AtomicInteger gpuTaskRuns = new AtomicInteger();
    private Runnable gpuTask = () -> {};
    private final SeamlessWorldFrameCapture capture = new SeamlessWorldFrameCapture(this::newCopy, this.now::get, () -> {
        this.gpuTaskRuns.incrementAndGet();
        this.gpuTask.run();
    });
    private boolean deferReadBacks;

    @Test
    void framesOnlyGetCopiedOnTheGpuWhileThePlayerIsInTheWorld() {
        // 10 seconds at 100 FPS, with both capture points of a frame calling the capture.
        for (long time = 0L; time < 10_000L; time += 10L) {
            this.now.set(time);
            this.capture.copyFrameIfDue(SOURCE);
            this.capture.copyFrameIfDue(SOURCE);
        }

        assertAll(
                () -> assertEquals(1, this.copies.size()),
                () -> assertEquals(10, this.copies.get(0).copies),
                () -> assertEquals(0, this.copies.get(0).readBacks)
        );
    }

    @Test
    void framesGetCopiedAtMostOncePerInterval() {
        this.capture.copyFrameIfDue(SOURCE);
        this.now.set(SeamlessWorldFrameCapture.COPY_INTERVAL_MS - 1L);
        this.capture.copyFrameIfDue(SOURCE);
        assertEquals(1, this.copies.get(0).copies);

        this.now.set(SeamlessWorldFrameCapture.COPY_INTERVAL_MS);
        this.capture.copyFrameIfDue(SOURCE);
        assertEquals(2, this.copies.get(0).copies);
    }

    @Test
    void theLatestFrameGetsReadBackOnceWhenTheWorldGetsLeft() {
        AtomicInteger delivered = new AtomicInteger();
        this.capture.copyFrameIfDue(SOURCE);
        this.now.set(5_000L);
        this.capture.copyFrameIfDue(SOURCE);

        assertTrue(this.capture.readBackAndReset(image -> delivered.incrementAndGet()));
        assertFalse(this.capture.readBackAndReset(image -> delivered.incrementAndGet()));

        assertAll(
                () -> assertEquals(1, this.copies.get(0).readBacks),
                () -> assertEquals(1, delivered.get()),
                () -> assertFalse(this.capture.hasPendingReadBacks())
        );
    }

    @Test
    void leavingWithoutAnyFrameReadsNothingBack() {
        AtomicInteger delivered = new AtomicInteger();

        assertFalse(this.capture.readBackAndReset(image -> delivered.incrementAndGet()));

        assertAll(() -> assertEquals(0, this.copies.size()), () -> assertEquals(0, delivered.get()));
    }

    @Test
    void aCopyWithNothingToCopyGetsReleasedInsteadOfReadBack() {
        AtomicInteger delivered = new AtomicInteger();
        this.capture.copyFrameIfDue(SOURCE);
        this.copies.get(0).copyResult = false;
        this.now.set(SeamlessWorldFrameCapture.COPY_INTERVAL_MS);
        this.capture.copyFrameIfDue(SOURCE);

        assertFalse(this.capture.readBackAndReset(image -> delivered.incrementAndGet()));

        FakeFrameCopy copy = this.copies.get(0);
        assertAll(() -> assertEquals(0, copy.readBacks), () -> assertEquals(1, copy.releases), () -> assertEquals(0, delivered.get()));
    }

    @Test
    void aFailingCopyGetsRetriedOncePerIntervalAndIsNoFrame() {
        this.capture.copyFrameIfDue(SOURCE);
        FakeFrameCopy copy = this.copies.get(0);
        copy.failCopies = true;
        this.now.set(SeamlessWorldFrameCapture.COPY_INTERVAL_MS);

        assertThrows(IllegalStateException.class, () -> this.capture.copyFrameIfDue(SOURCE));
        assertDoesNotThrow(() -> this.capture.copyFrameIfDue(SOURCE));
        assertEquals(2, copy.copies);
        assertFalse(this.capture.readBackAndReset(image -> {}));
    }

    @Test
    void theNextWorldStartsWithItsOwnCopyWhileTheLastFrameIsStillGettingReadBack() {
        this.deferReadBacks = true;
        this.capture.copyFrameIfDue(SOURCE);
        this.capture.readBackAndReset(image -> {});
        this.now.set(10L);

        this.capture.copyFrameIfDue(SOURCE);

        assertAll(
                () -> assertEquals(2, this.copies.size()),
                () -> assertEquals(1, this.copies.get(1).copies),
                () -> assertEquals(0, this.copies.get(0).releases),
                () -> assertTrue(this.capture.hasPendingReadBacks())
        );
    }

    @Test
    void resettingReleasesTheFrameButKeepsPendingReadBacks() {
        AtomicInteger delivered = new AtomicInteger();
        this.deferReadBacks = true;
        this.capture.copyFrameIfDue(SOURCE);
        this.capture.readBackAndReset(image -> delivered.incrementAndGet());
        this.capture.copyFrameIfDue(SOURCE);

        this.capture.reset();

        assertAll(
                () -> assertEquals(0, this.copies.get(0).releases),
                () -> assertEquals(1, this.copies.get(1).releases),
                () -> assertTrue(this.capture.hasPendingReadBacks())
        );
        this.copies.get(0).finishReadBack();
        assertAll(() -> assertFalse(this.capture.hasPendingReadBacks()), () -> assertEquals(1, delivered.get()));
    }

    @Test
    void aReadBackThatThrowsCompletesWithoutImage() {
        List<NativeImage> delivered = new ArrayList<>();
        this.capture.copyFrameIfDue(SOURCE);
        this.copies.get(0).failReadBacks = true;

        assertTrue(this.capture.readBackAndReset(delivered::add));

        assertAll(
                () -> assertEquals(1, delivered.size()),
                () -> assertNull(delivered.get(0)),
                () -> assertFalse(this.capture.hasPendingReadBacks())
        );
    }

    @Test
    void aConsumerThatThrowsDoesNotBreakTheReadBackTracking() {
        this.capture.copyFrameIfDue(SOURCE);

        assertDoesNotThrow(() -> this.capture.readBackAndReset(image -> {
            throw new IllegalStateException("Test");
        }));

        assertFalse(this.capture.hasPendingReadBacks());
    }

    @Test
    void finishingPendingReadBacksRunsGpuTasksUntilTheReadBackIsDone() {
        AtomicInteger delivered = new AtomicInteger();
        this.deferReadBacks = true;
        this.capture.copyFrameIfDue(SOURCE);
        this.capture.readBackAndReset(image -> delivered.incrementAndGet());
        this.gpuTask = () -> {
            if (this.gpuTaskRuns.get() == 3) this.copies.get(0).finishReadBack();
        };

        this.capture.finishPendingReadBacks(1_000L);

        assertAll(
                () -> assertEquals(3, this.gpuTaskRuns.get()),
                () -> assertEquals(1, delivered.get()),
                () -> assertFalse(this.capture.hasPendingReadBacks())
        );
    }

    @Test
    void finishingPendingReadBacksGivesUpAfterTheTimeout() {
        this.deferReadBacks = true;
        this.capture.copyFrameIfDue(SOURCE);
        this.capture.readBackAndReset(image -> {});
        this.gpuTask = () -> this.now.addAndGet(100L);

        this.capture.finishPendingReadBacks(1_000L);

        assertAll(() -> assertEquals(10, this.gpuTaskRuns.get()), () -> assertTrue(this.capture.hasPendingReadBacks()));
    }

    @Test
    void finishingPendingReadBacksDoesNothingWithoutPendingReadBacks() {
        this.capture.copyFrameIfDue(SOURCE);

        this.capture.finishPendingReadBacks(1_000L);

        assertEquals(0, this.gpuTaskRuns.get());
    }

    @NotNull
    private SeamlessWorldFrameCapture.FrameCopy newCopy() {
        FakeFrameCopy copy = new FakeFrameCopy(this.deferReadBacks);
        this.copies.add(copy);
        return copy;
    }

    private static final class FakeFrameCopy implements SeamlessWorldFrameCapture.FrameCopy {

        private final boolean deferReadBacks;
        private int copies;
        private int readBacks;
        private int releases;
        private boolean copyResult = true;
        private boolean failCopies;
        private boolean failReadBacks;
        @Nullable private Consumer<NativeImage> pendingConsumer;

        private FakeFrameCopy(boolean deferReadBacks) {
            this.deferReadBacks = deferReadBacks;
        }

        @Override
        public boolean copyFrom(@NotNull RenderTarget source) {
            this.copies++;
            if (this.failCopies) throw new IllegalStateException("Test");
            return this.copyResult;
        }

        @Override
        public void readBack(@NotNull Consumer<NativeImage> imageConsumer) {
            this.readBacks++;
            if (this.failReadBacks) throw new IllegalStateException("Test");
            if (this.deferReadBacks) {
                this.pendingConsumer = imageConsumer;
            } else {
                imageConsumer.accept(null);
            }
        }

        @Override
        public void release() {
            this.releases++;
        }

        private void finishReadBack() {
            Consumer<NativeImage> consumer = this.pendingConsumer;
            this.pendingConsumer = null;
            if (consumer != null) consumer.accept(null);
        }

    }

}
