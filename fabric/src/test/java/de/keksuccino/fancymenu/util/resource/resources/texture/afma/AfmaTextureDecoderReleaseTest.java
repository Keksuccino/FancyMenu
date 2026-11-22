package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Closes textures while their real stream threads stop because of the close, and checks that the decoder always gets released once the stream thread stopped.
 * Closing leaves releasing the decoder to a stream thread that still runs, so a stream thread that stops at the same moment must never miss it.
 */
class AfmaTextureDecoderReleaseTest {

    /** How many textures get closed. Their stream threads only stop right while closing decides who releases the decoder in a small part of them, so it takes many. **/
    private static final int CLOSES = 20_000;

    @Test
    void closingWhileTheStreamThreadStopsAlwaysReleasesTheDecoder() throws InterruptedException {
        for (int i = 0; i < CLOSES; i++) {
            InMemoryAfmaTexture texture = new InMemoryAfmaTexture();
            texture.startTickerIfNeeded();
            Thread stream = texture.streamThread;
            assertNotNull(stream);
            texture.awaitPreparedFrames();

            texture.close();

            ReadGate.join(stream);
            assertNull(texture.decoder, "Closing texture " + i + " didn't release its decoder");
        }
    }

    /**
     * A decoded texture whose frames have no payloads, so its real stream thread prepares frames without reading anything. Nothing gets rendered, so playback never touches Minecraft.
     */
    private static final class InMemoryAfmaTexture extends AfmaTexture {

        private volatile int preparedFrames;

        private InMemoryAfmaTexture() {
            this.decoder = new AfmaDecoder();
            this.frameCount = 4;
            this.frameDelaysMs = new long[]{100L, 100L, 100L, 100L};
            this.decoded.set(true);
        }

        @Override
        @Nullable
        protected PreparedFrame prepareNextFrame(int generation) {
            boolean intro = this.decodeIntro;
            int index = this.decodeIndex;
            long delay = this.resolveFrameDelay(intro, index);
            if (!this.advanceDecodeCursor(generation)) return null;
            this.preparedFrames++;
            return new PreparedFrame(intro, index, delay, AfmaFrameDescriptor.full("frame"), null, null);
        }

        /**
         * Waits until the stream thread prepared the two frames that fill its queue. Spins instead of waiting for a signal, so the test closes the texture while the stream thread
         * still finishes its iteration, which makes it stop right while closing decides who releases the decoder far more often.
         */
        private void awaitPreparedFrames() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ReadGate.TIMEOUT_SECONDS);
            while (this.preparedFrames < 2) {
                if (System.nanoTime() > deadline) fail("The stream thread didn't prepare its frames");
                Thread.onSpinWait();
            }
        }

    }

}
