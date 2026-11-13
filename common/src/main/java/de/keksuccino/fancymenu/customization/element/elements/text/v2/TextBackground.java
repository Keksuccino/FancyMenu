package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * The background of a text element: a texture behind its text and its scroll grabbers.<br>
 * It covers the text area of the element, grown by a padding on every side, so the text stays where it is when the background gets switched on or off.
 * Without a texture of the user, it is a dark panel that goes with the pixel look of the text.
 */
public final class TextBackground {

    /** The key of the property that switches the background on. Changing it breaks saved layouts. */
    public static final String ENABLED_KEY = "background";
    private static final String LOCALIZATION_KEY_BASE = "fancymenu.elements.text.background.";
    /** The built-in panel. The pixels of its borders have to stay as big as the pixels of the GUI, so it always gets nine-sliced with its own border size. */
    private static final Identifier PANEL_TEXTURE = Identifier.fromNamespaceAndPath("fancymenu", "textures/markdown/text_background.png");
    private static final int PANEL_TEXTURE_SIZE = 12;
    private static final int PANEL_TEXTURE_BORDER = 2;

    /** Elements that got saved without a background have none. */
    public final Property.BooleanProperty enabled;
    /** How far the background reaches beyond the text area on every side. */
    public final Property.IntegerProperty padding;
    /** If the background also covers the space next to the text area that a shown scroll bar has its grabber in. Otherwise the scroll bars stand outside of the background. */
    public final Property.BooleanProperty behindScrollBars;
    /** The texture of the user. Without one, the background is the built-in panel. Its entry shares its name with the background texture of tooltip elements. */
    public final Property<ResourceSupplier<ITexture>> texture;
    /** If the texture of the user gets nine-sliced instead of stretched as a whole. The nine-slice properties work like the ones of button backgrounds and share their names. */
    public final Property.BooleanProperty nineSlice;
    public final Property.IntegerProperty nineSliceBorderX;
    public final Property.IntegerProperty nineSliceBorderY;
    /**
     * If the texture of the user gets repeated instead of stretched, which keeps a pattern in it at its size. A nine-sliced texture repeats
     * its edges and its center the way button backgrounds do, and a texture that is not nine-sliced gets repeated as a whole.
     */
    public final Property.BooleanProperty tile;

    private final int[] area = new int[4];

    /**
     * @param holder The element that owns the background.
     */
    public TextBackground(@NotNull PropertyHolder holder) {
        this.enabled = holder.putProperty(Property.booleanProperty(ENABLED_KEY, false, LOCALIZATION_KEY_BASE + "enabled"));
        this.padding = holder.putProperty(Property.integerProperty("background_padding", 6, LOCALIZATION_KEY_BASE + "padding", Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 64).build()));
        this.behindScrollBars = holder.putProperty(Property.booleanProperty("background_behind_scroll_bars", true, LOCALIZATION_KEY_BASE + "behind_scroll_bars"));
        this.texture = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "background_texture", null, "fancymenu.elements.tooltip.background_texture", true, true, true, null));
        this.nineSlice = holder.putProperty(Property.booleanProperty("background_nine_slice", true, "fancymenu.elements.buttons.textures.nine_slice"));
        this.nineSliceBorderX = holder.putProperty(Property.integerProperty("background_nine_slice_border_x", PANEL_TEXTURE_BORDER, "fancymenu.elements.buttons.textures.nine_slice.border_x"));
        this.nineSliceBorderY = holder.putProperty(Property.integerProperty("background_nine_slice_border_y", PANEL_TEXTURE_BORDER, "fancymenu.elements.buttons.textures.nine_slice.border_y"));
        this.tile = holder.putProperty(Property.booleanProperty("background_tile", false, LOCALIZATION_KEY_BASE + "tile"));
    }

    /**
     * Turns the element into what a new element is: one with a background.
     */
    public void applyDefaultsOfNewElements() {
        this.enabled.set(true);
    }

    /**
     * Draws the background around the given text area, if the element has one. A texture of the user that is still loading or missing draws nothing.
     *
     * @param scrollArea The scroll area of the element, which knows which of its scroll bars are shown right now.
     * @param opacity The opacity of the element, between 0 and 1.
     */
    public void render(@NotNull GuiGraphics graphics, int x, int y, int width, int height, @NotNull ScrollArea scrollArea, float opacity) {
        if (!this.enabled.getBoolean()) return;
        Identifier location = PANEL_TEXTURE;
        int textureWidth = PANEL_TEXTURE_SIZE;
        int textureHeight = PANEL_TEXTURE_SIZE;
        int borderX = PANEL_TEXTURE_BORDER;
        int borderY = PANEL_TEXTURE_BORDER;
        boolean nineSliced = true;
        // The built-in panel has nothing in it that stretching would bend out of shape, and stretching is fewer pieces to draw
        boolean tiled = false;
        ResourceSupplier<ITexture> supplier = this.texture.get();
        if (supplier != null) {
            ITexture custom = supplier.get();
            if ((custom == null) || !custom.isReady()) return;
            location = custom.getResourceLocation();
            if (location == null) return;
            textureWidth = Math.max(1, custom.getWidth());
            textureHeight = Math.max(1, custom.getHeight());
            nineSliced = this.nineSlice.getBoolean();
            borderX = Math.max(0, this.nineSliceBorderX.getInteger());
            borderY = Math.max(0, this.nineSliceBorderY.getInteger());
            tiled = this.tile.getBoolean();
        }
        int[] area = this.updateArea(x, y, width, height, scrollArea.isVerticalScrollBarVisible(), scrollArea.isHorizontalScrollBarVisible());
        int color = RenderingUtils.multiplyAlphaInColor(-1, opacity);
        if (nineSliced) {
            // A stretched texture is nine pieces for every size of the element, a tiled one is a piece per repetition
            RenderingUtils.blitNineSlicedTexture(graphics, RenderPipelines.GUI_TEXTURED, location, area[0], area[1], area[2], area[3], textureWidth, textureHeight, borderY, borderX, borderY, borderX, color, !tiled);
        } else if (tiled) {
            RenderingUtils.blitRepeat(graphics, RenderPipelines.GUI_TEXTURED, location, area[0], area[1], area[2], area[3], textureWidth, textureHeight, color);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, location, area[0], area[1], 0.0F, 0.0F, area[2], area[3], textureWidth, textureHeight, textureWidth, textureHeight, color);
        }
    }

    /**
     * Returns how far the background moves the scroll bars of the element away from its text area.<br>
     * A background that does not reach behind the scroll bars has them outside of it, right behind its edge, so a grabber never lies across its frame.
     * Without a background, and with one that reaches behind the scroll bars, they stand right next to the text area.
     */
    public int getScrollBarDistance() {
        if (!this.enabled.getBoolean() || this.behindScrollBars.getBoolean()) return 0;
        return Math.max(0, this.padding.getInteger());
    }

    /**
     * Works out the area the background covers around the given text area: the text area grown by the padding on every side,
     * and by the space of every shown scroll bar the background reaches behind.
     *
     * @return X, Y, width and height of the background. Every call returns the same array.
     */
    int[] updateArea(int x, int y, int width, int height, boolean verticalScrollBarShown, boolean horizontalScrollBarShown) {
        int padding = Math.max(0, this.padding.getInteger());
        boolean behindScrollBars = this.behindScrollBars.getBoolean();
        this.area[0] = x - padding;
        this.area[1] = y - padding;
        this.area[2] = width + (padding * 2) + ((behindScrollBars && verticalScrollBarShown) ? TextElement.SCROLL_BAR_SPACE : 0);
        this.area[3] = height + (padding * 2) + ((behindScrollBars && horizontalScrollBarShown) ? TextElement.SCROLL_BAR_SPACE : 0);
        return this.area;
    }

}
