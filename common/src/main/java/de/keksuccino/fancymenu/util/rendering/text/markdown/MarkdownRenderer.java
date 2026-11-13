package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderScaleUtil;
import de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil;
import de.keksuccino.fancymenu.util.rendering.ui.FocuslessContainerEventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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
    protected DrawableColor codeBlockSingleLineColor = DrawableColor.of(new Color(115, 115, 115));
    @NotNull
    protected DrawableColor codeBlockMultiLineColor = DrawableColor.of(new Color(86, 86, 86));
    @NotNull
    protected DrawableColor headlineUnderlineColor = DrawableColor.of(new Color(169, 169, 169));
    @NotNull
    protected DrawableColor separationLineColor = DrawableColor.of(new Color(169, 169, 169));
    @NotNull
    protected DrawableColor hyperlinkColor = DrawableColor.of(new Color(7, 113, 252));
    @NotNull
    protected DrawableColor textClickEventColor = DrawableColor.of(new Color(7, 113, 252));
    @NotNull
    protected DrawableColor textHoverEventColor = DrawableColor.of(new Color(7, 113, 252));
    @NotNull
    protected DrawableColor quoteColor = DrawableColor.of(new Color(129, 129, 129));
    protected float quoteIndent = 8;
    protected boolean quoteItalic = false;
    @NotNull
    protected DrawableColor bulletListDotColor = DrawableColor.of(new Color(169, 169, 169));
    protected float bulletListIndent = 8;
    protected float bulletListSpacing = 3;
    @NotNull
    protected DrawableColor textBaseColor = DrawableColor.WHITE;
    @NotNull
    protected TextCase textCase = TextCase.NORMAL;
    protected float textBaseScale = 1.0f;
    protected boolean autoLineBreaks = true;
    protected boolean removeHtmlBreaks = true;
    protected boolean textShadow = true;
    protected boolean uiFontRenderingEnabled = false;
    protected float textOpacity = 1.0F;
    protected float lineSpacing = 2;
    protected float border = 2;
    public boolean skipRefresh = false;
    @Nullable
    protected Float parentRenderScale = null;
    @NotNull
    protected final Font font;
    @NotNull
    protected DrawableColor tableLineColor = DrawableColor.of(new Color(120, 120, 120));
    @NotNull
    protected DrawableColor tableHeaderBackgroundColor = DrawableColor.of(new Color(50, 50, 50));
    @NotNull
    protected DrawableColor tableRowBackgroundColor = DrawableColor.of(new Color(40, 40, 40));
    @NotNull
    protected DrawableColor tableAlternateRowColor = DrawableColor.of(new Color(60, 60, 60));
    protected float tableLineThickness = 1.0f;
    protected float tableCellPadding = 8.0f;
    protected float tableMargin = 4.0f;
    protected boolean tableAlternateRowColors = true;
    protected boolean tableShowHeader = true;
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
     * the colors of quotes, hyperlinks and text events, the text case and italic quotes. Fragments cache their component per version.
     */
    protected int fragmentStyleVersion = 0;
    protected final List<MarkdownTextLine> lines = new ArrayList<>();
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();
    final MarkdownTextWidthCache textWidthCache = new MarkdownTextWidthCache(this);
    final MarkdownLayout layout = new MarkdownLayout(this);
    final MarkdownInteractions interactions = new MarkdownInteractions(this);

    public MarkdownRenderer() {
        this(Minecraft.getInstance().font);
    }

    MarkdownRenderer(@NotNull Font font) {
        this.font = Objects.requireNonNull(font);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.tick();
        this.renderVisibleLines(graphics, mouseX, mouseY, partial);
        this.updateTextHoverEvents();
    }

    protected void renderVisibleLines(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.layout.updateVisibleLines();
        for (int i = this.layout.firstVisibleLine; (i < this.layout.visibleLinesEnd) && (i < this.lines.size()); i++) {
            MarkdownTextLine line = this.lines.get(i);
            if (!this.layout.isLineVisible(line)) continue;
            line.offsetX = this.getRenderOffsetX(line);
            line.render(graphics, mouseX, mouseY, partial);
        }
    }

    protected float getRenderOffsetX(@NotNull MarkdownTextLine line) {
        if ((line.alignment == MarkdownLineAlignment.CENTERED) && !this.uiFontRenderingEnabled && line.isAlignmentAllowed(line.alignment)) {
            // Odd-width centering can land on half GUI pixels. Vanilla bitmap text needs a physical-pixel-aligned origin, while UI fonts support fractional origins.
            float renderScale = this.parentRenderScale != null ? this.parentRenderScale : RenderScaleUtil.getCurrentRenderScale();
            float renderTranslationX = RenderTranslationUtil.getCurrentRenderTranslationX();
            return RenderScaleUtil.snapGuiCoordinateToPixel(this.x + line.layoutOffsetX, renderScale, renderTranslationX) - this.x;
        }
        return line.layoutOffsetX;
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
            if (this.skipRefresh) return;
            if (this.parseNeeded) {
                this.parseNeeded = false;
                this.layoutNeeded = true;
                this.rebuildFragments();
            }
            if (this.layoutNeeded) {
                this.layoutNeeded = false;
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
        if (this.renderText != null) {
            this.textWidthCache.beginParse();
            try {
                this.fragments.addAll(MarkdownParser.parse(this, this.renderText, this.parseMarkdown));
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
        return preprocessRenderText(this.text, this.removeHtmlBreaks, text -> PlaceholderParser.replacePlaceholdersWithoutLengthLimit(text, true));
    }

    @NotNull
    static String preprocessRenderText(@NotNull String rawText, boolean removeHtmlBreaks, @NotNull UnaryOperator<String> placeholderExpander) {
        String t = placeholderExpander.apply(rawText);
        t = StringUtils.replace(t, NEWLINE_PERCENT, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_R, NEWLINE);
        t = StringUtils.replace(t, NEWLINE_ESCAPED, NEWLINE);
        if (removeHtmlBreaks) t = StringUtils.replace(t, HTML_BREAK, EMPTY_STRING);
        return t;
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

    @NotNull
    public DrawableColor getSeparationLineColor() {
        return this.separationLineColor;
    }

    public MarkdownRenderer setSeparationLineColor(@NotNull DrawableColor separationLineColor) {
        this.separationLineColor = separationLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockSingleLineColor() {
        return this.codeBlockSingleLineColor;
    }

    public MarkdownRenderer setCodeBlockSingleLineColor(@NotNull DrawableColor codeBlockSingleLineColor) {
        this.codeBlockSingleLineColor = codeBlockSingleLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getCodeBlockMultiLineColor() {
        return this.codeBlockMultiLineColor;
    }

    public MarkdownRenderer setCodeBlockMultiLineColor(@NotNull DrawableColor codeBlockMultiLineColor) {
        this.codeBlockMultiLineColor = codeBlockMultiLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getHeadlineUnderlineColor() {
        return this.headlineUnderlineColor;
    }

    public MarkdownRenderer setHeadlineLineColor(@NotNull DrawableColor headlineUnderlineColor) {
        this.headlineUnderlineColor = headlineUnderlineColor;
        return this;
    }

    @NotNull
    public DrawableColor getHyperlinkColor() {
        return this.hyperlinkColor;
    }

    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.hyperlinkColor = Objects.requireNonNull(hyperlinkColor);
        this.fragmentStyleVersion++;
        return this;
    }

    @NotNull
    public DrawableColor getTextClickEventColor() {
        return this.textClickEventColor;
    }

    public MarkdownRenderer setTextClickEventColor(@NotNull DrawableColor textClickEventColor) {
        this.textClickEventColor = Objects.requireNonNull(textClickEventColor);
        this.fragmentStyleVersion++;
        return this;
    }

    @NotNull
    public DrawableColor getTextHoverEventColor() {
        return this.textHoverEventColor;
    }

    public MarkdownRenderer setTextHoverEventColor(@NotNull DrawableColor textHoverEventColor) {
        this.textHoverEventColor = Objects.requireNonNull(textHoverEventColor);
        this.fragmentStyleVersion++;
        return this;
    }

    @NotNull
    public DrawableColor getQuoteColor() {
        return this.quoteColor;
    }

    public MarkdownRenderer setQuoteColor(@NotNull DrawableColor quoteColor) {
        this.quoteColor = Objects.requireNonNull(quoteColor);
        this.fragmentStyleVersion++;
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
        return this.textBaseColor;
    }

    public MarkdownRenderer setTextBaseColor(@NotNull DrawableColor textBaseColor) {
        this.textBaseColor = textBaseColor;
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

    @NotNull
    public DrawableColor getBulletListDotColor() {
        return this.bulletListDotColor;
    }

    public MarkdownRenderer setBulletListDotColor(@NotNull DrawableColor bulletListDotColor) {
        this.bulletListDotColor = bulletListDotColor;
        return this;
    }

    public float getBulletListIndent() {
        return this.bulletListIndent;
    }

    public MarkdownRenderer setBulletListIndent(float bulletListIndent) {
        if (this.bulletListIndent != bulletListIndent) {
            this.bulletListIndent = bulletListIndent;
            this.layoutNeeded = true;
        }
        return this;
    }

    public float getBulletListSpacing() {
        return this.bulletListSpacing;
    }

    public MarkdownRenderer setBulletListSpacing(float bulletListSpacing) {
        if (this.bulletListSpacing != bulletListSpacing) {
            this.bulletListSpacing = bulletListSpacing;
            this.layoutNeeded = true;
        }
        return this;
    }

    public boolean isQuoteItalic() {
        return this.quoteItalic;
    }

    public MarkdownRenderer setQuoteItalic(boolean quoteItalic) {
        if (this.quoteItalic != quoteItalic) {
            this.quoteItalic = quoteItalic;
            this.fragmentStyleVersion++;
            // Fragments measure their text in the style it gets rendered in
            this.parseNeeded = true;
        }
        return this;
    }

    public float getQuoteIndent() {
        return this.quoteIndent;
    }

    public MarkdownRenderer setQuoteIndent(float quoteIndent) {
        if (this.quoteIndent != quoteIndent) {
            this.quoteIndent = quoteIndent;
            this.layoutNeeded = true;
        }
        return this;
    }

    public boolean isTextShadow() {
        return this.textShadow;
    }

    public MarkdownRenderer setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
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
        return this.lineSpacing;
    }

    public MarkdownRenderer setLineSpacing(float lineSpacing) {
        if (this.lineSpacing != lineSpacing) {
            this.lineSpacing = lineSpacing;
            this.layoutNeeded = true;
        }
        return this;
    }

    public float getBorder() {
        return Math.max(2.0F, this.border); // Cap min border size at 2, so text doesn't get cut off
    }

    public MarkdownRenderer setBorder(float border) {
        border = Math.max(2.0F, border); // Cap min border size at 2, so text doesn't get cut off
        if (this.border != border) {
            this.border = border;
            this.layoutNeeded = true;
        }
        return this;
    }

    @NotNull
    public DrawableColor getTableLineColor() {
        return this.tableLineColor;
    }

    public MarkdownRenderer setTableLineColor(@NotNull DrawableColor tableLineColor) {
        this.tableLineColor = tableLineColor;
        return this;
    }

    @NotNull
    public DrawableColor getTableHeaderBackgroundColor() {
        return this.tableHeaderBackgroundColor;
    }

    public MarkdownRenderer setTableHeaderBackgroundColor(@NotNull DrawableColor tableHeaderBackgroundColor) {
        this.tableHeaderBackgroundColor = tableHeaderBackgroundColor;
        return this;
    }

    @NotNull
    public DrawableColor getTableRowBackgroundColor() {
        return this.tableRowBackgroundColor;
    }

    public MarkdownRenderer setTableRowBackgroundColor(@NotNull DrawableColor tableRowBackgroundColor) {
        this.tableRowBackgroundColor = tableRowBackgroundColor;
        return this;
    }

    @NotNull
    public DrawableColor getTableAlternateRowColor() {
        return this.tableAlternateRowColor;
    }

    public MarkdownRenderer setTableAlternateRowColor(@NotNull DrawableColor tableAlternateRowColor) {
        this.tableAlternateRowColor = tableAlternateRowColor;
        return this;
    }

    public float getTableLineThickness() {
        return this.tableLineThickness;
    }

    public MarkdownRenderer setTableLineThickness(float tableLineThickness) {
        if (this.tableLineThickness != tableLineThickness) {
            this.tableLineThickness = tableLineThickness;
            this.layoutNeeded = true;
        }
        return this;
    }

    public float getTableCellPadding() {
        return this.tableCellPadding;
    }

    public MarkdownRenderer setTableCellPadding(float tableCellPadding) {
        if (this.tableCellPadding != tableCellPadding) {
            this.tableCellPadding = tableCellPadding;
            this.layoutNeeded = true;
        }
        return this;
    }

    public boolean isTableAlternateRowColors() {
        return this.tableAlternateRowColors;
    }

    public MarkdownRenderer setTableAlternateRowColors(boolean tableAlternateRowColors) {
        this.tableAlternateRowColors = tableAlternateRowColors;
        return this;
    }

    public boolean isTableShowHeader() {
        return this.tableShowHeader;
    }

    public MarkdownRenderer setTableShowHeader(boolean tableShowHeader) {
        this.tableShowHeader = tableShowHeader;
        return this;
    }

    public float getTableMargin() {
        return this.tableMargin;
    }

    public MarkdownRenderer setTableMargin(float tableMargin) {
        if (this.tableMargin != tableMargin) {
            this.tableMargin = tableMargin;
            this.layoutNeeded = true;
        }
        return this;
    }

    protected float getUnscaledTextWidth(@NotNull Component text) {
        if (this.uiFontRenderingEnabled) {
            return UIBase.getUITextWidth(text, UIBase.getUITextSizeNormal());
        }
        return this.font.width(text);
    }

    protected float getUnscaledTextWidth(@NotNull String text) {
        if (this.uiFontRenderingEnabled) {
            return UIBase.getUITextWidth(text, UIBase.getUITextSizeNormal());
        }
        return this.font.width(text);
    }

    protected float getUnscaledTextHeight() {
        if (this.uiFontRenderingEnabled) {
            return UIBase.getUITextHeight(UIBase.getUITextSizeNormal());
        }
        return this.font.lineHeight;
    }

    protected float getUnscaledSpaceWidth() {
        return this.layout.getSpaceWidth();
    }

    protected void renderText(@NotNull GuiGraphics graphics, @NotNull Component text, float x, float y, int color, boolean shadow) {
        if (this.uiFontRenderingEnabled) {
            UIBase.renderText(graphics, text, x, y, color, UIBase.getUITextSizeNormal());
            return;
        }
        graphics.drawString(this.font, text, (int)x, (int)y, color, shadow);
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

    protected boolean isHyperlinkAllowed(@NotNull String link) {
        return (this.hyperlinkFilter == null) || this.hyperlinkFilter.test(link);
    }

    protected void fireTextClickEvent(@NotNull String eventId) {
        if (this.textEventHandler != null) {
            this.textEventHandler.onTextClickEvent(eventId);
        }
    }

    protected void fireTextHoverEvent(@NotNull String eventId) {
        if (this.textEventHandler != null) {
            this.textEventHandler.onTextHoverEvent(eventId);
        }
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
