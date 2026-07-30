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

}
