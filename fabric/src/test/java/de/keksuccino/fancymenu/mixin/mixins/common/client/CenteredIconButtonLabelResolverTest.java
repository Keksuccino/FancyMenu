package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CenteredIconButtonLabelResolverTest {

    @Test
    void noCustomizationKeepsIcon() {
        assertNull(CenteredIconButtonLabelResolver.selectCustomLabel(null, null, false, true, true));
    }

    @Test
    void baseLabelReplacesIconOutsideHover() {
        Component baseLabel = Component.literal("Base");

        assertSame(baseLabel, CenteredIconButtonLabelResolver.selectCustomLabel(baseLabel, null, false, true, true));
    }

    @Test
    void activeVisibleHoverUsesHoverLabel() {
        Component baseLabel = Component.literal("Base");
        Component hoverLabel = Component.literal("Hover");

        assertSame(hoverLabel, CenteredIconButtonLabelResolver.selectCustomLabel(baseLabel, hoverLabel, true, true, true));
    }

    @Test
    void hoverOnlyCustomizationKeepsIconOutsideHover() {
        Component hoverLabel = Component.literal("Hover");

        assertNull(CenteredIconButtonLabelResolver.selectCustomLabel(null, hoverLabel, false, true, true));
    }

    @Test
    void inactiveAndInvisibleWidgetsDoNotUseHoverLabel() {
        Component baseLabel = Component.literal("Base");
        Component hoverLabel = Component.literal("Hover");

        assertSame(baseLabel, CenteredIconButtonLabelResolver.selectCustomLabel(baseLabel, hoverLabel, true, true, false));
        assertSame(baseLabel, CenteredIconButtonLabelResolver.selectCustomLabel(baseLabel, hoverLabel, true, false, true));
    }

    @Test
    void activeWidgetUsesFullyResolvedMessage() {
        Component customLabel = Component.literal("Base");
        Component resolvedLabel = Component.literal("Resolved").withStyle(style -> style.withUnderlined(true));

        assertSame(resolvedLabel, CenteredIconButtonLabelResolver.resolveRenderedLabel(customLabel, resolvedLabel, true));
    }

    @Test
    void inactiveWidgetUsesVanillaInactiveStylingForCustomLabel() {
        Component customLabel = Component.literal("Base");
        Component unrelatedActiveLabel = Component.literal("Original");

        Component renderedLabel = CenteredIconButtonLabelResolver.resolveRenderedLabel(customLabel, unrelatedActiveLabel, false);

        assertEquals(AbstractWidget.WithInactiveMessage.defaultInactiveMessage(customLabel), renderedLabel);
        assertEquals("Base", renderedLabel.getString());
    }

    @Test
    void emptyCustomLabelStillReplacesIconUntilReset() {
        Component emptyLabel = Component.empty();

        assertSame(emptyLabel, CenteredIconButtonLabelResolver.selectCustomLabel(emptyLabel, null, false, true, true));
        assertNull(CenteredIconButtonLabelResolver.selectCustomLabel(null, null, false, true, true));
    }

}
