package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownCalloutIcons;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Knows what turns a quote into a callout: the colors of its type, and the icon and the label in its first line.<br>
 * The parser leaves one empty fragment for the marker line of a callout ("> [!NOTE]"). That fragment shows the label
 * like every fragment shows its text, and keeps the room for the icon in front of it.
 */
final class MarkdownCalloutPainter {

    private static final float SMOOTH_ICON_SCALE = 1.25F;
    private static final String[] LABEL_KEYS = {"fancymenu.markdown.callout.note", "fancymenu.markdown.callout.tip", "fancymenu.markdown.callout.important", "fancymenu.markdown.callout.warning", "fancymenu.markdown.callout.caution"};

    private MarkdownCalloutPainter() {
    }

    /**
     * Returns the options of the callout the quote is, or NULL if it is a normal quote.
     */
    @Nullable
    static MarkdownStyle.Callout getCallout(@NotNull MarkdownRenderer renderer, @Nullable MarkdownTextFragment.QuoteContext context) {
        if ((context == null) || (context.calloutType == null)) return null;
        return renderer.style.callout(context.calloutType.ordinal());
    }

    /**
     * Returns the localized label of a callout type.
     */
    @NotNull
    static String getLabel(@NotNull MarkdownTextFragment.CalloutType type) {
        return I18n.get(LABEL_KEYS[type.ordinal()]);
    }

    /**
     * Returns the size of the icon in front of the label of a callout, in pixels of text at its normal size.
     */
    static float getIconSize(@NotNull MarkdownRenderer renderer) {
        switch (renderer.style.calloutIcons.get()) {
            case PIXEL:
                return MarkdownPixelArt.CALLOUT_ICON_SIZE;
            case SMOOTH:
                // A bit taller than a capital letter, because icons fill their area less than letters do
                return renderer.textFont.metrics().baseline * SMOOTH_ICON_SCALE;
            default:
                return 0.0F;
        }
    }

    /**
     * Returns the space the label of a callout keeps free in front of it for the icon, in pixels of text at its normal size.
     */
    static float getIconSpace(@NotNull MarkdownRenderer renderer) {
        float size = getIconSize(renderer);
        return (size > 0.0F) ? (size + renderer.style.calloutIconGap.get()) : 0.0F;
    }

    /**
     * Returns the Y position of the icon of the callout the given marker fragment starts.
     */
    static float getIconY(@NotNull MarkdownTextFragment marker) {
        MarkdownRenderer renderer = marker.parent;
        float scale = marker.getScale();
        float size = getIconSize(renderer) * scale;
        if (renderer.style.calloutIcons.get() == MarkdownCalloutIcons.SMOOTH) {
            // The middle of the icon is the middle of the capital letters of the label
            return marker.getTextY() + ((renderer.textFont.metrics().baseline * scale) - size) / 2;
        }
        // Pixel art stands in the middle of the line of the label, on whole pixels of the text, which keeps it on the grid of the font
        return marker.getTextY() + ((float) Math.floor(((marker.getTextRenderHeight() - size) / 2) / scale) * scale);
    }

    /**
     * Draws the icon of the callout the given marker fragment starts.
     */
    static void renderIcon(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment marker) {
        MarkdownRenderer renderer = marker.parent;
        MarkdownStyle.Callout callout = getCallout(renderer, marker.quoteContext);
        MarkdownCalloutIcons icons = renderer.style.calloutIcons.get();
        float scale = marker.getScale();
        if ((callout == null) || (icons == MarkdownCalloutIcons.NONE) || (scale <= 0.0F)) return;
        float x = marker.getTextX() - (getIconSpace(renderer) * scale);
        float y = getIconY(marker);
        int color = renderer.getRenderColor(callout.color);
        if (icons == MarkdownCalloutIcons.SMOOTH) {
            float size = getIconSize(renderer) * scale;
            getIcon(marker.quoteContext.calloutType).render(graphics, x, y, size, size, color);
        } else {
            MarkdownPixelArt.calloutIcon(graphics, marker.quoteContext.calloutType.ordinal(), x, y, scale, color, renderer.getRenderColor(renderer.style.calloutIconDetailColor), renderer.textFont.hasShadow());
        }
    }

    @NotNull
    private static MaterialIcon getIcon(@NotNull MarkdownTextFragment.CalloutType type) {
        switch (type) {
            case TIP:
                return MaterialIcons.LIGHTBULB;
            case IMPORTANT:
                return MaterialIcons.CHAT_ERROR;
            case WARNING:
                return MaterialIcons.WARNING;
            case CAUTION:
                return MaterialIcons.REPORT;
            default:
                return MaterialIcons.INFO;
        }
    }

}
