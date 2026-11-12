package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFragmentRenderComponentTest {

    @Test
    void componentStaysCachedWhileNothingChanges() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("plain **bold** [link](https://example.invalid)\n\n> quote", 400);
        renderer.tick();

        for (MarkdownTextFragment fragment : renderer.fragments) {
            Component component = fragment.getRenderComponent();
            assertSame(component, fragment.getRenderComponent());
            assertEquals(fragment.buildRenderComponent(false), component);
        }
    }

    @Test
    void settingsThatAreNotPartOfTheComponentKeepIt() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("plain [link](https://example.invalid)", 400);
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("link");
        Component component = link.getRenderComponent();

        renderer.setTextBaseColor(DrawableColor.BLACK);
        renderer.setTextShadow(false);
        renderer.setTextOpacity(0.3F);
        renderer.setCodeBlockSingleLineColor(DrawableColor.BLACK);
        renderer.setX(10).setY(20);
        renderer.setOptimalWidth(300);
        renderer.tick();

        assertSame(component, link.getRenderComponent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.invalid", "click:some_event", "hover:some_event"})
    void hoveringOneFragmentRebuildsAllFragmentsOfItsLink(String target) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("plain [first second](" + target + ") after", 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");
        MarkdownTextFragment plain = renderer.fragmentContaining("plain");
        Component firstComponent = first.getRenderComponent();
        Component plainComponent = plain.getRenderComponent();
        assertFalse(firstComponent.getStyle().isUnderlined());

        second.setHovered(true);

        Component hoveredComponent = first.getRenderComponent();
        assertNotSame(firstComponent, hoveredComponent);
        assertTrue(hoveredComponent.getStyle().isUnderlined());
        assertTrue(second.getRenderComponent().getStyle().isUnderlined());
        assertSame(hoveredComponent, first.getRenderComponent());
        assertSame(plainComponent, plain.getRenderComponent());

        second.setHovered(false);

        assertFalse(first.getRenderComponent().getStyle().isUnderlined());
        assertFalse(second.getRenderComponent().getStyle().isUnderlined());
    }

    @Test
    void linkStaysHoveredWhileAnyOfItsFragmentsIs() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[first second](https://example.invalid)", 400);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        MarkdownTextFragment second = renderer.fragmentContaining("second");

        first.setHovered(true);
        first.setHovered(true);
        second.setHovered(true);
        first.setHovered(false);

        assertTrue(first.hyperlink.isHovered());
        assertTrue(first.getRenderComponent().getStyle().isUnderlined());

        second.setHovered(false);

        assertFalse(first.hyperlink.isHovered());
        assertFalse(first.getRenderComponent().getStyle().isUnderlined());
    }

    @Test
    void linkColorChangeRebuildsTheComponent() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[link](https://example.invalid) [click](click:id) [hover](hover:id)", 400);
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("link");
        MarkdownTextFragment click = renderer.fragmentContaining("click");
        MarkdownTextFragment hover = renderer.fragmentContaining("hover");
        Component linkComponent = link.getRenderComponent();
        Component clickComponent = click.getRenderComponent();
        Component hoverComponent = hover.getRenderComponent();

        renderer.setHyperlinkColor(DrawableColor.of(1, 2, 3));
        renderer.setTextClickEventColor(DrawableColor.of(4, 5, 6));
        renderer.setTextHoverEventColor(DrawableColor.of(7, 8, 9));

        assertNotSame(linkComponent, link.getRenderComponent());
        assertNotSame(clickComponent, click.getRenderComponent());
        assertNotSame(hoverComponent, hover.getRenderComponent());
        assertEquals(0x010203, link.getRenderComponent().getStyle().getColor().getValue());
        assertEquals(0x040506, click.getRenderComponent().getStyle().getColor().getValue());
        assertEquals(0x070809, hover.getRenderComponent().getStyle().getColor().getValue());
    }

    @Test
    void quoteSettingsRebuildTheComponent() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("> quote", 400);
        renderer.tick();
        MarkdownTextFragment quote = renderer.fragmentContaining("quote");
        Component component = quote.getRenderComponent();
        assertFalse(component.getStyle().isItalic());

        renderer.setQuoteColor(DrawableColor.of(1, 2, 3));
        Component recolored = quote.getRenderComponent();
        renderer.setQuoteItalic(true);
        Component italic = quote.getRenderComponent();

        assertNotSame(component, recolored);
        assertEquals(0x010203, recolored.getStyle().getColor().getValue());
        assertNotSame(recolored, italic);
        assertTrue(italic.getStyle().isItalic());
    }

    @Test
    @ResourceLock("TextColorFormatterRegistry global state")
    void customFormatterColorChangeRebuildsTheComponent() {
        DrawableColor[] color = {DrawableColor.of(1, 2, 3)};
        TextColorFormatterRegistry.register("markdown_component_cache_test", new DynamicTextColorFormatter('w', () -> color[0]));
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("\u00A7wcolored", 400);
        renderer.tick();
        MarkdownTextFragment fragment = renderer.fragmentContaining("colored");
        Component component = fragment.getRenderComponent();
        assertEquals(0x010203, component.getStyle().getColor().getValue());
        assertSame(component, fragment.getRenderComponent());

        color[0] = DrawableColor.of(4, 5, 6);

        assertNotSame(component, fragment.getRenderComponent());
        assertEquals(0x040506, fragment.getRenderComponent().getStyle().getColor().getValue());
    }

    @Test
    void textCaseChangeRebuildsTheComponent() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Word", 400);
        renderer.tick();
        MarkdownTextFragment fragment = renderer.fragments.get(0);
        assertEquals("Word", fragment.getRenderComponent().getString());

        renderer.setTextCase(MarkdownRenderer.TextCase.ALL_UPPER);
        assertEquals("WORD", fragment.getRenderComponent().getString());

        renderer.setTextCase(MarkdownRenderer.TextCase.ALL_LOWER);
        assertEquals("word", fragment.getRenderComponent().getString());
    }

    @Test
    void updateWidthDropsTheComponent() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("word", 400);
        renderer.tick();
        MarkdownTextFragment fragment = renderer.fragments.get(0);
        Component component = fragment.getRenderComponent();

        fragment.text = "changed";
        fragment.bold = true;
        fragment.updateWidth();

        assertNotSame(component, fragment.getRenderComponent());
        assertEquals("changed", fragment.getRenderComponent().getString());
        assertTrue(fragment.getRenderComponent().getStyle().isBold());
    }

    @Test
    void lineEndRemovesTheTrailingSpaceOfLinks() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[first second](https://example.invalid) after", 2000);
        renderer.tick();
        MarkdownTextFragment first = renderer.fragmentContaining("first");
        assertTrue(first.text.endsWith(" "));
        assertEquals(first.text, first.getRenderComponent().getString());

        // Wraps the line right after the first word of the link
        renderer.setOptimalWidth(first.getRenderWidth() + 6);
        renderer.tick();

        assertTrue(first.autoLineBreakAfter);
        assertEquals("first", first.getRenderComponent().getString());

        renderer.setOptimalWidth(2000);
        renderer.tick();

        assertFalse(first.autoLineBreakAfter);
        assertEquals(first.text, first.getRenderComponent().getString());
    }

    @Test
    void lastFragmentOfALinkKeepsItsTrailingSpaceOutOfTheLinkStyle() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("[first second](https://example.invalid) after", 2000);
        renderer.tick();
        MarkdownTextFragment second = renderer.fragmentContaining("second");
        assertTrue(second.text.endsWith(" "));
        second.setHovered(true);

        Component component = second.getRenderComponent();

        assertEquals("second ", component.getString());
        assertTrue(component.getStyle().isUnderlined());
        assertEquals(1, component.getSiblings().size());
        assertEquals(" ", component.getSiblings().get(0).getString());
        assertFalse(component.getSiblings().get(0).getStyle().isUnderlined());
    }

}
