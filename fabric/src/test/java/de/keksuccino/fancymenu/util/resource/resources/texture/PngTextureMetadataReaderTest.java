package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PngTextureMetadataReaderTest {

    @Test
    void closesInputAndTemporaryImageAfterSuccessfulRead() throws IOException {
        TrackingInputStream input = new TrackingInputStream(null);
        NativeImage image = new NativeImage(3, 2, true);

        try {
            PngTextureMetadataReader.Dimensions dimensions = PngTextureMetadataReader.read(() -> input, ignored -> image);

            assertEquals(3, dimensions.width());
            assertEquals(2, dimensions.height());
            assertEquals(1, input.closeCalls);
            assertThrows(IllegalStateException.class, () -> image.getPixelRGBA(0, 0));
        } finally {
            image.close();
        }
    }

    @Test
    void closesInputWhenDecoderFails() {
        TrackingInputStream input = new TrackingInputStream(null);
        IOException decodeFailure = new IOException("expected decode failure");

        IOException thrown = assertThrows(IOException.class, () -> PngTextureMetadataReader.read(() -> input, ignored -> {
            throw decodeFailure;
        }));

        assertSame(decodeFailure, thrown);
        assertEquals(1, input.closeCalls);
    }

    @Test
    void preservesDecodeFailureWhenInputCloseAlsoFails() {
        IOException decodeFailure = new IOException("expected decode failure");
        IOException closeFailure = new IOException("expected close failure");
        TrackingInputStream input = new TrackingInputStream(closeFailure);

        IOException thrown = assertThrows(IOException.class, () -> PngTextureMetadataReader.read(() -> input, ignored -> {
            throw decodeFailure;
        }));

        assertSame(decodeFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(closeFailure, thrown.getSuppressed()[0]);
        assertEquals(1, input.closeCalls);
    }

    @Test
    void closesTemporaryImageBeforePropagatingInputCloseFailure() {
        IOException closeFailure = new IOException("expected close failure");
        TrackingInputStream input = new TrackingInputStream(closeFailure);
        NativeImage image = new NativeImage(1, 1, true);

        try {
            IOException thrown = assertThrows(IOException.class, () -> PngTextureMetadataReader.read(() -> input, ignored -> image));

            assertSame(closeFailure, thrown);
            assertEquals(1, input.closeCalls);
            assertThrows(IllegalStateException.class, () -> image.getPixelRGBA(0, 0));
        } finally {
            image.close();
        }
    }

    private static final class TrackingInputStream extends InputStream {

        private final IOException closeFailure;
        private int closeCalls;

        private TrackingInputStream(IOException closeFailure) {
            this.closeFailure = closeFailure;
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() throws IOException {
            this.closeCalls++;
            if (this.closeFailure != null) throw this.closeFailure;
        }

    }

}
