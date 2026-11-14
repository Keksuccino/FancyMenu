package de.keksuccino.fancymenu.customization.element.style;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

/**
 * What the grabbers of the scroll bars of an element look like: textures and colors of the user, and the built-in pixel art handle without any of them.<br>
 * Textures get stretched to the size of the grabber as a whole. A color draws the grabber as a plain bar.
 */
public final class ScrollBarGrabberStyle {

    public final Property<ResourceSupplier<ITexture>> verticalTextureNormal;
    public final Property<ResourceSupplier<ITexture>> verticalTextureHover;
    public final Property<ResourceSupplier<ITexture>> horizontalTextureNormal;
    public final Property<ResourceSupplier<ITexture>> horizontalTextureHover;
    public final Property.ColorProperty colorNormal;
    public final Property.ColorProperty colorHover;

    /** The colors of the user, read once per frame. The suppliers the scroll bars ask for them are the same objects for every frame. */
    @Nullable
    private DrawableColor currentColorNormal;
    @Nullable
    private DrawableColor currentColorHover;
    private final Supplier<DrawableColor> colorNormalSupplier = () -> (this.currentColorNormal != null) ? this.currentColorNormal : this.currentColorHover;
    private final Supplier<DrawableColor> colorHoverSupplier = () -> (this.currentColorHover != null) ? this.currentColorHover : this.currentColorNormal;

    /**
     * @param holder The element that owns the scroll bars.
     */
    public ScrollBarGrabberStyle(@NotNull PropertyHolder holder) {
        // The entries share their names with the grabber textures and colors of text elements, which are the same thing
        this.verticalTextureNormal = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "vertical_grabber_texture_normal", null, "fancymenu.elements.text.vertical_scroll_grabber_texture.normal", true, true, true, null));
        this.verticalTextureHover = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "vertical_grabber_texture_hover", null, "fancymenu.elements.text.vertical_scroll_grabber_texture.hover", true, true, true, null));
        this.horizontalTextureNormal = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "horizontal_grabber_texture_normal", null, "fancymenu.elements.text.horizontal_scroll_grabber_texture.normal", true, true, true, null));
        this.horizontalTextureHover = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "horizontal_grabber_texture_hover", null, "fancymenu.elements.text.horizontal_scroll_grabber_texture.hover", true, true, true, null));
        this.colorNormal = holder.putProperty(Property.hexColorProperty("grabber_color_normal", null, true, "fancymenu.elements.text.scroll_grabber_color.normal"));
        this.colorHover = holder.putProperty(Property.hexColorProperty("grabber_color_hover", null, true, "fancymenu.elements.text.scroll_grabber_color.hover"));
    }

    /**
     * Gives the scroll bars the look of their grabbers for this frame.
     *
     * @param opacity The opacity of the element, between 0 and 1.
     */
    public void apply(@NotNull ScrollBar vertical, @NotNull ScrollBar horizontal, float opacity) {
        this.currentColorNormal = (this.colorNormal.get() != null) ? this.colorNormal.getDrawable() : null;
        this.currentColorHover = (this.colorHover.get() != null) ? this.colorHover.getDrawable() : null;
        // A color of the user stands in for the pixel art handle, which would hide it
        boolean hasColor = (this.currentColorNormal != null) || (this.currentColorHover != null);
        apply(vertical, this.verticalTextureNormal.get(), this.verticalTextureHover.get(), this.colorNormalSupplier, this.colorHoverSupplier, hasColor ? null : PixelScrollGrabber.getTexture(true, false), hasColor ? null : PixelScrollGrabber.getTexture(true, true), opacity);
        apply(horizontal, this.horizontalTextureNormal.get(), this.horizontalTextureHover.get(), this.colorNormalSupplier, this.colorHoverSupplier, hasColor ? null : PixelScrollGrabber.getTexture(false, false), hasColor ? null : PixelScrollGrabber.getTexture(false, true), opacity);
    }

    /**
     * Gives a scroll bar the look of its grabber: a texture of the user if there is one, the built-in texture otherwise,
     * and the colors for a grabber without any texture.
     *
     * @param builtInNormalTexture The texture the grabber has without one of the user, or NULL if it gets drawn in the colors then.
     * @param opacity The opacity of the element, between 0 and 1.
     */
    public static void apply(@NotNull ScrollBar bar, @Nullable ResourceSupplier<ITexture> normalTexture, @Nullable ResourceSupplier<ITexture> hoverTexture, @NotNull Supplier<DrawableColor> normalColor, @NotNull Supplier<DrawableColor> hoverColor, @Nullable ResourceLocation builtInNormalTexture, @Nullable ResourceLocation builtInHoverTexture, float opacity) {
        bar.grabberOpacity = opacity;
        bar.idleBarColor = normalColor;
        bar.hoverBarColor = hoverColor;
        bar.idleBarTexture = resolveTexture(normalTexture, bar.idleBarTexture, builtInNormalTexture);
        bar.hoverBarTexture = resolveTexture(hoverTexture, bar.hoverBarTexture, builtInHoverTexture);
    }

    /**
     * Returns the texture a grabber gets drawn with, or NULL if it gets drawn in a color.
     *
     * @param custom The texture of the user for the grabber, if there is one.
     * @param current The texture the grabber has right now. A texture of the user that is still loading keeps it in place,
     *                unless it is a built-in texture, which the user did not ask for anymore.
     * @param builtInTexture The texture the grabber has without one of the user.
     */
    @Nullable
    public static ResourceLocation resolveTexture(@Nullable ResourceSupplier<ITexture> custom, @Nullable ResourceLocation current, @Nullable ResourceLocation builtInTexture) {
        if (custom == null) return builtInTexture;
        ITexture texture = custom.get();
        if (texture != null) return texture.getResourceLocation();
        return PixelScrollGrabber.isTexture(current) ? null : current;
    }

}
