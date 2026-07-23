package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WebUtilsTest {

    @Test
    void reportsUnavailableBeforeTheFirstAsynchronousProbeCompletes() {
        assertFalse(WebUtils.isInternetAvailable());
    }
}
