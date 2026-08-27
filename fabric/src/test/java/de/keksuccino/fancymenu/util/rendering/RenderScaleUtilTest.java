package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderScaleUtilTest {

    private static final float EPSILON = 0.00001F;

    @ParameterizedTest
    @CsvSource({
            "50.5, 1.0, 0.0, 50.0",
            "50.5, 2.0, 0.0, 50.5",
            "50.25, 2.0, 0.5, 50.25",
            "17.375, 4.0, 0.5, 17.375",
            "-4.25, 2.0, 0.0, -4.5"
    })
    void guiCoordinatesAreFlooredInFinalWindowPixelSpace(float coordinate, float renderScale, float renderTranslation, float expected) {
        assertEquals(expected, RenderScaleUtil.snapGuiCoordinateToPixel(coordinate, renderScale, renderTranslation), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void invalidRenderScalesLeaveCoordinatesUnchanged(float renderScale) {
        assertEquals(50.5F, RenderScaleUtil.snapGuiCoordinateToPixel(50.5F, renderScale, 0.0F));
    }

    @Test
    void invalidCoordinatesAndTranslationsRemainStable() {
        assertEquals(Float.NaN, RenderScaleUtil.snapGuiCoordinateToPixel(Float.NaN, 1.0F, 0.0F));
        assertEquals(50.5F, RenderScaleUtil.snapGuiCoordinateToPixel(50.5F, 1.0F, Float.NaN));
    }

}
