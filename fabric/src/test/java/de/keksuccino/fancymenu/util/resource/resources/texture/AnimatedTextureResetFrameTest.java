package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnimatedTextureResetFrameTest {

    @Test
    void retainsOwnedFirstFrameAcrossRepeatedRestoreRequests() {
        try (AnimatedTextureResetFrame resetFrame = new AnimatedTextureResetFrame()) {
            resetFrame.requestRestore();
            assertNull(resetFrame.copyForRequestedRestore());

            try (NativeImage source = new NativeImage(2, 1, true)) {
                source.setPixelRGBA(0, 0, 0x11223344);
                source.setPixelRGBA(1, 0, 0x55667788);
                resetFrame.captureIfAbsent(source);
                source.setPixelRGBA(0, 0, 0x00000000);
            }

            resetFrame.requestRestore();
            resetFrame.requestRestore();
            AnimatedTextureResetFrame.RequestedFrame restored = resetFrame.copyForRequestedRestore();
            assertNotNull(restored);
            try (restored; NativeImage restoredImage = restored.takeImage()) {
                assertEquals(0x11223344, restoredImage.getPixelRGBA(0, 0));
                assertEquals(0x55667788, restoredImage.getPixelRGBA(1, 0));
                resetFrame.markRestored(restored.restoreVersion());
            }

            assertNull(resetFrame.copyForRequestedRestore());

            resetFrame.requestRestore();
            AnimatedTextureResetFrame.RequestedFrame restoredAgain = resetFrame.copyForRequestedRestore();
            assertNotNull(restoredAgain);
            try (restoredAgain; NativeImage restoredImage = restoredAgain.takeImage()) {
                assertEquals(0x11223344, restoredImage.getPixelRGBA(0, 0));
                assertEquals(0x55667788, restoredImage.getPixelRGBA(1, 0));
            }

            resetFrame.requestRestore();
            resetFrame.markRestored(restoredAgain.restoreVersion());
            AnimatedTextureResetFrame.RequestedFrame newerRequest = resetFrame.copyForRequestedRestore();
            assertNotNull(newerRequest);
            newerRequest.close();

            resetFrame.clear();
            resetFrame.requestRestore();
            assertNull(resetFrame.copyForRequestedRestore());
        }
    }

}
