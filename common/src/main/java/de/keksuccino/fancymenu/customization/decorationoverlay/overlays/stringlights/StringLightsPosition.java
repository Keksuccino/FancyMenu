package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

/**
 * Where a string of the String Lights overlay hangs. Start and end are fractions of the screen size plus GUI pixel offsets.<br>
 * Layouts store the settings of every string under keys built from the lowercase names of these constants, so the names must never change.
 */
public enum StringLightsPosition {

    LEFT_CENTER_TO_TOP_CENTER(0.0F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F),
    RIGHT_CENTER_TO_TOP_CENTER(1.0F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.22F, 1.0F, 1.0F),
    BOTTOM_LEFT_TO_TOP_CENTER(0.0F, 0.95F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.28F, 1.05F, 1.05F),
    BOTTOM_RIGHT_TO_TOP_CENTER(1.0F, 0.95F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.28F, 1.05F, 1.05F),
    TOP_LEFT_TO_TOP_RIGHT(0.0F, 0.07F, 1.0F, 0.07F, 0.0F, 0.0F, 0.0F, 0.0F, 0.18F, 1.0F, 1.0F),
    BOTTOM_LEFT_TO_BOTTOM_RIGHT(0.0F, 0.85F, 1.0F, 0.85F, 0.0F, 0.0F, 0.0F, 0.0F, 0.14F, 1.0F, 1.0F),
    LOOSE_LEFT_TOP(0.1667F, 0.0F, 0.1667F, 0.48F, 0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.85F, 1.2F),
    LOOSE_RIGHT_TOP(0.8333F, 0.0F, 0.8333F, 0.48F, 0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.85F, 1.2F);

    public final float startX;
    public final float startY;
    public final float endX;
    public final float endY;
    public final float startOffsetX;
    public final float startOffsetY;
    public final float endOffsetX;
    public final float endOffsetY;
    /** How far the middle of the string hangs down, as a fraction of its length */
    public final float sagFactor;
    /** Multiplies the number of bulbs the string gets from its length */
    public final float densityMultiplier;
    /** Multiplies how far the string sways in the wind */
    public final float windMultiplier;

    StringLightsPosition(float startX, float startY, float endX, float endY, float startOffsetX, float startOffsetY, float endOffsetX, float endOffsetY, float sagFactor, float densityMultiplier, float windMultiplier) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.startOffsetX = startOffsetX;
        this.startOffsetY = startOffsetY;
        this.endOffsetX = endOffsetX;
        this.endOffsetY = endOffsetY;
        this.sagFactor = sagFactor;
        this.densityMultiplier = densityMultiplier;
        this.windMultiplier = windMultiplier;
    }

}
