package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

final class CenteredIconButtonLabelResolver {

    private CenteredIconButtonLabelResolver() {
    }

    @Nullable
    static Component resolve(@Nullable Component customLabel, @Nullable Component hoverLabel, boolean hoveredOrFocused, boolean visible, boolean active) {
        if (hoverLabel != null && hoveredOrFocused && visible && active) return hoverLabel;
        return customLabel;
    }

}
