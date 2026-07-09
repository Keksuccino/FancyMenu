package de.keksuccino.fancymenu.util.rendering.ui.widget;

public final class ImageButtonRenderPolicy {

    private ImageButtonRenderPolicy() {}

    /**
     * Image buttons do not render their message themselves. An explicit FancyMenu label therefore replaces the icon,
     * while a hover-only label does so only in the same active hover/focus state used by FancyMenu's label resolver.
     */
    public static boolean shouldRenderCustomLabel(boolean hasCustomLabel, boolean hasHoverLabel, boolean hoveredOrFocused, boolean visible, boolean active) {
        if (hasHoverLabel && hoveredOrFocused && visible && active) return true;
        return hasCustomLabel;
    }

    public static boolean shouldRenderVanillaIcon(boolean customBackgroundAllowsVanillaTexture, boolean renderCustomLabel) {
        return customBackgroundAllowsVanillaTexture && !renderCustomLabel;
    }

}
