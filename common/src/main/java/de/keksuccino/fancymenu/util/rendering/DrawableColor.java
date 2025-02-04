package de.keksuccino.fancymenu.util.rendering;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.Objects;

public class DrawableColor {

    public static final DrawableColor EMPTY = DrawableColor.of(new Color(255, 255, 255));
    public static final DrawableColor WHITE = DrawableColor.of(new Color(255, 255, 255));
    public static final DrawableColor BLACK = DrawableColor.of(new Color(0, 0, 0));

    protected Color color;
    protected int colorInt;
    protected String hex;
    protected FloatColor floatColor;

    /** Creates a {@link DrawableColor} out of the given {@link Color}. **/
    @NotNull
    public static DrawableColor of(@NotNull Color color) {
        Objects.requireNonNull(color);
        DrawableColor c = new DrawableColor();
        c.color = color;
        c.colorInt = color.getRGB();
        c.hex = convertColorToHexString(color);
        if (c.hex == null) c.hex = "#ffffffff";
        return c;
    }

    /**
     * Creates a {@link DrawableColor} out of the given HEX-{@link String}.<br>
     * Returns {@link DrawableColor#EMPTY} if the method failed to parse the HEX code.
     */
    @NotNull
    public static DrawableColor of(@NotNull String hex) {
        Objects.requireNonNull(hex);
        hex = hex.replace(" ", "");
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        DrawableColor c = new DrawableColor();
        c.color = convertHexStringToColor(hex);
        if (c.color == null) return EMPTY;
        c.colorInt = c.color.getRGB();
        c.hex = hex;
        return c;
    }

    /** Creates a {@link DrawableColor} out of the given RGB integers. The alpha channel will get defaulted to 255. **/
    @NotNull
    public static DrawableColor of(int r, int g, int b) {
        return of(r, g, b, 255);
    }

    /** Creates a {@link DrawableColor} out of the given RGBA integers. **/
    @NotNull
    public static DrawableColor of(int r, int g, int b, int a) {
        DrawableColor c = new DrawableColor();
        try {
            c.color = new Color(r, g, b, a);
        } catch (Exception ex) {
            ex.printStackTrace();
            return EMPTY;
        }
        c.colorInt = c.color.getRGB();
        c.hex = convertColorToHexString(c.color);
        if (c.hex != null) {
            return c;
        }
        return EMPTY;
    }

    protected DrawableColor() {
    }

    @NotNull
    public FloatColor getAsFloats() {
        if (this.floatColor == null) {
            float[] floats = argbToFloats(this.getColorInt());
            this.floatColor = new FloatColor(floats[0], floats[1], floats[2], floats[3]);
        }
        return this.floatColor;
    }

    @NotNull
    public Color getColor() {
        return this.color;
    }

    public int getColorInt() {
        return this.colorInt;
    }

    public int getColorIntWithAlpha(float alpha) {
        return RenderingUtils.replaceAlphaInColor(this.colorInt, alpha);
    }

    @NotNull
    public String getHex() {
        if (this.hex == null) {
            return "#ffffffff";
        }
        return this.hex;
    }

    public DrawableColor copy() {
        DrawableColor c = new DrawableColor();
        c.color = this.color;
        c.colorInt = this.colorInt;
        c.hex = this.hex;
        return c;
    }

    @Nullable
    protected static Color convertHexStringToColor(@NotNull String hex) {
        try {
            hex = hex.replace("#", "");
            if (hex.length() == 6) {
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16)
                );
            }
            if (hex.length() == 8) {
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16)
                );
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    protected static String convertColorToHexString(@NotNull Color color) {
        try {
            return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Converts an ARGB color integer to an array of four floats (red, green, blue, alpha),
     * each in the range 0.0 to 1.0.
     *
     * @param argb the color integer in ARGB format (0xAARRGGBB)
     * @return a float array where:
     *         index 0 = red,
     *         index 1 = green,
     *         index 2 = blue,
     *         index 3 = alpha.
     */
    protected static float[] argbToFloats(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8)  & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        return new float[]{r, g, b, a};
    }

    public static record FloatColor(float red, float green, float blue, float alpha) {
    }

}
