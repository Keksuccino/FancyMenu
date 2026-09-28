package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackFontProviderTest {

    @Test
    void successfulFontIsLoadedLazilyAndReused() {
        Object font = new Object();
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return font;
        });

        assertEquals(0, attempts.get());
        assertFalse(provider.shouldUseFallback(false));
        assertSame(font, provider.get());
        assertFalse(provider.shouldUseFallback(false));
        assertSame(font, provider.get());
        assertEquals(1, attempts.get());
    }

    @Test
    void firstFailedLoadSelectsFallbackAndIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return null;
        });

        assertTrue(provider.shouldUseFallback(false));
        for (int i = 0; i < 100; i++) {
            assertNull(provider.get());
            assertTrue(provider.shouldUseFallback(false));
        }
        assertEquals(1, attempts.get());
    }

    @Test
    void explicitFallbackPreferenceSkipsFontInitialization() {
        AtomicInteger attempts = new AtomicInteger();
        Object font = new Object();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return font;
        });

        assertTrue(provider.shouldUseFallback(true));
        provider.clear();
        assertTrue(provider.shouldUseFallback(true));
        assertEquals(0, attempts.get());
        assertFalse(provider.shouldUseFallback(false));
        assertSame(font, provider.get());
        assertEquals(1, attempts.get());
    }

    @Test
    void preferenceChangesPreserveSuccessfullyLoadedFont() {
        Object font = new Object();
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return font;
        });

        assertSame(font, provider.get());
        assertTrue(provider.shouldUseFallback(true));
        assertFalse(provider.shouldUseFallback(false));
        assertSame(font, provider.get());
        assertEquals(1, attempts.get());
    }

    @Test
    void reloadAllowsRecoveryFromFailureWithoutChangingPreference() {
        Object font = new Object();
        AtomicReference<Object> availableFont = new AtomicReference<>();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(availableFont::get);

        assertTrue(provider.shouldUseFallback(false));
        availableFont.set(font);
        assertTrue(provider.shouldUseFallback(true));
        assertTrue(provider.shouldUseFallback(false));
        provider.clear();
        assertFalse(provider.shouldUseFallback(false));
        assertSame(font, provider.get());
    }

    @Test
    void repeatedReloadFailuresAreAttemptedOncePerReload() {
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return null;
        });

        for (int reload = 0; reload < 3; reload++) {
            provider.clear();
            assertEquals(reload, attempts.get());
            assertTrue(provider.shouldUseFallback(false));
            assertNull(provider.get());
            assertEquals(reload + 1, attempts.get());
        }
    }

    @Test
    void reloadDiscardsPreviousFontAndCanFallBackOrLoadItsReplacement() {
        Object firstFont = new Object();
        Object replacementFont = new Object();
        AtomicReference<Object> availableFont = new AtomicReference<>(firstFont);
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(availableFont::get);

        assertSame(firstFont, provider.get());
        availableFont.set(null);
        provider.clear();
        assertTrue(provider.shouldUseFallback(false));
        assertNull(provider.get());
        availableFont.set(replacementFont);
        provider.clear();
        assertFalse(provider.shouldUseFallback(false));
        assertSame(replacementFont, provider.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentRequestsShareOneLoadAttempt(boolean available) throws Exception {
        Object font = available ? new Object() : null;
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            attempts.incrementAndGet();
            return font;
        });

        for (Object result : ConcurrentTestCalls.invoke(8, provider::get)) {
            assertSame(font, result);
        }
        assertEquals(1, attempts.get());
        assertEquals(!available, provider.shouldUseFallback(false));
    }

    @Test
    void unexpectedLoaderExceptionPropagatesWithoutPublishingAResult() {
        Object font = new Object();
        IllegalStateException failure = new IllegalStateException();
        AtomicInteger attempts = new AtomicInteger();
        FallbackFontProvider<Object> provider = new FallbackFontProvider<>(() -> {
            if (attempts.incrementAndGet() == 1) throw failure;
            return font;
        });

        assertSame(failure, assertThrows(IllegalStateException.class, provider::get));
        assertSame(font, provider.get());
        assertEquals(2, attempts.get());
    }

}
