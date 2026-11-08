package de.keksuccino.fancymenu.util.rendering.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UIScaleTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, 1.25",
            "1920, 1080, 1.25",
            "2399, 1299, 1.25",
            "2400, 1080, 1.5",
            "1920, 1300, 1.5",
            "2560, 1440, 1.5",
            "3000, 1700, 1.5",
            "3001, 1080, 2.0",
            "1920, 1701, 2.0",
            "3200, 1900, 2.0",
            "3201, 1080, 2.5",
            "1920, 1901, 2.5",
            "3840, 2160, 2.5",
            "5120, 2880, 2.5"
    })
    void autoScaleFollowsWindowFramebufferSize(int windowWidth, int windowHeight, float expectedScale) {
        assertEquals(expectedScale, UIScale.getAutoScaleForWindow(windowWidth, windowHeight));
    }

}
