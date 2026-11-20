package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import org.jetbrains.annotations.NotNull;

/**
 * Where the five leaf color ramps of the Leaves overlay come from.
 */
public enum LeavesColorMode {

    /** The five built-in autumn ramps: red, orange, yellow, green and brown. */
    AUTUMN_MIX("autumn_mix"),
    /** Five ramps derived along the range from the start color to the end color. */
    CUSTOM_RANGE("custom_range");

    @NotNull
    private final String name;

    LeavesColorMode(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getLocalizationKey() {
        return "fancymenu.decoration_overlays.leaves.colors." + this.name;
    }

}
