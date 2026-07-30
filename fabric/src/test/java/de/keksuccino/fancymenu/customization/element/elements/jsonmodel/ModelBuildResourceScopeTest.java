package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelBuildResourceScopeTest {

    @Test
    void failedBuildClosesOwnedResourcesInReverseOrderAndContinuesAfterFailure() {
        List<String> order = new ArrayList<>();
        FakeResource first = new FakeResource("first", order, false);
        FakeResource second = new FakeResource("second", order, true);
        ModelBuildResourceScope scope = new ModelBuildResourceScope();
        scope.own(first);
        scope.own(second);

        scope.close();
        scope.close();

        assertEquals(List.of("second", "first"), order);
        assertEquals(1, first.closes.get());
        assertEquals(1, second.closes.get());
    }

    @Test
    void ownershipReplacementClosesOnlyTheWrapper() {
        FakeResource image = new FakeResource("image", null, false);
        FakeResource wrapper = new FakeResource("wrapper", null, false);
        ModelBuildResourceScope scope = new ModelBuildResourceScope();
        scope.own(image);
        scope.replaceOwnership(image, wrapper);

        scope.close();

        assertEquals(0, image.closes.get());
        assertEquals(1, wrapper.closes.get());
    }

    @Test
    void transferredCandidateRemainsOpen() {
        FakeResource candidate = new FakeResource("candidate", null, false);
        FakeResource transferred;
        try (ModelBuildResourceScope scope = new ModelBuildResourceScope()) {
            scope.own(candidate);
            transferred = scope.transfer(candidate);
        }

        assertSame(candidate, transferred);
        assertEquals(0, candidate.closes.get());
    }

    @Test
    void closedScopeRejectsLateOwnershipWithoutTakingIt() {
        ModelBuildResourceScope scope = new ModelBuildResourceScope();
        scope.close();
        FakeResource late = new FakeResource("late", null, false);

        assertThrows(IllegalStateException.class, () -> scope.own(late));
        assertEquals(0, late.closes.get());
    }

    private static final class FakeResource implements AutoCloseable {
        private final String name;
        private final List<String> order;
        private final boolean fail;
        private final AtomicInteger closes = new AtomicInteger();

        private FakeResource(String name, List<String> order, boolean fail) {
            this.name = name;
            this.order = order;
            this.fail = fail;
        }

        @Override
        public void close() {
            this.closes.incrementAndGet();
            if (this.order != null) this.order.add(this.name);
            if (this.fail) throw new IllegalStateException("expected failure");
        }
    }
}
