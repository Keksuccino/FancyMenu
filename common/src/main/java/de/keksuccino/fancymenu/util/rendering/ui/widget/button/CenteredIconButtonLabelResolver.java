package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public final class CenteredIconButtonLabelResolver {

    private CenteredIconButtonLabelResolver() {}

    @Nullable
    public static Component resolve(@Nullable Component customLabel, @Nullable Component hoverLabel, boolean hoveredOrFocused, boolean visible, boolean active) {
        if (hoverLabel != null && hoveredOrFocused && visible && active) return hoverLabel;
        return customLabel;
    }

}
