package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("RequirementContainer caching settings")
class RequirementContainerCacheTest {

    private static final long CACHING_DURATION_MILLIS = 100L;
    private static final long CACHING_DURATION_NANOS = TimeUnit.MILLISECONDS.toNanos(CACHING_DURATION_MILLIS);
    /** System.nanoTime() has no fixed origin, so the tests don't start the clock at 0. */
    private static final long START_NANOS = 987_654_321L;

    private final AtomicLong cachingDurationMillis = new AtomicLong(CACHING_DURATION_MILLIS);
    private final AtomicLong nowNanos = new AtomicLong(START_NANOS);
    private RequirementContainer.CachingSettings originalSettings;

    @BeforeEach
    void useTestCachingSettings() {
        this.originalSettings = RequirementContainer.getCachingSettings();
        RequirementContainer.setCachingSettings(new RequirementContainer.CachingSettings(this.cachingDurationMillis::get, this.nowNanos::get));
    }

    @AfterEach
    void restoreCachingSettings() {
        RequirementContainer.setCachingSettings(this.originalSettings);
    }

    @Test
    void resultGetsReusedUntilTheCachingDurationIsOver() {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();

        assertTrue(container.requirementsMet());
        met.set(false);
        this.nowNanos.addAndGet(CACHING_DURATION_NANOS - 1L);
        assertTrue(container.requirementsMet());
        assertEquals(1, requirement.checks());

        // The result is exactly as old as the caching duration
        this.nowNanos.addAndGet(1L);
        assertFalse(container.requirementsMet());
        assertEquals(2, requirement.checks());
    }

    @Test
    void scriptLogicChecksAgainAndItsFreshResultGetsCached() {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();
        assertTrue(container.requirementsMet());

        met.set(false);
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertFalse(container.requirementsMet());
            assertFalse(container.requirementsMet());
        }
        assertEquals(3, requirement.checks());

        // Rendering gets the fresh result of the script from the cache
        met.set(true);
        assertFalse(container.requirementsMet());
        assertEquals(3, requirement.checks());
    }

    @Test
    void scopeOfAnotherThreadDoesNotBypassTheCacheOfThisThread() throws Exception {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();
        assertTrue(container.requirementsMet());
        met.set(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                assertTrue(executor.submit(container::requirementsMet).get(5L, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
        assertEquals(1, requirement.checks());
    }

    @Test
    void withoutCachingEveryCheckChecksAgainAndNothingGetsCachedForLater() {
        this.cachingDurationMillis.set(0L);
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();

        assertFalse(RequirementContainer.isCachingRequirements());
        assertTrue(container.requirementsMet());
        assertTrue(container.requirementsMet());
        assertEquals(2, requirement.checks());

        met.set(false);
        this.cachingDurationMillis.set(CACHING_DURATION_MILLIS);
        assertTrue(RequirementContainer.isCachingRequirements());
        assertFalse(container.requirementsMet());
        assertEquals(3, requirement.checks());
    }

    @Test
    void clearingTheCacheMakesEveryContainerCheckAgain() {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer first = requirement.newContainer();
        RequirementContainer second = requirement.newContainer();
        assertTrue(first.requirementsMet());
        assertTrue(second.requirementsMet());

        met.set(false);
        RequirementContainer.clearCooldownCache();

        assertFalse(first.requirementsMet());
        assertFalse(second.requirementsMet());
        assertEquals(4, requirement.checks());
        // The new results get cached again
        met.set(true);
        assertFalse(first.requirementsMet());
        assertEquals(4, requirement.checks());
    }

    @Test
    void resultFromTheFutureOfAClockThatWentBackwardsGetsCheckedAgain() {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();
        assertTrue(container.requirementsMet());

        met.set(false);
        this.nowNanos.addAndGet(-1L);

        assertFalse(container.requirementsMet());
        assertEquals(2, requirement.checks());
    }

    @Test
    void copiesDoNotShareTheCachedResult() {
        AtomicBoolean met = new AtomicBoolean(true);
        CountingTestRequirement requirement = new CountingTestRequirement(met::get);
        RequirementContainer container = requirement.newContainer();
        assertTrue(container.requirementsMet());

        met.set(false);

        assertFalse(container.copy(false).requirementsMet());
        assertFalse(container.copy(true).requirementsMet());
        assertTrue(container.requirementsMet());
        assertEquals(3, requirement.checks());
    }

    @Test
    void containersThatAreNoLongerUsedLeaveNoCachedResultsBehind() {
        // Layouts create their containers again on every load, so their cached results must go away with the old containers
        int containers = 200_000;
        for (int i = 0; i < 1_000; i++) new RequirementContainer().requirementsMet();
        long usedBefore = usedHeapAfterGc();

        for (int i = 0; i < containers; i++) {
            this.nowNanos.incrementAndGet();
            assertTrue(new RequirementContainer().requirementsMet());
        }
        long usedAfter = usedHeapAfterGc();

        // Even one map entry per container would keep tens of megabytes
        long allowedGrowth = 8L * 1024L * 1024L;
        assertTrue((usedAfter - usedBefore) < allowedGrowth, "Heap grew by " + (usedAfter - usedBefore) + " bytes");
    }

    @Test
    void concurrentChecksClearsAndScriptChecksKeepEveryResultCorrect() throws Exception {
        // Real time and a short duration, so results expire and get replaced while other threads read them
        RequirementContainer.setCachingSettings(new RequirementContainer.CachingSettings(() -> 1L, System::nanoTime));
        CountingTestRequirement metRequirement = new CountingTestRequirement(() -> true);
        CountingTestRequirement notMetRequirement = new CountingTestRequirement(() -> false);
        RequirementContainer met = metRequirement.newContainer();
        RequirementContainer notMet = notMetRequirement.newContainer();
        int iterations = 20_000;
        int threads = 8;

        List<Integer> wrongResults = ConcurrentTestCalls.invoke(threads, () -> {
            int wrong = 0;
            for (int i = 0; i < iterations; i++) {
                if ((i % 1_000) == 0) RequirementContainer.clearCooldownCache();
                if ((i % 2) == 0) {
                    try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                        if (!met.requirementsMet()) wrong++;
                        if (notMet.requirementsMet()) wrong++;
                    }
                } else {
                    if (!met.requirementsMet()) wrong++;
                    if (notMet.requirementsMet()) wrong++;
                }
            }
            return wrong;
        });

        for (int wrong : wrongResults) assertEquals(0, wrong);
        // Every check of script logic checked the requirements, no matter what the other threads cached meanwhile
        int scriptChecks = threads * (iterations / 2);
        assertEquals(scriptChecks, metRequirement.checks() - metRequirement.checksOutsideFreshValuesScope());
        assertEquals(scriptChecks, notMetRequirement.checks() - notMetRequirement.checksOutsideFreshValuesScope());
    }

    private static long usedHeapAfterGc() {
        Runtime runtime = Runtime.getRuntime();
        long used = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            System.gc();
            used = Math.min(used, runtime.totalMemory() - runtime.freeMemory());
        }
        return used;
    }

}
