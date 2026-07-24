package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelBuildResourceScopeTest {

    @Test
    void closesImageWhenContentsConstructionFails() {
        FakeResource image = new FakeResource("image", null, null, false);

        assertThrows(TestBuildFailure.class, () -> {
            try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
                resources.own(image);
                throw new TestBuildFailure();
            }
        });

        assertEquals(1, image.closeCalls.get());
    }

    @Test
    void closesContentsOwnerWhenSpriteConstructionFails() {
        FakeResource image = new FakeResource("image", null, null, false);
        FakeResource contents = new FakeResource("contents", image, null, false);

        assertThrows(TestBuildFailure.class, () -> {
            try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
                resources.own(image);
                resources.replaceOwnership(image, contents);
                throw new TestBuildFailure();
            }
        });

        assertAll(() -> assertEquals(1, contents.closeCalls.get()), () -> assertEquals(1, image.closeCalls.get()));
    }

    @Test
    void closesSpriteOwnerWhenBakeFails() {
        FakeResource image = new FakeResource("image", null, null, false);
        FakeResource contents = new FakeResource("contents", image, null, false);
        FakeResource sprite = new FakeResource("sprite", contents, null, false);

        assertThrows(TestBuildFailure.class, () -> {
            try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
                resources.own(image);
                resources.replaceOwnership(image, contents);
                resources.replaceOwnership(contents, sprite);
                throw new TestBuildFailure();
            }
        });

        assertAll(() -> assertEquals(1, sprite.closeCalls.get()), () -> assertEquals(1, contents.closeCalls.get()), () -> assertEquals(1, image.closeCalls.get()));
    }

    @Test
    void successfulTransferLeavesCandidateOpenForCacheLifecycle() {
        FakeResource image = new FakeResource("image", null, null, false);
        FakeResource contents = new FakeResource("contents", image, null, false);
        FakeResource sprite = new FakeResource("sprite", contents, null, false);
        FakeResource candidate = new FakeResource("candidate", sprite, null, false);
        FakeResource transferred;

        try (ModelBuildResourceScope resources = new ModelBuildResourceScope()) {
            resources.own(image);
            resources.replaceOwnership(image, contents);
            resources.replaceOwnership(contents, sprite);
            resources.replaceOwnership(sprite, candidate);
            transferred = resources.transfer(candidate);
        }

        assertAll(() -> assertSame(candidate, transferred), () -> assertEquals(0, candidate.closeCalls.get()), () -> assertEquals(0, sprite.closeCalls.get()), () -> assertEquals(0, contents.closeCalls.get()), () -> assertEquals(0, image.closeCalls.get()));
        transferred.close();
        assertAll(() -> assertEquals(1, candidate.closeCalls.get()), () -> assertEquals(1, sprite.closeCalls.get()), () -> assertEquals(1, contents.closeCalls.get()), () -> assertEquals(1, image.closeCalls.get()));
    }

    @Test
    void closeIsReverseOrderedIdempotentAndContinuesAfterFailure() {
        List<String> closeOrder = new ArrayList<>();
        FakeResource first = new FakeResource("first", null, closeOrder, false);
        FakeResource second = new FakeResource("second", null, closeOrder, true);
        FakeResource third = new FakeResource("third", null, closeOrder, false);
        ModelBuildResourceScope resources = new ModelBuildResourceScope();
        resources.own(first);
        resources.own(second);
        resources.own(third);

        resources.close();
        resources.close();

        assertAll(() -> assertEquals(List.of("third", "second", "first"), closeOrder), () -> assertEquals(1, first.closeCalls.get()), () -> assertEquals(1, second.closeCalls.get()), () -> assertEquals(1, third.closeCalls.get()));
    }

    @Test
    void rejectedLateOwnershipLeavesResourceWithCaller() {
        ModelBuildResourceScope resources = new ModelBuildResourceScope();
        resources.close();
        FakeResource late = new FakeResource("late", null, null, false);

        assertThrows(IllegalStateException.class, () -> resources.own(late));

        assertEquals(0, late.closeCalls.get());
    }

    @Test
    void failedReplacementLeavesOwnershipUnchangedWithoutClosingWrapperOrInnerTwice() {
        FakeResource image = new FakeResource("image", null, null, false);
        FakeResource wrapper = new FakeResource("wrapper", image, null, false);
        ModelBuildResourceScope resources = new ModelBuildResourceScope();
        resources.own(image);
        resources.close();

        assertThrows(IllegalStateException.class, () -> resources.replaceOwnership(image, wrapper));

        assertAll(() -> assertEquals(0, wrapper.closeCalls.get()), () -> assertEquals(1, image.closeCalls.get()));
    }

    private static final class FakeResource implements AutoCloseable {

        private final String name;
        private final FakeResource child;
        private final List<String> closeOrder;
        private final boolean failClose;
        private final AtomicInteger closeCalls = new AtomicInteger();

        private FakeResource(String name, FakeResource child, List<String> closeOrder, boolean failClose) {
            this.name = name;
            this.child = child;
            this.closeOrder = closeOrder;
            this.failClose = failClose;
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
            if (this.closeOrder != null) this.closeOrder.add(this.name);
            if (this.child != null) this.child.close();
            if (this.failClose) throw new TestBuildFailure();
        }

    }

    private static final class TestBuildFailure extends RuntimeException {
    }

}
