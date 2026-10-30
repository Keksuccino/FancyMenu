package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedImageLoopCountReaderTest {

    @Test
    void gifWithZeroLoopsLoopsForever() {
        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readGifLoopCount(gif(0, false, 1)));
    }

    @Test
    void gifWithLoopCountReturnsIt() {
        assertEquals(3, AnimatedImageLoopCountReader.readGifLoopCount(gif(3, false, 2)));
    }

    @Test
    void gifWithoutLoopExtensionPlaysOnce() {
        assertEquals(1, AnimatedImageLoopCountReader.readGifLoopCount(gif(-1, false, 3)));
    }

    @Test
    void gifLoopExtensionAfterFirstImageGetsFound() {
        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readGifLoopCount(gif(0, true, 3)));
    }

    @Test
    void truncatedGifPlaysOnce() {
        byte[] gif = gif(0, true, 3);
        byte[] truncated = new byte[gif.length / 2];
        System.arraycopy(gif, 0, truncated, 0, truncated.length);

        assertEquals(1, AnimatedImageLoopCountReader.readGifLoopCount(truncated));
    }

    @Test
    void gifReadingStopsAtLoopExtension() throws IOException {
        byte[] gif = gif(0, false, 50);
        CountingInputStream in = new CountingInputStream(gif);

        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readGifLoopCount(in, Long.MAX_VALUE));
        assertTrue(in.consumed < 64, "consumed " + in.consumed + " of " + gif.length + " bytes");
    }

    @Test
    void gifReadingStopsAtByteLimit() throws IOException {
        byte[] gif = gif(0, true, 50);
        CountingInputStream in = new CountingInputStream(gif);

        // The loop extension comes after the image data, so it's out of reach of the limit
        assertEquals(1, AnimatedImageLoopCountReader.readGifLoopCount(in, 100L));
        assertTrue(in.consumed <= 100L);
    }

    @Test
    void apngWithZeroPlaysLoopsForever() {
        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readApngLoopCount(apng(0, true)));
    }

    @Test
    void apngWithPlayCountReturnsIt() {
        assertEquals(2, AnimatedImageLoopCountReader.readApngLoopCount(apng(2, true)));
    }

    @Test
    void pngWithoutAnimationControlLoopsForever() {
        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readApngLoopCount(apng(-1, true)));
    }

    @Test
    void apngAnimationControlAfterImageDataGetsIgnored() {
        assertEquals(AnimatedImageLoopCountReader.INFINITE_LOOPS, AnimatedImageLoopCountReader.readApngLoopCount(apng(2, false)));
    }

    @Test
    void apngReadingStopsAtAnimationControl() throws IOException {
        byte[] apng = apng(2, true);
        CountingInputStream in = new CountingInputStream(apng);

        assertEquals(2, AnimatedImageLoopCountReader.readApngLoopCount(in, Long.MAX_VALUE));
        assertTrue(in.consumed < apng.length - 1000, "consumed " + in.consumed + " of " + apng.length + " bytes");
    }

    /**
     * Builds a GIF with a loop extension before or after its image data, or without loop extension if the loop count is negative.
     */
    private static byte[] gif(int loops, boolean loopExtensionAfterImages, int images) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("GIF89a".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[] {1, 0, 1, 0, (byte) 0x80, 0, 0}); // 1x1, global color table with 2 colors
        out.writeBytes(new byte[6]);
        if (!loopExtensionAfterImages) writeGifLoopExtension(out, loops);
        // A comment extension that needs to get skipped
        out.writeBytes(new byte[] {0x21, (byte) 0xFE, 3, 'f', 'm', '!', 0});
        for (int i = 0; i < images; i++) {
            out.writeBytes(new byte[] {0x21, (byte) 0xF9, 4, 0, 10, 0, 0, 0}); // graphic control extension
            out.writeBytes(new byte[] {0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2}); // image descriptor and LZW minimum code size
            out.write(255);
            out.writeBytes(new byte[255]);
            out.writeBytes(new byte[] {2, 0x4C, 0x01, 0});
        }
        if (loopExtensionAfterImages) writeGifLoopExtension(out, loops);
        out.write(0x3B);
        return out.toByteArray();
    }

    private static void writeGifLoopExtension(ByteArrayOutputStream out, int loops) {
        if (loops < 0) return;
        out.writeBytes(new byte[] {0x21, (byte) 0xFF, 11});
        out.writeBytes("NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[] {3, 1, (byte) (loops & 255), (byte) ((loops >> 8) & 255), 0});
    }

    /**
     * Builds a PNG with an animation control chunk before or after its image data, or without one if the play count is negative.
     */
    private static byte[] apng(int plays, boolean animationControlBeforeImageData) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        writePngChunk(out, "IHDR", new byte[13]);
        if (animationControlBeforeImageData) writeAnimationControl(out, plays);
        writePngChunk(out, "IDAT", new byte[2000]);
        if (!animationControlBeforeImageData) writeAnimationControl(out, plays);
        writePngChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void writeAnimationControl(ByteArrayOutputStream out, int plays) {
        if (plays < 0) return;
        writePngChunk(out, "acTL", new byte[] {0, 0, 0, 2, (byte) (plays >>> 24), (byte) (plays >>> 16), (byte) (plays >>> 8), (byte) plays});
    }

    private static void writePngChunk(ByteArrayOutputStream out, String type, byte[] data) {
        int length = data.length;
        out.writeBytes(new byte[] {(byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8), (byte) length});
        out.writeBytes(type.getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(data);
        out.writeBytes(new byte[4]); // CRC
    }

    private static final class CountingInputStream extends InputStream {

        private final InputStream in;
        private long consumed = 0L;

        private CountingInputStream(byte[] data) {
            this.in = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            int value = this.in.read();
            if (value >= 0) this.consumed++;
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = this.in.read(b, off, len);
            if (read > 0) this.consumed += read;
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = this.in.skip(n);
            this.consumed += skipped;
            return skipped;
        }

    }

}
