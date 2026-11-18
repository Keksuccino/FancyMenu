package de.keksuccino.fancymenu.mixin;

import de.keksuccino.fancymenu.mixin.mixins.common.client.MixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.WidgetLabelAlignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MixinAbstractWidgetLabelAlignmentTest {

    @Test
    void widgetsStartWithCenteredLabels() {
        TestWidget widget = new TestWidget();

        assertEquals(WidgetLabelAlignment.CENTERED, widget.getLabelAlignmentFancyMenu());
        assertEquals(WidgetLabelAlignment.CENTERED, widget.resolveLabelAlignmentFancyMenu());
    }

    @Test
    void widgetsThatAreNoButtonsOrSlidersOnlyUseTheirOwnAlignment() {
        TestWidget widget = new TestWidget();

        widget.setLabelAlignmentFancyMenu(WidgetLabelAlignment.RIGHT);

        assertEquals(WidgetLabelAlignment.RIGHT, widget.getLabelAlignmentFancyMenu());
        assertEquals(WidgetLabelAlignment.RIGHT, widget.resolveLabelAlignmentFancyMenu());
    }

    @Test
    void resettingWidgetCustomizationsCentersTheLabelAgain() {
        TestWidget widget = new TestWidget();
        widget.setLabelAlignmentFancyMenu(WidgetLabelAlignment.LEFT);

        widget.resetWidgetCustomizationsFancyMenu();

        assertEquals(WidgetLabelAlignment.CENTERED, widget.getLabelAlignmentFancyMenu());
    }

    @Test
    @SuppressWarnings("all")
    void missingAlignmentsGetRejected() {
        TestWidget widget = new TestWidget();

        assertThrows(NullPointerException.class, () -> widget.setLabelAlignmentFancyMenu(null));
        assertEquals(WidgetLabelAlignment.CENTERED, widget.getLabelAlignmentFancyMenu());
    }

    private static final class TestWidget extends MixinAbstractWidget {

        private boolean focused;

        private TestWidget() {
            this.active = true;
            this.visible = true;
            this.width = 100;
            this.height = 20;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }

        @Override
        public boolean isHoveredOrFocused() {
            return this.isHovered || this.focused;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

    }

}
