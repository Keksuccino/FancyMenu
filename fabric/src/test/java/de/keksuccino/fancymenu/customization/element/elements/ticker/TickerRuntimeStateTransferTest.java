package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.layout.Layout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickerRuntimeStateTransferTest {

    @AfterEach
    void clearTransferState() {
        TickerRuntimeStateTransfer.clear();
        TickerElementBuilder.clearOncePerSessionItems();
    }

    @Test
    void onMenuLoadCompletionSurvivesImmediateSameIdentifierReplacement() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        source.lastTick = 1234L;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        Object targetScreen = new Object();

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            target.restoreRuntimeState(targetScreen);
        }

        assertTrue(target.ticked);
        assertEquals(1234L, target.lastTick);
        assertFalse(target.suspendedAfterImmediateSameScreenReplacement);
    }

    @Test
    void positiveDelayNormalTickerPreservesItsCadence() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 5000L, "action");
        source.ticked = true;
        source.lastTick = 9876L;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 5000L, "action");
        Object targetScreen = new Object();

        transfer("screen-a", targetScreen, source, target);

        assertTrue(target.ticked);
        assertEquals(9876L, target.lastTick);
        assertFalse(target.suspendedAfterImmediateSameScreenReplacement);
    }

    @Test
    void causalZeroDelayNormalTickerIsSuspendedOnContinuation() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action");
        source.ticked = true;
        source.lastTick = 42L;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action");
        Object targetScreen = new Object();

        transfer("screen-a", targetScreen, source, target);

        assertTrue(target.ticked);
        assertEquals(42L, target.lastTick);
        assertTrue(target.suspendedAfterImmediateSameScreenReplacement);
    }

    @Test
    void stateIsCapturedOnlyWhenAReplacementActuallyStarts() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 1000L, "action");
        source.ticked = true;
        source.lastTick = 1L;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 1000L, "action");
        Object targetScreen = new Object();

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            source.lastTick = 2L;
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            source.lastTick = 3L;
            target.restoreRuntimeState(targetScreen);
        }

        assertEquals(2L, target.lastTick);
    }

    @Test
    void causalAsyncSourceStopsOnlyAfterSuccessfulSameIdentifierBinding() {
        Layout layout = new Layout("screen-a");
        TickerElement unboundSource = ticker(layout, "unbound", TickerElement.TickMode.NORMAL, 0L, "action-a");
        unboundSource.isAsync = true;
        unboundSource.asyncThreadController = new TickerElement.TickerElementThreadController();
        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(unboundSource), unboundSource)) {
        }
        assertFalse(unboundSource.suspendedAfterImmediateSameScreenReplacement);
        assertTrue(unboundSource.asyncThreadController.running);

        TickerElement rejectedSource = ticker(layout, "rejected", TickerElement.TickMode.NORMAL, 0L, "action-b");
        rejectedSource.isAsync = true;
        rejectedSource.asyncThreadController = new TickerElement.TickerElementThreadController();
        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(rejectedSource), rejectedSource)) {
            TickerRuntimeStateTransfer.bindTarget(new Object(), "screen-b");
        }
        assertFalse(rejectedSource.suspendedAfterImmediateSameScreenReplacement);
        assertTrue(rejectedSource.asyncThreadController.running);

        TickerElement boundSource = ticker(layout, "bound", TickerElement.TickMode.NORMAL, 0L, "action-c");
        boundSource.isAsync = true;
        boundSource.asyncThreadController = new TickerElement.TickerElementThreadController();
        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(boundSource), boundSource)) {
            TickerRuntimeStateTransfer.bindTarget(new Object(), "screen-a");
        }
        assertTrue(boundSource.suspendedAfterImmediateSameScreenReplacement);
        assertFalse(boundSource.asyncThreadController.running);
    }

    @Test
    void suspendedAsyncTargetDoesNotStartOrKeepAController() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action");
        source.isAsync = true;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action");
        target.isAsync = true;
        Object targetScreen = new Object();

        assertTrue(target.shouldStartAsyncThread());
        target.asyncThreadController = new TickerElement.TickerElementThreadController();
        transfer("screen-a", targetScreen, source, target);

        assertTrue(target.suspendedAfterImmediateSameScreenReplacement);
        assertFalse(target.shouldStartAsyncThread());
        assertFalse(target.asyncThreadController.running);
    }

    @Test
    void oncePerSessionTickerRetainsGlobalModeSemanticsWithoutSuspension() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ONCE_PER_SESSION, 0L, "action");
        source.ticked = true;
        source.lastTick = 42L;
        TickerElement target = ticker(layout, "ticker", TickerElement.TickMode.ONCE_PER_SESSION, 0L, "action");
        Object targetScreen = new Object();

        transfer("screen-a", targetScreen, source, target);

        assertTrue(target.ticked);
        assertEquals(42L, target.lastTick);
        assertFalse(target.suspendedAfterImmediateSameScreenReplacement);
    }

    @Test
    void oncePerSessionAdmissionIsAtomicAcrossTickerThreads() throws Exception {
        int threadCount = 16;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (TickerElementBuilder.tryMarkOncePerSessionItem("ticker")) {
                        admitted.incrementAndGet();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        ready.await();
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, admitted.get());
        assertEquals(List.of("ticker"), TickerElementBuilder.cachedOncePerSessionItems);
    }

    @Test
    void differentIdentifierAndLaterReentryDoNotReceiveContinuation() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        Object rejectedTargetScreen = new Object();
        TickerElement rejectedTarget = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(rejectedTargetScreen, "screen-b");
            rejectedTarget.restoreRuntimeState(rejectedTargetScreen);
        }

        Object laterTargetScreen = new Object();
        TickerElement laterTarget = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        laterTarget.restoreRuntimeState(laterTargetScreen);

        assertFalse(rejectedTarget.ticked);
        assertFalse(laterTarget.ticked);
        assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
    }

    @Test
    void continuationIsBoundToExactTargetIdentity() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        EqualTarget boundTarget = new EqualTarget();
        EqualTarget equalButDifferentTarget = new EqualTarget();
        TickerElement wrongTargetTicker = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        TickerElement boundTargetTicker = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(boundTarget, "screen-a");
            wrongTargetTicker.restoreRuntimeState(equalButDifferentTarget);
            boundTargetTicker.restoreRuntimeState(boundTarget);
        }

        assertFalse(wrongTargetTicker.ticked);
        assertTrue(boundTargetTicker.ticked);
    }

    @Test
    void layoutAndTickerConfigurationArePartOfRuntimeIdentity() {
        Layout sourceLayout = new Layout("screen-a");
        Layout reloadedLayout = new Layout("screen-a");
        assertNotEquals(sourceLayout.runtimeLayoutIdentifier, reloadedLayout.runtimeLayoutIdentifier);
        TickerElement source = ticker(sourceLayout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        Object targetScreen = new Object();
        TickerElement asyncMismatch = ticker(sourceLayout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        asyncMismatch.isAsync = true;
        List<TickerElement> mismatches = List.of(ticker(reloadedLayout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action"), ticker(sourceLayout, "other-ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action"), ticker(sourceLayout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action"), ticker(sourceLayout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 1L, "action"), ticker(sourceLayout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "other-action"), asyncMismatch);

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            mismatches.forEach(ticker -> ticker.restoreRuntimeState(targetScreen));
        }

        mismatches.forEach(ticker -> assertFalse(ticker.ticked));
    }

    @Test
    void multipleTickersAndLayoutsTransferIndependently() {
        Layout firstLayout = new Layout("screen-a");
        Layout secondLayout = new Layout("screen-a");
        TickerElement causal = ticker(firstLayout, "causal", TickerElement.TickMode.NORMAL, 0L, "action-a");
        causal.ticked = true;
        causal.lastTick = 10L;
        TickerElement noncausalZero = ticker(firstLayout, "noncausal", TickerElement.TickMode.NORMAL, 0L, "action-b");
        noncausalZero.ticked = true;
        noncausalZero.lastTick = 20L;
        TickerElement delayedOtherLayout = ticker(secondLayout, "delayed", TickerElement.TickMode.NORMAL, 1000L, "action-c");
        delayedOtherLayout.ticked = true;
        delayedOtherLayout.lastTick = 30L;
        TickerElement causalTarget = ticker(firstLayout, "causal", TickerElement.TickMode.NORMAL, 0L, "action-a");
        TickerElement noncausalTarget = ticker(firstLayout, "noncausal", TickerElement.TickMode.NORMAL, 0L, "action-b");
        TickerElement delayedTarget = ticker(secondLayout, "delayed", TickerElement.TickMode.NORMAL, 1000L, "action-c");
        Object targetScreen = new Object();

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(causal, noncausalZero, delayedOtherLayout), causal)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            causalTarget.restoreRuntimeState(targetScreen);
            noncausalTarget.restoreRuntimeState(targetScreen);
            delayedTarget.restoreRuntimeState(targetScreen);
        }

        assertEquals(10L, causalTarget.lastTick);
        assertTrue(causalTarget.suspendedAfterImmediateSameScreenReplacement);
        assertEquals(20L, noncausalTarget.lastTick);
        assertFalse(noncausalTarget.suspendedAfterImmediateSameScreenReplacement);
        assertEquals(30L, delayedTarget.lastTick);
        assertFalse(delayedTarget.suspendedAfterImmediateSameScreenReplacement);
    }

    @Test
    void nestedScopesRestoreOnlyTheirOwnTargets() {
        Layout layout = new Layout("screen-a");
        TickerElement outerSource = ticker(layout, "outer", TickerElement.TickMode.ON_MENU_LOAD, 0L, "outer-action");
        outerSource.ticked = true;
        outerSource.lastTick = 11L;
        TickerElement innerSource = ticker(layout, "inner", TickerElement.TickMode.ON_MENU_LOAD, 0L, "inner-action");
        innerSource.ticked = true;
        innerSource.lastTick = 22L;
        TickerElement outerTarget = ticker(layout, "outer", TickerElement.TickMode.ON_MENU_LOAD, 0L, "outer-action");
        TickerElement innerTarget = ticker(layout, "inner", TickerElement.TickMode.ON_MENU_LOAD, 0L, "inner-action");
        Object sharedTargetScreen = new Object();

        try (TickerRuntimeStateTransfer.ExecutionScope outer = TickerRuntimeStateTransfer.begin("screen-a", List.of(outerSource), outerSource)) {
            TickerRuntimeStateTransfer.bindTarget(sharedTargetScreen, "screen-a");
            try (TickerRuntimeStateTransfer.ExecutionScope inner = TickerRuntimeStateTransfer.begin("screen-a", List.of(innerSource), innerSource)) {
                TickerRuntimeStateTransfer.bindTarget(sharedTargetScreen, "screen-a");
                innerTarget.restoreRuntimeState(sharedTargetScreen);
                TickerRuntimeStateTransfer.finishInitialization(sharedTargetScreen);
            }
            outerTarget.restoreRuntimeState(sharedTargetScreen);
        }

        assertEquals(11L, outerTarget.lastTick);
        assertEquals(22L, innerTarget.lastTick);
        assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
    }

    @Test
    void executionContextsAreIsolatedByThread() throws Exception {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        Object targetScreen = new Object();
        TickerElement foreignThreadTarget = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        TickerElement owningThreadTarget = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            Thread foreignThread = new Thread(() -> {
                try {
                    foreignThreadTarget.restoreRuntimeState(targetScreen);
                    assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            });
            foreignThread.start();
            foreignThread.join();
            owningThreadTarget.restoreRuntimeState(targetScreen);
        }

        assertNull(failure.get());
        assertFalse(foreignThreadTarget.ticked);
        assertTrue(owningThreadTarget.ticked);
    }

    @Test
    void completionAndScopeCloseDiscardUnconsumedState() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        Object completedTarget = new Object();
        Object abandonedTarget = new Object();

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(completedTarget, "screen-a");
            TickerRuntimeStateTransfer.finishInitialization(completedTarget);
            TickerRuntimeStateTransfer.bindTarget(abandonedTarget, "screen-a");
            assertEquals(1, TickerRuntimeStateTransfer.pendingTargetCount());
        }

        assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
    }

    @Test
    void actionFailureAndRepeatedCloseCannotLeakContinuationState() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        Object targetScreen = new Object();
        TickerRuntimeStateTransfer.ExecutionScope scope = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source);

        assertThrows(IllegalStateException.class, () -> {
            try (scope) {
                TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
                throw new IllegalStateException("action failed");
            }
        });
        scope.close();

        assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
        TickerElement laterTarget = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        laterTarget.restoreRuntimeState(targetScreen);
        assertFalse(laterTarget.ticked);
    }

    @Test
    void completedTargetIsNotRepopulatedWithoutAnotherStartingBind() {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        source.ticked = true;
        Object targetScreen = new Object();
        TickerElement firstInitialization = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        TickerElement discoveryRebuild = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");
        TickerElement explicitSecondInitialization = ticker(layout, "ticker", TickerElement.TickMode.ON_MENU_LOAD, 0L, "action");

        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            firstInitialization.restoreRuntimeState(targetScreen);
            TickerRuntimeStateTransfer.finishInitialization(targetScreen);
            discoveryRebuild.restoreRuntimeState(targetScreen);
            TickerRuntimeStateTransfer.bindTarget(targetScreen, "screen-a");
            explicitSecondInitialization.restoreRuntimeState(targetScreen);
        }

        assertTrue(firstInitialization.ticked);
        assertFalse(discoveryRebuild.ticked);
        assertTrue(explicitSecondInitialization.ticked);
    }

    @Test
    void reloadGenerationInvalidatesScopeOnAnotherThread() throws Exception {
        Layout layout = new Layout("screen-a");
        TickerElement source = ticker(layout, "ticker", TickerElement.TickMode.NORMAL, 0L, "action");
        source.isAsync = true;
        source.asyncThreadController = new TickerElement.TickerElementThreadController();
        CountDownLatch scopeStarted = new CountDownLatch(1);
        CountDownLatch reloadCompleted = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin("screen-a", List.of(source), source)) {
                scopeStarted.countDown();
                reloadCompleted.await();
                TickerRuntimeStateTransfer.bindTarget(new Object(), "screen-a");
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        thread.start();
        scopeStarted.await();
        TickerRuntimeStateTransfer.clear();
        reloadCompleted.countDown();
        thread.join();

        assertNull(failure.get());
        assertEquals(0, TickerRuntimeStateTransfer.pendingTargetCount());
        assertFalse(source.suspendedAfterImmediateSameScreenReplacement);
        assertTrue(source.asyncThreadController.running);
    }

    private static void transfer(String screenIdentifier, Object targetScreen, TickerElement source, TickerElement target) {
        try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin(screenIdentifier, List.of(source), source)) {
            TickerRuntimeStateTransfer.bindTarget(targetScreen, screenIdentifier);
            target.restoreRuntimeState(targetScreen);
        }
    }

    private static TickerElement ticker(Layout layout, String identifier, TickerElement.TickMode tickMode, long delay, String actionIdentifier) {
        TickerElement ticker = new TickerElementBuilder().buildDefaultInstance();
        ticker.setParentLayout(layout);
        ticker.setInstanceIdentifier(identifier);
        ticker.tickMode = tickMode;
        ticker.tickDelayMs.set(delay);
        ticker.actionExecutor.identifier = actionIdentifier;
        return ticker;
    }

    private static final class EqualTarget {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EqualTarget;
        }

        @Override
        public int hashCode() {
            return 1;
        }

    }

}
