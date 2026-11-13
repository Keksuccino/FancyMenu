package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the Markdown settings of text and tooltip elements that got saved without the properties of {@link MarkdownSettings},
 * which is the data of every layout from before text had a style.<br>
 * Those elements saved a handful of values under keys of their own. Some of them stood for several options of the style at once,
 * like one color for the lines of all headline levels, and some got read in a way no property reads its value, like numbers that lost their decimals.<br><br>
 *
 * This class writes the values of the old keys into the saved data again, under the keys of the properties they belong to and as the value
 * the element used to end up with. The properties then load them like any other saved value. Options without an old key stay away from the data,
 * so their properties get the value of the classic look, which is how those elements looked.<br><br>
 *
 * Old values with placeholders get wrapped into placeholders that do what the old code did with their numbers: drop the decimals, hold them back at 0, double them.
 * That keeps such values changing while the game runs.
 */
public final class LegacyMarkdownSettings {

    private static final String STYLE = MarkdownSettings.STYLE_KEY_PREFIX;
    private static final String[] TEXT_SHADOW = {STYLE + "text_shadow"};
    private static final String[] TEXT_COLOR = {STYLE + "text_color"};
    private static final String[] PADDING = {STYLE + "padding"};
    private static final String[] LINE_SPACING = {STYLE + "line_spacing"};
    private static final String[] INLINE_CODE_BACKGROUND_COLOR = {STYLE + "inline_code_background_color"};
    private static final String[] CODE_BLOCK_BACKGROUND_COLOR = {STYLE + "code_block_background_color"};
    private static final String[] HEADLINE_DECORATION_COLORS = {STYLE + "headline_1_decoration_color", STYLE + "headline_2_decoration_color", STYLE + "headline_3_decoration_color"};
    private static final String[] SEPARATOR_COLOR = {STYLE + "separator_color"};
    private static final String[] HYPERLINK_COLOR = {STYLE + "hyperlink_color"};
    private static final String[] CLICK_EVENT_COLOR = {STYLE + "click_event_color"};
    private static final String[] HOVER_EVENT_COLOR = {STYLE + "hover_event_color"};
    private static final String[] QUOTE_COLORS = {STYLE + "quote_text_color", STYLE + "quote_bar_color"};
    private static final String[] QUOTE_ITALIC = {STYLE + "quote_italic"};
    private static final String[] QUOTE_INDENT = {STYLE + "quote_indent"};
    private static final String[] LIST_MARKER_COLORS = {STYLE + "list_level_1_marker_color", STYLE + "list_level_2_marker_color", STYLE + "list_level_3_marker_color"};
    private static final String[] LIST_INDENT = {STYLE + "list_indent"};
    private static final String[] LIST_SPACINGS = {STYLE + "list_item_spacing", STYLE + "list_space_above"};
    private static final String[] TABLE_SHOW_HEADER = {STYLE + "table_show_header"};
    private static final String[] TABLE_ALTERNATE_ROWS = {STYLE + "table_alternate_rows"};
    private static final String[] TABLE_LINE_COLORS = {STYLE + "table_line_color", STYLE + "table_header_separator_color"};
    private static final String[] TABLE_HEADER_BACKGROUND_COLOR = {STYLE + "table_header_background_color"};
    private static final String[] TABLE_ROW_BACKGROUND_COLOR = {STYLE + "table_row_background_color"};
    private static final String[] TABLE_ALTERNATE_ROW_BACKGROUND_COLOR = {STYLE + "table_alternate_row_background_color"};
    private static final String[] TABLE_LINE_THICKNESS = {STYLE + "table_line_thickness"};
    private static final String[] TABLE_HEADER_SEPARATOR_THICKNESS = {STYLE + "table_header_separator_thickness"};
    private static final String[] TABLE_CELL_PADDINGS = {STYLE + "table_cell_padding_left", STYLE + "table_cell_padding_right", STYLE + "table_cell_padding_top", STYLE + "table_cell_padding_bottom"};
    private static final String[] TABLE_SPACES = {STYLE + "table_space_above", STYLE + "table_space_below"};
    /** The value the property of the look saves for the classic look. */
    private static final String CLASSIC_LOOK = "classic";

    private LegacyMarkdownSettings() {
    }

    /**
     * Has to run on the saved data of a text element before its properties load it.
     */
    public static void upgradeTextElement(@NotNull PropertyContainer serialized) {
        if (!isLegacy(serialized)) return;
        upgradeSharedValues(serialized);
        copyBoolean(serialized, "table_show_header", TABLE_SHOW_HEADER);
        copyBoolean(serialized, "table_alternate_row_colors", TABLE_ALTERNATE_ROWS);
        copyPlainColor(serialized, "table_line_color", TABLE_LINE_COLORS);
        copyPlainColor(serialized, "table_header_background_color", TABLE_HEADER_BACKGROUND_COLOR);
        copyPlainColor(serialized, "table_row_background_color", TABLE_ROW_BACKGROUND_COLOR);
        copyPlainColor(serialized, "table_alternate_row_color", TABLE_ALTERNATE_ROW_BACKGROUND_COLOR);
        // The text color of text elements was a property of its own, which read placeholders like its property does now
        String textColor = serialized.getValue("base_color");
        if (textColor != null) put(serialized, TEXT_COLOR, textColor);
        copyWholeNumber(serialized, "quote_indent", 8, QUOTE_INDENT);
        copyWholeNumber(serialized, "bullet_list_indent", 8, LIST_INDENT);
        copyWholeNumber(serialized, "bullet_list_spacing", 3, LIST_SPACINGS);
        copyNumber(serialized, "table_line_thickness", 1.0F, false, TABLE_LINE_THICKNESS);
        // The line below a table header was twice as thick as the other lines
        copyNumber(serialized, "table_line_thickness", 1.0F, true, TABLE_HEADER_SEPARATOR_THICKNESS);
        copyNumber(serialized, "table_cell_padding", 8.0F, false, TABLE_CELL_PADDINGS);
        copyNumber(serialized, "table_margin", 4.0F, false, TABLE_SPACES);
    }

    /**
     * Has to run on the saved data of a tooltip element before its properties load it.
     */
    public static void upgradeTooltipElement(@NotNull PropertyContainer serialized) {
        if (!isLegacy(serialized)) return;
        upgradeSharedValues(serialized);
        copyPlainColor(serialized, "base_color", TEXT_COLOR);
        copyPlainWholeNumber(serialized, "quote_indent", QUOTE_INDENT);
        copyPlainWholeNumber(serialized, "bullet_list_indent", LIST_INDENT);
        copyPlainWholeNumber(serialized, "bullet_list_spacing", LIST_SPACINGS);
    }

    /**
     * Returns if the data got saved without the properties of {@link MarkdownSettings}, and marks it as data that has them now.
     * Every element with those properties saves its look, and the classic look is what the old values describe.
     */
    private static boolean isLegacy(@NotNull PropertyContainer serialized) {
        if (serialized.hasProperty(MarkdownSettings.LOOK_KEY)) return false;
        serialized.putProperty(MarkdownSettings.LOOK_KEY, CLASSIC_LOOK);
        return true;
    }

    /**
     * The values text elements and tooltip elements saved in the same way.
     */
    private static void upgradeSharedValues(@NotNull PropertyContainer serialized) {
        String parseMarkdown = serialized.getValue("parse_markdown");
        if (parseMarkdown != null) serialized.putProperty("parse_markdown", SerializationHelper.INSTANCE.deserializeBoolean(true, parseMarkdown));
        copyExactBoolean(serialized, "shadow", TEXT_SHADOW);
        copyExactBoolean(serialized, "quote_italic", QUOTE_ITALIC);
        copyPlainColor(serialized, "code_block_single_color", INLINE_CODE_BACKGROUND_COLOR);
        copyPlainColor(serialized, "code_block_multi_color", CODE_BLOCK_BACKGROUND_COLOR);
        copyPlainColor(serialized, "headline_line_color", HEADLINE_DECORATION_COLORS);
        copyPlainColor(serialized, "separation_line_color", SEPARATOR_COLOR);
        copyPlainColor(serialized, "hyperlink_color", HYPERLINK_COLOR);
        copyPlainColor(serialized, "click_event_color", CLICK_EVENT_COLOR);
        copyPlainColor(serialized, "hover_event_color", HOVER_EVENT_COLOR);
        copyPlainColor(serialized, "quote_color", QUOTE_COLORS);
        copyPlainColor(serialized, "bullet_list_dot_color", LIST_MARKER_COLORS);
        copyWholeNumber(serialized, "text_border", 2, PADDING);
        copyWholeNumber(serialized, "line_spacing", 2, LINE_SPACING);
    }

    private static void put(@NotNull PropertyContainer serialized, @NotNull String[] keys, @NotNull String value) {
        for (String key : keys) {
            serialized.putProperty(key, value);
        }
    }

    /**
     * A switch that only got read when its value was exactly "true" or "false".
     */
    private static void copyExactBoolean(@NotNull PropertyContainer serialized, @NotNull String oldKey, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if ("true".equals(value) || "false".equals(value)) put(serialized, keys, value);
    }

    /**
     * A switch that was on unless its value said "false", no matter how that was written.
     */
    private static void copyBoolean(@NotNull PropertyContainer serialized, @NotNull String oldKey, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if (value != null) put(serialized, keys, String.valueOf(SerializationHelper.INSTANCE.deserializeBoolean(true, value)));
    }

    /**
     * A color that got read from its text as it was. Text that was no color, which includes every placeholder, made the color white.
     */
    private static void copyPlainColor(@NotNull PropertyContainer serialized, @NotNull String oldKey, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if (value != null) put(serialized, keys, (DrawableColor.of(value) != DrawableColor.EMPTY) ? value : DrawableColor.EMPTY.getHex());
    }

    /**
     * A number that only got read when it was written as a whole number, and then got used as it was.
     */
    private static void copyPlainWholeNumber(@NotNull PropertyContainer serialized, @NotNull String oldKey, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if ((value != null) && MathUtils.isInteger(value)) put(serialized, keys, String.valueOf(Integer.parseInt(value)));
    }

    /**
     * The number of a property for whole numbers: it lost its decimals, and it was at least 0.
     *
     * @param oldDefault The number the property had when its value was no number.
     */
    private static void copyWholeNumber(@NotNull PropertyContainer serialized, @NotNull String oldKey, int oldDefault, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if (value == null) return;
        if (PlaceholderParser.containsPlaceholders(value)) {
            put(serialized, keys, atLeastZero(placeholder("math_floor", "num", value)));
            return;
        }
        int number = oldDefault;
        if (MathUtils.isInteger(value)) {
            number = Integer.parseInt(value);
        } else if (MathUtils.isDouble(value)) {
            number = (int) Double.parseDouble(value);
        } else if (MathUtils.isFloat(value)) {
            number = (int) Float.parseFloat(value);
        }
        put(serialized, keys, String.valueOf(Math.max(0, number)));
    }

    /**
     * The number of a property for numbers with decimals, which was at least 0.
     *
     * @param oldDefault The number the property had when its value was no number.
     * @param doubled If the new properties get twice the number.
     */
    private static void copyNumber(@NotNull PropertyContainer serialized, @NotNull String oldKey, float oldDefault, boolean doubled, @NotNull String[] keys) {
        String value = serialized.getValue(oldKey);
        if (value == null) return;
        if (PlaceholderParser.containsPlaceholders(value)) {
            String number = atLeastZero(value);
            put(serialized, keys, doubled ? placeholder("calc", "decimal", "true", "expression", number + " * 2") : number);
            return;
        }
        float number = Math.max(0.0F, MathUtils.isFloat(value) ? Float.parseFloat(value) : oldDefault);
        put(serialized, keys, String.valueOf(doubled ? (number * 2.0F) : number));
    }

    @NotNull
    private static String atLeastZero(@NotNull String number) {
        return placeholder("maxnum", "first", "0", "second", number);
    }

    /**
     * Builds the text of a placeholder.
     *
     * @param namesAndValues The name of every value of the placeholder, each followed by its value.
     */
    @NotNull
    private static String placeholder(@NotNull String identifier, @NotNull String... namesAndValues) {
        StringBuilder placeholder = new StringBuilder("{\"placeholder\":\"").append(identifier).append("\",\"values\":{");
        for (int i = 0; i < namesAndValues.length; i += 2) {
            if (i > 0) placeholder.append(',');
            placeholder.append('"').append(namesAndValues[i]).append("\":\"").append(namesAndValues[i + 1]).append('"');
        }
        return placeholder.append("}}").toString();
    }

}
