package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay.StringLightsPosition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringLightsOverlayTest {

    @Test
    void autoBulbCountFollowsStringLength() {
        assertAll(() -> assertEquals(10, StringLightsOverlay.resolveBulbCount(StringLightsOverlay.AUTO_BULB_COUNT, 300.0F, 30.0F, 1.0F)), () -> assertEquals(3, StringLightsOverlay.resolveBulbCount(StringLightsOverlay.AUTO_BULB_COUNT, 20.0F, 30.0F, 1.0F)), () -> assertEquals(36, StringLightsOverlay.resolveBulbCount(StringLightsOverlay.AUTO_BULB_COUNT, 5000.0F, 30.0F, 1.0F)), () -> assertEquals(0, StringLightsOverlay.resolveBulbCount(StringLightsOverlay.AUTO_BULB_COUNT, 0.0F, 30.0F, 1.0F)));
    }

    @Test
    void customBulbCountIgnoresStringLength() {
        assertAll(() -> assertEquals(5, StringLightsOverlay.resolveBulbCount(5, 1000.0F, 30.0F, 1.0F)), () -> assertEquals(1, StringLightsOverlay.resolveBulbCount(1, 0.0F, 30.0F, 1.0F)), () -> assertEquals(80, StringLightsOverlay.resolveBulbCount(80, 20.0F, 30.0F, 1.0F)), () -> assertEquals(StringLightsOverlay.MAX_CUSTOM_BULB_COUNT, StringLightsOverlay.resolveBulbCount(StringLightsOverlay.MAX_CUSTOM_BULB_COUNT + 1, 300.0F, 30.0F, 1.0F)));
    }

    @Test
    void patternEntriesRepeatAlongTheString() {
        List<Integer> pattern = Arrays.asList(1, null, 3);
        assertAll(() -> assertEquals(1, StringLightsOverlay.resolvePatternEntry(pattern, 0)), () -> assertNull(StringLightsOverlay.resolvePatternEntry(pattern, 1)), () -> assertEquals(3, StringLightsOverlay.resolvePatternEntry(pattern, 2)), () -> assertEquals(1, StringLightsOverlay.resolvePatternEntry(pattern, 3)), () -> assertEquals(3, StringLightsOverlay.resolvePatternEntry(pattern, 8)), () -> assertNull(StringLightsOverlay.resolvePatternEntry(List.of(), 4)));
    }

    @Test
    void bulbCountSetterClampsToSupportedRange() {
        StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
        overlay.setPositionBulbCount(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, -5);
        overlay.setPositionBulbCount(StringLightsPosition.LOOSE_LEFT_TOP, 500);
        overlay.setPositionBulbCount(StringLightsPosition.LOOSE_RIGHT_TOP, 42);

        assertAll(() -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, overlay.getPositionBulbCount(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(StringLightsOverlay.MAX_CUSTOM_BULB_COUNT, overlay.getPositionBulbCount(StringLightsPosition.LOOSE_LEFT_TOP)), () -> assertEquals(42, overlay.getPositionBulbCount(StringLightsPosition.LOOSE_RIGHT_TOP)), () -> assertEquals(StringLightsOverlay.AUTO_BULB_COUNT, overlay.getPositionBulbCount(StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER)));
    }

    @Test
    void bulbSizeSetterSanitizesEntries() {
        StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
        overlay.setPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, Arrays.asList(20.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY, null, 1.5F));

        List<Float> sizes = overlay.getPositionBulbSizes(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        assertAll(() -> assertEquals(Arrays.asList(StringLightsOverlay.MAX_BULB_SIZE_MULTIPLIER, StringLightsOverlay.MIN_BULB_SIZE_MULTIPLIER, null, null, null, 1.5F), sizes), () -> assertThrows(UnsupportedOperationException.class, () -> sizes.add(1.0F)), () -> assertEquals(List.of(), overlay.getPositionBulbSizes(StringLightsPosition.LOOSE_LEFT_TOP)));
    }

    @Test
    void bulbColorSetterCopiesEntries() {
        StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
        List<Integer> colors = Arrays.asList(0xFFFF0000, null, 0x8000FF00);
        overlay.setPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, colors);
        colors.set(0, 0xFF0000FF);

        List<Integer> stored = overlay.getPositionBulbColors(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT);
        assertAll(() -> assertEquals(Arrays.asList(0xFFFF0000, null, 0x8000FF00), stored), () -> assertThrows(UnsupportedOperationException.class, () -> stored.add(0xFFFFFFFF)), () -> assertEquals(List.of(), overlay.getPositionBulbColors(StringLightsPosition.LOOSE_LEFT_TOP)));
    }

    @Test
    void stringColorDefaultsToTranslucentDarkGrayPerPosition() {
        StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
        overlay.setPositionStringColor(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, 0x00FFFFFF);

        assertAll(() -> assertEquals(0x00FFFFFF, overlay.getPositionStringColor(StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT)), () -> assertEquals(StringLightsOverlay.DEFAULT_STRING_COLOR, overlay.getPositionStringColor(StringLightsPosition.LOOSE_LEFT_TOP)));
    }

}
