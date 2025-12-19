package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DrawableColor {

    public static final DrawableColor EMPTY = DrawableColor.of(new Color(255, 255, 255));
    public static final DrawableColor WHITE = DrawableColor.of(new Color(255, 255, 255));
    public static final DrawableColor BLACK = DrawableColor.of(new Color(0, 0, 0));
    private static final Map<String, String> HTML_NAMED_COLORS = createHtmlColorNameMap();

    protected Color color;
    protected int colorInt;
    protected String hex;
    protected FloatColor floatColor;

    @NotNull
    public static DrawableColor of(int color) {
        DrawableColor c = new DrawableColor();
        c.color = new Color(color);
        c.colorInt = color;
        c.hex = convertColorToHexString(c.color);
        if (c.hex == null) c.hex = "#ffffffff";
        return c;
    }

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

    /**
     * Creates a {@link DrawableColor} out of the given HTML-like color {@link String}.<br>
     * Supports hex colors (#RGB, #RGBA, #RRGGBB, #RRGGBBAA) and basic HTML color names.<br>
     * Returns {@link DrawableColor#EMPTY} if the method failed to parse the color.
     */
    @NotNull
    public static DrawableColor ofHtml(@NotNull String color) {
        Objects.requireNonNull(color);
        String cleaned = color.trim();
        if (cleaned.isEmpty()) return EMPTY;
        String normalized = cleaned.toLowerCase(Locale.ROOT);
        String normalizedHex = normalizeHtmlHex(normalized);
        if (normalizedHex != null) {
            DrawableColor parsed = of(normalizedHex);
            if (parsed != EMPTY) return parsed;
        }
        String namedHex = HTML_NAMED_COLORS.get(normalized);
        if (namedHex != null) {
            DrawableColor parsed = of(namedHex);
            if (parsed != EMPTY) return parsed;
        }
        return EMPTY;
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

    public void setAsShaderColor(@NotNull GuiGraphics graphics, float alpha) {
        FloatColor c = this.getAsFloats();
        graphics.setColor(c.red(), c.green(), c.blue(), alpha);
    }

    public void setAsShaderColor(@NotNull GuiGraphics graphics) {
        FloatColor c = this.getAsFloats();
        graphics.setColor(c.red(), c.green(), c.blue(), c.alpha());
    }

    public void resetShaderColor(@NotNull GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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

    /**
     * Returns an ARGB color int of this color.
     */
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

    @Nullable
    private static String normalizeHtmlHex(@NotNull String value) {
        String hex = value;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!isHtmlHexLength(hex.length())) return null;
        if (!isHexString(hex)) return null;
        if ((hex.length() == 3) || (hex.length() == 4)) {
            StringBuilder expanded = new StringBuilder(hex.length() * 2);
            for (int i = 0; i < hex.length(); i++) {
                char c = hex.charAt(i);
                expanded.append(c).append(c);
            }
            hex = expanded.toString();
        }
        return "#" + hex;
    }

    private static boolean isHtmlHexLength(int length) {
        return (length == 3) || (length == 4) || (length == 6) || (length == 8);
    }

    private static boolean isHexString(@NotNull String hex) {
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            boolean isDigit = (c >= '0') && (c <= '9');
            boolean isLowerHex = (c >= 'a') && (c <= 'f');
            boolean isUpperHex = (c >= 'A') && (c <= 'F');
            if (!(isDigit || isLowerHex || isUpperHex)) return false;
        }
        return true;
    }

    private static Map<String, String> createHtmlColorNameMap() {
        Map<String, String> map = new HashMap<>();
        map.put("black", "#000000");
        map.put("silver", "#c0c0c0");
        map.put("gray", "#808080");
        map.put("grey", "#808080");
        map.put("white", "#ffffff");
        map.put("maroon", "#800000");
        map.put("red", "#ff0000");
        map.put("purple", "#800080");
        map.put("fuchsia", "#ff00ff");
        map.put("magenta", "#ff00ff");
        map.put("green", "#008000");
        map.put("lime", "#00ff00");
        map.put("olive", "#808000");
        map.put("yellow", "#ffff00");
        map.put("navy", "#000080");
        map.put("blue", "#0000ff");
        map.put("teal", "#008080");
        map.put("aqua", "#00ffff");
        map.put("cyan", "#00ffff");
        map.put("transparent", "#00000000");
        return map;
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
