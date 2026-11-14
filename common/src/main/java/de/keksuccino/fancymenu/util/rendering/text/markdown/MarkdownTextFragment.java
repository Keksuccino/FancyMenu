package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownTextFragment implements Renderable, GuiEventListener {

    protected static final int TRAILING_SPACE_KEEP = 0;
    protected static final int TRAILING_SPACE_REMOVE = 1;
    protected static final int TRAILING_SPACE_SEPARATE = 2;
    private static final int COMPONENT_STATE_LINE_END = 1;
    private static final int COMPONENT_STATE_HYPERLINK_HOVERED = 2;
    private static final int COMPONENT_STATE_CLICK_EVENT_HOVERED = 4;
    private static final int COMPONENT_STATE_HOVER_EVENT_HOVERED = 8;

    public final MarkdownRenderer parent;
    public MarkdownTextLine parentLine;
    public String text;
    public float x;
    public float y;
    public float unscaledTextWidth;
    /**
     * How much of {@link #unscaledTextWidth} is the space at the end of link-like or struck through text. The width gets measured before the layout
     * knows where the fragment ends up, and the space is no part of that text when the fragment ends it or its line ({@link #getTrailingSpaceMode()}).
     */
    protected float unscaledTrailingSpaceWidth;
    public float unscaledTextHeight;
    public boolean startOfRenderLine = false;
    public boolean naturalLineBreakAfter;
    public boolean autoLineBreakAfter;
    public boolean endOfWord;
    public ResourceSupplier<ITexture> imageSupplier = null;
    /** The alternative text of the image of this fragment, without the backslashes of its escaped characters. NULL if the fragment is no image. */
    @Nullable
    public String imageAltText = null;
    /** The caption below the image of this fragment, if the style shows one. Wrapped by the layout whenever it measures the image. */
    @Nullable
    MarkdownImagePainter.Caption imageCaption = null;
    public boolean separationLine;
    public DrawableColor textColor = null;
    public boolean bold;
    public boolean italic;
    public boolean strikethrough;
    /** If the struck through text of this fragment is the last of its struck through text. {@link MarkdownStrikethroughPainter#markEnds(List)} looks it up. */
    protected boolean strikethroughEnd;
    @NotNull
    MinecraftFormattingState minecraftFormatting = MinecraftFormattingState.EMPTY;
    public boolean bulletListItemStart = false;
    public int bulletListLevel = 0;
    /**
     * The marker the list item of this fragment starts with, or NONE if the fragment is not part of a list item.<br>
     * It is set on the same fragments as {@link #bulletListLevel}: on all fragments of the line the item starts in.
     */
    @NotNull
    public ListItemMarker listItemMarker = ListItemMarker.NONE;
    /**
     * The number of the numbered list item of this fragment exactly as it was typed, with the "." or ")" behind it, like "1." or "07)".
     * NULL if the marker of the item is not {@link ListItemMarker#NUMBER}.<br>
     * It is set on the same fragments as {@link #listItemMarker}.
     */
    @Nullable
    public String listItemNumber = null;
    /**
     * If the list of the item of this fragment has an item that starts with "+". {@link MarkdownListMarkerPainter#markListsWithPlusItems(List)} looks it up.<br>
     * It is set on the same fragments as {@link #listItemMarker}.
     */
    protected boolean listHasPlusItems = false;
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    public Hyperlink hyperlink = null;
    public TextClickEvent clickEvent = null;
    public TextHoverEvent hoverEvent = null;
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    public QuoteContext quoteContext = null;
    /**
     * TRUE for the empty fragment that stands for the marker line of a callout ("> [!NOTE]"). It is the first fragment of the
     * {@link #quoteContext} of the callout and has its own line, so a callout without a body still has a fragment.
     */
    public boolean calloutMarker = false;
    public CodeBlockContext codeBlockContext = null;
    public boolean plainText = false;
    public ResourceLocation font = null;
    /** Only gets changed through {@link #setHovered(boolean)}, because the fragment's hyperlink and text events count their hovered fragments. */
    public boolean hovered = false;
    public TableContext tableContext = null;
    /** TRUE for the fragment that stands for a whole table. The fragments inside its cells share its table context, so the context alone does not tell them apart. */
    public boolean table = false;
    /** TRUE for the fragments in the cells of the header row of a table. Set after every parse. */
    public boolean tableHeader = false;
    /** If a line may break somewhere inside of the text, at a place {@link MarkdownLineBreaks} allows. Set when the fragment gets built. */
    protected boolean breaksInside = false;
    /** What the fragment was before the layout split it into pieces, and the pieces behind the first one. NULL while the fragment is whole. */
    @Nullable
    private Pieces pieces = null;
    @Nullable
    private Component renderComponent = null;
    @Nullable
    private MarkdownFixedWidthText fixedWidthText = null;
    private int renderComponentState;
    private int renderComponentStyleVersion;
    private int renderComponentCustomColor;

    public MarkdownTextFragment(@NotNull MarkdownRenderer parent, @NotNull String text) {
        this.parent = parent;
        this.text = text;
        this.unscaledTextHeight = this.parent.textFont.lineHeight();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.setHovered(this.isMouseOver(mouseX, mouseY));

        if (this.isLinkLike() && this.hovered) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }

        if (this.isTable()) {
            MarkdownTablePainter.render(graphics, this, mouseX, mouseY, partial);
            return;
        }

        if (this.imageSupplier != null) {
            MarkdownImagePainter.render(graphics, this);
        } else if (this.separationLine) {
            MarkdownSeparatorPainter.render(graphics, this);
        } else {

            float scale = this.getScale();
            if (scale <= 0.0F) return;

            this.renderInlineCodeBackground(graphics);

            if (this.calloutMarker) MarkdownCalloutPainter.renderIcon(graphics, this);

            // The fragments that only end a line and the ones of blank lines have nothing to draw
            if (!this.text.isEmpty() || this.calloutMarker) {
                int color = this.parent.getRenderColor(this.parent.style.textColor);
                if (this.strikethrough) MarkdownStrikethroughPainter.render(graphics, this, true);
                RenderSystem.enableBlend();
                graphics.pose().pushPose();
                try {
                    // Vanilla text rendering only accepts integer draw coordinates. Translate to the exact text origin and draw at local zero so fractional scaling cannot truncate the absolute position into the parent's scissor area.
                    graphics.pose().translate(this.getTextX(), this.getTextY(), 0.0F);
                    graphics.pose().scale(scale, scale, scale);
                    if (this.hasFixedWidthCells()) {
                        this.getFixedWidthText().render(graphics, this.parent, color);
                    } else {
                        this.parent.textFont.draw(graphics, this.getRenderComponent(), 0.0F, 0.0F, color, this.parent.textFont.hasShadow() && (this.codeBlockContext == null));
                    }
                } finally {
                    graphics.pose().popPose();
                }
            }
            RenderingUtils.resetShaderColor(graphics);

            if (this.strikethrough) MarkdownStrikethroughPainter.render(graphics, this, false);
            if (this.isLinkLike()) MarkdownLinkPainter.renderUnderline(graphics, this);

        }

    }

    /**
     * Draws the background of the inline code of this fragment, if the fragment is the first one of that code in its line.
     * Fragments in table cells have no line, so the table draws the backgrounds of the code in its cells.
     */
    protected void renderInlineCodeBackground(GuiGraphics graphics) {
        if ((this.codeBlockContext == null) || !this.codeBlockContext.singleLine || (this.parentLine == null)) return;
        MarkdownTextLine.SingleLineCodeBlockPart part = this.parentLine.singleLineCodeBlockStartEndPairs.get(this.codeBlockContext);
        if ((part != null) && (part.start == this) && (part.end != null)) MarkdownCodePainter.renderInline(graphics, this, part.end);
    }

    /**
     * Returns the Y position of the render line of the fragment, for where the renderer is right now.<br>
     * A fragment only gets its own position when its line renders, so this is the position to use for fragments that can be outside the visible range.
     */
    protected float getRenderLineY() {
        if (this.parentLine == null) return this.y;
        return this.parentLine.getRenderY();
    }

    /**
     * Returns the component that gets drawn for this fragment.<br>
     * Building it allocates a style chain and a component, so the result stays cached. The cache follows every input that changes without the
     * fragment knowing: the hover state of the fragment's hyperlink and text events and the end of a line (packed into a state value), the
     * renderer settings that {@link MarkdownRenderer#fragmentStyleVersion} counts, and the color of a custom color formatter.
     * Everything else the component gets built from belongs to the fragment itself, and changing it has to be followed by
     * {@link #updateWidth()}, which drops the cached component.
     */
    @NotNull
    protected Component getRenderComponent() {
        int state = this.getRenderComponentState();
        int customColor = this.minecraftFormatting.getCurrentCustomColor();
        if ((this.renderComponent == null) || (this.renderComponentState != state) || (this.renderComponentStyleVersion != this.parent.fragmentStyleVersion) || (this.renderComponentCustomColor != customColor)) {
            this.renderComponent = this.buildRenderComponent(false);
            this.fixedWidthText = null;
            this.renderComponentState = state;
            this.renderComponentStyleVersion = this.parent.fragmentStyleVersion;
            this.renderComponentCustomColor = customColor;
        }
        return this.renderComponent;
    }

    /**
     * Returns if the text of the fragment gets set into cells of the same width, which is what the style can do with the text of code blocks.
     */
    protected boolean hasFixedWidthCells() {
        return (this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.parent.style.codeBlockFixedWidth.get();
    }

    /**
     * Returns the text of the fragment set into cells. It gets built from the render component and stays cached as long as that one does.
     */
    @NotNull
    protected MarkdownFixedWidthText getFixedWidthText() {
        Component component = this.getRenderComponent();
        // The component of text with characters the font of code does not have keeps that font in its parts
        if (this.fixedWidthText == null) this.fixedWidthText = MarkdownFixedWidthText.of(this.parent, component.getString(), this.parent.textFont.withCodeFont(component.getStyle()));
        return this.fixedWidthText;
    }

    private int getRenderComponentState() {
        // Only link-like and struck through fragments get built differently at the end of a line, and only link-like ones when they are hovered
        if (!this.isLinkLike() && !this.strikethrough) return 0;
        int state = (this.naturalLineBreakAfter || this.autoLineBreakAfter) ? COMPONENT_STATE_LINE_END : 0;
        if ((this.hyperlink != null) && this.hyperlink.isHovered()) state |= COMPONENT_STATE_HYPERLINK_HOVERED;
        if ((this.clickEvent != null) && this.clickEvent.isHovered()) state |= COMPONENT_STATE_CLICK_EVENT_HOVERED;
        if ((this.hoverEvent != null) && this.hoverEvent.isHovered()) state |= COMPONENT_STATE_HOVER_EVENT_HOVERED;
        return state;
    }

    @NotNull
    protected Component buildRenderComponent(boolean forWidthCalculation) {
        int trailingSpaceMode = this.getTrailingSpaceMode();
        return this.parent.textFont.component(this.buildRenderStyle(forWidthCalculation), this.buildRenderText(trailingSpaceMode), trailingSpaceMode == TRAILING_SPACE_SEPARATE);
    }

    @NotNull
    protected static Component createRenderComponent(@NotNull Style style, @NotNull String renderText, boolean separateTrailingSpace) {
        MutableComponent comp = Component.literal(renderText).setStyle(style);
        if (separateTrailingSpace) {
            comp.append(Component.literal(" ").setStyle(Style.EMPTY.withUnderlined(false).withStrikethrough(false)));
        }
        return comp;
    }

    @NotNull
    protected Style buildRenderStyle(boolean forWidthCalculation) {
        Style style = Style.EMPTY;
        if (this.font != null) {
            style = style.withFont(this.font);
        }
        if (this.italic) {
            style = style.withItalic(true);
        }
        if (this.bold) {
            style = style.withBold(true);
        }
        if (this.strikethrough && !forWidthCalculation && !MarkdownStrikethroughPainter.drawsLine(this)) {
            style = style.withStrikethrough(true);
        }
        if (this.quoteContext != null) {
            // The text of a callout can have a color of its own
            boolean calloutColor = (this.quoteContext.calloutType != null) && this.parent.style.calloutTextColor.isSet();
            style = style.withColor(calloutColor ? this.parent.style.calloutTextColor.argb() : this.parent.style.quoteTextColor.argb());
            if (this.parent.style.quoteItalic.get()) {
                style = style.withItalic(true);
            }
        }
        if (this.calloutMarker) {
            // The label of a callout
            MarkdownStyle.Callout callout = MarkdownCalloutPainter.getCallout(this.parent, this.quoteContext);
            if (callout != null) style = style.withColor(callout.color.argb());
            if (this.parent.style.calloutLabelBold.get()) style = style.withBold(true);
        } else if (this.headlineType != HeadlineType.NONE) {
            MarkdownStyle.Headline headline = this.parent.style.headline(this.headlineType.level);
            if (headline.bold.get()) style = style.withBold(true);
            if (headline.color.isSet()) style = style.withColor(headline.color.argb());
        } else if (this.bold && this.parent.style.boldTextColor.isSet()) {
            style = style.withColor(this.parent.style.boldTextColor.argb());
        }
        if (this.tableHeader && this.parent.style.tableShowHeader.get()) {
            if (this.parent.style.tableHeaderBold.get()) style = style.withBold(true);
            if (this.parent.style.tableHeaderTextColor.isSet()) style = style.withColor(this.parent.style.tableHeaderTextColor.argb());
        }
        if (this.textColor != null) {
            style = style.withColor(this.textColor.getColorInt());
        }
        if (this.codeBlockContext != null) {
            // Code shows its text as it is, in the color and the font the style gives it
            MarkdownStyleOption.ColorOption codeColor = this.codeBlockContext.singleLine ? this.parent.style.inlineCodeTextColor : this.parent.style.codeBlockTextColor;
            style = this.parent.textFont.withCodeFont(codeColor.isSet() ? Style.EMPTY.withColor(codeColor.argb()) : Style.EMPTY);
        }
        if (this.plainText) {
            style = Style.EMPTY;
        }
        style = this.minecraftFormatting.applyTo(style);
        if (this.hyperlink != null) {
            style = this.applyLinkStyle(style, this.parent.style.hyperlinkColor, this.hyperlink.isHovered());
        }
        if (this.clickEvent != null) {
            style = this.applyLinkStyle(style, this.parent.style.clickEventColor, this.clickEvent.isHovered());
        }
        if (this.hoverEvent != null) {
            style = this.applyLinkStyle(style, this.parent.style.hoverEventColor, this.hoverEvent.isHovered());
        }
        return style;
    }

    /**
     * Gives the style the color of a link or text event, and its underline if that is part of the text (see {@link MarkdownLinkPainter}).
     */
    @NotNull
    private Style applyLinkStyle(@NotNull Style style, @NotNull MarkdownStyleOption.ColorOption color, boolean hovered) {
        MarkdownStyle markdownStyle = this.parent.style;
        style = style.withColor((hovered && markdownStyle.linkHoverColor.isSet()) ? markdownStyle.linkHoverColor.argb() : color.argb());
        if (!markdownStyle.linkUnderlineColor.isSet() && MarkdownLinkPainter.isUnderlined(this, hovered)) {
            style = style.withUnderlined(true);
        }
        return style;
    }

    /**
     * Returns if the mouse is over the hyperlink or one of the text events of the fragment.
     */
    protected boolean isLinkHovered() {
        return ((this.hyperlink != null) && this.hyperlink.isHovered()) || ((this.clickEvent != null) && this.clickEvent.isHovered()) || ((this.hoverEvent != null) && this.hoverEvent.isHovered());
    }

    /**
     * The space at the end of link-like or struck through text would look ugly with a line below or through it, so it gets removed
     * at the end of a line and drawn without the style of that text behind its last word.
     */
    protected int getTrailingSpaceMode() {
        boolean linkLike = this.isLinkLike();
        if ((!linkLike && !this.strikethrough) || !this.text.endsWith(" ")) return TRAILING_SPACE_KEEP;
        if (this.naturalLineBreakAfter || this.autoLineBreakAfter) return TRAILING_SPACE_REMOVE;
        return (linkLike ? this.isLastLinkLikeFragment() : this.strikethroughEnd) ? TRAILING_SPACE_SEPARATE : TRAILING_SPACE_KEEP;
    }

    /**
     * Returns the unscaled width of the text without the space at its end, if that space is no part of the link-like or struck through text of the fragment.
     */
    protected float getUnscaledStyledTextWidth() {
        return (this.getTrailingSpaceMode() != TRAILING_SPACE_KEEP) ? (this.unscaledTextWidth - this.unscaledTrailingSpaceWidth) : this.unscaledTextWidth;
    }

    @NotNull
    protected String buildRenderText(int trailingSpaceMode) {
        // The fragment of the marker line of a callout has no text and shows the label of the callout
        String t = (this.calloutMarker && (this.quoteContext != null) && (this.quoteContext.calloutType != null)) ? MarkdownCalloutPainter.getLabel(this.quoteContext.calloutType) : this.text;
        if (trailingSpaceMode != TRAILING_SPACE_KEEP) {
            t = t.substring(0, t.length()-1);
        }
        return this.applyTextCase(t);
    }

    @NotNull
    private String applyTextCase(@NotNull String text) {
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_UPPER) return text.toUpperCase();
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_LOWER) return text.toLowerCase();
        return text;
    }

    protected boolean isLinkLike() {
        return (this.hyperlink != null) || (this.clickEvent != null) || (this.hoverEvent != null);
    }

    protected boolean isLastLinkLikeFragment() {
        if (this.hyperlink != null) {
            return isLastPieceOf(this.hyperlink.hyperlinkFragments);
        }
        if (this.clickEvent != null) {
            return isLastPieceOf(this.clickEvent.eventFragments);
        }
        if (this.hoverEvent != null) {
            return isLastPieceOf(this.hoverEvent.eventFragments);
        }
        return false;
    }

    private boolean isLastPieceOf(@NotNull List<MarkdownTextFragment> fragments) {
        MarkdownTextFragment last = ListUtils.getLast(fragments);
        return (last != null) && (last.getLastPiece() == this);
    }

    /**
     * Measures the text of the fragment again.<br>
     * Has to get called after anything changed that the fragment's text or style gets built from, like its text or formatting.
     */
    protected void updateWidth() {
        this.renderComponent = null;
        this.fixedWidthText = null;
        this.unscaledTrailingSpaceWidth = 0.0F;
        int trailingSpaceMode = this.getTrailingSpaceMode();
        String renderText = this.buildRenderText(trailingSpaceMode);
        if (this.hasFixedWidthCells()) {
            this.unscaledTextWidth = MarkdownFixedWidthText.getWidth(this.parent, renderText);
            return;
        }
        Style style = this.buildRenderStyle(true);
        this.unscaledTextWidth = this.parent.textWidthCache.getWidth(style, renderText, trailingSpaceMode == TRAILING_SPACE_SEPARATE);
        if (trailingSpaceMode == TRAILING_SPACE_KEEP) {
            if ((this.isLinkLike() || this.strikethrough) && renderText.endsWith(" ")) this.unscaledTrailingSpaceWidth = this.unscaledTextWidth - this.parent.textWidthCache.getWidth(style, renderText.substring(0, renderText.length() - 1), false);
        } else if (trailingSpaceMode == TRAILING_SPACE_SEPARATE) {
            this.unscaledTrailingSpaceWidth = this.unscaledTextWidth - this.parent.textWidthCache.getWidth(style, renderText, false);
        }
    }

    /**
     * Sets if the mouse is over the fragment, and keeps the hover state of the fragment's hyperlink and text events up to date.
     */
    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        int change = hovered ? 1 : -1;
        if (this.hyperlink != null) this.hyperlink.hoveredFragments += change;
        if (this.clickEvent != null) this.clickEvent.hoveredFragments += change;
        if (this.hoverEvent != null) this.hoverEvent.hoveredFragments += change;
    }

    /**
     * Returns the last piece of the fragment, which is the fragment itself while it is whole.
     */
    @NotNull
    protected MarkdownTextFragment getLastPiece() {
        return ((this.pieces == null) || this.pieces.later.isEmpty()) ? this : this.pieces.later.get(this.pieces.later.size() - 1);
    }

    /**
     * Returns the text of the fragment with the text of all of its pieces.
     */
    @NotNull
    protected String getWholeText() {
        return (this.pieces != null) ? this.pieces.text : this.text;
    }

    /**
     * Returns the unscaled width the part of the whole text between the two indexes has as a piece that does not hold the end of the text.
     *
     * @param style The style for measuring text of this fragment, which {@link #buildRenderStyle(boolean)} builds.
     */
    protected float measurePiece(@NotNull Style style, int start, int end) {
        return this.parent.textWidthCache.getWidth(style, this.applyTextCase(this.getWholeText().substring(start, end)), false);
    }

    /**
     * Adds a piece behind the pieces of this fragment. It is formatted like the fragment and has no text until {@link #setPieceText(MarkdownTextFragment, int, int, float)} gives it one.<br>
     * The fragment has to be split already, which giving it the text of its first piece does.
     */
    @NotNull
    protected MarkdownTextFragment addPiece() {
        MarkdownTextFragment piece = new MarkdownTextFragment(this.parent, "");
        // Everything that describes the text of the fragment. What marks the fragment as the first one of something, like of a list item, stays with the first piece.
        piece.unscaledTextHeight = this.unscaledTextHeight;
        piece.textColor = this.textColor;
        piece.bold = this.bold;
        piece.italic = this.italic;
        piece.strikethrough = this.strikethrough;
        piece.minecraftFormatting = this.minecraftFormatting;
        piece.bulletListLevel = this.bulletListLevel;
        piece.listItemMarker = this.listItemMarker;
        piece.listItemNumber = this.listItemNumber;
        piece.listHasPlusItems = this.listHasPlusItems;
        piece.alignment = this.alignment;
        piece.hyperlink = this.hyperlink;
        piece.clickEvent = this.clickEvent;
        piece.hoverEvent = this.hoverEvent;
        piece.headlineType = this.headlineType;
        piece.quoteContext = this.quoteContext;
        piece.codeBlockContext = this.codeBlockContext;
        piece.plainText = this.plainText;
        piece.font = this.font;
        Objects.requireNonNull(this.pieces).later.add(piece);
        return piece;
    }

    /**
     * Gives a piece of this fragment the part of the whole text between the two indexes. The first call splits the fragment.
     *
     * @param piece This fragment, which is its own first piece, or a piece that {@link #addPiece()} added.
     * @param unscaledWidth The width of the part as {@link #measurePiece(Style, int, int)} returns it, or NaN to have the part measured.
     */
    protected void setPieceText(@NotNull MarkdownTextFragment piece, int start, int end, float unscaledWidth) {
        if (this.pieces == null) this.pieces = new Pieces(this);
        Pieces pieces = this.pieces;
        boolean endOfFragment = (end == pieces.text.length());
        piece.text = pieces.text.substring(start, end);
        piece.renderComponent = null;
        piece.fixedWidthText = null;
        // What the fragment is as the last fragment of something belongs to the piece with the end of its text
        piece.naturalLineBreakAfter = endOfFragment && pieces.naturalLineBreakAfter;
        piece.endOfWord = endOfFragment && pieces.endOfWord;
        piece.strikethroughEnd = endOfFragment && pieces.strikethroughEnd;
        if (endOfFragment || Float.isNaN(unscaledWidth)) {
            // The end of the text can be a space that gets measured in a way of its own
            piece.updateWidth();
        } else {
            piece.unscaledTextWidth = unscaledWidth;
            piece.unscaledTrailingSpaceWidth = 0.0F;
        }
    }

    /**
     * Makes the fragment whole again, the way it was before the layout split it. The pieces behind the first one are no part of the text after that.
     */
    protected void joinPieces() {
        Pieces pieces = this.pieces;
        if (pieces == null) return;
        this.resetHoveredPieces();
        this.pieces = null;
        this.text = pieces.text;
        this.unscaledTextWidth = pieces.unscaledTextWidth;
        this.unscaledTrailingSpaceWidth = pieces.unscaledTrailingSpaceWidth;
        this.naturalLineBreakAfter = pieces.naturalLineBreakAfter;
        this.endOfWord = pieces.endOfWord;
        this.strikethroughEnd = pieces.strikethroughEnd;
        this.renderComponent = null;
        this.fixedWidthText = null;
    }

    /**
     * Makes the pieces behind the first one forget that the mouse was over them. The hyperlink and the text events of the fragment
     * count their hovered fragments, so a piece has to do this before it goes away.
     */
    protected void resetHoveredPieces() {
        if (this.pieces == null) return;
        for (MarkdownTextFragment piece : this.pieces.later) piece.setHovered(false);
    }

    /**
     * Fragments of one render line share their Y position, so this is how a fragment checks if it sits in the first or last line of its code block.
     * Fragments outside of render lines, like the ones in table cells, always are on their own line.
     */
    protected boolean isInRenderLineOf(@Nullable MarkdownTextFragment fragment) {
        return (fragment != null) && (fragment.parentLine == this.parentLine);
    }

    public float getTextRenderX() {
        float scale = this.getScale();
        if (scale <= 0.0F) return 0.0F;
        return this.getTextX() / scale;
    }

    /**
     * Returns the space the fragment keeps free in front of its text. The first fragment of a render line holds the indents and paddings
     * of the line, and the first fragment of inline code holds the padding of its background. All of it scales with the text of the fragment.
     */
    protected float getLeadingSpace() {
        MarkdownStyle style = this.parent.style;
        float space = 0.0F;
        if (this.startOfRenderLine) {
            // A quote keeps its indent free on the side of its bar, which is the side it is aligned to, and its padding on the other side
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
                space += style.quoteIndent.get();
            }
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT)) {
                space += style.quotePaddingSide.get();
            }
            if (this.calloutMarker) {
                space += MarkdownCalloutPainter.getIconSpace(this.parent);
            }
            if (this.bulletListLevel > 0) {
                space += MarkdownListMarkerPainter.getTextIndent(this);
            }
            if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine) {
                space += style.codeBlockPaddingHorizontal.get();
            }
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            space += style.inlineCodePaddingLeft.get();
        }
        return space * this.getScale();
    }

    /**
     * Returns the space the fragment keeps free behind its text, as the last or as another fragment of its render line.
     */
    protected float getTrailingSpace(boolean endOfRenderLine) {
        return this.getTrailingSpace(endOfRenderLine, true);
    }

    /**
     * @param endOfFragment FALSE returns the space a piece of this fragment keeps free that does not hold the end of its text.
     *                      The layout asks for it before it splits the fragment.
     */
    protected float getTrailingSpace(boolean endOfRenderLine, boolean endOfFragment) {
        MarkdownStyle style = this.parent.style;
        float space = 0.0F;
        if (endOfRenderLine) {
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT)) {
                space += style.quoteIndent.get();
            }
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
                space += style.quotePaddingSide.get();
            }
            if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine) {
                space += style.codeBlockPaddingHorizontal.get();
            }
        }
        if (endOfFragment && (this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() == this)) {
            space += style.inlineCodePaddingRight.get();
        }
        return space * this.getScale();
    }

    public float getTextRenderY() {
        float scale = this.getScale();
        if (scale <= 0.0F) return 0.0F;
        return this.getTextY() / scale;
    }

    public float getRenderWidth() {
        return this.getRenderWidth(this.naturalLineBreakAfter || this.autoLineBreakAfter);
    }

    /**
     * Returns the width the fragment has as the last or as another fragment of its render line.<br>
     * The layout has to know what a fragment adds to a line when the line ends behind it before it decides that the line does.
     */
    protected float getRenderWidth(boolean endOfRenderLine) {

        // Handle table width - return actual table width for proper alignment
        if (this.isTable()) {
            return this.tableContext.totalWidth;
        }

        if (this.imageSupplier != null) {
            return MarkdownImagePainter.getWidth(this);
        }

        return this.getLeadingSpace() + this.getTextRenderWidth() + this.getTrailingSpace(endOfRenderLine);

    }

    /**
     * Returns the height of what the fragment shows. The paddings above and below it belong to its line.
     */
    public float getRenderHeight() {

        if (this.isTable()) {
            return MarkdownTablePainter.getHeight(this.parent, this.tableContext);
        }

        if (this.imageSupplier != null) {
            return MarkdownImagePainter.getHeight(this);
        }

        if (this.separationLine) {
            return MarkdownSeparatorPainter.getHeight(this);
        }

        return this.getTextRenderHeight();
    }

    public float getTextRenderWidth() {
        return this.unscaledTextWidth * this.getScale();
    }

    public float getTextRenderHeight() {
        return this.unscaledTextHeight * this.getScale();
    }

    public float getTextX() {
        return this.x + this.getLeadingSpace();
    }

    public float getTextY() {
        return this.y + ((this.parentLine != null) ? this.parentLine.paddingTop : 0.0F);
    }

    public float getTextWidth() {
        return this.getTextRenderWidth();
    }

    public float getTextHeight() {
        return this.getTextRenderHeight();
    }

    public float getScale() {
        return this.parent.scaling.textScale((this.headlineType == HeadlineType.NONE) ? 1.0F : this.parent.style.headline(this.headlineType.level).scale.get());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isTable()) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.x, this.y, this.tableContext.totalWidth, this.getRenderHeight());
        }
        if ((this.imageSupplier != null) && (this.imageSupplier.get() != null)) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.x, this.y, this.getRenderWidth(), this.getRenderHeight());
        }
        return RenderingUtils.isXYInArea(mouseX, mouseY, this.getTextX(), this.getTextY(), this.getTextWidth(), this.getTextHeight());
    }

    /**
     * Returns if a click at the given position does something in this fragment: it is on a hyperlink or on text with a click event.
     */
    public boolean takesClickAt(double mouseX, double mouseY) {
        if (this.isTable() && (this.tableContext != null)) {
            for (TableRow row : this.tableContext.rows) {
                for (TableCell cell : row.cells) {
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        if (fragment.isMouseOver(mouseX, mouseY) && fragment.takesClickAt(mouseX, mouseY)) return true;
                    }
                }
            }
            return false;
        }
        return (this.hyperlink != null) || (this.clickEvent != null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle clicks on table cells
        if (this.isTable() && this.tableContext != null) {
            // Check all cell fragments for clicks
            for (TableRow row : this.tableContext.rows) {
                for (TableCell cell : row.cells) {
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        if (fragment.isMouseOver(mouseX, mouseY) && fragment.mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        if ((this.clickEvent != null) && this.hovered) {
            this.parent.interactions.fireClickEvent(this.clickEvent.identifier);
            return true;
        }

        // Handle regular hyperlink clicks
        if ((this.hyperlink != null) && this.hovered) {
            if (this.parent.interactions.isHyperlinkAllowed(this.hyperlink.link)) WebUtils.openWebLink(this.hyperlink.link);
            return true;
        }
        return false;
    }

    public void setFocused(boolean var1) {
    }

    public boolean isFocused() {
        return false;
    }

    public boolean isTable() {
        return this.table && (this.tableContext != null);
    }

    /**
     * A line can end inside of a fragment whose text is of a script without spaces between words. The layout splits such a fragment into
     * one piece for every line it is in: the fragment itself becomes the first piece and keeps the start of its text, and every other
     * piece is a new fragment that is formatted like it.<br><br>
     *
     * The pieces behind the first one only are in the render lines, and only until the next layout, which starts with the whole fragment again.
     * They are in none of the lists the parser fills, like the fragments of a hyperlink or of a quote. The first fragment of such a list still is
     * the right one that way, and whoever needs the last one has to ask it for its {@link MarkdownTextFragment#getLastPiece() last piece}.
     */
    private static final class Pieces {

        /** What the fragment was before it got split. */
        private final String text;
        private final float unscaledTextWidth;
        private final float unscaledTrailingSpaceWidth;
        private final boolean naturalLineBreakAfter;
        private final boolean endOfWord;
        private final boolean strikethroughEnd;
        /** The pieces behind the fragment, which is its own first piece, in the order of the text. */
        private final List<MarkdownTextFragment> later = new ArrayList<>();

        private Pieces(@NotNull MarkdownTextFragment fragment) {
            this.text = fragment.text;
            this.unscaledTextWidth = fragment.unscaledTextWidth;
            this.unscaledTrailingSpaceWidth = fragment.unscaledTrailingSpaceWidth;
            this.naturalLineBreakAfter = fragment.naturalLineBreakAfter;
            this.endOfWord = fragment.endOfWord;
            this.strikethroughEnd = fragment.strikethroughEnd;
        }

    }

    /**
     * A part of the text that spans several fragments and gets hovered as a whole: a hyperlink, or a text click or hover event.
     */
    public abstract static class HoverGroup {

        /** How many fragments of the group the mouse is over. {@link MarkdownTextFragment#setHovered(boolean)} keeps it up to date. */
        protected int hoveredFragments = 0;

        public boolean isHovered() {
            return this.hoveredFragments > 0;
        }

    }

    public static class Hyperlink extends HoverGroup {

        public String link = null;
        public final List<MarkdownTextFragment> hyperlinkFragments = new ArrayList<>();

    }

    public static class TextClickEvent extends HoverGroup {

        public final String identifier;
        public final List<MarkdownTextFragment> eventFragments = new ArrayList<>();

        public TextClickEvent(@NotNull String identifier) {
            this.identifier = identifier;
        }

    }

    public static class TextHoverEvent extends HoverGroup {

        public final String identifier;
        public final List<MarkdownTextFragment> eventFragments = new ArrayList<>();

        public TextHoverEvent(@NotNull String identifier) {
            this.identifier = identifier;
        }

    }

    public static class QuoteContext {

        public final List<MarkdownTextFragment> quoteFragments = new ArrayList<>();
        /** The type of the callout this quote is, or NULL if it is a normal quote. */
        @Nullable
        public CalloutType calloutType = null;

        @Nullable
        public MarkdownTextFragment getQuoteStart() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(0);
            return null;
        }

        /**
         * Returns the last fragment of the quote, which is the last piece of a fragment that got split over several lines.
         */
        @Nullable
        public MarkdownTextFragment getQuoteEnd() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(quoteFragments.size()-1).getLastPiece();
            return null;
        }

    }

    public static class CodeBlockContext {

        public final List<MarkdownTextFragment> codeBlockFragments = new ArrayList<>();
        public boolean singleLine = true;

        @Nullable
        public MarkdownTextFragment getBlockStart() {
            if (!codeBlockFragments.isEmpty()) return codeBlockFragments.get(0);
            return null;
        }

        /**
         * Returns the last fragment of the code, which is the last piece of a fragment that got split over several lines.
         */
        @Nullable
        public MarkdownTextFragment getBlockEnd() {
            if (!codeBlockFragments.isEmpty()) return codeBlockFragments.get(codeBlockFragments.size()-1).getLastPiece();
            return null;
        }

    }

    public enum HeadlineType {

        NONE(0),
        BIG(3), // ###
        BIGGER(2), // ##
        BIGGEST(1); // #

        /** The level of the headline in the style, from 1 for the biggest headlines to 3 for the smallest ones. 0 if it is no headline. */
        public final int level;

        HeadlineType(int level) {
            this.level = level;
        }

    }

    /**
     * The types of callouts, which are quotes that start with a marker line like "> [!NOTE]".
     * The name of a type is what the marker holds, in any case.
     */
    public enum CalloutType {
        NOTE,
        TIP,
        IMPORTANT,
        WARNING,
        CAUTION
    }

    /**
     * The marker a list item starts with.
     */
    public enum ListItemMarker {
        NONE,
        DASH, // -
        PLUS, // +
        ASTERISK, // *
        TILDE, // ~
        NUMBER // 1. or 1)
    }

    public static class TableContext {

        public final List<TableRow> rows = new ArrayList<>();
        /** Width of every column. Measured by {@link #updateMetrics(MarkdownRenderer)}, like all other sizes of the table. */
        public float[] columnWidths = new float[0];
        public boolean hasHeader = false;
        public float totalWidth = 0;
        /** Height of all rows together, without the table lines. */
        public float totalRowHeight = 0;

        /**
         * Measures the columns, rows and cells of the table.<br>
         * The sizes only depend on things that need a new layout when they change, so the layout measures every table once,
         * and rendering, hit tests and size queries all use the measured sizes.
         */
        public void updateMetrics(@NotNull MarkdownRenderer renderer) {
            MarkdownTablePainter.measure(renderer, this);
        }

    }

    public static class TableRow {

        public final List<TableCell> cells = new ArrayList<>();
        public boolean isHeader = false;
        public MarkdownRenderer parent;
        /** Measured by {@link TableContext#updateMetrics(MarkdownRenderer)}. */
        public float height = 0;
        /** The distance from the top of the table to the top of the row. Measured by {@link TableContext#updateMetrics(MarkdownRenderer)}. */
        public float offsetY = 0;

        public TableRow(MarkdownRenderer parent) {
            this.parent = parent;
        }

    }

    public static class TableCell {

        public final List<MarkdownTextFragment> fragments = new ArrayList<>();
        public TableCellAlignment alignment = TableCellAlignment.LEFT;
        /** Width of all fragments of the cell together, with the paddings of the inline code in it. Measured by {@link TableContext#updateMetrics(MarkdownRenderer)}. */
        public float textWidth = 0;

        public enum TableCellAlignment {
            LEFT,
            CENTER,
            RIGHT
        }

    }

}
