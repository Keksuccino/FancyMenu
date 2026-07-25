package de.keksuccino.fancymenu.util.watermedia;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaReflectionBridgeTest {

    @Test
    void createsErrorMrlThroughWatermediaV22StringFactory() {
        // Malformed input is handled synchronously by MediaAPI#mrl and cannot start network or native media work.
        Object mrl = WatermediaReflectionBridge.createMrl("https://[");

        assertNotNull(mrl);
        assertEquals("ERROR", WatermediaReflectionBridge.mrlStatusName(mrl));
        assertTrue(WatermediaReflectionBridge.isMrlFailed(mrl));
    }

    @Test
    void decodesImageThroughWatermediaV22CodecsApi() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

        Object image = WatermediaReflectionBridge.decodeImage(png);
        assertNotNull(image);
        assertEquals(1, WatermediaReflectionBridge.imageWidth(image));
        assertEquals(1, WatermediaReflectionBridge.imageHeight(image));
        ByteBuffer[] frames = WatermediaReflectionBridge.imageFrames(image);
        assertNotNull(frames);
        assertEquals(1, frames.length);
        assertTrue(frames[0].hasRemaining());
    }

    @Test
    void preservesBackendNeutralTextureHandlesWithoutIntTruncation() {
        long negativeOpaqueHandle = 0xFEDCBA9876543210L;
        long largePositiveHandle = (long) Integer.MAX_VALUE + 42L;

        assertEquals(negativeOpaqueHandle, WatermediaReflectionBridge.playerTextureHandle(new TexturePlayer(negativeOpaqueHandle)));
        assertEquals(largePositiveHandle, WatermediaReflectionBridge.playerTextureHandle(new TexturePlayer(largePositiveHandle)));
        assertEquals(0L, WatermediaReflectionBridge.playerTextureHandle(new TexturePlayer(0L)));
    }

    @Test
    void acceptsOnlyPositiveIntSizedOpenGlTextureNames() {
        assertEquals(0, WatermediaReflectionBridge.openGlTextureId(0L));
        assertEquals(0, WatermediaReflectionBridge.openGlTextureId(-1L));
        assertEquals(1, WatermediaReflectionBridge.openGlTextureId(1L));
        assertEquals(Integer.MAX_VALUE, WatermediaReflectionBridge.openGlTextureId(Integer.MAX_VALUE));
        assertEquals(0, WatermediaReflectionBridge.openGlTextureId((long) Integer.MAX_VALUE + 1L));
    }

    public static final class TexturePlayer {

        private final long texture;

        public TexturePlayer(long texture) {
            this.texture = texture;
        }

        public long texture() {
            return this.texture;
        }

    }

}
