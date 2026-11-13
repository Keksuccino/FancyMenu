package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettings;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTestRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextBackgroundTest {

    /** A text element as its builder saved it before text elements had a background. */
    private static final String ELEMENT_SAVED_WITHOUT_BACKGROUND = """
              interactable = true
              source = # Headline One%n%Plain text
              source_mode = direct
              shadow = true
              enable_scrolling = true
              auto_line_wrapping = true
              remove_html_breaks = true
              code_block_single_color = #737373FF
              code_block_multi_color = #565656FF
              headline_line_color = #A9A9A9FF
              separation_line_color = #A9A9A9FF
              hyperlink_color = #0771FCFF
              click_event_color = #0771FCFF
              hover_event_color = #0771FCFF
              quote_color = #818181FF
              quote_italic = false
              bullet_list_dot_color = #A9A9A9FF
              parse_markdown = true
              table_show_header = true
              table_alternate_row_colors = true
              table_line_color = #787878FF
              table_header_background_color = #323232FF
              table_row_background_color = #282828FF
              table_alternate_row_color = #3C3C3CFF
              element_type = text_v2
              base_color = #FFFFFFFF
              scale = 1.0
              text_border = 2
              line_spacing = 2
              quote_indent = 8
              bullet_list_indent = 8
              bullet_list_spacing = 3
              table_line_thickness = 1.0
              table_cell_padding = 8.0
              table_margin = 4.0
            """;

    @Test
    void elementsSavedWithoutABackgroundHaveNone() {
        PropertyContainerSet layout = PropertiesParser.deserializeSetFromFancyString("type = fancymenu_layout\n\nelement {\n" + ELEMENT_SAVED_WITHOUT_BACKGROUND + "}\n");
        assertNotNull(layout);
        PropertyContainer saved = Objects.requireNonNull(layout.getFirstContainerOfType("element"));
        TestElement loaded = new TestElement();
        // An element that had a background before it loads would keep it, if loading did not take it away
        loaded.background.applyDefaultsOfNewElements();

        loaded.getPropertyMap().forEach((key, property) -> property.deserialize(saved));

        assertFalse(loaded.background.enabled.getBoolean());
        assertNull(loaded.background.texture.get());
        assertFalse(loaded.background.tile.getBoolean());
    }

    @Test
    void newElementsStartWithABackgroundThatIsTheBuiltInPanelBehindTheScrollBarsToo() {
        TestElement element = new TestElement();
        assertFalse(element.background.enabled.getBoolean());

        element.background.applyDefaultsOfNewElements();

        assertTrue(element.background.enabled.getBoolean());
        assertNull(element.background.texture.get());
        assertTrue(element.background.behindScrollBars.getBoolean());
        assertEquals(6, element.background.padding.getInteger());
        // A texture of the user starts nine-sliced and stretched
        assertTrue(element.background.nineSlice.getBoolean());
        assertFalse(element.background.tile.getBoolean());
    }

    @Test
    void settingsSurviveSavingAndLoading() {
        TestElement element = new TestElement();
        element.background.enabled.set(true);
        element.background.padding.set(9);
        element.background.behindScrollBars.set(false);
        element.background.texture.set(ResourceSupplier.image("[source:local]/config/fancymenu/assets/panel.png"));
        element.background.nineSlice.set(false);
        element.background.nineSliceBorderX.set(3);
        element.background.nineSliceBorderY.set(4);
        element.background.tile.set(true);
        SerializedElement saved = new SerializedElement();
        element.getPropertyMap().forEach((key, property) -> property.serialize(saved));

        TestElement loaded = new TestElement();
        loaded.getPropertyMap().forEach((key, property) -> property.deserialize(saved));

        assertTrue(saved.hasProperty(TextBackground.ENABLED_KEY));
        assertTrue(loaded.background.enabled.getBoolean());
        assertEquals(9, loaded.background.padding.getInteger());
        assertFalse(loaded.background.behindScrollBars.getBoolean());
        assertEquals(Objects.requireNonNull(element.background.texture.get()).getSourceWithPrefix(), Objects.requireNonNull(loaded.background.texture.get()).getSourceWithPrefix());
        assertFalse(loaded.background.nineSlice.getBoolean());
        assertEquals(3, loaded.background.nineSliceBorderX.getInteger());
        assertEquals(4, loaded.background.nineSliceBorderY.getInteger());
        assertTrue(loaded.background.tile.getBoolean());
    }

    @Test
    void backgroundThatIsOffSurvivesSavingAndLoadingToo() {
        TestElement element = new TestElement();
        element.background.applyDefaultsOfNewElements();
        element.background.enabled.set(false);
        SerializedElement saved = new SerializedElement();
        element.getPropertyMap().forEach((key, property) -> property.serialize(saved));

        TestElement loaded = new TestElement();
        loaded.background.applyDefaultsOfNewElements();
        loaded.getPropertyMap().forEach((key, property) -> property.deserialize(saved));

        assertFalse(loaded.background.enabled.getBoolean());
    }

    @Test
    void applyingALookLeavesTheBackgroundAlone() {
        for (MarkdownLook look : MarkdownLook.values()) {
            TestElement withBackground = new TestElement();
            withBackground.background.applyDefaultsOfNewElements();
            TestElement withoutBackground = new TestElement();

            withBackground.settings.applyLook(look);
            withoutBackground.settings.applyLook(look);

            assertTrue(withBackground.background.enabled.getBoolean(), look::name);
            assertFalse(withoutBackground.background.enabled.getBoolean(), look::name);
        }
    }

    @Test
    void backgroundIsTheTextAreaGrownByThePaddingOnEverySide() {
        TextBackground background = new TestElement().background;

        assertArrayEquals(new int[]{94, 44, 212, 92}, background.updateArea(100, 50, 200, 80, false, false));

        background.padding.set(10);
        assertArrayEquals(new int[]{90, 40, 220, 100}, background.updateArea(100, 50, 200, 80, false, false));

        background.padding.set(0);
        assertArrayEquals(new int[]{100, 50, 200, 80}, background.updateArea(100, 50, 200, 80, false, false));
    }

    @Test
    void backgroundGrowsByTheSpaceOfEveryShownScrollBar() {
        TextBackground background = new TestElement().background;
        int space = TextElement.SCROLL_BAR_SPACE;

        assertArrayEquals(new int[]{94, 44, 212 + space, 92}, background.updateArea(100, 50, 200, 80, true, false));
        assertArrayEquals(new int[]{94, 44, 212, 92 + space}, background.updateArea(100, 50, 200, 80, false, true));
        assertArrayEquals(new int[]{94, 44, 212 + space, 92 + space}, background.updateArea(100, 50, 200, 80, true, true));
    }

    @Test
    void backgroundOnlyCoversTheTextAreaWhenItShouldNotReachBehindScrollBars() {
        TextBackground background = new TestElement().background;
        background.behindScrollBars.set(false);

        assertArrayEquals(new int[]{94, 44, 212, 92}, background.updateArea(100, 50, 200, 80, true, true));
        assertArrayEquals(new int[]{94, 44, 212, 92}, background.updateArea(100, 50, 200, 80, false, false));
    }

    @Test
    void paddingBelowZeroCountsAsNoPadding() {
        TextBackground background = new TestElement().background;
        background.padding.set(-5);

        assertArrayEquals(new int[]{100, 50, 200, 80}, background.updateArea(100, 50, 200, 80, false, false));
        background.enabled.set(true);
        background.behindScrollBars.set(false);
        assertEquals(0, background.getScrollBarDistance());
    }

    @Test
    void scrollBarsStandOutsideOfABackgroundThatDoesNotReachBehindThem() {
        TextBackground background = new TestElement().background;
        background.enabled.set(true);
        background.behindScrollBars.set(false);

        // The scroll bars start where the background ends, which is the padding away from the text area
        assertEquals(6, background.getScrollBarDistance());
        assertEquals(94 + 212, 100 + 200 + background.getScrollBarDistance());
        background.padding.set(10);
        assertEquals(10, background.getScrollBarDistance());
        background.padding.set(0);
        assertEquals(0, background.getScrollBarDistance());
    }

    @Test
    void scrollBarsStayNextToTheTextAreaWithoutABackgroundAndOnABackgroundThatReachesBehindThem() {
        TextBackground background = new TestElement().background;

        // What elements that got saved without a background have
        assertEquals(0, background.getScrollBarDistance());
        background.behindScrollBars.set(false);
        assertEquals(0, background.getScrollBarDistance());
        background.enabled.set(true);
        background.behindScrollBars.set(true);
        assertEquals(0, background.getScrollBarDistance());
    }

    @Test
    void everySettingHasANameInTheEnglishLanguageFile() throws IOException {
        JsonObject english;
        try (InputStream stream = TextBackgroundTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            english = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        TextBackground background = new TestElement().background;

        for (Property<?> property : List.of(background.enabled, background.padding, background.behindScrollBars, background.texture, background.nineSlice, background.nineSliceBorderX, background.nineSliceBorderY, background.tile)) {
            assertTrue(english.has(property.getContextMenuEntryLocalizationKeyBase()), property::getKey);
        }
    }

    /**
     * The properties a text element has for its background and its Markdown settings.
     */
    private static final class TestElement implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();
        private final MarkdownSettings settings = new MarkdownSettings(this, new MarkdownTestRenderer());
        private final TextBackground background = new TextBackground(this);

        @Override
        public @NotNull Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

    }

}
