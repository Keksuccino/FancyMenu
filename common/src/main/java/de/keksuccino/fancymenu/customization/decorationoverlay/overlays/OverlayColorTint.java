package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Recolors the hand-picked palette of a pixel overlay for a color the user chose, so a custom color keeps all light and shade steps of the
 * palette instead of flattening it. The palette has a reference color (its default user color): every channel of every palette color gets
 * scaled by how the user's channel relates to the reference channel. The reference itself turns into the user color, the default color
 * changes nothing, and brighter or darker steps stay brighter or darker.<br>
 * The alpha of the user color fades the whole palette.<br><br>
 *
 * An instance also remembers the last color it read from a color property, so overlays can read their color every frame and rebuild their
 * palettes only when it changed.
 */
public final class OverlayColorTint {

    private final int referenceRgb;
    @Nullable
    private String lastRaw = null;
    private int argb;

    /**
     * @param referenceRgb the palette's default user color, which leaves the palette as it is
     */
    public OverlayColorTint(int referenceRgb) {
        this.referenceRgb = referenceRgb & 0xFFFFFF;
        this.argb = PixelMath.opaque(referenceRgb);
    }

    /**
     * Reads the color of the property and returns true if it is another color than the last time.
     * Fixed colors only get parsed when the text of the property changes; colors with placeholders get resolved on every call.
     */
    public boolean update(@NotNull Property.ColorProperty property) {
        String raw = property.get();
        // Property values are kept as the same string instance until something sets a new value
        if ((raw != null) && (raw == this.lastRaw) && !raw.contains("{")) return false;
        this.lastRaw = raw;
        int resolved = property.getDrawable().getColorInt();
        if (resolved == this.argb) return false;
        this.argb = resolved;
        return true;
    }

    /**
     * The last read user color, ARGB.
     */
    public int argb() {
        return this.argb;
    }

    /**
     * The alpha of the last read user color, from 0 to 255.
     */
    public int alpha() {
        return this.argb >>> 24;
    }

    /**
     * Recolors one RGB color of the palette for the last read user color and gives it the alpha of the palette color times the user alpha.
     */
    public int tint(int rgb, int alpha) {
        return PixelMath.withAlpha(recolor(rgb, this.referenceRgb, this.argb), Math.round(alpha * (this.alpha() / 255.0F)));
    }

    /**
     * Recolors one opaque RGB color of the palette for the last read user color, see {@link #tint(int, int)}.
     */
    public int tint(int rgb) {
        return this.tint(rgb, 255);
    }

    /**
     * Recolors an RGB color of a palette whose reference color is {@code referenceRgb} for the color {@code userRgb}.
     * Alpha bytes get ignored; the result is RGB without alpha.
     */
    public static int recolor(int rgb, int referenceRgb, int userRgb) {
        int r = scaleChannel(PixelMath.red(rgb), PixelMath.red(referenceRgb), PixelMath.red(userRgb));
        int g = scaleChannel(PixelMath.green(rgb), PixelMath.green(referenceRgb), PixelMath.green(userRgb));
        int b = scaleChannel(PixelMath.blue(rgb), PixelMath.blue(referenceRgb), PixelMath.blue(userRgb));
        return (r << 16) | (g << 8) | b;
    }

    private static int scaleChannel(int value, int reference, int user) {
        if (reference <= 0) return user;
        return Math.min(255, Math.round(value * (user / (float)reference)));
    }

}
