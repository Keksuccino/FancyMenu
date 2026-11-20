package de.keksuccino.fancymenu.customization.decorationoverlay;

import org.jetbrains.annotations.NotNull;

/**
 * The two looks overlays can offer through their Look option, see {@link DecorationOverlayOptions.Look}.
 */
public enum DecorationOverlayLook {

    /** The full pixel art with shading and extra effects. */
    DETAILED("detailed"),
    /** Flatter art, closer to the classic look of the overlay. */
    SIMPLE("simple");

    @NotNull
    private final String name;

    DecorationOverlayLook(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getLocalizationKey() {
        return "fancymenu.decoration_overlay.look." + this.name;
    }

}
