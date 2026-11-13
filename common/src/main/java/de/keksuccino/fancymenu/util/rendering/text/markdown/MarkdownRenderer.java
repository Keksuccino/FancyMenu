package de.keksuccino.fancymenu.util.rendering.text.markdown;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.ui.FocuslessContainerEventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class MarkdownRenderer implements Renderable, FocuslessContainerEventHandler, NarratableEntry, NavigatableWidget {

    /**
     * The text of the renderer never gets longer than this, see {@link #setText(String)}. The placeholder parser has a lower limit of its own,
     * so the renderer always has to ask it for its placeholders with {@link PlaceholderParser#replacePlaceholdersWithoutLengthLimit(String, boolean)}.
     */
    private static final int MAX_TEXT_LENGTH = 45000;
    private static final String NEWLINE_PERCENT = "%n%";
    private static final String NEWLINE = "\n";
    private static final String NEWLINE_R = "\r";
    private static final String NEWLINE_ESCAPED = "\\n";
    private static final String EMPTY_STRING = "";
    private static final String HTML_BREAK = "<br>";

    protected boolean parseMarkdown = true;
    @NotNull
    protected String text = "";
    @NotNull
    protected String textPlaceholders = "";
    protected String lastTextPlaceholders;
    @Nullable
    protected String renderText;
    protected float x;
    protected float y;
    protected float optimalWidth;
    protected float realWidth;
    protected float realHeight;
    @NotNull
    protected TextCase textCase = TextCase.NORMAL;
    protected float textBaseScale = 1.0f;
    protected boolean autoLineBreaks = true;
    protected boolean removeHtmlBreaks = true;
    /** If "+ ", "* " and "~ " start list items like "- " does. */
    protected boolean extendedBulletListMarkers = false;
    /** If a number with a "." or ")" and a space behind it starts a list item. */
    protected boolean numberedLists = false;
    /** If a quote that starts with a marker line like "> [!NOTE]" is a callout. */
    protected boolean callouts = false;
    protected boolean uiFontRenderingEnabled = false;
    protected float textOpacity = 1.0F;
    public boolean skipRefresh = false;
    @Nullable
    protected Float parentRenderScale = null;
    @NotNull
    protected final Font font;
    protected boolean dragging;
    @Nullable
    protected TextEventHandler textEventHandler = null;
    /** Decides which hyperlinks open on click. NULL opens all of them. */
    @Nullable
    protected Predicate<String> hyperlinkFilter = null;
    protected float visibleTop = Float.NEGATIVE_INFINITY;
    protected float visibleBottom = Float.POSITIVE_INFINITY;
    /**
     * The fragments are outdated, because the render text, the way it gets parsed or the way its words get measured changed.
     * Parsing makes new fragments, so a parse always is followed by a layout.
     */
    protected boolean parseNeeded = false;
    /**
     * The lines are outdated, because something changed that decides where lines wrap, or how big and where lines and fragments are,
     * like the optimal width, a scale, a border, a spacing or an indent.
     */
    protected boolean layoutNeeded = false;
    protected boolean refreshing = false;
    /**
     * Counts the changes to settings that the render components of fragments get built from without the fragments knowing:
     * the text case and every option of the style that is part of the components. Fragments cache their component per version.
     */
    protected int fragmentStyleVersion = 0;
    protected final MarkdownStyle style;
    protected final List<MarkdownTextLine> lines = new ArrayList<>();
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();
    final MarkdownTextWidthCache textWidthCache = new MarkdownTextWidthCache(this);
    final MarkdownScaling scaling = new MarkdownScaling(this);
    final MarkdownTextFont textFont = new MarkdownTextFont(this);
    final MarkdownLayout layout = new MarkdownLayout(this);
    final MarkdownInteractions interactions = new MarkdownInteractions(this);

    public MarkdownRenderer() {
        this(Minecraft.getInstance().font);
    }

    MarkdownRenderer(@NotNull Font font) {
        this(font, () -> UIBase.getUITheme());
    }

    /**
     * @param theme The UI theme the colors of the neutral look follow.
     */
    MarkdownRenderer(@NotNull Font font, @NotNull Supplier<UITheme> theme) {
        this.font = Objects.requireNonNull(font);
        this.style = new MarkdownStyle(this::onStyleChanged, theme);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.tick();
        this.renderVisibleLines(graphics, mouseX, mouseY, partial);
        this.updateTextHoverEvents();
    }

    protected void renderVisibleLines(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.layout.updateVisibleLines();
        this.scaling.updateRenderTranslation();
        for (int i = this.layout.firstVisibleLine; (i < this.layout.visibleLinesEnd) && (i < this.lines.size()); i++) {
            MarkdownTextLine line = this.lines.get(i);
            if (!this.layout.isLineVisible(line)) continue;
            line.offsetX = this.scaling.lineOffsetX(line);
            line.render(graphics, mouseX, mouseY, partial);
        }
    }

    public void tick() {
        this.refresh(true);
    }

    /**
     * Measures the real size of the text again and positions its lines again.
     */
    public void updateSize() {
        this.refresh(false);
        if (this.refreshing) return;
        this.refreshing = true;
        try {
            this.layout.updateMetrics();
        } finally {
            this.refreshing = false;
        }
    }

    /**
     * Makes the renderer parse its text again the next time its fragments, lines or size are needed.
     */
    public void refreshRenderer() {
        if (this.skipRefresh) return;
        this.parseNeeded = true;
    }

    /**
     * Brings the fragments, the lines and the real size of the text up to date.<br>
     * Setters only mark what they make outdated ({@link #parseNeeded}, {@link #layoutNeeded}), so any number of changes costs at most one
     * parse and one layout. That work happens here, the next time the result is needed: at the start of a frame and when the real size gets asked for.<br>
     * While {@link #skipRefresh} is set, the renderer keeps its outdated fragments and lines.
     *
     * @param startOfFrame Placeholder values and images get checked for changes once per frame, not for every size query.
     */
    protected void refresh(boolean startOfFrame) {
        // Replacing placeholders and measuring lines can ask for the size of this renderer, which must not start another refresh
        if (this.refreshing) return;
        if (!startOfFrame && !this.parseNeeded && !this.layoutNeeded && (this.lastTextPlaceholders != null)) return;
        this.refreshing = true;
        try {
            if (startOfFrame || (this.lastTextPlaceholders == null)) {
                this.updateRenderText();
            }
            if (startOfFrame) {
                MarkdownStyleOption.Impact impact = this.style.updateFollowedColors();
                if (impact != null) this.onStyleChanged(impact);
                if (this.scaling.updateRenderScale()) this.layoutNeeded = true;
            }
            if (this.skipRefresh) return;
            if (this.parseNeeded) {
                this.parseNeeded = false;
                this.layoutNeeded = true;
                this.rebuildFragments();
            }
            if (this.layoutNeeded) {
                this.layoutNeeded = false;
                // Size queries can come before the first frame, which is when nothing told the scaling about the render scale yet
                this.scaling.updateRenderScale();
                this.rebuildLines();
            } else if (startOfFrame && this.layout.haveImagesChanged()) {
                this.layout.updateMetrics();
            }
        } finally {
            this.refreshing = false;
        }
    }

    /**
     * Builds the render text again when the text got changed or one of its placeholders has a new value.
     * The text only needs a new parse when that results in a different render text.
     */
    protected void updateRenderText() {
        // A text without placeholders has nothing that changes on its own, so asking the placeholder parser every frame would be a waste
        String replaced = this.textPlaceholders.isEmpty() ? EMPTY_STRING : PlaceholderParser.replacePlaceholdersWithoutLengthLimit(this.textPlaceholders, false);
        if ((this.renderText == null) || !replaced.equals(this.lastTextPlaceholders)) {
            String newRenderText = this.buildRenderText();
            if (!newRenderText.equals(this.renderText)) {
                this.renderText = newRenderText;
                this.parseNeeded = true;
            }
        }
        this.lastTextPlaceholders = replaced;
    }

    protected void rebuildFragments() {
        this.fragments.clear();
        this.textFont.forgetMetrics();
        if (this.renderText != null) {
            this.textWidthCache.beginParse();
            try {
                this.fragments.addAll(MarkdownParser.parse(this, this.renderText, this.parseMarkdown));
                MarkdownTablePainter.prepareHeaders(this, this.fragments);
                MarkdownStrikethroughPainter.markEnds(this.fragments);
                MarkdownListMarkerPainter.markListsWithPlusItems(this.fragments);
            } finally {
                this.textWidthCache.endParse();
            }
        }
        this.interactions.collect(this.fragments);
    }

    protected void rebuildLines() {
        this.layout.rebuildLines();
    }

    @NotNull
    protected String buildRenderText() {
        String t = PlaceholderParser.replacePlaceholdersWithoutLengthLimit(this.text, true);
        t = StringUtils.replace(t, NEWLINE_PERCENT, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_R, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_ESCAPED, NEWLINE);
        if (this.removeHtmlBreaks) t = StringUtils.replace(t, HTML_BREAK, EMPTY_STRING);
        return t;
    }

    /**
     * Returns the style of the renderer, which holds every option that decides how the text looks.
     */
    @NotNull
    public MarkdownStyle getStyle() {
        return this.style;
    }

    /**
     * Marks what a change to the style made outdated.
     */
    protected void onStyleChanged(@NotNull MarkdownStyleOption.Impact impact) {
        switch (impact) {
            case PARSE:
                this.fragmentStyleVersion++;
                this.parseNeeded = true;
                break;
            case LAYOUT:
                this.layoutNeeded = true;
                break;
            case COMPONENTS:
                this.fragmentStyleVersion++;
                break;
            default:
                break;
        }
    }

    public boolean isParseMarkdown() {
        return this.parseMarkdown;
    }

    public void setParseMarkdown(boolean parseMarkdown) {
        if (this.parseMarkdown != parseMarkdown) {
            this.parseMarkdown = parseMarkdown;
            this.parseNeeded = true;
        }
    }

    @NotNull
    public String getText() {
        return this.text;
    }

    public void setText(@NotNull String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            this.text = I18n.get("fancymenu.markdown.error.text_too_long");
            this.textPlaceholders = "";
        } else {
            this.text = text;
            StringBuilder builder = new StringBuilder();
            for (PlaceholderParser.ParsedPlaceholder p : PlaceholderParser.findPlaceholders(this.text, new HashMap<>())) {
                builder.append(p.placeholderString);
            }
            this.textPlaceholders = builder.toString();
        }
        this.lastTextPlaceholders = null;
    }

    public float getX() {
        return this.x;
    }

    public MarkdownRenderer setX(float x) {
        this.x = x;
        return this;
    }

    public float getY() {
        return this.y;
    }

    public MarkdownRenderer setY(float y) {
        this.y = y;
        return this;
    }

    public MarkdownRenderer setOptimalWidth(float width) {
        if (this.optimalWidth != width) {
            this.optimalWidth = width;
            this.layoutNeeded = true;
        }
        return this;
    }

    public float getOptimalWidth() {
        return this.optimalWidth;
    }

    public float getRealWidth() {
        this.refresh(false);
        //If real width smaller than optimal, return optimal
        return Math.max(this.realWidth, this.optimalWidth);
    }

    public float getRealHeight() {
        this.refresh(false);
        return this.realHeight;
    }

    /**
     * Tells the renderer which vertical range of the screen is visible, in the same coordinates as its Y position.<br>
     * Lines that are completely above or below that range don't get rendered, and the renderer finds the lines inside the range without walking the whole text.
     * Whatever clips the text, like a scroll area, should set its visible range here before every render. Every line is visible by default.
     */
    public MarkdownRenderer setVisibleVerticalRange(float top, float bottom) {
        this.visibleTop = top;
        this.visibleBottom = bottom;
        return this;
    }

    @Nullable
    public Float getParentRenderScale() {
        return this.parentRenderScale;
    }

    public MarkdownRenderer setParentRenderScale(@Nullable Float parentRenderScale) {
        this.parentRenderScale = parentRenderScale;
        return this;
    }

    public boolean isAutoLineBreakingEnabled() {
        return this.autoLineBreaks;
    }

    public MarkdownRenderer setAutoLineBreakingEnabled(boolean enabled) {
        if (this.autoLineBreaks != enabled) {
            this.autoLineBreaks = enabled;
            this.layoutNeeded = true;
        }
        return this;
    }

    public boolean isRemoveHtmlBreaks() {
        return this.removeHtmlBreaks;
    }

    public MarkdownRenderer setRemoveHtmlBreaks(boolean removeHtmlBreaks) {
        if (this.removeHtmlBreaks != removeHtmlBreaks) {
            this.removeHtmlBreaks = removeHtmlBreaks;
            // Makes the render text get built again
            this.lastTextPlaceholders = null;
        }
        return this;
    }

    public boolean isExtendedBulletListMarkersEnabled() {
        return this.extendedBulletListMarkers;
    }

    /**
     * Sets if "+ ", "* " and "~ " at the start of a line start list items like "- " does.<br>
     * Fragments tell the marker of their list item with {@link MarkdownTextFragment#listItemMarker}.
     */
    public MarkdownRenderer setExtendedBulletListMarkersEnabled(boolean enabled) {
        if (this.extendedBulletListMarkers != enabled) {
            this.extendedBulletListMarkers = enabled;
            this.parseNeeded = true;
        }
        return this;
    }

    public boolean isNumberedListsEnabled() {
        return this.numberedLists;
    }

    /**
     * Sets if a number with a "." or ")" and a space behind it starts a list item at the start of a line, like "1. " or "2) ".<br>
     * Numbered items use the list level and item start of bullet items. Their fragments have the marker
     * {@link MarkdownTextFragment.ListItemMarker#NUMBER} and tell their number with {@link MarkdownTextFragment#listItemNumber}.
     */
    public MarkdownRenderer setNumberedListsEnabled(boolean enabled) {
        if (this.numberedLists != enabled) {
            this.numberedLists = enabled;
            this.parseNeeded = true;
        }
        return this;
    }

    public boolean isCalloutsEnabled() {
        return this.callouts;
    }

    /**
     * Sets if a quote whose first line holds nothing but a callout marker, like "> [!NOTE]", is a callout.<br>
     * The quote context of a callout tells its type with {@link MarkdownTextFragment.QuoteContext#calloutType}. The marker line shows no text
     * and becomes one empty fragment, which has {@link MarkdownTextFragment#calloutMarker} set.
     */
    public MarkdownRenderer setCalloutsEnabled(boolean enabled) {
        if (this.callouts != enabled) {
            this.callouts = enabled;
            this.parseNeeded = true;
        }
        return this;
    }

    @NotNull
    public DrawableColor getSeparationLineColor() {
        return Objects.requireNonNull(this.style.separatorColor.get());
    }

    public MarkdownRenderer setSeparationLineColor(@NotNull DrawableColor separationLineColor) {
        this.style.separatorColor.set(separationLineColor);
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockSingleLineColor() {
        return Objects.requireNonNull(this.style.inlineCodeBackgroundColor.get());
    }

    public MarkdownRenderer setCodeBlockSingleLineColor(@NotNull DrawableColor codeBlockSingleLineColor) {
        this.style.inlineCodeBackgroundColor.set(codeBlockSingleLineColor);
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockMultiLineColor() {
        return Objects.requireNonNull(this.style.codeBlockBackgroundColor.get());
    }

    public MarkdownRenderer setCodeBlockMultiLineColor(@NotNull DrawableColor codeBlockMultiLineColor) {
        this.style.codeBlockBackgroundColor.set(codeBlockMultiLineColor);
        return this;
    }

    /**
     * Returns the color the biggest headlines get decorated with. Every headline level has a color of its own in the {@linkplain #getStyle() style}.
     */
    @NotNull
    public DrawableColor getHeadlineUnderlineColor() {
        return Objects.requireNonNull(this.style.headline(1).decorationColor.get());
    }

    /**
     * Sets the color the headlines of all levels get decorated with.
     */
    public MarkdownRenderer setHeadlineLineColor(@NotNull DrawableColor headlineUnderlineColor) {
        for (int level = 1; level <= 3; level++) {
            this.style.headline(level).decorationColor.set(headlineUnderlineColor);
        }
        return this;
    }

    @NotNull
    public DrawableColor getHyperlinkColor() {
        return Objects.requireNonNull(this.style.hyperlinkColor.get());
    }

    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.style.hyperlinkColor.set(hyperlinkColor);
        return this;
    }

    @NotNull
    public DrawableColor getTextClickEventColor() {
        return Objects.requireNonNull(this.style.clickEventColor.get());
    }

    public MarkdownRenderer setTextClickEventColor(@NotNull DrawableColor textClickEventColor) {
        this.style.clickEventColor.set(textClickEventColor);
        return this;
    }

    @NotNull
    public DrawableColor getTextHoverEventColor() {
        return Objects.requireNonNull(this.style.hoverEventColor.get());
    }

    public MarkdownRenderer setTextHoverEventColor(@NotNull DrawableColor textHoverEventColor) {
        this.style.hoverEventColor.set(textHoverEventColor);
        return this;
    }

    /**
     * Returns the color of the text of quotes. The bar next to a quote has a color of its own in the {@linkplain #getStyle() style}.
     */
    @NotNull
    public DrawableColor getQuoteColor() {
        return Objects.requireNonNull(this.style.quoteTextColor.get());
    }

    /**
     * Sets the color of the text of quotes and of the bar next to them.
     */
    public MarkdownRenderer setQuoteColor(@NotNull DrawableColor quoteColor) {
        this.style.quoteTextColor.set(quoteColor);
        this.style.quoteBarColor.set(quoteColor);
        return this;
    }

    @NotNull
    public TextCase getTextCase() {
        return this.textCase;
    }

    public MarkdownRenderer setTextCase(@NotNull TextCase textCase) {
        if (this.textCase != Objects.requireNonNull(textCase)) {
            this.textCase = textCase;
            this.fragmentStyleVersion++;
            // Fragments measure their text in the case it gets rendered in
            this.parseNeeded = true;
        }
        return this;
    }

    public float getTextBaseScale() {
        return this.textBaseScale;
    }

    public MarkdownRenderer setTextBaseScale(float textBaseScale) {
        if (this.textBaseScale != textBaseScale) {
            this.textBaseScale = textBaseScale;
            this.layoutNeeded = true;
        }
        return this;
    }

    @NotNull
    public DrawableColor getTextBaseColor() {
        return Objects.requireNonNull(this.style.textColor.get());
    }

    public MarkdownRenderer setTextBaseColor(@NotNull DrawableColor textBaseColor) {
        this.style.textColor.set(textBaseColor);
        return this;
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public float getTextOpacity() {
        return this.textOpacity;
    }

    /**
     * Value between 0.0F and 1.0F.
     */
    public MarkdownRenderer setTextOpacity(float opacity) {
        if (opacity > 1.0F) opacity = 1.0F;
        if (opacity < 0.0F) opacity = 0.0F;
        this.textOpacity = opacity;
        return this;
    }

    /**
     * Returns the color of the markers in front of the items of the first list level. Every list level has a color of its own in the {@linkplain #getStyle() style}.
     */
    @NotNull
    public DrawableColor getBulletListDotColor() {
        return Objects.requireNonNull(this.style.listLevel(1).markerColor.get());
    }

    /**
     * Sets the color of the markers in front of the items of all list levels.
     */
    public MarkdownRenderer setBulletListDotColor(@NotNull DrawableColor bulletListDotColor) {
        for (int level = 1; level <= 3; level++) {
            this.style.listLevel(level).markerColor.set(bulletListDotColor);
        }
        return this;
    }

    public float getBulletListIndent() {
        return this.style.listIndent.get();
    }

    public MarkdownRenderer setBulletListIndent(float bulletListIndent) {
        this.style.listIndent.set(bulletListIndent);
        return this;
    }

    /**
     * Returns the space between two list items. The space above the first item of a list is an option of its own in the {@linkplain #getStyle() style}.
     */
    public float getBulletListSpacing() {
        return this.style.listItemSpacing.get();
    }

    /**
     * Sets the space above every list item, which includes the first item of a list.
     */
    public MarkdownRenderer setBulletListSpacing(float bulletListSpacing) {
        this.style.listItemSpacing.set(bulletListSpacing);
        this.style.listSpaceAbove.set(bulletListSpacing);
        return this;
    }

    public boolean isQuoteItalic() {
        return this.style.quoteItalic.get();
    }

    public MarkdownRenderer setQuoteItalic(boolean quoteItalic) {
        this.style.quoteItalic.set(quoteItalic);
        return this;
    }

    public float getQuoteIndent() {
        return this.style.quoteIndent.get();
    }

    public MarkdownRenderer setQuoteIndent(float quoteIndent) {
        this.style.quoteIndent.set(quoteIndent);
        return this;
    }

    public boolean isTextShadow() {
        return this.style.textShadow.get();
    }

    public MarkdownRenderer setTextShadow(boolean textShadow) {
        this.style.textShadow.set(textShadow);
        return this;
    }

    public boolean isUIFontRenderingEnabled() {
        return this.uiFontRenderingEnabled;
    }

    public MarkdownRenderer setUIFontRenderingEnabled(boolean enabled) {
        if (this.uiFontRenderingEnabled != enabled) {
            this.uiFontRenderingEnabled = enabled;
            // Fragments get their text size from the font they got parsed with
            this.parseNeeded = true;
        }
        return this;
    }

    public float getLineSpacing() {
        return this.style.lineSpacing.get();
    }

    public MarkdownRenderer setLineSpacing(float lineSpacing) {
        this.style.lineSpacing.set(lineSpacing);
        return this;
    }

    /**
     * Returns the space between the edges of the renderer and its text area, which is the padding of the {@linkplain #getStyle() style}.
     */
    public float getBorder() {
        return this.style.padding.get();
    }

    /**
     * Sets the space between the edges of the renderer and its text area. It is at least 2, so text doesn't get cut off by containers that clip their edges.
     */
    public MarkdownRenderer setBorder(float border) {
        this.style.padding.set(border);
        return this;
    }

    @NotNull
    public DrawableColor getTableLineColor() {
        return Objects.requireNonNull(this.style.tableLineColor.get());
    }

    /**
     * Sets the color of all lines of tables, which includes the line below a table header.
     */
    public MarkdownRenderer setTableLineColor(@NotNull DrawableColor tableLineColor) {
        this.style.tableLineColor.set(tableLineColor);
        this.style.tableHeaderSeparatorColor.set(tableLineColor);
        return this;
    }

    @NotNull
    public DrawableColor getTableHeaderBackgroundColor() {
        return Objects.requireNonNull(this.style.tableHeaderBackgroundColor.get());
    }

    public MarkdownRenderer setTableHeaderBackgroundColor(@NotNull DrawableColor tableHeaderBackgroundColor) {
        this.style.tableHeaderBackgroundColor.set(tableHeaderBackgroundColor);
        return this;
    }

    @NotNull
    public DrawableColor getTableRowBackgroundColor() {
        return Objects.requireNonNull(this.style.tableRowBackgroundColor.get());
    }

    public MarkdownRenderer setTableRowBackgroundColor(@NotNull DrawableColor tableRowBackgroundColor) {
        this.style.tableRowBackgroundColor.set(tableRowBackgroundColor);
        return this;
    }

    @NotNull
    public DrawableColor getTableAlternateRowColor() {
        return Objects.requireNonNull(this.style.tableAlternateRowBackgroundColor.get());
    }

    public MarkdownRenderer setTableAlternateRowColor(@NotNull DrawableColor tableAlternateRowColor) {
        this.style.tableAlternateRowBackgroundColor.set(tableAlternateRowColor);
        return this;
    }

    public float getTableLineThickness() {
        return this.style.tableLineThickness.get();
    }

    /**
     * Sets the thickness of the lines of tables. The line below a table header becomes twice as thick.
     */
    public MarkdownRenderer setTableLineThickness(float tableLineThickness) {
        this.style.tableLineThickness.set(tableLineThickness);
        this.style.tableHeaderSeparatorThickness.set(tableLineThickness * 2);
        return this;
    }

    /**
     * Returns the space between the left edge of a table cell and its text. Every side of a cell has a padding of its own in the {@linkplain #getStyle() style}.
     */
    public float getTableCellPadding() {
        return this.style.tableCellPaddingLeft.get();
    }

    /**
     * Sets the space between the text of a table cell and all four edges of the cell.
     */
    public MarkdownRenderer setTableCellPadding(float tableCellPadding) {
        this.style.tableCellPaddingLeft.set(tableCellPadding);
        this.style.tableCellPaddingRight.set(tableCellPadding);
        this.style.tableCellPaddingTop.set(tableCellPadding);
        this.style.tableCellPaddingBottom.set(tableCellPadding);
        return this;
    }

    public boolean isTableAlternateRowColors() {
        return this.style.tableAlternateRows.get();
    }

    public MarkdownRenderer setTableAlternateRowColors(boolean tableAlternateRowColors) {
        this.style.tableAlternateRows.set(tableAlternateRowColors);
        return this;
    }

    public boolean isTableShowHeader() {
        return this.style.tableShowHeader.get();
    }

    public MarkdownRenderer setTableShowHeader(boolean tableShowHeader) {
        this.style.tableShowHeader.set(tableShowHeader);
        return this;
    }

    /**
     * Returns the space above a table. The space below a table is an option of its own in the {@linkplain #getStyle() style}.
     */
    public float getTableMargin() {
        return this.style.tableSpaceAbove.get();
    }

    /**
     * Sets the space above and below a table.
     */
    public MarkdownRenderer setTableMargin(float tableMargin) {
        this.style.tableSpaceAbove.set(tableMargin);
        this.style.tableSpaceBelow.set(tableMargin);
        return this;
    }

    /**
     * Returns the color the given color of the style gets drawn in, which has the alpha of the color multiplied with the opacity of the text.
     */
    protected int getRenderColor(@NotNull MarkdownStyleOption.ColorOption color) {
        int argb = color.argb();
        int alpha = (int)((argb >>> 24) * this.textOpacity);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    protected float getUnscaledSpaceWidth() {
        return this.layout.getSpaceWidth();
    }

    /**
     * Makes all fragments forget that the mouse was over them.
     */
    public void resetHovered() {
        this.interactions.resetHovered(this.fragments);
    }

    protected void updateTextHoverEvents() {
        this.interactions.updateHoverEvents();
    }

    public MarkdownRenderer setTextEventHandler(@Nullable TextEventHandler textEventHandler) {
        this.textEventHandler = textEventHandler;
        return this;
    }

    /**
     * Restricts which hyperlinks open on click, for example to only allow web links in text that comes from other users.
     */
    public MarkdownRenderer setHyperlinkFilter(@Nullable Predicate<String> hyperlinkFilter) {
        this.hyperlinkFilter = hyperlinkFilter;
        return this;
    }

    /**
     * Only returns the fragments that react to input, which are the link-like ones and tables that contain link-like fragments.
     * Every input event walks all children, and most fragments of a text are plain words.
     */
    @Override
    @NotNull
    public List<MarkdownTextFragment> children() {
        return this.interactions.interactiveFragments;
    }

    /**
     * Returns the fragment under the mouse, no matter if it reacts to input.
     * {@link #children()} doesn't hold plain fragments, so the fragment gets looked up in the visible lines.
     */
    @Override
    @NotNull
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        this.layout.updateVisibleLines();
        for (int i = this.layout.firstVisibleLine; (i < this.layout.visibleLinesEnd) && (i < this.lines.size()); i++) {
            MarkdownTextLine line = this.lines.get(i);
            if (!this.layout.isLineVisible(line)) continue;
            for (MarkdownTextFragment fragment : line.fragments) {
                if (fragment.isMouseOver(mouseX, mouseY)) return Optional.of(fragment);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MarkdownRenderers are not focusable.");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("MarkdownRenderers are not navigatable.");
    }

    public interface TextEventHandler {
        void onTextClickEvent(@NotNull String eventId);
        void onTextHoverEvent(@NotNull String eventId);
    }

    public enum TextCase {
        NORMAL,
        ALL_LOWER,
        ALL_UPPER
    }

    public enum MarkdownLineAlignment {
        LEFT,
        CENTERED,
        RIGHT
    }

}
