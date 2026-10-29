package de.keksuccino.fancymenu.util.rendering.ui.screen;

import org.jetbrains.annotations.NotNull;
import java.awt.Color;

/**
 * The color edited in a {@link ColorPickerWindowBody}.<br>
 * The color is stored as HSV + alpha, because the picker controls (saturation/value area, hue slider and alpha slider) operate on these values.
 * The resulting ARGB color gets cached and updated with every change.<br><br>
 *
 * Editing a single RGBA channel converts the color back to HSV. Not every RGB color defines a hue and saturation (grays have no hue, black has neither),
 * so the previous values are kept in these cases. This keeps the picker markers in place instead of letting them jump around, without changing the resulting color.
 */
final class ColorPickerState {

    static final int CHANNEL_MAX_VALUE = 255;

    private final float[] hsvBuffer = new float[3];
    private float hue = 0.0F;
    private float saturation = 0.0F;
    private float value = 1.0F;
    private float alpha = 1.0F;
    private int argb = 0xFFFFFFFF;

    ColorPickerState(int argb) {
        this.setArgb(argb);
    }

    /**
     * Replaces the whole color, including its HSV values.
     */
    void setArgb(int argb) {
        Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, this.hsvBuffer);
        this.hue = this.hsvBuffer[0];
        this.saturation = this.hsvBuffer[1];
        this.value = this.hsvBuffer[2];
        this.alpha = ((argb >>> 24) & 0xFF) / (float) CHANNEL_MAX_VALUE;
        this.updateArgb();
    }

    /**
     * @return True if the resulting color changed.
     */
    boolean setHue(float hue) {
        this.hue = clamp(hue);
        return this.updateArgb();
    }

    /**
     * @return True if the resulting color changed.
     */
    boolean setSaturationAndValue(float saturation, float value) {
        this.saturation = clamp(saturation);
        this.value = clamp(value);
        return this.updateArgb();
    }

    /**
     * @return True if the resulting color changed.
     */
    boolean setAlpha(float alpha) {
        this.alpha = clamp(alpha);
        return this.updateArgb();
    }

    /**
     * Sets a single RGBA channel. The value gets clamped to 0-255.
     *
     * @return True if the resulting color changed.
     */
    boolean setChannel(@NotNull Channel channel, int channelValue) {
        int clampedValue = Math.max(0, Math.min(CHANNEL_MAX_VALUE, channelValue));
        if (this.getChannel(channel) == clampedValue) return false;
        if (channel == Channel.ALPHA) {
            this.alpha = clampedValue / (float) CHANNEL_MAX_VALUE;
            return this.updateArgb();
        }
        int red = (channel == Channel.RED) ? clampedValue : this.getChannel(Channel.RED);
        int green = (channel == Channel.GREEN) ? clampedValue : this.getChannel(Channel.GREEN);
        int blue = (channel == Channel.BLUE) ? clampedValue : this.getChannel(Channel.BLUE);
        Color.RGBtoHSB(red, green, blue, this.hsvBuffer);
        float newHue = this.hsvBuffer[0];
        float newSaturation = this.hsvBuffer[1];
        float newValue = this.hsvBuffer[2];
        // Black defines no saturation and grays define no hue, so keep the previous ones to not move the picker markers
        if (newValue > 0.0F) {
            // Hue 0 and 1 are both red, so a hue slider at its right end stays there
            boolean sameRedHue = (newHue == 0.0F) && (this.hue == 1.0F);
            if ((newSaturation > 0.0F) && !sameRedHue) {
                this.hue = newHue;
            }
            this.saturation = newSaturation;
        }
        this.value = newValue;
        return this.updateArgb();
    }

    int getChannel(@NotNull Channel channel) {
        return switch (channel) {
            case RED -> (this.argb >> 16) & 0xFF;
            case GREEN -> (this.argb >> 8) & 0xFF;
            case BLUE -> this.argb & 0xFF;
            case ALPHA -> (this.argb >>> 24) & 0xFF;
        };
    }

    int getArgb() {
        return this.argb;
    }

    float getHue() {
        return this.hue;
    }

    float getSaturation() {
        return this.saturation;
    }

    float getValue() {
        return this.value;
    }

    float getAlpha() {
        return this.alpha;
    }

    /**
     * @return True if the resulting color changed.
     */
    private boolean updateArgb() {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.value) & 0xFFFFFF;
        int newArgb = (Math.round(this.alpha * CHANNEL_MAX_VALUE) << 24) | rgb;
        boolean changed = newArgb != this.argb;
        this.argb = newArgb;
        return changed;
    }

    /**
     * Parses the text of an RGBA channel input.
     *
     * @return The channel value clamped to 0-255, or -1 if the text is not a number (for example while the input is empty).
     */
    static int parseChannelValue(@NotNull String text) {
        if (text.isEmpty()) return -1;
        int parsed = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < '0') || (c > '9')) return -1;
            // Clamping every step keeps long inputs from overflowing, and is safe because appending digits never makes the number smaller
            parsed = Math.min(CHANNEL_MAX_VALUE, (parsed * 10) + (c - '0'));
        }
        return parsed;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    enum Channel {

        RED("R"),
        GREEN("G"),
        BLUE("B"),
        ALPHA("A");

        @NotNull
        private final String label;

        Channel(@NotNull String label) {
            this.label = label;
        }

        @NotNull
        String getLabel() {
            return this.label;
        }

    }

}
