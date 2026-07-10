package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LayerScopedTemplateCacheTest {

    @Test
    void isolatesInterleavedLayerScopesByIdentityWithinCacheWindow() {
        AtomicLong clock = new AtomicLong(1_000L);
        LayerScopedTemplateCache<EqualScope, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        EqualScope layerA = new EqualScope();
        EqualScope layerB = new EqualScope();
        AtomicInteger layerAResolutions = new AtomicInteger();
        AtomicInteger layerBResolutions = new AtomicInteger();

        assertEquals("template-a", cache.resolve(layerA, false, () -> {
            layerAResolutions.incrementAndGet();
            return "template-a";
        }));
        assertEquals("template-b", cache.resolve(layerB, false, () -> {
            layerBResolutions.incrementAndGet();
            return "template-b";
        }));
        assertEquals("template-a", cache.resolve(layerA, false, () -> "wrong-a"));
        assertEquals("template-b", cache.resolve(layerB, false, () -> "wrong-b"));
        assertEquals(1, layerAResolutions.get());
        assertEquals(1, layerBResolutions.get());
    }

    @Test
    void keepsButtonAndSliderResultsSeparateWithinOneLayer() {
        AtomicLong clock = new AtomicLong(2_000L);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object layer = new Object();

        assertEquals("button-template", cache.resolve(layer, false, () -> "button-template"));
        assertEquals("slider-template", cache.resolve(layer, true, () -> "slider-template"));
        assertEquals("button-template", cache.resolve(layer, false, () -> "wrong-button"));
        assertEquals("slider-template", cache.resolve(layer, true, () -> "wrong-slider"));
    }

    @Test
    void cachesNullWithoutContaminatingAnotherLayer() {
        AtomicLong clock = new AtomicLong(3_000L);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object layerWithoutTemplate = new Object();
        Object layerWithTemplate = new Object();
        AtomicInteger nullLayerResolutions = new AtomicInteger();

        assertNull(cache.resolve(layerWithoutTemplate, false, () -> {
            nullLayerResolutions.incrementAndGet();
            return null;
        }));
        assertEquals("template", cache.resolve(layerWithTemplate, false, () -> "template"));
        assertNull(cache.resolve(layerWithoutTemplate, false, () -> {
            nullLayerResolutions.incrementAndGet();
            return "unexpected";
        }));
        assertEquals("template", cache.resolve(layerWithTemplate, false, () -> "wrong"));
        assertEquals(1, nullLayerResolutions.get());
    }

    @Test
    void refreshesEachLayerIndependentlyAtCacheExpiry() {
        AtomicLong clock = new AtomicLong(4_000L);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object layerA = new Object();
        Object layerB = new Object();

        assertEquals("a-1", cache.resolve(layerA, false, () -> "a-1"));
        assertEquals("b-1", cache.resolve(layerB, false, () -> "b-1"));
        clock.addAndGet(99L);
        assertEquals("a-1", cache.resolve(layerA, false, () -> "a-too-early"));
        assertEquals("b-1", cache.resolve(layerB, false, () -> "b-too-early"));
        clock.incrementAndGet();
        assertEquals("a-2", cache.resolve(layerA, false, () -> "a-2"));
        assertEquals("b-2", cache.resolve(layerB, false, () -> "b-2"));
    }

    @Test
    void invalidatesClockRollbackAndUsesTheRolledBackTimeAsTheNewBaseline() {
        AtomicLong clock = new AtomicLong(5_000L);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object layer = new Object();
        cache.resolve(layer, false, () -> "before-rollback");

        clock.set(4_999L);

        assertEquals("after-rollback", cache.resolve(layer, false, () -> "after-rollback"));
        assertEquals("after-rollback", cache.resolve(layer, false, () -> "unexpected-refresh"));
    }

    @Test
    void handlesElapsedTimeOverflowAndStillCachesNearLongMaximum() {
        AtomicLong clock = new AtomicLong(Long.MIN_VALUE);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object overflowLayer = new Object();
        cache.resolve(overflowLayer, false, () -> "extremely-old");

        clock.set(Long.MAX_VALUE);

        assertEquals("refreshed", cache.resolve(overflowLayer, false, () -> "refreshed"));

        Object nearMaximumLayer = new Object();
        clock.set(Long.MAX_VALUE - 50L);
        cache.resolve(nearMaximumLayer, false, () -> "near-maximum");
        clock.set(Long.MAX_VALUE);

        assertEquals("near-maximum", cache.resolve(nearMaximumLayer, false, () -> "unexpected-refresh"));
    }

    @Test
    void invalidatesOnlyTheTargetIdentityScope() {
        AtomicLong clock = new AtomicLong(6_000L);
        LayerScopedTemplateCache<EqualScope, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        EqualScope layerA = new EqualScope();
        EqualScope layerB = new EqualScope();
        cache.resolve(layerA, false, () -> "a-1");
        cache.resolve(layerA, true, () -> "a-slider-1");
        cache.resolve(layerB, false, () -> "b-1");

        cache.invalidate(layerA);

        assertEquals("a-2", cache.resolve(layerA, false, () -> "a-2"));
        assertEquals("a-slider-2", cache.resolve(layerA, true, () -> "a-slider-2"));
        assertEquals("b-1", cache.resolve(layerB, false, () -> "unexpected-b-refresh"));
    }

    @Test
    void clearInvalidatesAllScopesAndTemplateTypes() {
        AtomicLong clock = new AtomicLong(7_000L);
        LayerScopedTemplateCache<Object, String> cache = new LayerScopedTemplateCache<>(100L, clock::get);
        Object layer = new Object();
        cache.resolve(layer, false, () -> "button-1");
        cache.resolve(layer, true, () -> "slider-1");

        cache.clear();

        assertEquals("button-2", cache.resolve(layer, false, () -> "button-2"));
        assertEquals("slider-2", cache.resolve(layer, true, () -> "slider-2"));
    }

    private static final class EqualScope {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualScope;
        }

        @Override
        public int hashCode() {
            return 1;
        }

    }

}
