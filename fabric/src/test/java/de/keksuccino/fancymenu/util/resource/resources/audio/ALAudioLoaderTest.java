package de.keksuccino.fancymenu.util.resource.resources.audio;

import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import javax.sound.sampled.AudioFormat;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ALAudioLoaderTest {

    private static final byte[] AUDIO_DATA = {1, 2, 3, 4, 5, 6, 7, 8};

    @Test
    void closesSourceRightAfterReadingItBeforeDecoding() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);

        ALAudioLoader.load(source, "test.ogg", audio);

        assertTrue(audio.sourceClosedWhenDecoding);
        assertArrayEquals(AUDIO_DATA, audio.decodedData);
        assertEquals(1, source.closeCalls);
    }

    @Test
    void keepsBufferThatGotAttached() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);

        ALAudioLoader.load(source, "test.ogg", audio);

        assertNotNull(audio.decodedBuffer);
        assertSame(audio.decodedBuffer, audio.attachedBuffer);
        assertEquals(0, audio.decodedBuffer.deleteCalls);
        assertEquals(0, audio.failures);
    }

    @Test
    void closesSourceWhenAudioGetsClosedWhileReading() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);
        source.onEnd = audio::close;

        ALAudioLoader.load(source, "test.ogg", audio);

        assertEquals(1, source.closeCalls);
        assertNull(audio.decodedData);
        assertNull(audio.attachedBuffer);
        assertEquals(0, audio.failures);
    }

    @Test
    void deletesBufferWhenAudioGetsClosedWhileDecoding() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);
        audio.closeWhileDecoding = true;

        ALAudioLoader.load(source, "test.ogg", audio);

        assertNotNull(audio.decodedBuffer);
        assertNull(audio.attachedBuffer);
        assertEquals(1, audio.decodedBuffer.deleteCalls);
        assertEquals(1, source.closeCalls);
        assertEquals(0, audio.failures);
    }

    @Test
    void closesSourceWhenReadingFails() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        source.readFailure = new IOException("expected read failure");
        FakeAudio audio = new FakeAudio(source);

        ALAudioLoader.load(source, "test.ogg", audio);

        assertEquals(1, source.closeCalls);
        assertNull(audio.decodedData);
        assertEquals(1, audio.failures);
    }

    @Test
    void closesSourceWhenDecodingFails() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);
        audio.decodeFailure = new IOException("expected decode failure");

        ALAudioLoader.load(source, "test.ogg", audio);

        assertEquals(1, source.closeCalls);
        assertNull(audio.attachedBuffer);
        assertEquals(1, audio.failures);
    }

    @Test
    void deletesBufferWhenAttachingItFails() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);
        audio.attachFailure = new IllegalStateException("expected attach failure");

        ALAudioLoader.load(source, "test.ogg", audio);

        assertNotNull(audio.decodedBuffer);
        assertNull(audio.attachedBuffer);
        assertEquals(1, audio.decodedBuffer.deleteCalls);
        assertEquals(1, source.closeCalls);
        assertEquals(1, audio.failures);
    }

    @Test
    void givesOpenedSourceAndKeepsPendingClip() {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        TrackingClip pendingClip = new TrackingClip();
        AtomicInteger failures = new AtomicInteger();

        InputStream opened = ALAudioLoader.openSource(() -> source, "https://example.com/test.ogg", pendingClip, failures::incrementAndGet);

        assertSame(source, opened);
        assertEquals(0, source.closeCalls);
        assertEquals(0, pendingClip.closeCalls);
        assertEquals(0, failures.get());
    }

    @Test
    void closesPendingClipAndFailsWhenSourceStreamIsMissing() {
        TrackingClip pendingClip = new TrackingClip();
        AtomicInteger failures = new AtomicInteger();

        InputStream opened = ALAudioLoader.openSource(() -> null, "https://example.com/test.ogg", pendingClip, failures::incrementAndGet);

        assertNull(opened);
        assertEquals(1, pendingClip.closeCalls);
        assertEquals(1, failures.get());
    }

    @Test
    void closesPendingClipAndFailsWhenOpeningSourceFails() {
        TrackingClip pendingClip = new TrackingClip();
        AtomicInteger failures = new AtomicInteger();

        InputStream opened = ALAudioLoader.openSource(() -> {
            throw new IOException("expected open failure");
        }, "https://example.com/test.ogg", pendingClip, failures::incrementAndGet);

        assertNull(opened);
        assertEquals(1, pendingClip.closeCalls);
        assertEquals(1, failures.get());
    }

    @Test
    void failsWithoutPendingClipWhenSourceIsMissing() {
        AtomicInteger failures = new AtomicInteger();

        // Like the resource manager, which throws a FileNotFoundException when a resource doesn't exist
        InputStream opened = ALAudioLoader.openSource(() -> {
            throw new FileNotFoundException("fancymenu:missing.ogg");
        }, "fancymenu:missing.ogg", null, failures::incrementAndGet);

        assertNull(opened);
        assertEquals(1, failures.get());
    }

    @Test
    void loadsInBackgroundAndClosesSource() throws InterruptedException {
        TrackingInputStream source = new TrackingInputStream(AUDIO_DATA);
        FakeAudio audio = new FakeAudio(source);

        ALAudioLoader.loadInBackground(source, "test.ogg", audio);

        assertTrue(audio.attachAttempted.await(10, TimeUnit.SECONDS));
        assertNotNull(audio.attachedBuffer);
        assertEquals(1, source.closeCalls);
    }

    /**
     * Behaves like the OpenAL audios the loader runs for: once it got closed, it stops loading and doesn't take buffers anymore.
     */
    private static final class FakeAudio implements ALAudioLoader.Target {

        @NotNull
        private final TrackingInputStream source;
        private final CountDownLatch attachAttempted = new CountDownLatch(1);
        private volatile boolean closed;
        private volatile boolean closeWhileDecoding;
        @Nullable
        private volatile Exception decodeFailure;
        @Nullable
        private volatile RuntimeException attachFailure;
        private volatile boolean sourceClosedWhenDecoding;
        @Nullable
        private volatile byte[] decodedData;
        @Nullable
        private volatile TrackingBuffer decodedBuffer;
        @Nullable
        private volatile ALAudioBuffer attachedBuffer;
        private volatile int failures;

        private FakeAudio(@NotNull TrackingInputStream source) {
            this.source = source;
        }

        private void close() {
            this.closed = true;
        }

        @Override
        public boolean canContinueLoading() {
            return !this.closed;
        }

        @Override
        public @NotNull ALAudioBuffer decode(@NotNull byte[] audioData) throws Exception {
            this.decodedData = audioData;
            this.sourceClosedWhenDecoding = this.source.closeCalls > 0;
            Exception failure = this.decodeFailure;
            if (failure != null) throw failure;
            if (this.closeWhileDecoding) this.close();
            TrackingBuffer buffer = new TrackingBuffer();
            this.decodedBuffer = buffer;
            return buffer;
        }

        @Override
        public boolean attachDecodedBuffer(@NotNull ALAudioBuffer buffer) {
            try {
                RuntimeException failure = this.attachFailure;
                if (failure != null) throw failure;
                if (this.closed) return false;
                this.attachedBuffer = buffer;
                return true;
            } finally {
                this.attachAttempted.countDown();
            }
        }

        @Override
        public void onLoadingFailed() {
            this.failures++;
        }

    }

    /**
     * A buffer that never gets uploaded to OpenAL, so it works without OpenAL, and counts how often it got deleted.
     */
    private static final class TrackingBuffer extends ALAudioBuffer {

        private volatile int deleteCalls;

        private TrackingBuffer() {
            super(ByteBuffer.allocateDirect(4), new AudioFormat(44100.0F, 16, 2, true, false));
        }

        @Override
        public void delete() {
            this.deleteCalls++;
        }

        @Override
        public void deleteQuietly() {
            this.deleteCalls++;
        }

    }

    /**
     * A clip without an OpenAL source, so it works without OpenAL, and counts how often it got closed.
     */
    private static final class TrackingClip extends ALAudioClip {

        private int closeCalls;

        private TrackingClip() {
            super(0);
        }

        /**
         * Gets called by the constructor of the clip and would set the volume of its OpenAL source.
         */
        @Override
        public void tryUpdateVolume() {
        }

        @Override
        public boolean isValidOpenAlSource() {
            return false;
        }

        @Override
        public void close() {
            this.closeCalls++;
            super.close();
        }

    }

    private static final class TrackingInputStream extends InputStream {

        private final byte[] data;
        private int position;
        @Nullable
        private IOException readFailure;
        /**
         * Runs when the reader reaches the end of the data, like a close() of the audio while its file gets read.
         */
        @Nullable
        private Runnable onEnd;
        private volatile int closeCalls;

        private TrackingInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            return (this.read(single, 0, 1) < 0) ? -1 : (single[0] & 0xFF);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (this.readFailure != null) throw this.readFailure;
            if (this.position >= this.data.length) {
                if (this.onEnd != null) this.onEnd.run();
                return -1;
            }
            int count = Math.min(length, this.data.length - this.position);
            System.arraycopy(this.data, this.position, buffer, offset, count);
            this.position += count;
            return count;
        }

        @Override
        public void close() {
            this.closeCalls++;
        }

    }

}
