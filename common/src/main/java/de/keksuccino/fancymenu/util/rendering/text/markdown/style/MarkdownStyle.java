package de.keksuccino.fancymenu.util.rendering.text.markdown.style;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.BooleanOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.ColorOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FloatOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FontOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.Group;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.Impact;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.LookColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.resources.ResourceLocation;
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

    private static final LookColor NONE = LookColor.NONE;
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
    private static final LookColor PIXEL_MINT = rgb(180, 240, 198);
    /**
     * The font the pixel look draws code with: a pixel font whose glyphs all are 5 pixels wide, with the 7 pixels tall capital letters, the 5 pixels
     * tall lowercase letters and the 9 pixels tall lines of the Minecraft font. It has the printable characters of ASCII.<br>
     * The font is a bitmap font, which every Minecraft version loads. Minecraft takes the advance of a bitmap glyph from its rightmost pixel
     * that is not fully transparent, so every glyph that does not reach the fifth column of its cell has a pixel with an alpha of 1 there.
     * Without those pixels, narrow glyphs like "." would get a smaller advance and the font would not be fixed-width anymore.
     */
    public static final ResourceLocation PIXEL_CODE_FONT = new ResourceLocation("fancymenu", "markdown_code");
    /** How much of the text color the muted text of the neutral look keeps. The rest is the background color. */
    private static final float MUTED_TEXT_STRENGTH = 0.7F;

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
    /**
     * How far lines and backgrounds that span the text area stay away from its right edge, which are the lines of headlines and
     * the backgrounds of code blocks. The backgrounds of code blocks stay away from the bottom of their last line by this as well.
     */
    public final FloatOption blockEdgeInset = this.size("block_edge_inset", Group.TEXT, Impact.DRAW, 1.0F, 0.0F, 0.0F);
    /**
     * If the empty column of pixels the Minecraft font puts behind every letter is no part of a text where something hugs the text:
     * underlines with a color of their own, the backgrounds of inline code and the columns of tables. The smooth UI font has no such column.
     */
    public final BooleanOption trimGlyphSpacing = this.bool("trim_glyph_spacing", Group.TEXT, Impact.LAYOUT, false, true, true);
    /**
     * If the corners of backgrounds and frames are smooth arcs, like the ones of the widgets of the UI, which never are rounder than
     * the UI theme makes its widgets. Otherwise a corner radius cuts a square of its size out of every corner, the way pixel art rounds corners.
     */
    public final BooleanOption smoothCorners = this.bool("smooth_corners", Group.TEXT, Impact.DRAW, false, false, true);
    /** If text has a shadow. Text in the smooth UI font never has one. */
    public final BooleanOption textShadow = this.bool("text_shadow", Group.TEXT, Impact.DRAW, true, true, false);
    /** The space between the edges of the renderer and its text area. The outer 2 pixels can get clipped by containers like scroll areas, so it is never smaller. */
    public final FloatOption padding = this.size("padding", Group.TEXT, Impact.LAYOUT, 2.0F, 2.0F, 4.0F, 4.0F);
    /** The color of bold text, if it has a color of its own. Text of headlines keeps the color of its headline. */
    public final ColorOption boldTextColor = this.optionalColor("bold_text_color", Group.TEXT, Impact.COMPONENTS, NONE, WHITE, NONE);
    /** The space between two lines of the same block, like the lines of a paragraph. */
    public final FloatOption lineSpacing = this.size("line_spacing", Group.TEXT, Impact.LAYOUT, 2.0F, 2.0F, 3.0F);
    /** What a blank line of the text becomes. */
    public final EnumOption<MarkdownBlankLineMode> blankLineMode = this.mode("blank_line_mode", Group.TEXT, Impact.LAYOUT, MarkdownBlankLineMode.EMPTY_LINE, MarkdownBlankLineMode.PARAGRAPH_GAP, MarkdownBlankLineMode.PARAGRAPH_GAP);
    /** If blank lines that follow each other count as one. Otherwise every blank line behind the first one of a row is an empty line. */
    public final BooleanOption collapseBlankLines = this.bool("collapse_blank_lines", Group.TEXT, Impact.LAYOUT, false, true, true);
    /** How the spaces above and below the blocks of the text come together. */
    public final EnumOption<MarkdownBlockGapMode> blockGapMode = this.mode("block_gap_mode", Group.TEXT, Impact.LAYOUT, MarkdownBlockGapMode.ADD, MarkdownBlockGapMode.LARGEST, MarkdownBlockGapMode.LARGEST);
    public final FloatOption paragraphSpaceAbove = this.size("paragraph_space_above", Group.TEXT, Impact.LAYOUT, 0.0F, 5.0F, 8.0F);
    public final FloatOption paragraphSpaceBelow = this.size("paragraph_space_below", Group.TEXT, Impact.LAYOUT, 0.0F, 5.0F, 8.0F);
    /**
     * If the sizes of the style that the classic look keeps at a fixed size grow with the scale of the text, like everything else:
     * the padding, the line spacing, the spaces around blocks, and the paddings and lines of headlines, quotes and tables.
     */
    public final BooleanOption scaleSizesWithText = this.bool("scale_sizes_with_text", Group.TEXT, Impact.LAYOUT, false, true, true);
    /**
     * If the scale of text gets rounded so that one pixel of the font covers a whole number of screen pixels, at every GUI scale and text scale.
     * That keeps the Minecraft font sharp and makes text a bit smaller or bigger than its scale says.
     */
    public final BooleanOption snapTextScale = this.bool("snap_text_scale", Group.TEXT, Impact.LAYOUT, false, true, false);
    /** If every line gets moved onto whole screen pixels, so the Minecraft font does not shimmer while the text scrolls. Otherwise only centered lines do. */
    public final BooleanOption snapPositions = this.bool("snap_positions", Group.TEXT, Impact.DRAW, false, true, false);
    /**
     * If the line through struck through text of the Minecraft font sits on the pixel grid of the font and is as wide as the text.
     * Otherwise the font draws it, half a pixel above that and one pixel wider. The smooth UI font always draws its own line.
     */
    public final BooleanOption snapStrikethrough = this.bool("snap_strikethrough", Group.TEXT, Impact.COMPONENTS, false, true, false);
    /**
     * If lines are as tall as the letters of the font, down to their descenders. Otherwise lines of the smooth UI font have its tight
     * line height, which ends at the baseline of the text. Lines of the Minecraft font are 9 pixels tall either way.
     */
    public final BooleanOption fullLineHeight = this.bool("full_line_height", Group.TEXT, Impact.PARSE, false, true, true);

    // ---------------------------------------------------------------- Headlines

    /** The options of the three headline levels, starting with the biggest one. */
    private final Headline[] headlines = {new Headline(1), new Headline(2), new Headline(3)};
    /** How much of the right end of a {@link MarkdownHeadlineDecoration#RULE} fades out through dithering. */
    public final FloatOption headlineRuleFadeLength = this.size("headline_rule_fade_length", Group.HEADLINES, Impact.DRAW, 0.0F, 28.0F, 0.0F);
    /** The color of the shadow below a {@link MarkdownHeadlineDecoration#RULE}, if it has one. */
    public final ColorOption headlineRuleShadowColor = this.optionalColor("headline_rule_shadow_color", Group.HEADLINES, Impact.DRAW, NONE, rgba(0, 0, 0, 115), NONE);
    /** The space between the end of a headline and the first dot of its {@link MarkdownHeadlineDecoration#LEADER}. */
    public final FloatOption headlineLeaderGap = this.size("headline_leader_gap", Group.HEADLINES, Impact.DRAW, 3.0F, 3.0F, 3.0F);

    // ---------------------------------------------------------------- Lists

    /** How far every list level indents its items. */
    public final FloatOption listIndent = this.size("list_indent", Group.LISTS, Impact.LAYOUT, 8.0F, 9.0F, 12.0F);
    /** What the text of the items of all levels gets indented by on top of the indent of their level. */
    public final FloatOption listTextOffset = this.size("list_text_offset", Group.LISTS, Impact.LAYOUT, 5.0F, 0.0F, 0.0F);
    /** The space between the marker of an item and its text. */
    public final FloatOption listMarkerGap = this.size("list_marker_gap", Group.LISTS, Impact.DRAW, 7.0F, 4.0F, 5.0F);
    /** The width and height of the area a marker stands in, which a square marker fills. Pixel art markers are 5 pixels big and stand in the middle of it. */
    public final FloatOption listMarkerSize = this.size("list_marker_size", Group.LISTS, Impact.DRAW, 3.0F, 5.0F, 4.0F);
    public final EnumOption<MarkdownMarkerAlignment> listMarkerAlignment = this.mode("list_marker_alignment", Group.LISTS, Impact.DRAW, MarkdownMarkerAlignment.LINE_CENTER, MarkdownMarkerAlignment.X_HEIGHT, MarkdownMarkerAlignment.X_HEIGHT);
    /** The space between two list items that follow each other. */
    public final FloatOption listItemSpacing = this.size("list_item_spacing", Group.LISTS, Impact.LAYOUT, 3.0F, 3.0F, 4.0F);
    /** The space above the first item of a list. */
    public final FloatOption listSpaceAbove = this.size("list_space_above", Group.LISTS, Impact.LAYOUT, 3.0F, 4.0F, 6.0F);
    /** The space below the last item of a list. */
    public final FloatOption listSpaceBelow = this.size("list_space_below", Group.LISTS, Impact.LAYOUT, 0.0F, 5.0F, 8.0F);
    /** The options of the three list levels. Deeper levels look like the third one. */
    private final ListLevel[] listLevels = {new ListLevel(1), new ListLevel(2), new ListLevel(3)};
    /** The marker of list items that start with "+", which stands for things that got added. */
    public final EnumOption<MarkdownMarkerShape> listPlusMarkerShape = this.mode("list_plus_marker_shape", Group.LISTS, Impact.DRAW, MarkdownMarkerShape.PLUS, MarkdownMarkerShape.PLUS, MarkdownMarkerShape.ICON_ADD);
    public final ColorOption listPlusMarkerColor = this.color("list_plus_marker_color", Group.LISTS, Impact.DRAW, rgb(85, 255, 85), rgb(134, 223, 108), this.theme(theme -> theme.success_color));
    /** When list items that start with "-" get the marker for things that got removed instead of the marker of their list level. */
    public final EnumOption<MarkdownRemovedMarkerMode> listMinusMarkerMode = this.mode("list_minus_marker_mode", Group.LISTS, Impact.DRAW, MarkdownRemovedMarkerMode.AUTOMATIC, MarkdownRemovedMarkerMode.AUTOMATIC, MarkdownRemovedMarkerMode.AUTOMATIC);
    /** The marker of list items that start with "-", where it stands for things that got removed. */
    public final EnumOption<MarkdownMarkerShape> listMinusMarkerShape = this.mode("list_minus_marker_shape", Group.LISTS, Impact.DRAW, MarkdownMarkerShape.MINUS, MarkdownMarkerShape.MINUS, MarkdownMarkerShape.ICON_REMOVE);
    public final ColorOption listMinusMarkerColor = this.color("list_minus_marker_color", Group.LISTS, Impact.DRAW, rgb(255, 85, 85), rgb(255, 123, 107), this.theme(theme -> theme.error_color));
    /** The marker of list items that start with "~", which stands for things that got changed. */
    public final EnumOption<MarkdownMarkerShape> listTildeMarkerShape = this.mode("list_tilde_marker_shape", Group.LISTS, Impact.DRAW, MarkdownMarkerShape.WAVE, MarkdownMarkerShape.WAVE, MarkdownMarkerShape.ICON_EDIT);
    public final ColorOption listTildeMarkerColor = this.color("list_tilde_marker_color", Group.LISTS, Impact.DRAW, rgb(85, 255, 255), PIXEL_LINK, this.theme(theme -> theme.info_color));
    /** The marker of list items that start with "*", which stands for highlights. */
    public final EnumOption<MarkdownMarkerShape> listAsteriskMarkerShape = this.mode("list_asterisk_marker_shape", Group.LISTS, Impact.DRAW, MarkdownMarkerShape.STAR, MarkdownMarkerShape.STAR, MarkdownMarkerShape.ICON_STAR);
    public final ColorOption listAsteriskMarkerColor = this.color("list_asterisk_marker_color", Group.LISTS, Impact.DRAW, rgb(255, 170, 0), PIXEL_ACCENT, this.theme(theme -> theme.warning_color));

    public final ColorOption listNumberColor = this.color("list_number_color", Group.LISTS, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT, this.mutedThemeText());
    public final EnumOption<MarkdownNumberAlignment> listNumberAlignment = this.mode("list_number_alignment", Group.LISTS, Impact.DRAW, MarkdownNumberAlignment.RIGHT, MarkdownNumberAlignment.RIGHT, MarkdownNumberAlignment.RIGHT);
    /** What the text of numbered items gets indented by on top of the indent of other items, to make room for their numbers. */
    public final FloatOption listNumberIndent = this.size("list_number_indent", Group.LISTS, Impact.LAYOUT, 6.0F, 6.0F, 6.0F);
    /** The space between the number of an item and its text. */
    public final FloatOption listNumberGap = this.size("list_number_gap", Group.LISTS, Impact.DRAW, 3.0F, 3.0F, 4.0F);

    // ---------------------------------------------------------------- Links and text events

    public final ColorOption hyperlinkColor = this.color("hyperlink_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** The color of text that fires a click event. */
    public final ColorOption clickEventColor = this.color("click_event_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** The color of text that fires a hover event. */
    public final ColorOption hoverEventColor = this.color("hover_event_color", Group.LINKS, Impact.COMPONENTS, rgb(7, 113, 252), PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** When links and text events are underlined. */
    public final EnumOption<MarkdownUnderlineMode> linkUnderline = this.mode("link_underline", Group.LINKS, Impact.COMPONENTS, MarkdownUnderlineMode.ON_HOVER, MarkdownUnderlineMode.ALWAYS, MarkdownUnderlineMode.ALWAYS);
    /** The color of the underline of links and text events, if it has another color than their text. */
    public final ColorOption linkUnderlineColor = this.optionalColor("link_underline_color", Group.LINKS, Impact.COMPONENTS, NONE, rgb(62, 124, 158), this.theme(theme -> theme.input_field_suggestions_text_color_selected, 110));
    /** The color of links and text events while the mouse is over them, if they change their color then. */
    public final ColorOption linkHoverColor = this.optionalColor("link_hover_color", Group.LINKS, Impact.COMPONENTS, NONE, WHITE, NONE);
    /** The color of the underline of links and text events while the mouse is over them, if it changes then. Only underlines with a color of their own can have it. */
    public final ColorOption linkHoverUnderlineColor = this.optionalColor("link_hover_underline_color", Group.LINKS, Impact.DRAW, NONE, PIXEL_LINK, this.theme(theme -> theme.input_field_suggestions_text_color_selected));

    // ---------------------------------------------------------------- Inline code and code blocks

    public final ColorOption inlineCodeBackgroundColor = this.color("inline_code_background_color", Group.CODE, Impact.DRAW, rgb(115, 115, 115), PIXEL_INK, this.theme(theme -> theme.ui_interface_generic_text_color, 26));
    /** The color of inline code, if it has another color than the text around it. */
    public final ColorOption inlineCodeTextColor = this.optionalColor("inline_code_text_color", Group.CODE, Impact.COMPONENTS, NONE, PIXEL_MINT, NONE);
    /** The space between the left edge of the background of inline code and its text. */
    public final FloatOption inlineCodePaddingLeft = this.size("inline_code_padding_left", Group.CODE, Impact.LAYOUT, 1.0F, 2.0F, 3.0F);
    public final FloatOption inlineCodePaddingRight = this.size("inline_code_padding_right", Group.CODE, Impact.LAYOUT, 1.0F, 2.0F, 3.0F);
    /** How far the background of inline code reaches above the line of its text. */
    public final FloatOption inlineCodePaddingTop = this.size("inline_code_padding_top", Group.CODE, Impact.DRAW, 2.0F, 1.0F, 1.0F);
    /** How far the background of inline code reaches below the line of its text. */
    public final FloatOption inlineCodePaddingBottom = this.size("inline_code_padding_bottom", Group.CODE, Impact.DRAW, 0.0F, 0.0F, 1.0F);
    /**
     * The font of inline code and code blocks, if they have another font than the text around them. Only the Minecraft font gets replaced,
     * so code keeps the smooth UI font. Characters the font has no glyph for keep the font of the text.
     */
    public final FontOption codeFont = this.font("code_font", Group.CODE, Impact.PARSE, null, PIXEL_CODE_FONT, null);
    public final ColorOption codeBlockBackgroundColor = this.color("code_block_background_color", Group.CODE, Impact.DRAW, rgb(86, 86, 86), PIXEL_INK, this.theme(theme -> theme.ui_interface_generic_text_color, 18));
    /** The color of the text of code blocks, if it has another color than normal text. */
    public final ColorOption codeBlockTextColor = this.optionalColor("code_block_text_color", Group.CODE, Impact.COMPONENTS, NONE, PIXEL_MINT, NONE);
    /** The space between the left and right edge of a code block and its text. */
    public final FloatOption codeBlockPaddingHorizontal = this.size("code_block_padding_horizontal", Group.CODE, Impact.LAYOUT, 10.0F, 6.0F, 8.0F);
    /** The space between the upper and lower edge of a code block and its text. */
    public final FloatOption codeBlockPaddingVertical = this.size("code_block_padding_vertical", Group.CODE, Impact.LAYOUT, 10.0F, 5.0F, 7.0F);
    /** The color of the upper and left edge of the frame of a code block, if it has one. */
    public final ColorOption codeBlockFrameTopColor = this.optionalColor("code_block_frame_top_color", Group.CODE, Impact.DRAW, NONE, rgb(4, 5, 11), this.theme(theme -> theme.ui_interface_generic_text_color, 30));
    /** The color of the lower and right edge of the frame of a code block, if it has one. */
    public final ColorOption codeBlockFrameBottomColor = this.optionalColor("code_block_frame_bottom_color", Group.CODE, Impact.DRAW, NONE, PIXEL_FRAME, this.theme(theme -> theme.ui_interface_generic_text_color, 30));
    /** The color of the shadow below the upper edge of a code block, if it has one. */
    public final ColorOption codeBlockInnerShadowColor = this.optionalColor("code_block_inner_shadow_color", Group.CODE, Impact.DRAW, NONE, rgba(0, 0, 0, 102), NONE);
    /** If every character of a code block stands in a cell of the same width, so the characters of its lines stand in columns. */
    public final BooleanOption codeBlockFixedWidth = this.bool("code_block_fixed_width", Group.CODE, Impact.PARSE, false, true, false);
    /** The width of the cells of a code block with a fixed width. 0 makes them as wide as the letter "m" of the font, which is 6 in the Minecraft font. */
    public final FloatOption codeBlockCellWidth = this.size("code_block_cell_width", Group.CODE, Impact.PARSE, 0.0F, 0.0F, 0.0F, 0.0F);
    /** How much of every corner of the backgrounds of inline code and code blocks gets cut off. */
    public final FloatOption codeCornerRadius = this.size("code_corner_radius", Group.CODE, Impact.DRAW, 1.0F, 1.0F, 3.0F);
    public final FloatOption codeBlockSpaceAbove = this.size("code_block_space_above", Group.CODE, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);
    public final FloatOption codeBlockSpaceBelow = this.size("code_block_space_below", Group.CODE, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);

    // ---------------------------------------------------------------- Quotes

    public final ColorOption quoteTextColor = this.color("quote_text_color", Group.QUOTES, Impact.COMPONENTS, rgb(129, 129, 129), PIXEL_SOFT, this.mutedThemeText());
    /** The color of the bar next to a quote. */
    public final ColorOption quoteBarColor = this.color("quote_bar_color", Group.QUOTES, Impact.DRAW, rgb(129, 129, 129), PIXEL_ACCENT, this.theme(theme -> theme.ui_interface_generic_text_color, 72));
    /** The space between the edge of the text area and the text of a quote, which holds the bar. */
    public final FloatOption quoteIndent = this.size("quote_indent", Group.QUOTES, Impact.LAYOUT, 8.0F, 8.0F, 11.0F);
    public final BooleanOption quoteItalic = this.bool("quote_italic", Group.QUOTES, Impact.PARSE, false, false, false);
    public final FloatOption quoteBarWidth = this.size("quote_bar_width", Group.QUOTES, Impact.DRAW, 2.0F, 2.0F, 3.0F);
    public final EnumOption<MarkdownQuoteBarPosition> quoteBarPosition = this.mode("quote_bar_position", Group.QUOTES, Impact.DRAW, MarkdownQuoteBarPosition.INSIDE, MarkdownQuoteBarPosition.INSIDE, MarkdownQuoteBarPosition.INSIDE);
    /** How far the bar reaches above the first line of its quote. */
    public final FloatOption quoteBarOverhangTop = this.size("quote_bar_overhang_top", Group.QUOTES, Impact.DRAW, 2.0F, 0.0F, 0.0F);
    /** How far the bar reaches below the last line of its quote. */
    public final FloatOption quoteBarOverhangBottom = this.size("quote_bar_overhang_bottom", Group.QUOTES, Impact.DRAW, 1.0F, 0.0F, 0.0F);
    /** The color of the panel behind a quote, if it has one. The panel is as wide as the text area. */
    public final ColorOption quoteBackgroundColor = this.optionalColor("quote_background_color", Group.QUOTES, Impact.DRAW, NONE, rgba(255, 201, 77, 23), this.theme(theme -> theme.ui_interface_generic_text_color, 14));
    /** The space between the upper edge of the panel of a quote and its first line. */
    public final FloatOption quotePaddingTop = this.size("quote_padding_top", Group.QUOTES, Impact.LAYOUT, 0.0F, 4.0F, 5.0F);
    public final FloatOption quotePaddingBottom = this.size("quote_padding_bottom", Group.QUOTES, Impact.LAYOUT, 0.0F, 4.0F, 5.0F);
    /** The space between the text of a quote and the edge of the text area across from its bar. */
    public final FloatOption quotePaddingSide = this.size("quote_padding_side", Group.QUOTES, Impact.LAYOUT, 0.0F, 4.0F, 6.0F);
    /** How much of every corner of the panel of a quote gets cut off. */
    public final FloatOption quoteCornerRadius = this.size("quote_corner_radius", Group.QUOTES, Impact.DRAW, 0.0F, 0.0F, 3.0F);
    public final FloatOption quoteSpaceAbove = this.size("quote_space_above", Group.QUOTES, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);
    public final FloatOption quoteSpaceBelow = this.size("quote_space_below", Group.QUOTES, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);

    // ---------------------------------------------------------------- Callouts

    /** The options of the callout types, in the order note, tip, important, warning, caution. */
    private final Callout[] callouts = {
            new Callout("note", rgb(85, 153, 255), PIXEL_LINK, rgba(127, 216, 255, 23), theme -> theme.info_color),
            new Callout("tip", rgb(85, 255, 85), rgb(134, 223, 108), rgba(134, 223, 108, 23), theme -> theme.success_color),
            new Callout("important", rgb(170, 119, 255), rgb(199, 155, 255), rgba(199, 155, 255, 23), theme -> theme.input_field_suggestions_text_color_selected),
            new Callout("warning", rgb(255, 170, 0), rgb(255, 162, 77), rgba(255, 162, 77, 28), theme -> theme.warning_color),
            new Callout("caution", rgb(255, 85, 85), rgb(255, 123, 107), rgba(255, 123, 107, 28), theme -> theme.error_color)
    };
    /** The color of the text of callouts, if it has another color than the text of quotes. */
    public final ColorOption calloutTextColor = this.optionalColor("callout_text_color", Group.CALLOUTS, Impact.COMPONENTS, NONE, NONE, this.theme(theme -> theme.ui_interface_generic_text_color));
    public final EnumOption<MarkdownCalloutIcons> calloutIcons = this.mode("callout_icons", Group.CALLOUTS, Impact.LAYOUT, MarkdownCalloutIcons.PIXEL, MarkdownCalloutIcons.PIXEL, MarkdownCalloutIcons.SMOOTH);
    /** The color of the details inside of pixel art callout icons, like the mark of the warning sign. */
    public final ColorOption calloutIconDetailColor = this.color("callout_icon_detail_color", Group.CALLOUTS, Impact.DRAW, rgb(12, 15, 30), rgb(12, 15, 30), rgb(12, 15, 30));
    /** The space between the icon of a callout and its label. */
    public final FloatOption calloutIconGap = this.size("callout_icon_gap", Group.CALLOUTS, Impact.LAYOUT, 3.0F, 3.0F, 4.0F);
    public final BooleanOption calloutLabelBold = this.bool("callout_label_bold", Group.CALLOUTS, Impact.PARSE, true, true, true);
    /** The space between the line with the label of a callout and its text, on top of the line spacing. */
    public final FloatOption calloutHeaderGap = this.size("callout_header_gap", Group.CALLOUTS, Impact.LAYOUT, 1.0F, 1.0F, 2.0F);

    // ---------------------------------------------------------------- Tables

    /** The space between the left edge of a cell and its text. */
    public final FloatOption tableCellPaddingLeft = this.size("table_cell_padding_left", Group.TABLES, Impact.LAYOUT, 8.0F, 4.0F, 6.0F);
    public final FloatOption tableCellPaddingRight = this.size("table_cell_padding_right", Group.TABLES, Impact.LAYOUT, 8.0F, 4.0F, 6.0F);
    public final FloatOption tableCellPaddingTop = this.size("table_cell_padding_top", Group.TABLES, Impact.LAYOUT, 8.0F, 3.0F, 4.0F);
    public final FloatOption tableCellPaddingBottom = this.size("table_cell_padding_bottom", Group.TABLES, Impact.LAYOUT, 8.0F, 1.0F, 4.0F);
    public final FloatOption tableSpaceAbove = this.size("table_space_above", Group.TABLES, Impact.LAYOUT, 4.0F, 6.0F, 8.0F);
    public final FloatOption tableSpaceBelow = this.size("table_space_below", Group.TABLES, Impact.LAYOUT, 4.0F, 6.0F, 8.0F);
    /** If the first row of a table looks like a header. Otherwise it is a row like every other one. */
    public final BooleanOption tableShowHeader = this.bool("table_show_header", Group.TABLES, Impact.PARSE, true, true, true);
    public final ColorOption tableHeaderBackgroundColor = this.color("table_header_background_color", Group.TABLES, Impact.DRAW, rgb(50, 50, 50), rgba(255, 201, 77, 41), this.theme(theme -> theme.ui_interface_generic_text_color, 20));
    /** The color of the text of a table header, if it has another color than normal text. */
    public final ColorOption tableHeaderTextColor = this.optionalColor("table_header_text_color", Group.TABLES, Impact.COMPONENTS, NONE, PIXEL_ACCENT, NONE);
    public final BooleanOption tableHeaderBold = this.bool("table_header_bold", Group.TABLES, Impact.PARSE, false, true, true);
    /** The color of the line below a table header. */
    public final ColorOption tableHeaderSeparatorColor = this.color("table_header_separator_color", Group.TABLES, Impact.DRAW, rgb(120, 120, 120), PIXEL_ACCENT_DIM, this.theme(theme -> theme.ui_interface_generic_text_color, 46));
    public final FloatOption tableHeaderSeparatorThickness = this.size("table_header_separator_thickness", Group.TABLES, Impact.LAYOUT, 2.0F, 1.0F, 1.0F);
    public final ColorOption tableRowBackgroundColor = this.color("table_row_background_color", Group.TABLES, Impact.DRAW, rgb(40, 40, 40), rgba(0, 0, 0, 0), rgba(0, 0, 0, 0));
    /** If every second row has the alternate row background. */
    public final BooleanOption tableAlternateRows = this.bool("table_alternate_rows", Group.TABLES, Impact.DRAW, true, true, true);
    public final ColorOption tableAlternateRowBackgroundColor = this.color("table_alternate_row_background_color", Group.TABLES, Impact.DRAW, rgb(60, 60, 60), rgba(255, 255, 255, 13), this.theme(theme -> theme.ui_interface_generic_text_color, 9));
    /** The color behind the whole table, below the backgrounds of its rows, if it has one. */
    public final ColorOption tableBackgroundColor = this.optionalColor("table_background_color", Group.TABLES, Impact.DRAW, NONE, rgba(8, 10, 22, 128), NONE);
    /** Which lines a table has. */
    public final EnumOption<MarkdownTableGrid> tableGrid = this.mode("table_grid", Group.TABLES, Impact.LAYOUT, MarkdownTableGrid.FULL, MarkdownTableGrid.FRAME, MarkdownTableGrid.FRAME);
    public final ColorOption tableLineColor = this.color("table_line_color", Group.TABLES, Impact.DRAW, rgb(120, 120, 120), PIXEL_FRAME, this.theme(theme -> theme.ui_interface_generic_text_color, 46));
    public final FloatOption tableLineThickness = this.size("table_line_thickness", Group.TABLES, Impact.LAYOUT, 1.0F, 1.0F, 1.0F);
    /** How much of every corner of the background and the frame of a table gets cut off. */
    public final FloatOption tableCornerRadius = this.size("table_corner_radius", Group.TABLES, Impact.DRAW, 0.0F, 1.0F, 3.0F);

    // ---------------------------------------------------------------- Images

    public final EnumOption<MarkdownImageScaling> imageScaling = this.mode("image_scaling", Group.IMAGES, Impact.LAYOUT, MarkdownImageScaling.NATIVE, MarkdownImageScaling.WHOLE_MULTIPLE, MarkdownImageScaling.NATIVE);
    /** The biggest factor an image gets enlarged by with {@link MarkdownImageScaling#WHOLE_MULTIPLE}. */
    public final FloatOption imageMaxScale = this.size("image_max_scale", Group.IMAGES, Impact.LAYOUT, 1.0F, 1.0F, 3.0F, 1.0F);
    public final EnumOption<MarkdownImageAlignment> imageAlignment = this.mode("image_alignment", Group.IMAGES, Impact.LAYOUT, MarkdownImageAlignment.TEXT, MarkdownImageAlignment.CENTER, MarkdownImageAlignment.TEXT);
    /** The thickness of each of the two lines of the frame around an image. 0 leaves the frame away. */
    public final FloatOption imageFrameWidth = this.size("image_frame_width", Group.IMAGES, Impact.LAYOUT, 0.0F, 0.0F, 1.0F, 0.0F);
    /** The color of the outer line of the frame around an image, if it has one. */
    public final ColorOption imageFrameOuterColor = this.optionalColor("image_frame_outer_color", Group.IMAGES, Impact.DRAW, NONE, rgb(4, 5, 11), NONE);
    /** The color of the inner line of the frame around an image, if it has one. */
    public final ColorOption imageFrameInnerColor = this.optionalColor("image_frame_inner_color", Group.IMAGES, Impact.DRAW, NONE, PIXEL_FRAME, NONE);
    /** If the alternative text of an image shows as a caption below it. */
    public final BooleanOption imageCaption = this.bool("image_caption", Group.IMAGES, Impact.LAYOUT, false, true, false);
    public final ColorOption imageCaptionColor = this.color("image_caption_color", Group.IMAGES, Impact.DRAW, rgb(169, 169, 169), PIXEL_DIM, this.mutedThemeText());
    /** The space between an image and its caption. */
    public final FloatOption imageCaptionGap = this.size("image_caption_gap", Group.IMAGES, Impact.LAYOUT, 3.0F, 3.0F, 4.0F);
    public final FloatOption imageSpaceAbove = this.size("image_space_above", Group.IMAGES, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);
    public final FloatOption imageSpaceBelow = this.size("image_space_below", Group.IMAGES, Impact.LAYOUT, 0.0F, 6.0F, 8.0F);

    // ---------------------------------------------------------------- Separators

    public final EnumOption<MarkdownSeparatorStyle> separatorStyle = this.mode("separator_style", Group.SEPARATORS, Impact.LAYOUT, MarkdownSeparatorStyle.LINE, MarkdownSeparatorStyle.ORNAMENT, MarkdownSeparatorStyle.LINE);
    /** The color of the line or lines of a separator. */
    public final ColorOption separatorColor = this.color("separator_color", Group.SEPARATORS, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT_DIM, this.theme(theme -> theme.ui_interface_generic_text_color, 46));
    public final FloatOption separatorThickness = this.size("separator_thickness", Group.SEPARATORS, Impact.LAYOUT, 1.0F, 1.0F, 1.0F);
    /** The shape in the middle of a {@link MarkdownSeparatorStyle#ORNAMENT} separator. */
    public final EnumOption<MarkdownMarkerShape> separatorOrnamentShape = this.mode("separator_ornament_shape", Group.SEPARATORS, Impact.DRAW, MarkdownMarkerShape.DIAMOND, MarkdownMarkerShape.DIAMOND, MarkdownMarkerShape.DISC);
    public final ColorOption separatorOrnamentColor = this.color("separator_ornament_color", Group.SEPARATORS, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT, this.theme(theme -> theme.input_field_suggestions_text_color_selected));
    /** The width and height of the area the ornament stands in. Pixel art ornaments are 5 pixels big and stand in the middle of it. */
    public final FloatOption separatorOrnamentSize = this.size("separator_ornament_size", Group.SEPARATORS, Impact.LAYOUT, 5.0F, 5.0F, 5.0F);
    /** The space between the ornament and the lines next to it. */
    public final FloatOption separatorOrnamentGap = this.size("separator_ornament_gap", Group.SEPARATORS, Impact.DRAW, 2.0F, 2.0F, 4.0F);
    /** The longest each of the two lines next to the ornament gets. 0 lets them reach to the edges of the text area. */
    public final FloatOption separatorReach = this.size("separator_reach", Group.SEPARATORS, Impact.DRAW, 0.0F, 0.0F, 62.0F, 0.0F);
    /** How much of the outer end of the lines next to the ornament fades out through dithering. */
    public final FloatOption separatorFadeLength = this.size("separator_fade_length", Group.SEPARATORS, Impact.DRAW, 0.0F, 0.0F, 26.0F, 0.0F);
    public final FloatOption separatorSpaceAbove = this.size("separator_space_above", Group.SEPARATORS, Impact.LAYOUT, 0.0F, 7.0F, 10.0F);
    public final FloatOption separatorSpaceBelow = this.size("separator_space_below", Group.SEPARATORS, Impact.LAYOUT, 0.0F, 7.0F, 10.0F);

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
     * Returns the options of a callout type.
     *
     * @param type 0 for notes, 1 for tips, 2 for important things, 3 for warnings and 4 for cautions.
     */
    @NotNull
    public Callout callout(int type) {
        return this.callouts[Math.max(0, Math.min(this.callouts.length - 1, type))];
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

    private <E extends Enum<E>> EnumOption<E> mode(String key, Group group, Impact impact, E classicValue, E pixelValue, E neutralValue) {
        return new EnumOption<>(this, key, group, impact, classicValue, pixelValue, neutralValue);
    }

    private FontOption font(String key, Group group, Impact impact, @Nullable ResourceLocation classicValue, @Nullable ResourceLocation pixelValue, @Nullable ResourceLocation neutralValue) {
        return new FontOption(this, key, group, impact, classicValue, pixelValue, neutralValue);
    }

    private ColorOption color(String key, Group group, Impact impact, LookColor classicValue, LookColor pixelValue, LookColor neutralValue) {
        return new ColorOption(this, key, group, impact, false, classicValue, pixelValue, neutralValue);
    }

    private ColorOption optionalColor(String key, Group group, Impact impact, LookColor classicValue, LookColor pixelValue, LookColor neutralValue) {
        return new ColorOption(this, key, group, impact, true, classicValue, pixelValue, neutralValue);
    }

    private static LookColor rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 255);
    }

    private static LookColor rgba(int red, int green, int blue, int alpha) {
        return LookColor.of((alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    /**
     * Returns the value of the given level out of one value per level.
     */
    private static <T> T pick(int level, T first, T second, T third) {
        return (level == 1) ? first : (level == 2) ? second : third;
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
     * A color for text that steps back behind normal text: the text color of the UI theme, moved towards the background color of the theme.<br>
     * The inactive label color of the theme is made for disabled widgets and too faint for text that still has to be read.
     */
    private LookColor mutedThemeText() {
        return LookColor.following(() -> {
            UITheme theme = this.theme.get();
            int text = theme.ui_interface_generic_text_color.getColorInt();
            int background = theme.ui_interface_background_color.getColorInt();
            return 0xFF000000 | (mutedChannel(text, background, 16) << 16) | (mutedChannel(text, background, 8) << 8) | mutedChannel(text, background, 0);
        });
    }

    private static int mutedChannel(int text, int background, int shift) {
        int backgroundChannel = (background >> shift) & 0xFF;
        return backgroundChannel + Math.round((((text >> shift) & 0xFF) - backgroundChannel) * MUTED_TEXT_STRENGTH);
    }

    /**
     * The options of one headline level.
     */
    public final class Headline {

        /** How much bigger than normal text the headline is. */
        public final FloatOption scale;
        /** The color of the headline, if it has a color of its own. */
        public final ColorOption color;
        public final BooleanOption bold;
        public final EnumOption<MarkdownHeadlineDecoration> decoration;
        /** The color of the line or dots a headline gets decorated with. */
        public final ColorOption decorationColor;
        /** The space between the text of the headline and the line below it. */
        public final FloatOption decorationOffset;
        /** How thick the line or the dots of the decoration are. A line is never thinner than one screen pixel. */
        public final FloatOption decorationThickness;
        /** The space below the text of every render line of the headline, which is where an underline stands. */
        public final FloatOption linePadding;
        public final FloatOption spaceAbove;
        public final FloatOption spaceBelow;

        private Headline(int level) {
            String key = "headline_" + level + "_";
            this.scale = MarkdownStyle.this.size(key + "scale", Group.HEADLINES, Impact.LAYOUT, pick(level, 2.0F, 1.6F, 1.2F), pick(level, 2.0F, 1.0F, 1.0F), pick(level, 1.6F, 1.3F, 1.1F));
            this.color = MarkdownStyle.this.optionalColor(key + "color", Group.HEADLINES, Impact.COMPONENTS, NONE, pick(level, PIXEL_ACCENT, PIXEL_ACCENT, WHITE), NONE);
            this.bold = MarkdownStyle.this.bool(key + "bold", Group.HEADLINES, Impact.PARSE, false, level > 1, true);
            this.decoration = MarkdownStyle.this.mode(key + "decoration", Group.HEADLINES, Impact.LAYOUT, pick(level, MarkdownHeadlineDecoration.UNDERLINE, MarkdownHeadlineDecoration.UNDERLINE, MarkdownHeadlineDecoration.NONE), pick(level, MarkdownHeadlineDecoration.RULE, MarkdownHeadlineDecoration.LEADER, MarkdownHeadlineDecoration.NONE), pick(level, MarkdownHeadlineDecoration.RULE, MarkdownHeadlineDecoration.NONE, MarkdownHeadlineDecoration.NONE));
            this.decorationOffset = MarkdownStyle.this.size(key + "decoration_offset", Group.HEADLINES, Impact.LAYOUT, 1.0F, 3.0F, 3.0F);
            this.decorationThickness = MarkdownStyle.this.size(key + "decoration_thickness", Group.HEADLINES, Impact.LAYOUT, 0.5F, 1.0F, 1.0F);
            this.linePadding = MarkdownStyle.this.size(key + "line_padding", Group.HEADLINES, Impact.LAYOUT, pick(level, 8.0F, 8.0F, 6.0F), 0.0F, 0.0F);
            this.spaceAbove = MarkdownStyle.this.size(key + "space_above", Group.HEADLINES, Impact.LAYOUT, 0.0F, pick(level, 10.0F, 9.0F, 7.0F), pick(level, 14.0F, 12.0F, 10.0F));
            this.spaceBelow = MarkdownStyle.this.size(key + "space_below", Group.HEADLINES, Impact.LAYOUT, 0.0F, pick(level, 6.0F, 4.0F, 3.0F), pick(level, 7.0F, 6.0F, 5.0F));
            this.decorationColor = MarkdownStyle.this.color(key + "decoration_color", Group.HEADLINES, Impact.DRAW, rgb(169, 169, 169), PIXEL_ACCENT_DIM, MarkdownStyle.this.theme(theme -> theme.ui_interface_generic_text_color, 46));
        }

    }

    /**
     * The options of one list level.
     */
    public final class ListLevel {

        /** The shape of the markers in front of the items of the level. */
        public final EnumOption<MarkdownMarkerShape> markerShape;
        /** The color of the markers in front of the items of the level. */
        public final ColorOption markerColor;

        private ListLevel(int level) {
            String key = "list_level_" + level + "_";
            this.markerShape = MarkdownStyle.this.mode(key + "marker_shape", Group.LISTS, Impact.DRAW, MarkdownMarkerShape.SQUARE, pick(level, MarkdownMarkerShape.CURSOR, MarkdownMarkerShape.CURSOR_SMALL, MarkdownMarkerShape.DOT), pick(level, MarkdownMarkerShape.DISC, MarkdownMarkerShape.RING, MarkdownMarkerShape.DISC));
            this.markerColor = MarkdownStyle.this.color(key + "marker_color", Group.LISTS, Impact.DRAW, rgb(169, 169, 169), pick(level, PIXEL_ACCENT, PIXEL_DIM, PIXEL_DIM), MarkdownStyle.this.theme(theme -> pick(level, theme.bullet_list_dot_color_1, theme.bullet_list_dot_color_2, theme.bullet_list_dot_color_3)));
        }

    }

    /**
     * The options of one callout type.
     */
    public final class Callout {

        /** The color of the bar, the icon and the label of the callout. */
        public final ColorOption color;
        /** The color of the panel behind the callout, if it has another one than the panel of quotes. */
        public final ColorOption backgroundColor;

        private Callout(String name, LookColor classicColor, LookColor pixelColor, LookColor pixelBackground, Function<UITheme, DrawableColor> neutralColor) {
            String key = "callout_" + name + "_";
            this.color = MarkdownStyle.this.color(key + "color", Group.CALLOUTS, Impact.COMPONENTS, classicColor, pixelColor, MarkdownStyle.this.theme(neutralColor));
            this.backgroundColor = MarkdownStyle.this.optionalColor(key + "background_color", Group.CALLOUTS, Impact.DRAW, NONE, pixelBackground, MarkdownStyle.this.theme(neutralColor, 26));
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
