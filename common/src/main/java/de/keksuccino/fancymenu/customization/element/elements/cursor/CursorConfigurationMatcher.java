package de.keksuccino.fancymenu.customization.element.elements.cursor;

import org.jetbrains.annotations.NotNull;

/** Native-free cursor configuration comparisons shared by runtime code and regression tests. */
final class CursorConfigurationMatcher {

    private CursorConfigurationMatcher() {}

    static boolean matches(@NotNull Object registeredTexture, int registeredHotspotX, int registeredHotspotY, @NotNull Object currentTexture, int currentHotspotX, int currentHotspotY) {
        return registeredTexture == currentTexture && registeredHotspotX == currentHotspotX && registeredHotspotY == currentHotspotY;
    }

}
