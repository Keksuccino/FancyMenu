package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import org.jetbrains.annotations.NotNull;

/**
 * The edge of the screen the sun sits behind. The Sun Angle option moves it along that edge from there.
 */
public enum SunbeamsSide {

    TOP("top", 0.0F, -1.0F),
    RIGHT("right", 1.0F, 0.0F),
    BOTTOM("bottom", 0.0F, 1.0F),
    LEFT("left", -1.0F, 0.0F);

    @NotNull
    private final String name;
    /** Unit direction from the middle of the screen out through this edge, in screen coordinates (y points down). */
    final float outwardX;
    final float outwardY;

    SunbeamsSide(@NotNull String name, float outwardX, float outwardY) {
        this.name = name;
        this.outwardX = outwardX;
        this.outwardY = outwardY;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getLocalizationKey() {
        return "fancymenu.decoration_overlays.sunbeams.sun_side." + this.name;
    }

}
