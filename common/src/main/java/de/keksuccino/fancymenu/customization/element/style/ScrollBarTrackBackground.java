package de.keksuccino.fancymenu.customization.element.style;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * The backgrounds of the scroll bars of an element: a texture per bar that fills the track its grabber moves in.<br>
 * The textures get nine-sliced, so their edges keep their shape on tracks of every length. Without a texture, a bar has no background.
 */
public final class ScrollBarTrackBackground {

    private static final String LOCALIZATION_KEY_BASE = "fancymenu.elements.scroll_bar_background.";

    public final Property<ResourceSupplier<ITexture>> verticalTexture;
    public final Property<ResourceSupplier<ITexture>> horizontalTexture;
    /** The nine-slice borders work like the ones of button backgrounds and share their names. */
    public final Property.IntegerProperty nineSliceBorderX;
    public final Property.IntegerProperty nineSliceBorderY;

    /**
     * @param holder The element that owns the scroll bars.
     */
    public ScrollBarTrackBackground(@NotNull PropertyHolder holder) {
        this.verticalTexture = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "vertical_scroll_bar_background_texture", null, LOCALIZATION_KEY_BASE + "vertical_texture", true, true, true, null));
        this.horizontalTexture = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "horizontal_scroll_bar_background_texture", null, LOCALIZATION_KEY_BASE + "horizontal_texture", true, true, true, null));
        this.nineSliceBorderX = holder.putProperty(Property.integerProperty("scroll_bar_background_nine_slice_border_x", 2, "fancymenu.elements.buttons.textures.nine_slice.border_x"));
        this.nineSliceBorderY = holder.putProperty(Property.integerProperty("scroll_bar_background_nine_slice_border_y", 2, "fancymenu.elements.buttons.textures.nine_slice.border_y"));
    }

    /**
     * Returns if any of the bars has a background.
     */
    public boolean hasAnyTexture() {
        return (this.verticalTexture.get() != null) || (this.horizontalTexture.get() != null);
    }

    /**
     * Draws the background of a scroll bar into its track. Has to be called before the bar renders its grabber.
     * A texture that is still loading or missing draws nothing.
     *
     * @param vertical If the bar scrolls up and down.
     * @param opacity The opacity of the element, between 0 and 1.
     */
    public void render(@NotNull GuiGraphicsExtractor graphics, @NotNull ScrollBar bar, boolean vertical, float opacity) {
        ResourceSupplier<ITexture> supplier = vertical ? this.verticalTexture.get() : this.horizontalTexture.get();
        if (supplier == null) return;
        ITexture texture = supplier.get();
        if ((texture == null) || !texture.isReady()) return;
        Identifier location = texture.getResourceLocation();
        if (location == null) return;
        int x = Math.round(bar.getTrackX());
        int y = Math.round(bar.getTrackY());
        int width = Math.round(bar.getTrackWidth());
        int height = Math.round(bar.getTrackHeight());
        if ((width <= 0) || (height <= 0)) return;
        int borderX = Math.max(0, this.nineSliceBorderX.getInteger());
        int borderY = Math.max(0, this.nineSliceBorderY.getInteger());
        RenderingUtils.blitNineSlicedTexture(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, width, height, Math.max(1, texture.getWidth()), Math.max(1, texture.getHeight()), borderY, borderX, borderY, borderX, RenderingUtils.multiplyAlphaInColor(-1, opacity), true);
    }

}
