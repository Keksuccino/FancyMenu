package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettingsTest.TestElement;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.CalculatorPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.MathFloorPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.MaxNumberPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.UppercaseTextPlaceholder;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTestRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Objects;
import java.util.stream.Stream;

import static de.keksuccino.fancymenu.customization.element.elements.MarkdownSettingsTest.assertStylesEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Elements saved before their Markdown settings were properties have to look the way they did.<br>
 * The expected values come from {@link #readLikeTheOldTextElement} and {@link #readLikeTheOldTooltipElement}, which do
 * what the two elements did with their saved data back then.
 */
@ResourceLock("PlaceholderParser global state")
class LegacyMarkdownSettingsTest {

    /** A text element nobody ever changed, as the element builder saved it right before the Markdown settings became properties. */
    private static final String UNCHANGED_TEXT_ELEMENT = """
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

    /** A text element with every old option changed, saved by the same element builder. */
    private static final String CHANGED_TEXT_ELEMENT = """
              interactable = true
              source = # Headline One%n%Plain text
              source_mode = direct
              shadow = false
              enable_scrolling = true
              auto_line_wrapping = true
              remove_html_breaks = true
              code_block_single_color = #3355AA
              code_block_multi_color = #224422
              headline_line_color = #FF5555
              separation_line_color = #55FF55
              hyperlink_color = #FFAA00
              click_event_color = #AA00FF
              hover_event_color = #00AAFF
              quote_color = #55FFFF
              quote_italic = true
              bullet_list_dot_color = #FF55FF
              parse_markdown = false
              table_show_header = false
              table_alternate_row_colors = false
              table_line_color = #FFFF55
              table_header_background_color = #553311
              table_row_background_color = #113355
              table_alternate_row_color = #335511
              element_type = text_v2
              base_color = #FFE0A0
              scale = 1.5
              text_border = 6
              line_spacing = 4
              quote_indent = 14
              bullet_list_indent = 12
              bullet_list_spacing = 6
              table_line_thickness = 2.0
              table_cell_padding = 5.0
              table_margin = 9.0
            """;

    /** The text element of a layout from 2023. Elements had no table values back then and saved their whole numbers with decimals. */
    private static final String TEXT_ELEMENT_FROM_2023 = """
              source = [source:web]https://raw.githubusercontent.com/Keksuccino/FancyMenu/dev/README.md
              source_mode = resource
              shadow = true
              scale = 1.0
              base_color = #FFFFFFFF
              text_border = 2
              line_spacing = 2
              enable_scrolling = true
              auto_line_wrapping = true
              remove_html_breaks = true
              code_block_single_color = #737373FF
              code_block_multi_color = #565656FF
              headline_line_color = #A9A9A9FF
              separation_line_color = #A9A9A9FF
              hyperlink_color = #0771FCFF
              quote_color = #818181FF
              quote_indent = 8.0
              quote_italic = true
              bullet_list_dot_color = #A9A9A9FF
              bullet_list_indent = 8.0
              bullet_list_spacing = 3.0
              parse_markdown = true
              element_type = text_v2
            """;

    /** A text element whose old properties got their values from placeholders, or had numbers the element held back at 0 or cut the decimals off. */
    private static final String TEXT_ELEMENT_WITH_PLACEHOLDERS = """
              interactable = true
              source = # Headline One%n%Plain text
              source_mode = direct
              shadow = true
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
              base_color = {"placeholder":"uppercase_text","values":{"text":"#ffaa66"}}
              scale = 1.5
              text_border = {"placeholder":"calc","values":{"decimal":"true","expression":"3 + 4.6"}}
              line_spacing = {"placeholder":"calc","values":{"decimal":"true","expression":"2.5 + 1"}}
              quote_indent = -4
              bullet_list_indent = {"placeholder":"calc","values":{"decimal":"true","expression":"10.9 + 4"}}
              bullet_list_spacing = 7.9
              table_line_thickness = {"placeholder":"calc","values":{"decimal":"true","expression":"1 + 0.5"}}
              table_cell_padding = {"placeholder":"calc","values":{"decimal":"true","expression":"2.5 + 1"}}
              table_margin = -6.0
            """;

    /** A tooltip element nobody ever changed, as the element builder saved it right before the Markdown settings became properties. */
    private static final String UNCHANGED_TOOLTIP_ELEMENT = """
              interactable = false
              mouse_following = false
              source = Tooltip text
              source_mode = direct
              shadow = true
              base_color = #FFFFFFFF
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
              quote_indent = 8.0
              quote_italic = false
              bullet_list_dot_color = #A9A9A9FF
              bullet_list_indent = 8.0
              bullet_list_spacing = 3.0
              parse_markdown = true
              element_type = tooltip
              nine_slice_border_top = 5
              scale = 1.0
              text_border = 2
              line_spacing = 2
            """;

    /** A tooltip element with every old option changed, saved by the same element builder. */
    private static final String CHANGED_TOOLTIP_ELEMENT = """
              interactable = false
              mouse_following = false
              source = Tooltip text
              source_mode = direct
              shadow = false
              base_color = #A0E0FF
              auto_line_wrapping = true
              remove_html_breaks = true
              code_block_single_color = #3355AA
              code_block_multi_color = #224422
              headline_line_color = #FF5555
              separation_line_color = #55FF55
              hyperlink_color = #FFAA00
              click_event_color = #0771FCFF
              hover_event_color = #0771FCFF
              quote_color = #55FFFF
              quote_indent = 8.0
              quote_italic = true
              bullet_list_dot_color = #FF55FF
              bullet_list_indent = 8.0
              bullet_list_spacing = 3.0
              parse_markdown = true
              element_type = tooltip
              nine_slice_border_top = 5
              scale = 1.2
              text_border = 5
              line_spacing = 5
            """;

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeAll
    static void registerPlaceholders() {
        // The check of the real placeholders needs a running Minecraft client to compare threads
        register(new CalculatorPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new MaxNumberPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new MathFloorPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new UppercaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
    }

    private static void register(@NotNull Placeholder placeholder) {
        if (PlaceholderRegistry.getPlaceholder(placeholder.getIdentifier()) == null) PlaceholderRegistry.register(placeholder);
    }

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    static Stream<String> savedTextElements() {
        return Stream.of(UNCHANGED_TEXT_ELEMENT, CHANGED_TEXT_ELEMENT, TEXT_ELEMENT_FROM_2023, TEXT_ELEMENT_WITH_PLACEHOLDERS);
    }

    static Stream<String> savedTooltipElements() {
        return Stream.of(UNCHANGED_TOOLTIP_ELEMENT, CHANGED_TOOLTIP_ELEMENT);
    }

    @ParameterizedTest
    @MethodSource("savedTextElements")
    void textElementsSavedWithoutStylePropertiesLookLikeTheyDid(String saved) {
        MarkdownTestRenderer expected = readLikeTheOldTextElement(parseElement(saved));

        TestElement loaded = loadTextElement(parseElement(saved));

        assertSameSettings(expected, loaded);
    }

    @ParameterizedTest
    @MethodSource("savedTooltipElements")
    void tooltipElementsSavedWithoutStylePropertiesLookLikeTheyDid(String saved) {
        MarkdownTestRenderer expected = readLikeTheOldTooltipElement(parseElement(saved));

        TestElement loaded = loadTooltipElement(parseElement(saved));

        assertSameSettings(expected, loaded);
    }

    @Test
    void unchangedOldElementsAreTheClassicLook() {
        MarkdownTestRenderer classic = new MarkdownTestRenderer();

        TestElement text = loadTextElement(parseElement(UNCHANGED_TEXT_ELEMENT));
        TestElement tooltip = loadTooltipElement(parseElement(UNCHANGED_TOOLTIP_ELEMENT));

        assertStylesEqual(classic.getStyle(), text.style());
        assertStylesEqual(classic.getStyle(), tooltip.style());
        assertEquals(MarkdownLook.CLASSIC, text.settings.getLook());
        assertEquals(MarkdownLook.CLASSIC, tooltip.settings.getLook());
    }

    @Test
    void oneOldValueBecomesEveryOptionItStoodFor() {
        TestElement loaded = loadTextElement(parseElement(CHANGED_TEXT_ELEMENT));

        for (int level = 1; level <= 3; level++) {
            assertEquals(0xFFFF5555, loaded.style().headline(level).decorationColor.argb());
            assertEquals(0xFFFF55FF, loaded.style().listLevel(level).markerColor.argb());
        }
        assertEquals(0xFF55FFFF, loaded.style().quoteTextColor.argb());
        assertEquals(0xFF55FFFF, loaded.style().quoteBarColor.argb());
        assertEquals(0xFFFFFF55, loaded.style().tableLineColor.argb());
        assertEquals(0xFFFFFF55, loaded.style().tableHeaderSeparatorColor.argb());
        assertEquals(2.0F, loaded.style().tableLineThickness.get());
        assertEquals(4.0F, loaded.style().tableHeaderSeparatorThickness.get());
        assertEquals(5.0F, loaded.style().tableCellPaddingLeft.get());
        assertEquals(5.0F, loaded.style().tableCellPaddingRight.get());
        assertEquals(5.0F, loaded.style().tableCellPaddingTop.get());
        assertEquals(5.0F, loaded.style().tableCellPaddingBottom.get());
        assertEquals(9.0F, loaded.style().tableSpaceAbove.get());
        assertEquals(9.0F, loaded.style().tableSpaceBelow.get());
        assertEquals(6.0F, loaded.style().listItemSpacing.get());
        assertEquals(6.0F, loaded.style().listSpaceAbove.get());
    }

    @Test
    void oldValuesWithPlaceholdersKeepFollowingThem() {
        String key = "legacy_markdown_settings_changing";
        TestPlaceholders.setValue(key, "3.9");
        PropertyContainer saved = parseElement(UNCHANGED_TEXT_ELEMENT);
        saved.putProperty("line_spacing", TestPlaceholders.value(key));
        saved.putProperty("bullet_list_spacing", TestPlaceholders.value(key));
        saved.putProperty("table_line_thickness", TestPlaceholders.value(key));
        saved.putProperty("table_cell_padding", TestPlaceholders.value(key));
        saved.putProperty("base_color", TestPlaceholders.value(key + "_color"));
        TestPlaceholders.setValue(key + "_color", "#102030");
        TestElement loaded = loadTextElement(saved);

        for (String[] values : new String[][]{{"3.9", "#102030"}, {"12.2", "#405060"}, {"-5", "nothing"}, {"no number", "#AABBCC"}, {"0.4", "#102030"}}) {
            TestPlaceholders.setValue(key, values[0]);
            TestPlaceholders.setValue(key + "_color", values[1]);
            MarkdownTestRenderer expected = readLikeTheOldTextElement(saved);

            loaded.settings.tick();

            assertStylesEqual(expected.getStyle(), loaded.style());
        }
    }

    static Stream<String[]> singleOldValues() {
        return Stream.of(
                new String[]{"shadow", "false"}, new String[]{"shadow", "False"}, new String[]{"shadow", "TRUE"}, new String[]{"shadow", "nonsense"},
                new String[]{"quote_italic", "true"}, new String[]{"quote_italic", " true"},
                new String[]{"parse_markdown", "false"}, new String[]{"parse_markdown", " F a l s e "}, new String[]{"parse_markdown", "nonsense"},
                new String[]{"table_show_header", "false"}, new String[]{"table_show_header", " False "}, new String[]{"table_show_header", "nonsense"},
                new String[]{"table_alternate_row_colors", "FALSE"}, new String[]{"table_alternate_row_colors", "0"},
                new String[]{"base_color", "#12345"}, new String[]{"base_color", "ff8800"}, new String[]{"base_color", "# 11 22 33"}, new String[]{"base_color", TestPlaceholders.echo("#ABCDEF")},
                new String[]{"code_block_single_color", "#12345"}, new String[]{"code_block_single_color", "ff8800"}, new String[]{"code_block_multi_color", TestPlaceholders.echo("#ABCDEF")},
                new String[]{"headline_line_color", "#11223344"}, new String[]{"separation_line_color", "red"}, new String[]{"hyperlink_color", "#010203"},
                new String[]{"click_event_color", "#040506"}, new String[]{"hover_event_color", "#070809"}, new String[]{"quote_color", "#0A0B0C"},
                new String[]{"bullet_list_dot_color", "#0D0E0F"}, new String[]{"table_line_color", "#101112"}, new String[]{"table_header_background_color", "#131415"},
                new String[]{"table_row_background_color", "#161718"}, new String[]{"table_alternate_row_color", "#191A1B"},
                new String[]{"text_border", "0"}, new String[]{"text_border", "-3"}, new String[]{"text_border", "7"}, new String[]{"text_border", "7.9"}, new String[]{"text_border", "many"},
                new String[]{"line_spacing", "-2"}, new String[]{"line_spacing", "0"}, new String[]{"line_spacing", "4.9"}, new String[]{"line_spacing", "1e1"}, new String[]{"line_spacing", "+6"}, new String[]{"line_spacing", "many"},
                new String[]{"quote_indent", "12"}, new String[]{"quote_indent", "-12"}, new String[]{"quote_indent", "12.7"}, new String[]{"quote_indent", "8.0"},
                new String[]{"bullet_list_indent", "3"}, new String[]{"bullet_list_indent", "-3"}, new String[]{"bullet_list_indent", "3.5"},
                new String[]{"bullet_list_spacing", "9"}, new String[]{"bullet_list_spacing", "-9"}, new String[]{"bullet_list_spacing", "9.99"},
                new String[]{"table_line_thickness", "0.5"}, new String[]{"table_line_thickness", "3"}, new String[]{"table_line_thickness", "-1"}, new String[]{"table_line_thickness", "thick"},
                new String[]{"table_cell_padding", "2.5"}, new String[]{"table_cell_padding", "-2.5"}, new String[]{"table_cell_padding", "wide"},
                new String[]{"table_margin", "0"}, new String[]{"table_margin", "11.25"}, new String[]{"table_margin", "-11.25"},
                new String[]{"scale", "2.5"}, new String[]{"scale", "-1"}
        );
    }

    @ParameterizedTest
    @MethodSource("singleOldValues")
    void everyOldValueGetsReadTheWayTheElementsReadIt(String key, String value) {
        PropertyContainer text = new PropertyContainer("element");
        text.putProperty(key, value);
        PropertyContainer tooltip = new PropertyContainer("element");
        tooltip.putProperty(key, value);

        assertSameSettings(readLikeTheOldTextElement(text), loadTextElement(text));
        assertSameSettings(readLikeTheOldTooltipElement(tooltip), loadTooltipElement(tooltip));
    }

    @Test
    void dataWithStylePropertiesStaysUntouched() {
        TestElement element = new TestElement();
        element.settings.applyLook(MarkdownLook.PIXEL);
        PropertyContainer saved = MarkdownSettingsTest.saveThroughLayoutFile(element);
        // Old keys in data with style properties are leftovers nobody reads
        saved.putProperty("shadow", "false");
        saved.putProperty("quote_color", "#010203");
        saved.putProperty("table_cell_padding", "40");
        PropertyContainer savedForTooltip = MarkdownSettingsTest.saveThroughLayoutFile(element);
        savedForTooltip.putProperty("base_color", "#010203");
        savedForTooltip.putProperty("quote_indent", "40");

        TestElement text = loadTextElement(saved);
        TestElement tooltip = loadTooltipElement(savedForTooltip);

        element.settings.tick();
        assertStylesEqual(element.style(), text.style());
        assertStylesEqual(element.style(), tooltip.style());
        assertEquals(MarkdownLook.PIXEL, text.settings.getLook());
    }

    @Test
    void upgradedDataGetsReadTheSameWayEveryTime() {
        PropertyContainer saved = parseElement(TEXT_ELEMENT_WITH_PLACEHOLDERS);
        TestElement first = loadTextElement(saved);
        assertTrue(saved.hasProperty(MarkdownSettings.LOOK_KEY));
        int savedValues = saved.getProperties().size();

        // Layouts keep the data of their elements and build new elements from it whenever their screen opens
        TestElement second = loadTextElement(saved);

        assertEquals(savedValues, saved.getProperties().size());
        assertStylesEqual(first.style(), second.style());
    }

    @Test
    void upgradedElementsSaveLikeAllOthers() {
        TestElement loaded = loadTextElement(parseElement(CHANGED_TEXT_ELEMENT));

        TestElement reloaded = MarkdownSettingsTest.load(MarkdownSettingsTest.saveThroughLayoutFile(loaded));

        assertStylesEqual(loaded.style(), reloaded.style());
        assertEquals(loaded.renderer.getTextBaseScale(), reloaded.renderer.getTextBaseScale());
        assertEquals(loaded.renderer.isParseMarkdown(), reloaded.renderer.isParseMarkdown());
    }

    /**
     * Reads the lines of a saved element the way a layout file gets read.
     */
    @NotNull
    private static PropertyContainer parseElement(@NotNull String lines) {
        PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString("type = fancymenu_layout\n\nelement {\n" + lines + "}\n");
        assertNotNull(set);
        return Objects.requireNonNull(set.getFirstContainerOfType("element"));
    }

    @NotNull
    private static TestElement loadTextElement(@NotNull PropertyContainer serialized) {
        return MarkdownSettingsTest.load(serialized);
    }

    @NotNull
    private static TestElement loadTooltipElement(@NotNull PropertyContainer serialized) {
        TestElement element = new TestElement();
        LegacyMarkdownSettings.upgradeTooltipElement(serialized);
        element.getPropertyMap().forEach((key, property) -> property.deserialize(serialized));
        element.settings.tick();
        return element;
    }

    private static void assertSameSettings(@NotNull MarkdownTestRenderer expected, @NotNull TestElement actual) {
        assertStylesEqual(expected.getStyle(), actual.style());
        assertEquals(expected.getTextBaseScale(), actual.renderer.getTextBaseScale());
        assertEquals(expected.isParseMarkdown(), actual.renderer.isParseMarkdown());
        // Old elements had none of the optional Markdown syntax
        assertFalse(actual.renderer.isExtendedBulletListMarkersEnabled());
        assertFalse(actual.renderer.isNumberedListsEnabled());
        assertFalse(actual.renderer.isCalloutsEnabled());
    }

    /**
     * What a text element did with its saved data before its Markdown settings were properties: its builder read some values right into the renderer,
     * and the element pushed the values of a handful of properties into the renderer with every frame.
     */
    @NotNull
    private static MarkdownTestRenderer readLikeTheOldTextElement(@NotNull PropertyContainer serialized) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        readSharedValuesLikeTheOldBuilders(serialized, renderer);
        String tableShowHeader = serialized.getValue("table_show_header");
        if (tableShowHeader != null) renderer.setTableShowHeader(SerializationHelper.INSTANCE.deserializeBoolean(true, tableShowHeader));
        String tableAlternateRowColors = serialized.getValue("table_alternate_row_colors");
        if (tableAlternateRowColors != null) renderer.setTableAlternateRowColors(SerializationHelper.INSTANCE.deserializeBoolean(true, tableAlternateRowColors));
        String tableLineColor = serialized.getValue("table_line_color");
        if (tableLineColor != null) renderer.setTableLineColor(DrawableColor.of(tableLineColor));
        String tableHeaderBackgroundColor = serialized.getValue("table_header_background_color");
        if (tableHeaderBackgroundColor != null) renderer.setTableHeaderBackgroundColor(DrawableColor.of(tableHeaderBackgroundColor));
        String tableRowBackgroundColor = serialized.getValue("table_row_background_color");
        if (tableRowBackgroundColor != null) renderer.setTableRowBackgroundColor(DrawableColor.of(tableRowBackgroundColor));
        String tableAlternateRowColor = serialized.getValue("table_alternate_row_color");
        if (tableAlternateRowColor != null) renderer.setTableAlternateRowColor(DrawableColor.of(tableAlternateRowColor));

        Property.ColorProperty baseColor = Property.hexColorProperty("base_color", DrawableColor.WHITE.getHex(), true, "test");
        Property.IntegerProperty quoteIndent = Property.integerProperty("quote_indent", 8, "test");
        Property.IntegerProperty bulletListIndent = Property.integerProperty("bullet_list_indent", 8, "test");
        Property.IntegerProperty bulletListSpacing = Property.integerProperty("bullet_list_spacing", 3, "test");
        Property.FloatProperty tableLineThickness = Property.floatProperty("table_line_thickness", 1.0F, "test");
        Property.FloatProperty tableCellPadding = Property.floatProperty("table_cell_padding", 8.0F, "test");
        Property.FloatProperty tableMargin = Property.floatProperty("table_margin", 4.0F, "test");
        for (Property<?> property : new Property<?>[]{baseColor, quoteIndent, bulletListIndent, bulletListSpacing, tableLineThickness, tableCellPadding, tableMargin}) {
            property.deserialize(serialized);
        }
        pushSharedPropertiesLikeTheOldElements(serialized, renderer);
        renderer.setTextBaseColor(baseColor.getDrawable());
        renderer.setQuoteIndent(Math.max(0, quoteIndent.getInteger()));
        renderer.setBulletListIndent(Math.max(0, bulletListIndent.getInteger()));
        renderer.setBulletListSpacing(Math.max(0, bulletListSpacing.getInteger()));
        renderer.setTableLineThickness(Math.max(0.0F, tableLineThickness.getFloat()));
        renderer.setTableCellPadding(Math.max(0.0F, tableCellPadding.getFloat()));
        renderer.setTableMargin(Math.max(0.0F, tableMargin.getFloat()));
        return renderer;
    }

    /**
     * What a tooltip element did with its saved data before its Markdown settings were properties.
     */
    @NotNull
    private static MarkdownTestRenderer readLikeTheOldTooltipElement(@NotNull PropertyContainer serialized) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        readSharedValuesLikeTheOldBuilders(serialized, renderer);
        String baseColor = serialized.getValue("base_color");
        if (baseColor != null) renderer.setTextBaseColor(DrawableColor.of(baseColor));
        String quoteIndent = serialized.getValue("quote_indent");
        if ((quoteIndent != null) && MathUtils.isInteger(quoteIndent)) renderer.setQuoteIndent(Integer.parseInt(quoteIndent));
        String bulletListIndent = serialized.getValue("bullet_list_indent");
        if ((bulletListIndent != null) && MathUtils.isInteger(bulletListIndent)) renderer.setBulletListIndent(Integer.parseInt(bulletListIndent));
        String bulletListSpacing = serialized.getValue("bullet_list_spacing");
        if ((bulletListSpacing != null) && MathUtils.isInteger(bulletListSpacing)) renderer.setBulletListSpacing(Integer.parseInt(bulletListSpacing));
        pushSharedPropertiesLikeTheOldElements(serialized, renderer);
        return renderer;
    }

    private static void readSharedValuesLikeTheOldBuilders(@NotNull PropertyContainer serialized, @NotNull MarkdownRenderer renderer) {
        String shadow = serialized.getValue("shadow");
        if (shadow != null) {
            if (shadow.equals("false")) renderer.setTextShadow(false);
            if (shadow.equals("true")) renderer.setTextShadow(true);
        }
        String codeBlockSingleColor = serialized.getValue("code_block_single_color");
        if (codeBlockSingleColor != null) renderer.setCodeBlockSingleLineColor(DrawableColor.of(codeBlockSingleColor));
        String codeBlockMultiColor = serialized.getValue("code_block_multi_color");
        if (codeBlockMultiColor != null) renderer.setCodeBlockMultiLineColor(DrawableColor.of(codeBlockMultiColor));
        String headlineLineColor = serialized.getValue("headline_line_color");
        if (headlineLineColor != null) renderer.setHeadlineLineColor(DrawableColor.of(headlineLineColor));
        String separationLineColor = serialized.getValue("separation_line_color");
        if (separationLineColor != null) renderer.setSeparationLineColor(DrawableColor.of(separationLineColor));
        String hyperlinkColor = serialized.getValue("hyperlink_color");
        if (hyperlinkColor != null) renderer.setHyperlinkColor(DrawableColor.of(hyperlinkColor));
        String clickEventColor = serialized.getValue("click_event_color");
        if (clickEventColor != null) renderer.setTextClickEventColor(DrawableColor.of(clickEventColor));
        String hoverEventColor = serialized.getValue("hover_event_color");
        if (hoverEventColor != null) renderer.setTextHoverEventColor(DrawableColor.of(hoverEventColor));
        String quoteColor = serialized.getValue("quote_color");
        if (quoteColor != null) renderer.setQuoteColor(DrawableColor.of(quoteColor));
        String quoteItalic = serialized.getValue("quote_italic");
        if (quoteItalic != null) {
            if (quoteItalic.equals("true")) renderer.setQuoteItalic(true);
            if (quoteItalic.equals("false")) renderer.setQuoteItalic(false);
        }
        String bulletListDotColor = serialized.getValue("bullet_list_dot_color");
        if (bulletListDotColor != null) renderer.setBulletListDotColor(DrawableColor.of(bulletListDotColor));
        renderer.setParseMarkdown(SerializationHelper.INSTANCE.deserializeBoolean(true, serialized.getValue("parse_markdown")));
    }

    /**
     * The properties both elements had, and what they did with their values every frame.
     */
    private static void pushSharedPropertiesLikeTheOldElements(@NotNull PropertyContainer serialized, @NotNull MarkdownRenderer renderer) {
        Property.FloatProperty textScale = Property.floatProperty("scale", 1.0F, "test");
        Property.IntegerProperty textBorder = Property.integerProperty("text_border", 2, "test");
        Property.IntegerProperty lineSpacing = Property.integerProperty("line_spacing", 2, "test");
        textScale.deserialize(serialized);
        textBorder.deserialize(serialized);
        lineSpacing.deserialize(serialized);
        renderer.setTextBaseScale(Math.max(0.0F, textScale.getFloat()));
        renderer.setBorder(Math.max(0, textBorder.getInteger()));
        renderer.setLineSpacing(Math.max(0, lineSpacing.getInteger()));
    }

}
