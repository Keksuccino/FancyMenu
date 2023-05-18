package de.keksuccino.fancymenu.rendering;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

public class DrawableColor {

    protected Color color;
    protected int colorInt;
    protected String hex;

    /** Creates a {@link DrawableColor} out of the given {@link Color}. **/
    @NotNull
    public static DrawableColor create(@NotNull Color color) {
        Objects.requireNonNull(color);
        DrawableColor c = new DrawableColor();
        c.color = color;
        c.colorInt = color.getRGB();
        c.hex = convertColorToHexString(color);
        return c;
    }

    /** Tries to create a {@link DrawableColor} out of the given HEX-{@link String}. Returns NULL if it failed to create the color. **/
    @Nullable
    public static DrawableColor create(@NotNull String hex) {
        Objects.requireNonNull(hex);
        hex = hex.replace(" ", "");
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        DrawableColor c = new DrawableColor();
        c.color = convertHexStringToColor(hex);
        if (c.color != null) {
            c.colorInt = c.color.getRGB();
        }
        c.hex = hex;
        if (c.color != null) {
            return c;
        }
        return null;
    }

    /** Creates a {@link DrawableColor} out of the given RGB integers. The alpha channel will get defaulted to 255. **/
    @NotNull
    public static DrawableColor create(int r, int g, int b) {
        return create(r, g, b, 255);
    }

    /** Creates a {@link DrawableColor} out of the given RGBA integers. **/
    @NotNull
    public static DrawableColor create(int r, int g, int b, int a) {
        DrawableColor c = new DrawableColor();
        c.color = new Color(r, g, b, a);
        c.colorInt = c.color.getRGB();
        c.hex = convertColorToHexString(c.color);
        return c;
    }

    protected DrawableColor() {
    }

    @NotNull
    public Color getColor() {
        return this.color;
    }

    public int getColorInt() {
        return this.colorInt;
    }

    @NotNull
    public String getHex() {
        if (this.hex == null) {
            return "#ffffffff";
        }
        return this.hex;
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

}
