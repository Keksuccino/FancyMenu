package de.keksuccino.fancymenu.mixin.support.client;

import de.keksuccino.fancymenu.mixin.support.client.EntityVisibilityRaycastCache.ObserverState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityVisibilityRaycastCacheTest {

    private static final Object MAIN_HAND_ITEM = new Object();
    private static final Object FEET_ITEM = new Object();

    @Test
    void stableTargetsReuseBothVisibilityResultsWithinTheSameTick() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object visibleTarget = new Object();
        Object hiddenTarget = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(world, 10L, observer, observerState(1.0D));

        assertTrue(resolve(cache, visibleTarget, uuid(1), 1, 4.0D, true, raycasts));
        assertFalse(resolve(cache, hiddenTarget, uuid(2), 2, 8.0D, false, raycasts));
        assertTrue(resolve(cache, visibleTarget, uuid(1), 1, 4.0D, false, raycasts));
        assertFalse(resolve(cache, hiddenTarget, uuid(2), 2, 8.0D, true, raycasts));

        assertEquals(2, raycasts.get());
    }

    @Test
    void advancingToTheNextTickExpiresEveryEntry() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object target = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        ObserverState state = observerState(1.0D);
        cache.beginFrame(world, 10L, observer, state);
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        cache.beginFrame(world, 11L, observer, state);
        assertAll(() -> assertEquals(1, cache.entryCount()), () -> assertEquals(0, cache.currentContextEntryCount()));
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        assertAll(() -> assertEquals(2, raycasts.get()), () -> assertEquals(1, cache.entryCount()), () -> assertEquals(1, cache.currentContextEntryCount()));
    }

    @Test
    void tickRollbackClearsEntriesInsteadOfMakingThemLongLived() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object target = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        ObserverState state = observerState(1.0D);
        cache.beginFrame(world, 20L, observer, state);
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        cache.beginFrame(world, 3L, observer, state);
        assertEquals(0, cache.entryCount());
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        assertEquals(2, raycasts.get());
    }

    @Test
    void worldIdentityChangeInvalidatesEvenWhenDimensionLikeStateAndTickMatch() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object observer = new Object();
        Object target = new Object();
        Object firstWorld = new Object();
        Object secondWorld = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        ObserverState state = observerState(1.0D);
        cache.beginFrame(firstWorld, 10L, observer, state);
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        cache.beginFrame(secondWorld, 10L, observer, state);
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        assertEquals(2, raycasts.get());
    }

    @Test
    void observerIdentityAndExactRayOriginChangesInvalidateAllTargets() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object firstObserver = new Object();
        Object target = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(world, 10L, firstObserver, observerState(1.0D));
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        cache.beginFrame(world, 10L, firstObserver, observerState(Math.nextUp(1.0D)));
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);
        cache.beginFrame(world, 10L, new Object(), observerState(Math.nextUp(1.0D)));
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        assertEquals(3, raycasts.get());
    }

    @Test
    void everyVanillaCollisionContextInputParticipatesInTheObserverState() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object target = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        List<ObserverState> states = List.of(new ObserverState(1.0D, 2.0D, 3.0D, 1.5D, false, 0.0D, MAIN_HAND_ITEM, FEET_ITEM), new ObserverState(1.0D, 2.0D, 3.0D, 1.6D, false, 0.0D, MAIN_HAND_ITEM, FEET_ITEM), new ObserverState(1.0D, 2.0D, 3.0D, 1.6D, true, 0.0D, MAIN_HAND_ITEM, FEET_ITEM), new ObserverState(1.0D, 2.0D, 3.0D, 1.6D, true, 2.6D, MAIN_HAND_ITEM, FEET_ITEM), new ObserverState(1.0D, 2.0D, 3.0D, 1.6D, true, 2.6D, new Object(), FEET_ITEM), new ObserverState(1.0D, 2.0D, 3.0D, 1.6D, true, 2.6D, MAIN_HAND_ITEM, new Object()));

        for (ObserverState state : states) {
            cache.beginFrame(world, 10L, observer, state);
            resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);
        }

        assertAll(() -> assertEquals(states.size(), raycasts.get()), () -> assertEquals(1, cache.entryCount()));
    }

    @Test
    void exactTargetMovementInvalidatesOnlyThatTarget() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object movingTarget = new Object();
        Object stableTarget = new Object();
        AtomicInteger movingRaycasts = new AtomicInteger();
        AtomicInteger stableRaycasts = new AtomicInteger();
        cache.beginFrame(world, 10L, observer, observerState(1.0D));
        resolve(cache, movingTarget, uuid(1), 1, 4.0D, true, movingRaycasts);
        resolve(cache, stableTarget, uuid(2), 2, 8.0D, true, stableRaycasts);

        resolve(cache, movingTarget, uuid(1), 1, Math.nextUp(4.0D), true, movingRaycasts);
        resolve(cache, stableTarget, uuid(2), 2, 8.0D, true, stableRaycasts);

        assertAll(() -> assertEquals(2, movingRaycasts.get()), () -> assertEquals(1, stableRaycasts.get()));
    }

    @Test
    void targetUuidNumericIdAndObjectIdentityAreAllValidated() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object world = new Object();
        Object observer = new Object();
        Object firstTarget = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(world, 10L, observer, observerState(1.0D));
        resolve(cache, firstTarget, uuid(1), 1, 4.0D, true, raycasts);

        resolve(cache, firstTarget, uuid(2), 1, 4.0D, true, raycasts);
        resolve(cache, firstTarget, uuid(2), 2, 4.0D, true, raycasts);
        resolve(cache, new Object(), uuid(2), 2, 4.0D, true, raycasts);

        assertEquals(4, raycasts.get());
    }

    @Test
    void capacityOverflowRetainsFutureHitsAcrossTickAndObserverContextChanges() {
        int capacity = 3;
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache(capacity);
        Object world = new Object();
        Object observer = new Object();
        List<Object> targets = new ArrayList<>();
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(world, 10L, observer, observerState(1.0D));
        for (int index = 0; index < capacity + 2; index++) targets.add(new Object());

        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);
        cache.beginFrame(world, 10L, observer, observerState(1.0D));
        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);

        cache.beginFrame(world, 11L, observer, observerState(1.0D));
        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);
        cache.beginFrame(world, 11L, observer, observerState(1.0D));
        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);

        cache.beginFrame(world, 11L, observer, observerState(Math.nextUp(1.0D)));
        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);
        cache.beginFrame(world, 11L, observer, observerState(Math.nextUp(1.0D)));
        for (int index = 0; index < targets.size(); index++) resolve(cache, targets.get(index), uuid(index), index + 1, index + 4.0D, true, raycasts);

        assertAll(() -> assertEquals(capacity, cache.entryCount()), () -> assertEquals(capacity, cache.currentContextEntryCount()), () -> assertEquals(21, raycasts.get()));
    }

    @Test
    void staleCapacityIsReusedForNewTargetsAfterTickAdvance() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache(2);
        Object world = new Object();
        Object observer = new Object();
        Object replacementTarget = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        ObserverState state = observerState(1.0D);
        cache.beginFrame(world, 10L, observer, state);
        resolve(cache, new Object(), uuid(1), 1, 4.0D, true, raycasts);
        resolve(cache, new Object(), uuid(2), 2, 5.0D, true, raycasts);

        cache.beginFrame(world, 11L, observer, state);
        resolve(cache, replacementTarget, uuid(3), 3, 6.0D, true, raycasts);
        resolve(cache, replacementTarget, uuid(3), 3, 6.0D, false, raycasts);

        assertAll(() -> assertEquals(2, cache.entryCount()), () -> assertEquals(1, cache.currentContextEntryCount()), () -> assertEquals(3, raycasts.get()));
    }

    @Test
    void resetClearsEntriesAndFallsBackToAnUncachedRaycastWithoutFrameContext() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object target = new Object();
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(new Object(), 10L, new Object(), observerState(1.0D));
        resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts);

        cache.reset();
        assertTrue(resolve(cache, target, uuid(1), 1, 4.0D, true, raycasts));

        assertAll(() -> assertEquals(0, cache.entryCount()), () -> assertEquals(2, raycasts.get()));
    }

    @Test
    void failedRaycastIsNeverCached() {
        EntityVisibilityRaycastCache cache = new EntityVisibilityRaycastCache();
        Object target = new Object();
        UUID uuid = uuid(1);
        AtomicInteger raycasts = new AtomicInteger();
        cache.beginFrame(new Object(), 10L, new Object(), observerState(1.0D));

        assertNull(cache.getCachedVisibility(target, uuid, 1, 4.0D, 5.0D, 6.0D));
        assertAll(() -> assertEquals(0, cache.entryCount()), () -> assertEquals(0, cache.currentContextEntryCount()));
        assertThrows(IllegalStateException.class, () -> throwRaycastFailure(raycasts));
        assertAll(() -> assertNull(cache.getCachedVisibility(target, uuid, 1, 4.0D, 5.0D, 6.0D)), () -> assertEquals(0, cache.entryCount()), () -> assertEquals(0, cache.currentContextEntryCount()));
        assertTrue(resolve(cache, target, uuid, 1, 4.0D, true, raycasts));
        assertTrue(resolve(cache, target, uuid, 1, 4.0D, false, raycasts));

        assertAll(() -> assertEquals(2, raycasts.get()), () -> assertEquals(1, cache.entryCount()));
    }

    @Test
    void nonPositiveCapacityIsRejected() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> new EntityVisibilityRaycastCache(0)), () -> assertThrows(IllegalArgumentException.class, () -> new EntityVisibilityRaycastCache(-1)));
    }

    private static ObserverState observerState(double rayX) {
        return new ObserverState(rayX, 2.0D, 3.0D, 1.5D, false, 0.0D, MAIN_HAND_ITEM, FEET_ITEM);
    }

    private static boolean resolve(EntityVisibilityRaycastCache cache, Object target, UUID targetUuid, int targetId, double targetX, boolean result, AtomicInteger raycasts) {
        Boolean cached = cache.getCachedVisibility(target, targetUuid, targetId, targetX, 5.0D, 6.0D);
        if (cached != null) return cached;
        raycasts.incrementAndGet();
        cache.store(target, targetUuid, targetId, targetX, 5.0D, 6.0D, result);
        return result;
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }

    private static void throwRaycastFailure(AtomicInteger raycasts) {
        raycasts.incrementAndGet();
        throw new IllegalStateException("raycast failed");
    }

}
