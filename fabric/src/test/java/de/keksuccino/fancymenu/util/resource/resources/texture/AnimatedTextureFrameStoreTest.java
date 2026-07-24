package de.keksuccino.fancymenu.util.resource.resources.texture;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedTextureFrameStoreTest {

    @Test
    void clearClosesDetachedFramesAndRejectsLateDecoderDelivery() {
        AnimatedTextureFrameStore<TestFrame> store = new AnimatedTextureFrameStore<>();
        long oldGeneration = store.generation();
        TestFrame first = new TestFrame();
        TestFrame second = new TestFrame();
        assertTrue(store.add(oldGeneration, first));
        assertTrue(store.add(oldGeneration, second));
        assertTrue(store.setCurrent(oldGeneration, first));
        AnimatedTextureFrameStore.Snapshot<TestFrame> oldSnapshot = store.snapshot();

        store.clear();

        assertEquals(1, first.closeCalls.get());
        assertEquals(1, second.closeCalls.get());
        assertTrue(store.isEmpty());
        assertNull(store.current());
        assertFalse(store.setCurrent(oldSnapshot.generation(), oldSnapshot.current()));

        TestFrame lateFrame = new TestFrame();
        assertFalse(store.add(oldGeneration, lateFrame));
        assertEquals(1, lateFrame.closeCalls.get());
    }

    @Test
    void replacementGenerationPublishesIndependentSnapshot() {
        AnimatedTextureFrameStore<TestFrame> store = new AnimatedTextureFrameStore<>();
        long oldGeneration = store.generation();
        TestFrame oldFrame = new TestFrame();
        store.add(oldGeneration, oldFrame);
        store.setCurrent(oldGeneration, oldFrame);
        AnimatedTextureFrameStore.Snapshot<TestFrame> oldSnapshot = store.snapshot();
        store.clear();
        long replacementGeneration = store.generation();
        TestFrame replacement = new TestFrame();

        assertTrue(store.add(replacementGeneration, replacement));
        assertTrue(store.setCurrent(replacementGeneration, replacement));
        AnimatedTextureFrameStore.Snapshot<TestFrame> replacementSnapshot = store.snapshot();

        assertEquals(oldGeneration, oldSnapshot.generation());
        assertSame(oldFrame, oldSnapshot.current());
        assertEquals(replacementGeneration, replacementSnapshot.generation());
        assertSame(replacement, replacementSnapshot.current());
        assertEquals(1, replacementSnapshot.frames().size());
        assertSame(replacement, replacementSnapshot.frames().getFirst());
        assertFalse(store.setCurrent(oldSnapshot.generation(), oldFrame));
        assertSame(replacement, store.current());
    }

    @Test
    void staleGenerationCannotPublishMetadata() {
        AnimatedTextureFrameStore<TestFrame> store = new AnimatedTextureFrameStore<>();
        long generation = store.generation();
        AtomicInteger publications = new AtomicInteger();
        assertTrue(store.runIfGenerationActive(generation, publications::incrementAndGet));

        store.clear();

        assertFalse(store.runIfGenerationActive(generation, publications::incrementAndGet));
        assertEquals(1, publications.get());
    }

    @Test
    void replacementSnapshotNeverInheritsPreviousGenerationCompletion() {
        AnimatedTextureFrameStore<TestFrame> store = new AnimatedTextureFrameStore<>();
        long oldGeneration = store.generation();
        assertTrue(store.add(oldGeneration, new TestFrame()));
        assertTrue(store.markComplete(oldGeneration));
        AnimatedTextureFrameStore.Snapshot<TestFrame> oldSnapshot = store.snapshot();

        store.clear();
        long replacementGeneration = store.generation();
        assertTrue(store.add(replacementGeneration, new TestFrame()));
        AnimatedTextureFrameStore.Snapshot<TestFrame> replacementSnapshot = store.snapshot();

        assertTrue(oldSnapshot.complete());
        assertFalse(replacementSnapshot.complete());
        assertFalse(store.markComplete(oldGeneration));
        assertFalse(store.snapshot().complete());
    }

    @Test
    void closeIsIdempotentAndPermanentlyRejectsFrames() {
        AnimatedTextureFrameStore<TestFrame> store = new AnimatedTextureFrameStore<>();
        long generation = store.generation();
        TestFrame active = new TestFrame();
        store.add(generation, active);

        store.close();
        store.close();

        assertEquals(1, active.closeCalls.get());
        TestFrame late = new TestFrame();
        assertFalse(store.add(store.generation(), late));
        assertEquals(1, late.closeCalls.get());
        assertTrue(store.isEmpty());
    }

    private static final class TestFrame implements AutoCloseable {

        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }

    }

}
