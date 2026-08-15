package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebUtilsTest {

    @Test
    void reportsUnavailableBeforeTheFirstAsynchronousProbeCompletes() {
        assertFalse(WebUtils.isInternetAvailable());
    }

    @Test
    void usesGoogleAsInternetAvailabilityEndpoint() throws ReflectiveOperationException {
        // Keep the production configuration private while verifying the exact endpoint without live network I/O.
        Field endpointField = WebUtils.class.getDeclaredField("INTERNET_AVAILABILITY_ENDPOINT");
        endpointField.setAccessible(true);

        assertEquals(URI.create("https://google.com"), endpointField.get(null));
    }
}
