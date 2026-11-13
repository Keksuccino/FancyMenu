package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownCalloutIcons;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownCalloutStyleTest {

    private static final int LINE_HEIGHT = 9;

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void everyTypeHasItsOwnLabelAndColorsInEveryLook(MarkdownLook look) {
        for (MarkdownTextFragment.CalloutType type : MarkdownTextFragment.CalloutType.values()) {
            MarkdownTestRenderer renderer = renderer("> [!" + type.name() + "]\n> body", look);
            MarkdownStyle.Callout callout = renderer.getStyle().callout(type.ordinal());
            MarkdownTextFragment marker = renderer.fragments.get(0);
            assertTrue(marker.calloutMarker);

            Component label = marker.getRenderComponent();

            assertFalse(label.getString().isEmpty());
            assertEquals(MarkdownCalloutPainter.getLabel(type), label.getString());
            assertEquals(callout.color.argb() & 0xFFFFFF, label.getStyle().getColor().getValue());
            assertTrue(label.getStyle().isBold());
            assertEquals(label.getString().length() * 6, marker.unscaledTextWidth);
            assertSame(callout.color, MarkdownQuotePainter.getBarColor(renderer, marker.quoteContext));
            // The text of a callout looks like the text of a quote unless the style gives it a color of its own
            MarkdownStyleOption.ColorOption textColor = renderer.getStyle().calloutTextColor.isSet() ? renderer.getStyle().calloutTextColor : renderer.getStyle().quoteTextColor;
            assertEquals(textColor.argb() & 0xFFFFFF, renderer.fragmentContaining("body").getRenderComponent().getStyle().getColor().getValue());
            for (MarkdownTextFragment.CalloutType other : MarkdownTextFragment.CalloutType.values()) {
                if (other != type) assertNotSame(callout, renderer.getStyle().callout(other.ordinal()));
            }
        }
    }

    @Test
    void pixelLookPutsTheIconAndTheLabelIntoTheFirstLineOfThePanel() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> [!TIP]\n> body\n> more", 400);
        renderer.setParentRenderScale(3.0F);
        renderer.setCalloutsEnabled(true);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.setX(30).setY(50);
        renderer.tick();
        MarkdownTextFragment marker = renderer.fragments.get(0);
        MarkdownTextFragment body = renderer.fragmentContaining("body");
        MarkdownTextFragment more = renderer.fragmentContaining("more");

        // The icon stands where the text of the quote starts, and the label 3 pixels behind the 7 pixels of the icon
        assertEquals(30 + 4 + 8 + 10, marker.getTextX());
        assertEquals(30 + 4 + 8, body.getTextX());
        assertEquals(marker.getTextY() + 1, MarkdownCalloutPainter.getIconY(marker));
        // The text starts 12 pixels below the label, and the panel has 4 pixels of padding above and below
        assertEquals(marker.y + 4, marker.getTextY());
        assertEquals(marker.getTextY() + 12, body.getTextY());
        assertEquals(body.getTextY() + 11, more.getTextY());
        assertEquals(more.getTextY() + LINE_HEIGHT + 4, MarkdownQuotePainter.getBottom(marker.quoteContext));
        assertSame(renderer.getStyle().callout(1).backgroundColor, MarkdownQuotePainter.getBackgroundColor(renderer, marker.quoteContext));
    }

    @Test
    void calloutWithoutTextIsAPanelWithNothingButItsLabel() {
        MarkdownTestRenderer renderer = renderer("> [!NOTE]\n\nafter", MarkdownLook.PIXEL);
        MarkdownTextFragment marker = renderer.fragments.get(0);

        assertEquals(4 + LINE_HEIGHT + 4, marker.parentLine.getLineHeight());
        assertSame(marker.parentLine, renderer.lines.get(0));
        assertEquals(MarkdownBlockType.QUOTE, marker.parentLine.blockType);
    }

    @Test
    void calloutWithoutPanelColorOfItsOwnHasThePanelOfQuotes() {
        MarkdownTestRenderer renderer = renderer("> [!WARNING]\n> body\n\n> quote", MarkdownLook.CLASSIC);
        MarkdownStyle style = renderer.getStyle();
        MarkdownTextFragment marker = renderer.fragments.get(0);

        assertFalse(MarkdownQuotePainter.getBackgroundColor(renderer, marker.quoteContext).isSet());

        style.quoteBackgroundColor.set(DrawableColor.of(1, 2, 3));
        assertSame(style.quoteBackgroundColor, MarkdownQuotePainter.getBackgroundColor(renderer, marker.quoteContext));

        style.callout(3).backgroundColor.set(DrawableColor.of(4, 5, 6));
        assertSame(style.callout(3).backgroundColor, MarkdownQuotePainter.getBackgroundColor(renderer, marker.quoteContext));
        assertSame(style.quoteBackgroundColor, MarkdownQuotePainter.getBackgroundColor(renderer, renderer.fragmentContaining("quote").quoteContext));
        assertSame(style.quoteBarColor, MarkdownQuotePainter.getBarColor(renderer, renderer.fragmentContaining("quote").quoteContext));
    }

    @Test
    void labelWithoutAnIconStartsWhereTheTextOfTheCalloutStarts() {
        MarkdownTestRenderer renderer = renderer("> [!NOTE]\n> body", MarkdownLook.PIXEL);
        renderer.getStyle().calloutIcons.set(MarkdownCalloutIcons.NONE);
        renderer.tick();

        assertEquals(renderer.fragmentContaining("body").getTextX(), renderer.fragments.get(0).getTextX());
    }

    @Test
    void calloutMarkerStaysTextWhileCalloutsAreOff() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> [!NOTE]\n> body", 400);
        renderer.tick();

        assertFalse(renderer.fragments.get(0).calloutMarker);
        assertSame(renderer.getStyle().quoteBarColor, MarkdownQuotePainter.getBarColor(renderer, renderer.fragments.get(0).quoteContext));
    }

    private static MarkdownTestRenderer renderer(String text, MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.setCalloutsEnabled(true);
        renderer.getStyle().applyLook(look);
        renderer.tick();
        return renderer;
    }

}
