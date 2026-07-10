package de.keksuccino.fancymenu.util.rendering.ui.widget;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleScreenBrandingCaptureControllerTest {

    @Test
    void capturesFinalInvocationArgumentBeforeSuppressingOriginalDraw() {
        List<String> captures = new ArrayList<>();
        String transformedBranding = "Minecraft 26.1.2/42 mods";

        boolean shouldDraw = TitleScreenBrandingCaptureController.captureAndSuppress(captures::add, transformedBranding);

        assertFalse(shouldDraw);
        assertEquals(List.of(transformedBranding), captures);
    }

    @Test
    void separateControllersKeepCapturesScopedToTheirScreens() {
        TitleScreenBrandingCaptureController first = new TitleScreenBrandingCaptureController();
        TitleScreenBrandingCaptureController second = new TitleScreenBrandingCaptureController();
        FakeTarget firstTarget = new FakeTarget();
        FakeTarget secondTarget = new FakeTarget();
        first.setTarget(firstTarget);
        second.setTarget(secondTarget);

        first.capture("first");
        second.capture("second");
        FakeTarget firstReplacement = new FakeTarget();
        FakeTarget secondReplacement = new FakeTarget();
        first.setTarget(firstReplacement);
        second.setTarget(secondReplacement);

        assertEquals(List.of("first"), firstTarget.brandingValues);
        assertEquals(List.of("second"), secondTarget.brandingValues);
        assertEquals(List.of("first"), firstReplacement.brandingValues);
        assertEquals(List.of("second"), secondReplacement.brandingValues);
    }

    @Test
    void unchangedBrandingDoesNotRepeatRendererOrBoundsUpdates() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        controller.setTarget(target);

        assertTrue(controller.capture("same"));
        assertFalse(controller.capture("same"));

        assertEquals(List.of("same"), target.brandingValues);
        assertEquals(1, target.widthResizeCount);
        assertEquals(1, target.heightResizeCount);
    }

    @Test
    void changedBrandingUpdatesRendererAndBoundsAgain() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        controller.setTarget(target);

        assertTrue(controller.capture("initial"));
        assertTrue(controller.capture("transformed"));

        assertEquals(List.of("initial", "transformed"), target.brandingValues);
        assertEquals(2, target.widthResizeCount);
        assertEquals(2, target.heightResizeCount);
    }

    @Test
    void uncustomizedBoundsResizeToChangedContent() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        controller.setTarget(target);

        controller.capture("expanded branding");

        assertEquals(1, target.widthResizeCount);
        assertEquals(1, target.heightResizeCount);
    }

    @Test
    void customizedBoundsArePreservedIndependently() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        target.customWidth = true;
        controller.setTarget(target);

        controller.capture("expanded branding");

        assertEquals(0, target.widthResizeCount);
        assertEquals(1, target.heightResizeCount);

        TitleScreenBrandingCaptureController otherController = new TitleScreenBrandingCaptureController();
        FakeTarget otherTarget = new FakeTarget();
        otherTarget.customHeight = true;
        otherController.setTarget(otherTarget);
        otherController.capture("expanded branding");

        assertEquals(1, otherTarget.widthResizeCount);
        assertEquals(0, otherTarget.heightResizeCount);
    }

    @Test
    void targetInitializationWithoutCaptureDoesNotChangeRendererOrBounds() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();

        controller.setTarget(target);

        assertEquals(List.of(), target.brandingValues);
        assertEquals(0, target.widthResizeCount);
        assertEquals(0, target.heightResizeCount);
    }

    @Test
    void replacementTargetImmediatelyReceivesExistingCaptureAndFutureChanges() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget first = new FakeTarget();
        FakeTarget replacement = new FakeTarget();
        controller.setTarget(first);
        controller.capture("initial");

        controller.setTarget(replacement);
        controller.capture("changed after replacement");

        assertEquals(List.of("initial"), first.brandingValues);
        assertEquals(List.of("initial", "changed after replacement"), replacement.brandingValues);
        assertEquals(2, replacement.widthResizeCount);
        assertEquals(2, replacement.heightResizeCount);
    }

    @Test
    void unchangedCaptureAfterReinitializationDoesNotRepeatRestoration() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget first = new FakeTarget();
        FakeTarget replacement = new FakeTarget();
        controller.setTarget(first);
        controller.capture("initial");

        controller.setTarget(replacement);

        assertFalse(controller.capture("initial"));
        assertEquals(List.of("initial"), replacement.brandingValues);
        assertEquals(1, replacement.widthResizeCount);
        assertEquals(1, replacement.heightResizeCount);
    }

    @Test
    void repeatedReinitializationRestoresEveryReplacementTarget() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget first = new FakeTarget();
        FakeTarget second = new FakeTarget();
        FakeTarget third = new FakeTarget();
        controller.setTarget(first);
        controller.capture("transformed");

        controller.setTarget(second);
        controller.setTarget(third);

        assertEquals(List.of("transformed"), second.brandingValues);
        assertEquals(List.of("transformed"), third.brandingValues);
        assertEquals(1, second.widthResizeCount);
        assertEquals(1, second.heightResizeCount);
        assertEquals(1, third.widthResizeCount);
        assertEquals(1, third.heightResizeCount);
    }

    @Test
    void replacementTargetPreservesCustomizedBoundsIndependently() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget initial = new FakeTarget();
        controller.setTarget(initial);
        controller.capture("transformed");
        FakeTarget customWidth = new FakeTarget();
        customWidth.customWidth = true;
        FakeTarget customHeight = new FakeTarget();
        customHeight.customHeight = true;

        controller.setTarget(customWidth);
        controller.setTarget(customHeight);

        assertEquals(List.of("transformed"), customWidth.brandingValues);
        assertEquals(0, customWidth.widthResizeCount);
        assertEquals(1, customWidth.heightResizeCount);
        assertEquals(List.of("transformed"), customHeight.brandingValues);
        assertEquals(1, customHeight.widthResizeCount);
        assertEquals(0, customHeight.heightResizeCount);
    }

    @Test
    void failedReplacementInitializationCanBeRetried() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget initial = new FakeTarget();
        FakeTarget replacement = new FakeTarget();
        controller.setTarget(initial);
        controller.capture("transformed");
        replacement.failWidthResize = true;

        assertThrows(IllegalStateException.class, () -> controller.setTarget(replacement));
        assertEquals("transformed", controller.getCapturedBrandingText());

        replacement.failWidthResize = false;
        controller.setTarget(replacement);

        assertEquals(List.of("transformed", "transformed"), replacement.brandingValues);
        assertEquals(1, replacement.widthResizeCount);
        assertEquals(1, replacement.heightResizeCount);
    }

    @Test
    void detachedTargetCannotReceiveLaterCaptures() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget oldTarget = new FakeTarget();
        controller.setTarget(oldTarget);
        controller.capture("initial");

        controller.setTarget(null);
        assertFalse(controller.capture("while detached"));

        assertEquals(List.of("initial"), oldTarget.brandingValues);
        assertEquals("initial", controller.getCapturedBrandingText());
    }

    @Test
    void valueObservedWithoutTargetRemainsAvailableForLaterCapture() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();

        assertFalse(controller.capture("transformed"));
        assertNull(controller.getCapturedBrandingText());

        FakeTarget target = new FakeTarget();
        controller.setTarget(target);
        assertTrue(controller.capture("transformed"));
        assertEquals(List.of("transformed"), target.brandingValues);
    }

    @Test
    void nullInputsFailBeforeMutatingTarget() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        controller.setTarget(target);

        assertThrows(NullPointerException.class, () -> controller.capture(null));
        assertThrows(NullPointerException.class, () -> TitleScreenBrandingCaptureController.captureAndSuppress(null, "branding"));
        assertThrows(NullPointerException.class, () -> TitleScreenBrandingCaptureController.captureAndSuppress(value -> {}, null));
        assertEquals(List.of(), target.brandingValues);
        assertNull(controller.getCapturedBrandingText());
    }

    @Test
    void rendererFailureDoesNotCommitCaptureAndCanBeRetried() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        target.failBrandingUpdate = true;
        controller.setTarget(target);

        assertThrows(IllegalStateException.class, () -> controller.capture("transformed"));
        assertNull(controller.getCapturedBrandingText());

        target.failBrandingUpdate = false;
        assertTrue(controller.capture("transformed"));
        assertEquals(List.of("transformed"), target.brandingValues);
    }

    @Test
    void boundsFailureDoesNotCommitCaptureAndCanBeRetried() {
        TitleScreenBrandingCaptureController controller = new TitleScreenBrandingCaptureController();
        FakeTarget target = new FakeTarget();
        target.failWidthResize = true;
        controller.setTarget(target);

        assertThrows(IllegalStateException.class, () -> controller.capture("transformed"));
        assertNull(controller.getCapturedBrandingText());

        target.failWidthResize = false;
        assertTrue(controller.capture("transformed"));
        assertEquals(List.of("transformed", "transformed"), target.brandingValues);
        assertEquals(1, target.widthResizeCount);
        assertEquals(1, target.heightResizeCount);
    }

    private static final class FakeTarget implements TitleScreenBrandingCaptureController.Target {

        private final List<String> brandingValues = new ArrayList<>();
        private boolean customWidth;
        private boolean customHeight;
        private boolean failBrandingUpdate;
        private boolean failWidthResize;
        private int widthResizeCount;
        private int heightResizeCount;

        @Override
        public void setBrandingText(String brandingText) {
            if (this.failBrandingUpdate) throw new IllegalStateException("branding update failed");
            this.brandingValues.add(brandingText);
        }

        @Override
        public boolean hasCustomWidth() {
            return this.customWidth;
        }

        @Override
        public void resizeWidthToContent() {
            if (this.failWidthResize) throw new IllegalStateException("width resize failed");
            this.widthResizeCount++;
        }

        @Override
        public boolean hasCustomHeight() {
            return this.customHeight;
        }

        @Override
        public void resizeHeightToContent() {
            this.heightResizeCount++;
        }
    }
}
