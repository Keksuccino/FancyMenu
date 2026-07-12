package de.keksuccino.fancymenu.mixin.support.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public final class CenteredIconButtonLabelResolver {

    private CenteredIconButtonLabelResolver() {
    }

    @Nullable
    public static Component selectCustomLabel(@Nullable Component customLabel, @Nullable Component hoverLabel, boolean hoveredOrFocused, boolean visible, boolean active) {
        if (hoverLabel != null && hoveredOrFocused && visible && active) return hoverLabel;
        return customLabel;
    }

    public static Component resolveRenderedLabel(Component customLabel, Component activeLabel, boolean active) {
        return active ? activeLabel : AbstractWidget.WithInactiveMessage.defaultInactiveMessage(customLabel);
    }

}
