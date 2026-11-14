package de.keksuccino.fancymenu.customization.element.style;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The built-in pixel art handle for scroll grabbers of elements. Its textures are made for grabbers of {@link #THICKNESS} by {@link #LENGTH},
 * one texture pixel per GUI pixel.
 */
public final class PixelScrollGrabber {

    /** The size of the handle across the direction its scroll bar scrolls in. */
    public static final int THICKNESS = 10;
    /** The size of the handle along the direction its scroll bar scrolls in. */
    public static final int LENGTH = 20;

    private static final Identifier VERTICAL_NORMAL = texture("vertical_normal");
    private static final Identifier VERTICAL_HOVER = texture("vertical_hover");
    private static final Identifier HORIZONTAL_NORMAL = texture("horizontal_normal");
    private static final Identifier HORIZONTAL_HOVER = texture("horizontal_hover");

    private PixelScrollGrabber() {
    }

    @NotNull
    private static Identifier texture(@NotNull String name) {
        return Identifier.fromNamespaceAndPath("fancymenu", "textures/markdown/scroll_grabber_" + name + ".png");
    }

    /**
     * Returns the texture of the handle.
     *
     * @param vertical If the handle is for a scroll bar that scrolls up and down.
     * @param hovered If the mouse is over the grabber or drags it.
     */
    @NotNull
    public static Identifier getTexture(boolean vertical, boolean hovered) {
        if (vertical) return hovered ? VERTICAL_HOVER : VERTICAL_NORMAL;
        return hovered ? HORIZONTAL_HOVER : HORIZONTAL_NORMAL;
    }

    /**
     * Returns if the texture is one of the textures of the handle.
     */
    public static boolean isTexture(@Nullable Identifier texture) {
        return (texture != null) && (texture.equals(VERTICAL_NORMAL) || texture.equals(VERTICAL_HOVER) || texture.equals(HORIZONTAL_NORMAL) || texture.equals(HORIZONTAL_HOVER));
    }

}
