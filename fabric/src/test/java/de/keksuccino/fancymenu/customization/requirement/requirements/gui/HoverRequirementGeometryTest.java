package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverRequirementGeometryTest {

    @Test
    void rotatedElementUsesItsVisibleFootprint() {
        ProbeElement element = new ProbeElement();
        element.rotation = 90.0F;

        assertTrue(hovered(element, 50, 40));
        assertFalse(hovered(element, 5, 10));
    }

    @Test
    void tiltedElementRejectsPointsOutsideItsCompressedFootprint() {
        ProbeElement element = new ProbeElement();
        element.horizontalTilt = 60.0F;

        assertTrue(hovered(element, 50, 10));
        assertFalse(hovered(element, 10, 10));
        element.horizontalTilt = 0.0F;
        element.verticalTilt = 60.0F;
        assertFalse(hovered(element, 50, 2));
    }

    @Test
    void visibilityAndEmptyBoundsStillGateHover() {
        ProbeElement element = new ProbeElement();
        assertTrue(hovered(element, 50, 10));
        element.setHidden(true);
        assertFalse(hovered(element, 50, 10));
        element.setHidden(false);
        element.visible = false;
        assertFalse(hovered(element, 50, 10));
        element.visible = true;
        element.baseWidth = 0;
        assertFalse(hovered(element, 0, 10));
        element.baseWidth = 100;
        element.baseHeight = -1;
        assertFalse(hovered(element, 50, 0));
    }

    private static boolean hovered(ProbeElement element, int x, int y) {
        return HoverRequirementUtils.isElementHovered(element, x, y, null);
    }

    private static final class ProbeElement extends AnimationControllerElement implements HideableElement {

        private float rotation;
        private float verticalTilt;
        private float horizontalTilt;
        private boolean hidden;

        private ProbeElement() {
            super(new AnimationControllerElementBuilder());
            this.baseWidth = 100;
            this.baseHeight = 20;
        }

        @Override
        public int getAbsoluteX() {
            return 0;
        }

        @Override
        public int getAbsoluteY() {
            return 0;
        }

        @Override
        public int getAbsoluteWidth() {
            return this.baseWidth;
        }

        @Override
        public int getAbsoluteHeight() {
            return this.baseHeight;
        }

        @Override
        public float getRotationDegrees() {
            return this.rotation;
        }

        @Override
        public float getVerticalTiltDegrees() {
            return this.verticalTilt;
        }

        @Override
        public float getHorizontalTiltDegrees() {
            return this.horizontalTilt;
        }

        @Override
        public boolean shouldRender() {
            return this.visible;
        }

        @Override
        public boolean isHidden() {
            return this.hidden;
        }

        @Override
        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

    }

}
