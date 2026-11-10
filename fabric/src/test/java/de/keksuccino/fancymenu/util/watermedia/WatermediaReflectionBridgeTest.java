package de.keksuccino.fancymenu.util.watermedia;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaReflectionBridgeTest {

    @Test
    void createMrlReturnsNullInsteadOfThrowingBeforeWatermediaStarted() {
        // PlayerAPI#mrl throws until WaterMedia#start ran, which never happens in unit tests. Malformed input keeps the call free of network work in case it ever gets through.
        assertNull(WatermediaReflectionBridge.createMrl("https://["));
    }

    @Test
    void decodesImageThroughWatermediaCodecsApi() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

        Object image = WatermediaReflectionBridge.decodeImage(png);
        assertNotNull(image);
        assertEquals(1, WatermediaReflectionBridge.imageWidth(image));
        assertEquals(1, WatermediaReflectionBridge.imageHeight(image));
        ByteBuffer[] frames = WatermediaReflectionBridge.imageFrames(image);
        assertNotNull(frames);
        assertEquals(1, frames.length);
        assertTrue(frames[0].hasRemaining());
        long[] delays = WatermediaReflectionBridge.imageDelay(image);
        assertNotNull(delays);
        assertEquals(1, delays.length);
    }

}
