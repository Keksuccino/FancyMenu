package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettings;
import de.keksuccino.fancymenu.customization.element.style.PixelScrollGrabber;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * What the scroll grabbers of a text element look like while the user gave them neither textures nor colors of their own.
 */
public enum ScrollGrabberDesign {

    /** A bar in the grabber colors of the UI theme. */
    THEME_BAR(false),
    /**
     * A pixel art handle that goes with the pixel look of the text. Its textures are as big as the grabbers of a text element,
     * one texture pixel per GUI pixel: 10x20 for the vertical grabber and 20x10 for the horizontal one.
     */
    PIXEL_HANDLE(true);

    /** The key of the property that holds the design of an element. Changing it breaks saved layouts. */
    public static final String PROPERTY_KEY = "scroll_grabber_design";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.text.scroll_grabber_design";

    @Nullable
    private final ResourceLocation verticalNormalTexture;
    @Nullable
    private final ResourceLocation verticalHoverTexture;
    @Nullable
    private final ResourceLocation horizontalNormalTexture;
    @Nullable
    private final ResourceLocation horizontalHoverTexture;
    private final String localizationKey = LOCALIZATION_KEY + "." + this.name().toLowerCase(Locale.ROOT);

    ScrollGrabberDesign(boolean textured) {
        this.verticalNormalTexture = textured ? PixelScrollGrabber.getTexture(true, false) : null;
        this.verticalHoverTexture = textured ? PixelScrollGrabber.getTexture(true, true) : null;
        this.horizontalNormalTexture = textured ? PixelScrollGrabber.getTexture(false, false) : null;
        this.horizontalHoverTexture = textured ? PixelScrollGrabber.getTexture(false, true) : null;
    }

    /**
     * Builds the property that holds the design of a text element. Elements that got saved without it keep the bar in the colors of the UI theme.
     */
    @NotNull
    public static Property<ScrollGrabberDesign> buildProperty() {
        return Property.enumProperty(PROPERTY_KEY, THEME_BAR, LOCALIZATION_KEY, ScrollGrabberDesign::getLocalizationKey);
    }

    /**
     * Makes the design of an element the one that goes with every look that gets applied to its text from now on.
     * The property stays one of its own, so the design can get changed without touching the look.
     */
    public static void followAppliedLooks(@NotNull Property<ScrollGrabberDesign> property, @NotNull MarkdownSettings settings) {
        settings.setLookListener(look -> property.set(ofLook(look)));
    }

    /**
     * Returns the design that goes with the given look of the text.
     */
    @NotNull
    public static ScrollGrabberDesign ofLook(@NotNull MarkdownLook look) {
        return (look == MarkdownLook.PIXEL) ? PIXEL_HANDLE : THEME_BAR;
    }

    /**
     * Returns the design the grabbers show: this one, or the bar in the colors of the UI theme if the user gave the grabbers
     * a texture or a color of their own. What the user set always wins, and it is made to stand in for the bar.
     */
    @NotNull
    public ScrollGrabberDesign unlessCustomized(boolean customized) {
        return customized ? THEME_BAR : this;
    }

    /**
     * Returns the texture of a grabber, or NULL if the design draws its grabbers in a color.
     *
     * @param hovered If the mouse is over the grabber or drags it.
     */
    @Nullable
    public ResourceLocation getTexture(boolean vertical, boolean hovered) {
        if (vertical) return hovered ? this.verticalHoverTexture : this.verticalNormalTexture;
        return hovered ? this.horizontalHoverTexture : this.horizontalNormalTexture;
    }

    @NotNull
    public String getLocalizationKey() {
        return this.localizationKey;
    }

}
