package de.keksuccino.fancymenu.customization.global;

import de.keksuccino.fancymenu.util.rendering.ui.widget.IExtendedWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalLabelCustomizationTargetTest {

    @Test
    void vanillaButtonsAndSlidersGetGlobalLabelCustomizations() {
        AbstractSliderButton slider = new AbstractSliderButton(0, 0, 100, 20, Component.literal("Vanilla"), 0.5D) {
            @Override
            protected void updateMessage() {
            }
            @Override
            protected void applyValue() {
            }
        };
        assertTrue(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(Button.builder(Component.literal("Vanilla"), button -> {}).build()));
        assertTrue(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(slider));
    }

    @Test
    void buttonsAndSlidersWithVanillaStyleLabelsGetGlobalLabelCustomizations() {
        // Like the buttons and sliders of layouts, which render their labels the Vanilla way
        assertTrue(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(new TestButton(false)));
        assertTrue(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(new TestSlider(false)));
    }

    @Test
    void buttonsAndSlidersOfFancyMenusOwnUiKeepTheirLabelsUntouched() {
        assertFalse(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(new TestButton(true)));
        assertFalse(GlobalCustomizationHandler.isGlobalLabelCustomizationTarget(new TestSlider(true)));
    }

    private static final class TestButton extends Button implements IExtendedWidget {

        private final boolean labelRenderedWithUiBase;

        private TestButton(boolean labelRenderedWithUiBase) {
            super(0, 0, 100, 20, Component.literal("Test"), button -> {}, DEFAULT_NARRATION);
            this.labelRenderedWithUiBase = labelRenderedWithUiBase;
        }

        @Override
        public boolean isLabelRenderedWithUiBase() {
            return this.labelRenderedWithUiBase;
        }

    }

    private static final class TestSlider extends AbstractSliderButton implements IExtendedWidget {

        private final boolean labelRenderedWithUiBase;

        private TestSlider(boolean labelRenderedWithUiBase) {
            super(0, 0, 100, 20, Component.literal("Test"), 0.5D);
            this.labelRenderedWithUiBase = labelRenderedWithUiBase;
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
        }

        @Override
        public boolean isLabelRenderedWithUiBase() {
            return this.labelRenderedWithUiBase;
        }

    }

}
