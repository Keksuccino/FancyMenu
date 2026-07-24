package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorRegistryTest {

    @Test
    void replacementIsPublishedBeforeOldAllocationIsRetired() {
        AtomicReference<CursorRegistry<FakeCursor>> registryReference = new AtomicReference<>();
        AtomicReference<FakeCursor> visibleDuringRetirement = new AtomicReference<>();
        FakeRetirement retirement = new FakeRetirement() {
            @Override
            public boolean markRetired(FakeCursor cursor) {
                if (cursor.name.equals("old")) visibleDuringRetirement.set(registryReference.get().get("cursor"));
                return super.markRetired(cursor);
            }
        };
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        registryReference.set(registry);
        FakeCursor old = new FakeCursor("old", 7L);
        FakeCursor replacement = new FakeCursor("replacement", 8L);
        registry.register("cursor", old, FakeCursor::isLive);

        registry.register("cursor", replacement, FakeCursor::isLive);

        assertSame(replacement, visibleDuringRetirement.get());
        assertSame(replacement, registry.get("cursor"));
        assertEquals(List.of(old), retirement.executed);
    }

    @Test
    void unregisterAndRepeatedRetirementReleaseAllocationOnce() {
        FakeRetirement retirement = new FakeRetirement();
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        FakeCursor cursor = new FakeCursor("cursor", 9L);
        registry.register("cursor", cursor, FakeCursor::isLive);

        assertTrue(registry.unregister("cursor"));
        assertFalse(registry.unregister("cursor"));
        registry.retire(cursor);

        assertNull(registry.get("cursor"));
        assertEquals(List.of(cursor), retirement.executed);
    }

    @Test
    void staleIdentityUnregisterDoesNotRemoveReplacementWithSameNativeValue() {
        FakeRetirement retirement = new FakeRetirement();
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        FakeCursor staleOwner = new FakeCursor("stale", 42L);
        FakeCursor replacement = new FakeCursor("replacement", 42L);
        registry.register("stale-alias", staleOwner, FakeCursor::isLive);
        registry.register("cursor", replacement, FakeCursor::isLive);

        assertFalse(registry.unregister("cursor", staleOwner));

        assertSame(replacement, registry.get("cursor"));
        assertNull(registry.get("stale-alias"));
        assertEquals(List.of(staleOwner), retirement.executed);
        assertTrue(replacement.isLive());
    }

    @Test
    void closeRetiresAliasesOnceAndRejectsLateRegistration() {
        FakeRetirement retirement = new FakeRetirement();
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        FakeCursor shared = new FakeCursor("shared", 1L);
        FakeCursor late = new FakeCursor("late", 2L);
        registry.register("first", shared, FakeCursor::isLive);
        registry.register("second", shared, FakeCursor::isLive);

        registry.close();
        registry.close();
        assertFalse(registry.register("late", late, FakeCursor::isLive));

        assertEquals(List.of(shared, late), retirement.executed);
        assertNull(registry.get("first"));
        assertNull(registry.get("second"));
        assertNull(registry.get("late"));
    }

    @Test
    void nativeRetirementExecutionRunsOutsideRegistryMonitor() {
        AtomicReference<CursorRegistry<FakeCursor>> registryReference = new AtomicReference<>();
        AtomicBoolean executedInsideMonitor = new AtomicBoolean();
        FakeRetirement retirement = new FakeRetirement() {
            @Override
            public void executeRetirement(FakeCursor cursor) {
                executedInsideMonitor.set(Thread.holdsLock(registryReference.get()));
                super.executeRetirement(cursor);
            }
        };
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        registryReference.set(registry);
        FakeCursor cursor = new FakeCursor("cursor", 1L);
        registry.register("cursor", cursor, FakeCursor::isLive);

        registry.unregister("cursor");

        assertFalse(executedInsideMonitor.get());
    }

    @Test
    void concurrentReplacementAndConditionalUnregisterRetireEveryAllocationOnce() throws Exception {
        FakeRetirement retirement = new FakeRetirement();
        CursorRegistry<FakeCursor> registry = new CursorRegistry<>(retirement);
        AtomicInteger ids = new AtomicInteger();

        List<FakeCursor> cursors = ConcurrentTestCalls.invoke(32, () -> {
            FakeCursor cursor = new FakeCursor("cursor-" + ids.incrementAndGet(), 77L);
            registry.register("cursor", cursor, FakeCursor::isLive);
            registry.unregister("cursor", cursor);
            return cursor;
        });
        registry.close();

        assertNull(registry.get("cursor"));
        assertEquals(cursors.size(), retirement.executed.size());
        assertEquals(cursors.size(), Set.copyOf(retirement.executed).size());
        assertTrue(cursors.stream().noneMatch(FakeCursor::isLive));
    }

    private static class FakeRetirement implements CursorRegistry.Retirement<FakeCursor> {

        private final List<FakeCursor> executed = Collections.synchronizedList(new ArrayList<>());

        @Override
        public boolean markRetired(FakeCursor cursor) {
            return cursor.live.compareAndSet(true, false);
        }

        @Override
        public void executeRetirement(FakeCursor cursor) {
            this.executed.add(cursor);
        }

    }

    private static final class FakeCursor {

        private final String name;
        @SuppressWarnings("unused")
        private final long nativeHandle;
        private final AtomicBoolean live = new AtomicBoolean(true);

        private FakeCursor(String name, long nativeHandle) {
            this.name = name;
            this.nativeHandle = nativeHandle;
        }

        private boolean isLive() {
            return this.live.get();
        }

    }

}
