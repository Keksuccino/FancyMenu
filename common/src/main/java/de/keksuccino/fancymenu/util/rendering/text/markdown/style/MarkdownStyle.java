package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.BooleanOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.ColorOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FloatOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.Group;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.Impact;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.LookColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Everything that decides how Markdown text looks: every color, size, gap, shape and mode of a Markdown renderer.<br>
 * Each of these values is a {@link MarkdownStyleOption} with a getter and a setter of its own. The fields below hold the options by name,
 * and {@link #getOptions()} lists all of them, for code that handles them alike, like menus or saved data.<br><br>
 *
 * A {@link MarkdownLook} is nothing but one value for every option. The three numbers or colors at the end of each line below are the values
 * the classic, the pixel and the neutral look give that option, in that order, and {@link #applyLook(MarkdownLook)} sets all of them.
 * A new style has the classic look.<br><br>
 *
 * Sizes are in pixels of the text at its normal size. Which of them grow with the scale of the text is up to the renderer.
 */
public final class MarkdownStyle {

    private static final LookColor WHITE = rgb(255, 255, 255);
    // The palette of the pixel look
    private static final LookColor PIXEL_TEXT = rgb(236, 229, 211);
    private static final LookColor PIXEL_SOFT = rgb(220, 211, 188);
    private static final LookColor PIXEL_DIM = rgb(164, 156, 138);
    private static final LookColor PIXEL_ACCENT = rgb(255, 201, 77);
    private static final LookColor PIXEL_ACCENT_DIM = rgb(168, 120, 44);
    private static final LookColor PIXEL_LINK = rgb(127, 216, 255);
    private static final LookColor PIXEL_FRAME = rgb(57, 65, 95);
    private static final LookColor PIXEL_INK = rgba(8, 10, 22, 209);

    private final Listener listener;
    private final Supplier<UITheme> theme;
    private final List<MarkdownStyleOption<?>> options = new ArrayList<>();
    private final Map<String, MarkdownStyleOption<?>> optionsByKey = new HashMap<>();
    private final List<ColorOption> colorOptions = new ArrayList<>();
    @NotNull
    private MarkdownLook look = MarkdownLook.CLASSIC;
    /** Set while a look gets applied, to tell the listener about all of its changes at once. */
    private boolean collectingChanges = false;
    @Nullable
    private Impact collectedImpact = null;

    // ---------------------------------------------------------------- Text

    /** The color of text that has no color of its own. */
    public final ColorOption textColor = this.color("text_color", Group.TEXT, Impact.DRAW, WHITE, PIXEL_TEXT, this.theme(theme -> theme.ui_interface_generic_text_color));
    /** If text has a shadow. Text in the smooth UI font never has one. */
    public final BooleanOption textShadow = this.bool("text_shadow", Group.TEXT, Impact.DRAW, true, true, false);
    /** The space between the edges of the renderer and its text area. The outer 2 pixels can get clipped by containers like scroll areas, so it is never smaller. */
    public final FloatOption padding = this.size("padding", Group.TEXT, Impact.LAYOUT, 2.0F, 2.0F, 4.0F, 4.0F);
    /** The space between two lines of the same block, like the lines of a paragraph. */
    public final FloatOption lineSpacing = this.size("line_spacing", Group.TEXT, Impact.LAYOUT, 2.0F, 2.0F, 3.0F);

    // ---------------------------------------------------------------- Headlines

    /** The options of the three headline levels, starting with the biggest one. */
    private final Headline[] headlines = {new Headline(1), new Headline(2), new Headline(3)};

    // ---------------------------------------------------------------- Lists

    /** How far every list level indents its items. */
    public final FloatOption listIndent = this.size("list_indent", Group.LISTS, Impact.LAYOUT, 8.0F, 9.0F, 10.0F);
    /** The space above every list item. */
    public final FloatOption listItemSpacing = this.size("list_item_spacing", Group.LISTS, Impact.LAYOUT, 3.0F, 3.0F, 3.0F);
    /** The options of the three list levels. Deeper levels look like the third one. */
    private final ListLevel[] listLevels = {new ListLevel(1), new ListLevel(2), new ListLevel(3)};

    // ---------------------------------------------------------------- Links and text events

    public final ColorOption hyperlinkColor = this.color("hyperlink_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** The color of text that fires a click event. */
    public final ColorOption clickEventColor = this.color("click_event_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** The color of text that fires a hover event. */
    public final ColorOption hoverEventColor = this.color("hover_event_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));

    // ---------------------------------------------------------------- Inline code and code blocks

    public final ColorOption inlineCodeBackgroundColor = this.color("inline_code_background_color", Group.CODE, Impact.DRAW, rgb(115, 115, 115), PIXEL_INK, this.theme(theme -> theme.ui_interface_generic_text_color, 26));
    public final ColorOption codeBlockBackgroundColor = this.color("code_block_background_color", Group.CODE, Impact.DRAW, rgb(86, 86, 86), PIXEL_INK, this.theme(theme -> theme.ui_interface_generic_text_color, 18));

    // ---------------------------------------------------------------- Quotes

    public final ColorOption quoteTextColor = this.color("quote_text_color", Group.QUOTES, Impact.COMPONENTS, rgb(129, 129, 129), PIXEL_SOFT, this.theme(theme -> theme.ui_interface_widget_label_color_inactive));
    /** The color of the bar next to a quote. */
    public final ColorOption quoteBarColor = this.color("quote_bar_color", Group.QUOTES, Impact.DRAW, rgb(129, 129, 129), PIXEL_ACCENT, this.theme(theme -> theme.ui_interface_generic_text_color, 72));
    /** The space between the edge of the text area and the text of a quote, which holds the bar. */
    public final FloatOption quoteIndent = this.size("quote_indent", Group.QUOTES, Impact.LAYOUT, 8.0F, 8.0F, 10.0F);
    public final BooleanOption quoteItalic = this.bool("quote_italic", Group.QUOTES, Impact.PARSE, false, false, false);

    // ---------------------------------------------------------------- Tables

    /** The space between the text of a cell and the edges of the cell. */
    public final FloatOption tableCellPadding = this.size("table_cell_padding", Group.TABLES, Impact.LAYOUT, 8.0F, 4.0F, 5.0F);
    /** The space above and below a table. */
    public final FloatOption tableMargin = this.size("table_margin", Group.TABLES, Impact.LAYOUT, 4.0F, 6.0F, 6.0F);
    /** If the first row of a table looks like a header. Otherwise it is a row like every other one. */
    public final BooleanOption tableShowHeader = this.bool("table_show_header", Group.TABLES, Impact.DRAW, true, true, true);
    public final ColorOption tableHeaderBackgroundColor = this.color("table_header_background_color", Group.TABLES, Impact.DRAW, rgb(50, 50, 50), rgba(255, 201, 77, 41), this.theme(theme -> theme.ui_interface_generic_text_color, 20));
    public final ColorOption tableRowBackgroundColor = this.color("table_row_background_color", Group.TABLES, Impact.DRAW, rgb(40, 40, 40), rgba(0, 0, 0, 0), rgba(0, 0, 0, 0));
    /** If every second row has the alternate row background. */
    public final BooleanOption tableAlternateRows = this.bool("table_alternate_rows", Group.TABLES, Impact.DRAW, true, true, true);
    public final ColorOption tableAlternateRowBackgroundColor = this.color("table_alternate_row_background_color", Group.TABLES, Impact.DRAW, rgb(60, 60, 60), rgba(255, 255, 255, 13), this.theme(theme -> theme.ui_interface_generic_text_color, 9));
    public final ColorOption tableLineColor = this.color("table_line_color", Group.TABLES, Impact.DRAW, rgb(120, 120, 120), PIXEL_FRAME, this.theme(theme -> theme.ui_interface_generic_text_color, 46));
    public final FloatOption tableLineThickness = this.size("table_line_thickness", Group.TABLES, Impact.LAYOUT, 1.0F, 1.0F, 1.0F);

    // ---------------------------------------------------------------- Separators

    public final ColorOption separatorColor = this.color("separator_color", Group.SEPARATORS, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT_DIM, this.theme(theme -> theme.ui_interface_generic_text_color, 46));

    /**
     * @param listener Gets told about every change to the style.
     * @param theme The UI theme the colors of the neutral look follow.
     */
    public MarkdownStyle(@NotNull Listener listener, @NotNull Supplier<UITheme> theme) {
        this.listener = Objects.requireNonNull(listener);
        this.theme = Objects.requireNonNull(theme);
    }

    /**
     * Gives every option of the style the value the given look has for it.
     */
    public void applyLook(@NotNull MarkdownLook look) {
        this.look = Objects.requireNonNull(look);
        this.collectingChanges = true;
        try {
            for (MarkdownStyleOption<?> option : this.options) {
                option.applyLook(look);
            }
        } finally {
            this.collectingChanges = false;
            Impact impact = this.collectedImpact;
            this.collectedImpact = null;
            if (impact != null) this.listener.onStyleChanged(impact);
        }
    }

    /**
     * Returns the look that got applied last. Options can have other values than that look gives them, because every option can get set on its own.
     */
    @NotNull
    public MarkdownLook getLook() {
        return this.look;
    }

    /**
     * Returns all options of the style, in the order of their groups.
     */
    @NotNull
    public List<MarkdownStyleOption<?>> getOptions() {
        return Collections.unmodifiableList(this.options);
    }

    @Nullable
    public MarkdownStyleOption<?> getOption(@NotNull String key) {
        return this.optionsByKey.get(key);
    }

    /**
     * Returns the options of a headline level.
     *
     * @param level 1 for the biggest headlines ("#") to 3 for the smallest ones ("###").
     */
    @NotNull
    public Headline headline(int level) {
        return this.headlines[Math.max(1, Math.min(this.headlines.length, level)) - 1];
    }

    /**
     * Returns the options of a list level. Levels deeper than the third one share its options.
     *
     * @param level 1 for the items of a list, 2 for the items of a list inside of a list item, and so on.
     */
    @NotNull
    public ListLevel listLevel(int level) {
        return this.listLevels[Math.max(1, Math.min(this.listLevels.length, level)) - 1];
    }

    /**
     * Reads the sources of all colors again that follow one, like the colors of the UI theme.<br>
     * Has to run before the style gets drawn with, because the options keep the last colors of their sources.
     *
     * @return What the changed colors make outdated, or NULL if no color changed.
     */
    @Nullable
    public Impact updateFollowedColors() {
        Impact impact = null;
        for (int i = 0; i < this.colorOptions.size(); i++) {
            ColorOption option = this.colorOptions.get(i);
            if (option.updateFollowedColor() && ((impact == null) || (option.getImpact().compareTo(impact) > 0))) impact = option.getImpact();
        }
        return impact;
    }

    void register(@NotNull MarkdownStyleOption<?> option) {
        if (this.optionsByKey.put(option.getKey(), option) != null) throw new IllegalStateException("Duplicate option key: " + option.getKey());
        this.options.add(option);
        if (option instanceof ColorOption colorOption) this.colorOptions.add(colorOption);
    }

    void onOptionChanged(@NotNull MarkdownStyleOption<?> option) {
        if (this.collectingChanges) {
            if ((this.collectedImpact == null) || (option.getImpact().compareTo(this.collectedImpact) > 0)) this.collectedImpact = option.getImpact();
            return;
        }
        this.listener.onStyleChanged(option.getImpact());
    }

    private BooleanOption bool(String key, Group group, Impact impact, boolean classicValue, boolean pixelValue, boolean neutralValue) {
        return new BooleanOption(this, key, group, impact, classicValue, pixelValue, neutralValue);
    }

    private FloatOption size(String key, Group group, Impact impact, float classicValue, float pixelValue, float neutralValue) {
        return this.size(key, group, impact, Float.NEGATIVE_INFINITY, classicValue, pixelValue, neutralValue);
    }

    private FloatOption size(String key, Group group, Impact impact, float minimum, float classicValue, float pixelValue, float neutralValue) {
        return new FloatOption(this, key, group, impact, minimum, classicValue, pixelValue, neutralValue);
    }

    private ColorOption color(String key, Group group, Impact impact, LookColor classicValue, LookColor pixelValue, LookColor neutralValue) {
        return new ColorOption(this, key, group, impact, false, classicValue, pixelValue, neutralValue);
    }

    private static LookColor rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 255);
    }

    private static LookColor rgba(int red, int green, int blue, int alpha) {
        return LookColor.of((alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    /**
     * A color that follows a color of the UI theme.
     */
    private LookColor theme(Function<UITheme, DrawableColor> color) {
        return LookColor.following(() -> color.apply(this.theme.get()).getColorInt());
    }

    /**
     * A color that follows a color of the UI theme, but has the given alpha (0 to 255).
     * Tints of the text color work as backgrounds and lines on every background the theme puts behind text.
     */
    private LookColor theme(Function<UITheme, DrawableColor> color, int alpha) {
        return LookColor.following(() -> (color.apply(this.theme.get()).getColorInt() & 0x00FFFFFF) | (alpha << 24));
    }

    /**
     * The options of one headline level.
     */
    public final class Headline {

        /** The color of the line or dots a headline gets decorated with. */
        public final ColorOption decorationColor;

        private Headline(int level) {
            String key = "headline_" + level + "_";
            this.decorationColor = MarkdownStyle.this.color(key + "decoration_color", Group.HEADLINES, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT_DIM, MarkdownStyle.this.theme(theme -> theme.ui_interface_generic_text_color, 46));
        }

    }

    /**
     * The options of one list level.
     */
    public final class ListLevel {

        /** The color of the markers in front of the items of the level. */
        public final ColorOption markerColor;

        private ListLevel(int level) {
            String key = "list_level_" + level + "_";
            this.markerColor = MarkdownStyle.this.color(key + "marker_color", Group.LISTS, Impact.DRAW, rgb(169, 169, 169), (level == 1) ? PIXEL_ACCENT : PIXEL_DIM, MarkdownStyle.this.theme(theme -> (level == 1) ? theme.bullet_list_dot_color_1 : (level == 2) ? theme.bullet_list_dot_color_2 : theme.bullet_list_dot_color_3));
        }

    }

    /**
     * Gets told when an option of a style changed.
     */
    @FunctionalInterface
    public interface Listener {

        /**
         * @param impact What the change makes outdated. If several options changed at once, this is the most expensive impact of them.
         */
        void onStyleChanged(@NotNull Impact impact);

    }

}
