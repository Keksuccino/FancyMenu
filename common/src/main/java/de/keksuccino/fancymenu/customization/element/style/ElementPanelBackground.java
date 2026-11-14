package de.keksuccino.fancymenu.customization.element.style;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.function.IntSupplier;

/**
 * The background of an element that shows content in an area of its own, like text or other elements: a texture behind the content and its scroll grabbers.<br>
 * It covers the content area of the element, grown by a padding on every side, so the content stays where it is when the background gets switched on or off.
 * Without a texture of the user, it is a dark panel in a pixel look.
 */
public final class ElementPanelBackground {

    /** The key of the property that switches the background on. Changing it breaks saved layouts. */
    public static final String ENABLED_KEY = "background";
    /** The key the texts start with that are the same for the backgrounds of all elements. */
    public static final String SHARED_LOCALIZATION_KEY_BASE = "fancymenu.elements.text.background";
    /** The built-in panel. The pixels of its borders have to stay as big as the pixels of the GUI, so it always gets nine-sliced with its own border size. */
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/markdown/text_background.png");
    private static final int PANEL_TEXTURE_SIZE = 12;
    private static final int PANEL_TEXTURE_BORDER = 2;

    /** Elements that got saved without a background have none. */
    public final Property.BooleanProperty enabled;
    /** How far the background reaches beyond the content area on every side. */
    public final Property.IntegerProperty padding;
    /** If the background also covers the space next to the content area that a shown scroll bar has its grabber in. Otherwise the scroll bars stand outside of the background. */
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
    private final String descriptionKeyBase;
    private final IntSupplier scrollBarSpace;

    /**
     * @param holder The element that owns the background.
     * @param descriptionKeyBase The key the descriptions start with that say what the background is behind, without a dot at its end. Every kind of element has its own.
     * @param scrollBarSpace Returns the space a scroll bar of the element has its grabber in, next to the content area.
     */
    public ElementPanelBackground(@NotNull PropertyHolder holder, @NotNull String descriptionKeyBase, @NotNull IntSupplier scrollBarSpace) {
        this.descriptionKeyBase = descriptionKeyBase;
        this.scrollBarSpace = scrollBarSpace;
        this.enabled = holder.putProperty(Property.booleanProperty(ENABLED_KEY, false, SHARED_LOCALIZATION_KEY_BASE + ".enabled"));
        this.padding = holder.putProperty(Property.integerProperty("background_padding", 6, SHARED_LOCALIZATION_KEY_BASE + ".padding", Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 64).build()));
        this.behindScrollBars = holder.putProperty(Property.booleanProperty("background_behind_scroll_bars", true, SHARED_LOCALIZATION_KEY_BASE + ".behind_scroll_bars"));
        this.texture = holder.putProperty(Property.resourceSupplierProperty(ITexture.class, "background_texture", null, "fancymenu.elements.tooltip.background_texture", true, true, true, null));
        this.nineSlice = holder.putProperty(Property.booleanProperty("background_nine_slice", true, "fancymenu.elements.buttons.textures.nine_slice"));
        this.nineSliceBorderX = holder.putProperty(Property.integerProperty("background_nine_slice_border_x", PANEL_TEXTURE_BORDER, "fancymenu.elements.buttons.textures.nine_slice.border_x"));
        this.nineSliceBorderY = holder.putProperty(Property.integerProperty("background_nine_slice_border_y", PANEL_TEXTURE_BORDER, "fancymenu.elements.buttons.textures.nine_slice.border_y"));
        this.tile = holder.putProperty(Property.booleanProperty("background_tile", false, SHARED_LOCALIZATION_KEY_BASE + ".tile"));
    }

    /**
     * Returns the key the descriptions start with that say what this background is behind, without a dot at its end.
     */
    @NotNull
    public String getDescriptionKeyBase() {
        return this.descriptionKeyBase;
    }

    /**
     * Turns the element into what a new element is: one with a background.
     */
    public void applyDefaultsOfNewElements() {
        this.enabled.set(true);
    }

    /**
     * Draws the background around the given content area, if the element has one. A texture of the user that is still loading or missing draws nothing.
     *
     * @param verticalScrollBarShown If the element shows its vertical scroll bar right now.
     * @param horizontalScrollBarShown If the element shows its horizontal scroll bar right now.
     * @param opacity The opacity of the element, between 0 and 1.
     */
    public void render(@NotNull GuiGraphics graphics, int x, int y, int width, int height, boolean verticalScrollBarShown, boolean horizontalScrollBarShown, float opacity) {
        this.render(graphics, x, y, width, height, verticalScrollBarShown, horizontalScrollBarShown, false, false, opacity);
    }

    /**
     * Draws the background around the given content area, for an element whose scroll bars can also be at the left and the top of it.
     *
     * @param verticalScrollBarAtStart If the vertical scroll bar is at the left of the content area instead of its right.
     * @param horizontalScrollBarAtStart If the horizontal scroll bar is at the top of the content area instead of its bottom.
     */
    public void render(@NotNull GuiGraphics graphics, int x, int y, int width, int height, boolean verticalScrollBarShown, boolean horizontalScrollBarShown, boolean verticalScrollBarAtStart, boolean horizontalScrollBarAtStart, float opacity) {
        if (!this.enabled.getBoolean()) return;
        ResourceLocation location = PANEL_TEXTURE;
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
        int[] area = this.updateArea(x, y, width, height, verticalScrollBarShown, horizontalScrollBarShown, verticalScrollBarAtStart, horizontalScrollBarAtStart);
        int color = RenderingUtils.multiplyAlphaInColor(-1, opacity);
        // Textures get drawn in the shader color, and they only let what is behind them shine through while blending is on
        RenderSystem.enableBlend();
        RenderingUtils.setShaderColor(graphics, color);
        if (nineSliced) {
            // A stretched texture is nine pieces for every size of the element, a tiled one is a piece per repetition
            RenderingUtils.blitNineSlicedTexture(graphics, location, area[0], area[1], area[2], area[3], textureWidth, textureHeight, borderY, borderX, borderY, borderX, !tiled);
        } else if (tiled) {
            RenderingUtils.blitRepeat(graphics, location, area[0], area[1], area[2], area[3], textureWidth, textureHeight);
        } else {
            graphics.blit(location, area[0], area[1], area[2], area[3], 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    /**
     * Returns how far the background moves the scroll bars of the element away from its content area.<br>
     * A background that does not reach behind the scroll bars has them outside of it, right behind its edge, so a grabber never lies across its frame.
     * Without a background, and with one that reaches behind the scroll bars, they stand right next to the content area.
     */
    public int getScrollBarDistance() {
        if (!this.enabled.getBoolean() || this.behindScrollBars.getBoolean()) return 0;
        return Math.max(0, this.padding.getInteger());
    }

    /**
     * Works out the area the background covers around the given content area: the content area grown by the padding on every side,
     * and by the space of every shown scroll bar the background reaches behind.
     *
     * @param verticalScrollBarAtStart If the vertical scroll bar is at the left of the content area instead of its right.
     * @param horizontalScrollBarAtStart If the horizontal scroll bar is at the top of the content area instead of its bottom.
     * @return X, Y, width and height of the background. Every call returns the same array.
     */
    public int[] updateArea(int x, int y, int width, int height, boolean verticalScrollBarShown, boolean horizontalScrollBarShown, boolean verticalScrollBarAtStart, boolean horizontalScrollBarAtStart) {
        int padding = Math.max(0, this.padding.getInteger());
        boolean behindScrollBars = this.behindScrollBars.getBoolean();
        int verticalBarSpace = (behindScrollBars && verticalScrollBarShown) ? this.scrollBarSpace.getAsInt() : 0;
        int horizontalBarSpace = (behindScrollBars && horizontalScrollBarShown) ? this.scrollBarSpace.getAsInt() : 0;
        this.area[0] = x - padding - (verticalScrollBarAtStart ? verticalBarSpace : 0);
        this.area[1] = y - padding - (horizontalScrollBarAtStart ? horizontalBarSpace : 0);
        this.area[2] = width + (padding * 2) + verticalBarSpace;
        this.area[3] = height + (padding * 2) + horizontalBarSpace;
        return this.area;
    }

}
