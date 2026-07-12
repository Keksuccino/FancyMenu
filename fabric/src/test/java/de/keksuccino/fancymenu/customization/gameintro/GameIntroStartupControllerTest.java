package de.keksuccino.fancymenu.customization.gameintro;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameIntroStartupControllerTest {

    @Test
    void alreadyPlayedIntroDelegatesWithoutResolvingOrStartingAResource() {
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger starterCalls = new AtomicInteger();

        boolean started = GameIntroStartupController.tryStartIntro(true, () -> {
            supplierCalls.incrementAndGet();
            return new Object();
        }, intro -> starterCalls.incrementAndGet());

        assertFalse(started);
        assertEquals(0, supplierCalls.get());
        assertEquals(0, starterCalls.get());
    }

    @Test
    void unavailableIntroDelegatesAfterCallingSupplierOnce() {
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger starterCalls = new AtomicInteger();

        boolean started = GameIntroStartupController.tryStartIntro(false, () -> {
            supplierCalls.incrementAndGet();
            return null;
        }, intro -> starterCalls.incrementAndGet());

        assertFalse(started);
        assertEquals(1, supplierCalls.get());
        assertEquals(0, starterCalls.get());
    }

    @Test
    void availableIntroStartsWithoutEagerConsumptionAndCallsSupplierOnce() {
        Object intro = new Object();
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicInteger starterCalls = new AtomicInteger();
        AtomicReference<Object> startedIntro = new AtomicReference<>();

        boolean started = GameIntroStartupController.tryStartIntro(false, () -> {
            supplierCalls.incrementAndGet();
            return intro;
        }, resource -> {
            starterCalls.incrementAndGet();
            startedIntro.set(resource);
        });

        assertTrue(started);
        assertEquals(1, supplierCalls.get());
        assertEquals(1, starterCalls.get());
        assertSame(intro, startedIntro.get());
    }

}
