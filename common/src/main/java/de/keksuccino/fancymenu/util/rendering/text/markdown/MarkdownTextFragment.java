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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MarkdownTextFragment implements Renderable, GuiEventListener {

    protected static final int QUOTE_LINE_WIDTH = 2;
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
    public float unscaledTextHeight;
    public boolean startOfRenderLine = false;
    public boolean naturalLineBreakAfter;
    public boolean autoLineBreakAfter;
    public boolean endOfWord;
    public ResourceSupplier<ITexture> imageSupplier = null;
    /** The alternative text of the image of this fragment, without the backslashes of its escaped characters. NULL if the fragment is no image. */
    @Nullable
    public String imageAltText = null;
    public boolean separationLine;
    public DrawableColor textColor = null;
    public boolean bold;
    public boolean italic;
    public boolean strikethrough;
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
        this.unscaledTextHeight = this.parent.getUnscaledTextHeight();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.setHovered(this.isMouseOver(mouseX, mouseY));

        if (this.isLinkLike() && this.hovered) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }

        // Handle table rendering
        if (this.isTable()) {
            renderTable(graphics, mouseX, mouseY, partial);
            return;
        }

        if (this.imageSupplier != null) {
            this.imageSupplier.forRenderable((iTexture, location) -> {
                RenderSystem.enableBlend();
                float width = this.getRenderWidth();
                float height = this.getRenderHeight();
                graphics.setColor(1.0F, 1.0F, 1.0F, this.parent.textOpacity);
                RenderingUtils.blitF(graphics, location, this.x, this.y, 0.0F, 0.0F, width, height, width, height);
                RenderingUtils.resetShaderColor(graphics);
            });
        } else if (this.separationLine) {

            RenderSystem.enableBlend();
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.scaling.padding(), this.y, this.parent.x + this.parent.getRealWidth() - this.parent.scaling.padding(), this.y + this.getRenderHeight(), this.parent.getRenderColor(this.parent.style.separatorColor));
            RenderingUtils.resetShaderColor(graphics);

        } else {

            float scale = this.getScale();
            if (scale <= 0.0F) return;

            this.renderInlineCodeBackground(graphics);

            // The fragments that only end a line and the ones of blank lines have nothing to draw
            if (!this.text.isEmpty()) {
                int color = this.parent.getRenderColor(this.parent.style.textColor);
                RenderSystem.enableBlend();
                graphics.pose().pushPose();
                try {
                    // Vanilla text rendering only accepts integer draw coordinates. Translate to the exact text origin and draw at local zero so fractional scaling cannot truncate the absolute position into the parent's scissor area.
                    graphics.pose().translate(this.getTextX(), this.getTextY(), 0.0F);
                    graphics.pose().scale(scale, scale, scale);
                    if (this.hasFixedWidthCells()) {
                        this.getFixedWidthText().render(graphics, this.parent, color);
                    } else {
                        this.parent.renderText(graphics, this.getRenderComponent(), 0.0F, 0.0F, color, this.parent.hasTextShadow() && (this.codeBlockContext == null));
                    }
                } finally {
                    graphics.pose().popPose();
                }
            }
            RenderingUtils.resetShaderColor(graphics);

            if (this.isLinkLike()) MarkdownLinkPainter.renderUnderline(graphics, this);

            this.renderQuoteLine(graphics);

        }

    }

    protected void renderTable(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.tableContext == null) return;

        float[] columnWidths = this.tableContext.columnWidths;
        float cellPadding = this.parent.scaling.sized(this.parent.style.tableCellPadding.get());

        // Use the fragment's x position which already includes alignment offset
        float tableX = this.x;
        float tableY = this.y;
        float currentY = tableY;

        // Use the actual table width for rendering (not the full area width)
        float actualTableWidth = this.tableContext.totalWidth;
        
        RenderSystem.enableBlend();
        
        // Render rows
        for (int rowIndex = 0; rowIndex < this.tableContext.rows.size(); rowIndex++) {
            TableRow row = this.tableContext.rows.get(rowIndex);
            float rowHeight = row.height;
            float currentX = tableX;

            // Draw row background
            RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.getRenderColor(this.tableContext.getRowBackgroundColor(this.parent, rowIndex)));

            // Render cells
            for (int cellIndex = 0; cellIndex < row.cells.size(); cellIndex++) {
                if (cellIndex >= columnWidths.length) break;

                TableCell cell = row.cells.get(cellIndex);
                float cellWidth = columnWidths[cellIndex];

                // Calculate text position based on alignment
                float textX = currentX + cellPadding;
                float textY = currentY + cellPadding;

                float alignmentOffset = 0;
                if (cell.alignment == TableCell.TableCellAlignment.CENTER) {
                    alignmentOffset = (cellWidth - cellPadding * 2 - cell.textWidth) / 2;
                } else if (cell.alignment == TableCell.TableCellAlignment.RIGHT) {
                    alignmentOffset = cellWidth - cellPadding * 2 - cell.textWidth;
                }

                // First pass: position fragments and render the backgrounds of inline code
                float fragmentX = textX + alignmentOffset;
                MarkdownTextFragment codeStart = null;

                for (int fragIndex = 0; fragIndex < cell.fragments.size(); fragIndex++) {
                    MarkdownTextFragment fragment = cell.fragments.get(fragIndex);

                    fragment.x = fragmentX;
                    fragment.y = textY;

                    if ((fragment.codeBlockContext != null) && fragment.codeBlockContext.singleLine) {
                        if ((codeStart == null) || (codeStart.codeBlockContext != fragment.codeBlockContext)) codeStart = fragment;
                        boolean isLastFragment = (fragIndex == cell.fragments.size() - 1) || (cell.fragments.get(fragIndex + 1).codeBlockContext != fragment.codeBlockContext);
                        if (isLastFragment) {
                            MarkdownCodePainter.renderInline(graphics, codeStart, fragment);
                            codeStart = null;
                        }
                    } else {
                        codeStart = null;
                    }

                    // The paddings of inline code are part of the width of its fragments, like in a line of text
                    fragmentX += fragment.getRenderWidth();
                }

                // Second pass: render the fragments
                for (MarkdownTextFragment fragment : cell.fragments) {
                    fragment.render(graphics, mouseX, mouseY, partial); // Pass actual mouse coords for hover/click detection

                    // Check if any hyperlink in the table is hovered
                    if (fragment.isLinkLike() && fragment.hovered) {
                        CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
                    }
                }

                currentX += cellWidth;
            }

            currentY += rowHeight;
        }

        // Draw table borders
        int lineColor = this.parent.getRenderColor(this.parent.style.tableLineColor);
        float lineThickness = this.parent.scaling.sized(this.parent.style.tableLineThickness.get());

        // Nothing else gets drawn between the lines of the table, so all of them get drawn as one batch

        // Horizontal lines
        float y = tableY;
        for (int i = 0; i <= this.tableContext.rows.size(); i++) {
            RenderingUtils.batchFillF(graphics, tableX, y, tableX + actualTableWidth, y + lineThickness, lineColor);
            if (i < this.tableContext.rows.size()) {
                y += this.tableContext.rows.get(i).height;
            }
        }

        // Draw thicker line under header
        if (this.tableContext.hasHeader && this.parent.style.tableShowHeader.get() && !this.tableContext.rows.isEmpty()) {
            float headerY = tableY + this.tableContext.rows.get(0).height;
            RenderingUtils.batchFillF(graphics, tableX, headerY, tableX + actualTableWidth, headerY + lineThickness * 2, lineColor);
        }

        // Vertical lines
        float x = tableX;
        for (int i = 0; i <= columnWidths.length; i++) {
            RenderingUtils.batchFillF(graphics, x, tableY, x + lineThickness, currentY + lineThickness, lineColor);
            if (i < columnWidths.length) {
                x += columnWidths[i];
            }
        }

        graphics.flush();
        RenderingUtils.resetShaderColor(graphics);
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

    protected void renderQuoteLine(GuiGraphics graphics) {
        if ((this.quoteContext != null) && this.drawsQuoteLine()) {
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).getRenderLineY() - 2;
            float yEnd = this.getQuoteLineBottom();
            RenderSystem.enableBlend();
            if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) {
                float lineX = this.getLeftQuoteLineX();
                RenderingUtils.fillF(graphics, lineX, yStart, lineX + QUOTE_LINE_WIDTH, yEnd, this.parent.getRenderColor(this.parent.style.quoteBarColor));
            } else if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                float lineX = this.getRightQuoteLineX();
                RenderingUtils.fillF(graphics, lineX, yStart, lineX + QUOTE_LINE_WIDTH, yEnd, this.parent.getRenderColor(this.parent.style.quoteBarColor));
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /**
     * Returns where the line next to the quote of this fragment ends, which is right below the last render line of the quote.<br>
     * The whole render line counts and not only the last fragment of the quote, because the first fragment of a line can make it
     * taller than the others, like the one that starts a list item.
     */
    protected float getQuoteLineBottom() {
        MarkdownTextFragment end = Objects.requireNonNull(this.quoteContext.getQuoteEnd());
        float lastLineHeight = (end.parentLine != null) ? end.parentLine.getLineHeight() : end.getRenderHeight();
        return end.getRenderLineY() + lastLineHeight + 1;
    }

    /**
     * Returns the X position of the line next to a left-aligned quote.<br>
     * The line stands at the start of the text area, in the space the quote indent keeps free in front of the text.
     * It must not reach into the border in front of the text area, because containers like scroll areas clip their content there.
     */
    protected float getLeftQuoteLineX() {
        return this.parent.x + this.parent.scaling.padding();
    }

    /**
     * Returns the X position of the line next to a right-aligned quote, which ends where the text area ends.
     */
    protected float getRightQuoteLineX() {
        return this.parent.x + this.parent.getRealWidth() - this.parent.scaling.padding() - QUOTE_LINE_WIDTH;
    }

    /**
     * The line next to a quote gets drawn once per frame: by the last fragment of the quote,
     * or, when that one is outside the visible range, by the last fragment of the last line of the quote that is inside of it.
     */
    protected boolean drawsQuoteLine() {
        if (this.quoteContext.getQuoteEnd() == this) return true;
        if ((this.parentLine == null) || (this.parentLine.fragments.get(this.parentLine.fragments.size()-1) != this)) return false;
        MarkdownTextLine nextLine = this.parentLine.getNextLine();
        if ((nextLine == null) || nextLine.isVisible()) return false;
        return nextLine.fragments.get(0).quoteContext == this.quoteContext;
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
        if (this.fixedWidthText == null) this.fixedWidthText = MarkdownFixedWidthText.of(this.parent, component.getString(), component.getStyle());
        return this.fixedWidthText;
    }

    private int getRenderComponentState() {
        // Only link-like fragments get built differently when they are hovered or at the end of a line
        if (!this.isLinkLike()) return 0;
        int state = (this.naturalLineBreakAfter || this.autoLineBreakAfter) ? COMPONENT_STATE_LINE_END : 0;
        if ((this.hyperlink != null) && this.hyperlink.isHovered()) state |= COMPONENT_STATE_HYPERLINK_HOVERED;
        if ((this.clickEvent != null) && this.clickEvent.isHovered()) state |= COMPONENT_STATE_CLICK_EVENT_HOVERED;
        if ((this.hoverEvent != null) && this.hoverEvent.isHovered()) state |= COMPONENT_STATE_HOVER_EVENT_HOVERED;
        return state;
    }

    @NotNull
    protected Component buildRenderComponent(boolean forWidthCalculation) {
        int trailingSpaceMode = this.getTrailingSpaceMode();
        return createRenderComponent(this.buildRenderStyle(forWidthCalculation), this.buildRenderText(trailingSpaceMode), trailingSpaceMode == TRAILING_SPACE_SEPARATE);
    }

    @NotNull
    protected static Component createRenderComponent(@NotNull Style style, @NotNull String renderText, boolean separateTrailingSpace) {
        MutableComponent comp = Component.literal(renderText).setStyle(style);
        if (separateTrailingSpace) {
            comp.append(Component.literal(" ").setStyle(Style.EMPTY.withUnderlined(false)));
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
        if (this.strikethrough && !forWidthCalculation) {
            style = style.withStrikethrough(true);
        }
        if (this.quoteContext != null) {
            style = style.withColor(this.parent.style.quoteTextColor.argb());
            if (this.parent.style.quoteItalic.get()) {
                style = style.withItalic(true);
            }
        }
        if (this.headlineType != HeadlineType.NONE) {
            MarkdownStyle.Headline headline = this.parent.style.headline(this.headlineType.level);
            if (headline.bold.get()) style = style.withBold(true);
            if (headline.color.isSet()) style = style.withColor(headline.color.argb());
        } else if (this.bold && this.parent.style.boldTextColor.isSet()) {
            style = style.withColor(this.parent.style.boldTextColor.argb());
        }
        if (this.textColor != null) {
            style = style.withColor(this.textColor.getColorInt());
        }
        if (this.codeBlockContext != null) {
            // Code shows its text as it is, in the color the style gives it
            MarkdownStyleOption.ColorOption codeColor = this.codeBlockContext.singleLine ? this.parent.style.inlineCodeTextColor : this.parent.style.codeBlockTextColor;
            style = codeColor.isSet() ? Style.EMPTY.withColor(codeColor.argb()) : Style.EMPTY;
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
     * The space at the end of link-like text would look ugly when underlined, so it gets removed at the end of a line
     * and drawn without the link's style at the end of the link.
     */
    protected int getTrailingSpaceMode() {
        if (!this.isLinkLike() || !this.text.endsWith(" ")) return TRAILING_SPACE_KEEP;
        if (this.naturalLineBreakAfter || this.autoLineBreakAfter) return TRAILING_SPACE_REMOVE;
        return this.isLastLinkLikeFragment() ? TRAILING_SPACE_SEPARATE : TRAILING_SPACE_KEEP;
    }

    @NotNull
    protected String buildRenderText(int trailingSpaceMode) {
        String t = this.text;
        if (trailingSpaceMode != TRAILING_SPACE_KEEP) {
            t = t.substring(0, t.length()-1);
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_UPPER) {
            t = t.toUpperCase();
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_LOWER) {
            t = t.toLowerCase();
        }
        return t;
    }

    protected boolean isLinkLike() {
        return (this.hyperlink != null) || (this.clickEvent != null) || (this.hoverEvent != null);
    }

    protected boolean isLastLinkLikeFragment() {
        if (this.hyperlink != null) {
            return ListUtils.getLast(this.hyperlink.hyperlinkFragments) == this;
        }
        if (this.clickEvent != null) {
            return ListUtils.getLast(this.clickEvent.eventFragments) == this;
        }
        if (this.hoverEvent != null) {
            return ListUtils.getLast(this.hoverEvent.eventFragments) == this;
        }
        return false;
    }

    /**
     * Measures the text of the fragment again.<br>
     * Has to get called after anything changed that the fragment's text or style gets built from, like its text or formatting.
     */
    protected void updateWidth() {
        this.renderComponent = null;
        this.fixedWidthText = null;
        int trailingSpaceMode = this.getTrailingSpaceMode();
        String renderText = this.buildRenderText(trailingSpaceMode);
        if (this.hasFixedWidthCells()) {
            this.unscaledTextWidth = MarkdownFixedWidthText.getWidth(this.parent, renderText);
            return;
        }
        this.unscaledTextWidth = this.parent.textWidthCache.getWidth(this.buildRenderStyle(true), renderText, trailingSpaceMode == TRAILING_SPACE_SEPARATE);
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
     * Fragments of one render line share their Y position, so this is how a fragment checks if it sits in the first or last line of its code block.
     * Fragments outside of render lines, like the ones in table cells, always are on their own line.
     */
    protected boolean isInRenderLineOf(@Nullable MarkdownTextFragment fragment) {
        return (fragment != null) && (fragment.parentLine == this.parentLine);
    }

    public float getTextRenderX() {
        return MarkdownTextRenderMath.toLocalCoordinate(this.getTextX(), this.getScale());
    }

    /**
     * Returns the space the fragment keeps free in front of its text. The first fragment of a render line holds the indents and paddings
     * of the line, and the first fragment of inline code holds the padding of its background. All of it scales with the text of the fragment.
     */
    protected float getLeadingSpace() {
        MarkdownStyle style = this.parent.style;
        float space = 0.0F;
        if (this.startOfRenderLine) {
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
                space += style.quoteIndent.get();
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
        MarkdownStyle style = this.parent.style;
        float space = 0.0F;
        if (endOfRenderLine) {
            if ((this.quoteContext != null) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT)) {
                space += style.quoteIndent.get();
            }
            if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine) {
                space += style.codeBlockPaddingHorizontal.get();
            }
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() == this)) {
            space += style.inlineCodePaddingRight.get();
        }
        return space * this.getScale();
    }

    public float getTextRenderY() {
        return MarkdownTextRenderMath.toLocalCoordinate(this.getTextY(), this.getScale());
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
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            if (t.getWidth() <= (this.parent.getRealWidth() - this.parent.scaling.padding() - this.parent.scaling.padding())) {
                return t.getWidth();
            }
            return this.parent.getRealWidth() - this.parent.scaling.padding() - this.parent.scaling.padding();
        }

        return this.getLeadingSpace() + this.getTextRenderWidth() + this.getTrailingSpace(endOfRenderLine);

    }

    /**
     * Returns the height of what the fragment shows. The paddings above and below it belong to its line.
     */
    public float getRenderHeight() {

        // Handle table height
        if (this.isTable()) {
            float totalHeight = this.tableContext.totalRowHeight;
            // Add line thickness for borders
            totalHeight += this.parent.scaling.sized(this.parent.style.tableLineThickness.get()) * (this.tableContext.rows.size() + 1);
            return totalHeight;
        }

        if (this.imageSupplier != null) {
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            return t.getAspectRatio().getAspectRatioHeight((int)this.getRenderWidth());
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
            this.parent.fireTextClickEvent(this.clickEvent.identifier);
            return true;
        }

        // Handle regular hyperlink clicks
        if ((this.hyperlink != null) && this.hovered) {
            if (this.parent.isHyperlinkAllowed(this.hyperlink.link)) WebUtils.openWebLink(this.hyperlink.link);
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

        @Nullable
        public MarkdownTextFragment getQuoteEnd() {
            if (!quoteFragments.isEmpty()) return quoteFragments.get(quoteFragments.size()-1);
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

        @Nullable
        public MarkdownTextFragment getBlockEnd() {
            if (!codeBlockFragments.isEmpty()) return codeBlockFragments.get(codeBlockFragments.size()-1);
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
        /** Height of all rows together, without the table lines and margins. */
        public float totalRowHeight = 0;

        /**
         * Measures the columns, rows and cells of the table.<br>
         * The sizes only depend on things that need a new layout when they change, so the layout measures every table once,
         * and rendering, hit tests and size queries all use the measured sizes.
         */
        public void updateMetrics(@NotNull MarkdownRenderer renderer) {
            int columnCount = this.rows.isEmpty() ? 0 : this.rows.get(0).cells.size();
            if (this.columnWidths.length != columnCount) {
                this.columnWidths = new float[columnCount];
            } else {
                Arrays.fill(this.columnWidths, 0.0F);
            }
            this.totalWidth = 0;
            this.totalRowHeight = 0;
            for (TableRow row : this.rows) {
                for (int i = 0; i < row.cells.size(); i++) {
                    TableCell cell = row.cells.get(i);
                    float textWidth = 0;
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        textWidth += fragment.getRenderWidth();
                    }
                    cell.textWidth = textWidth;
                    if (i < columnCount) {
                        float cellWidth = textWidth + (renderer.scaling.sized(renderer.style.tableCellPadding.get()) * 2);
                        if (cellWidth > this.columnWidths[i]) {
                            this.columnWidths[i] = cellWidth;
                        }
                    }
                }
                row.height = this.getRowHeight(row);
                this.totalRowHeight += row.height;
            }
            for (float width : this.columnWidths) {
                this.totalWidth += width;
            }
        }

        /**
         * Returns the background color of the row at the given index.<br>
         * The header row only looks like a header while the renderer shows table headers. Otherwise it is a row like every other
         * one, so the colors alternate from the first row of the table and not from the first row behind the header.
         */
        @NotNull
        public MarkdownStyleOption.ColorOption getRowBackgroundColor(@NotNull MarkdownRenderer renderer, int rowIndex) {
            if (this.isHeaderRow(renderer, rowIndex)) return renderer.style.tableHeaderBackgroundColor;
            int alternatingIndex = this.isHeaderRow(renderer, 0) ? (rowIndex - 1) : rowIndex;
            return (renderer.style.tableAlternateRows.get() && ((alternatingIndex % 2) == 1)) ? renderer.style.tableAlternateRowBackgroundColor : renderer.style.tableRowBackgroundColor;
        }

        /**
         * Returns if the row at the given index gets shown as the header of the table.
         */
        public boolean isHeaderRow(@NotNull MarkdownRenderer renderer, int rowIndex) {
            return renderer.style.tableShowHeader.get() && (rowIndex < this.rows.size()) && this.rows.get(rowIndex).isHeader;
        }

        public float getRowHeight(TableRow row) {
            float maxHeight = 0;
            for (TableCell cell : row.cells) {
                float cellHeight = 0;
                for (MarkdownTextFragment fragment : cell.fragments) {
                    float fragmentHeight = fragment.getTextRenderHeight();
                    if (fragmentHeight > cellHeight) {
                        cellHeight = fragmentHeight;
                    }
                }
                if (cellHeight > maxHeight) {
                    maxHeight = cellHeight;
                }
            }
            return maxHeight + (row.parent.scaling.sized(row.parent.style.tableCellPadding.get()) * 2);
        }

    }

    public static class TableRow {

        public final List<TableCell> cells = new ArrayList<>();
        public boolean isHeader = false;
        public MarkdownRenderer parent;
        /** Measured by {@link TableContext#updateMetrics(MarkdownRenderer)}. */
        public float height = 0;

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
