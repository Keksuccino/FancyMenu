package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
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

    protected static final int BULLET_LIST_SPACE_AFTER_INDENT = 5;
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
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border, this.y + this.getRenderHeight(), this.parent.separationLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);

        } else {

            float scale = this.getScale();
            if (scale <= 0.0F) return;

            this.renderCodeBlock(graphics);

            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            try {
                // Vanilla text rendering only accepts integer draw coordinates. Translate to the exact text origin and draw at local zero so fractional scaling cannot truncate the absolute position into the parent's scissor area.
                graphics.pose().translate(this.getTextX(), this.getTextY(), 0.0F);
                graphics.pose().scale(scale, scale, scale);
                this.parent.renderText(graphics, this.getRenderComponent(), 0.0F, 0.0F, this.parent.textBaseColor.getColorIntWithAlpha(this.parent.textOpacity), this.parent.textShadow && (this.codeBlockContext == null));
            } finally {
                graphics.pose().popPose();
            }
            RenderingUtils.resetShaderColor(graphics);

            this.renderQuoteLine(graphics);

            this.renderBulletListDot(graphics);

            this.renderHeadlineUnderline(graphics);

        }

    }

    protected void renderTable(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.tableContext == null) return;

        float[] columnWidths = this.tableContext.columnWidths;
        float cellPadding = this.parent.tableCellPadding;

        // Use the fragment's x position which already includes alignment offset
        float tableX = this.x;
        float tableY = this.y + this.parent.tableMargin; // Add margin before table
        float currentY = tableY;

        // Use the actual table width for rendering (not the full area width)
        float actualTableWidth = this.tableContext.totalWidth;
        
        RenderSystem.enableBlend();
        
        // Render rows
        int dataRowIndex = 0; // Track data rows separately for alternating colors
        for (int rowIndex = 0; rowIndex < this.tableContext.rows.size(); rowIndex++) {
            TableRow row = this.tableContext.rows.get(rowIndex);
            float rowHeight = row.height;
            float currentX = tableX;

            // Draw row background
            if (row.isHeader && this.parent.tableShowHeader) {
                // Header background
                RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableHeaderBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (!row.isHeader) {
                // Regular row background - alternate between two colors
                if (this.parent.tableAlternateRowColors && dataRowIndex % 2 == 1) {
                    // Alternate rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableAlternateRowColor.getColorIntWithAlpha(this.parent.textOpacity));
                } else {
                    // Base rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, currentY + rowHeight, this.parent.tableRowBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
                }
                dataRowIndex++;
            }

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

                // First pass: position fragments and render code block backgrounds
                float fragmentX = textX + alignmentOffset;
                float codeBlockStartX = -1;
                MarkdownTextFragment.CodeBlockContext currentCodeBlock = null;

                for (int fragIndex = 0; fragIndex < cell.fragments.size(); fragIndex++) {
                    MarkdownTextFragment fragment = cell.fragments.get(fragIndex);

                    fragment.x = fragmentX;
                    fragment.y = textY;

                    // Track and render code block backgrounds
                    if (fragment.codeBlockContext != null && fragment.codeBlockContext.singleLine) {
                        if (currentCodeBlock == null || currentCodeBlock != fragment.codeBlockContext) {
                            // Start of a new code block
                            currentCodeBlock = fragment.codeBlockContext;
                            codeBlockStartX = fragment.x - 1;
                        }

                        // Check if this is the last fragment of the code block
                        boolean isLastFragment = (fragIndex == cell.fragments.size() - 1) || (fragIndex + 1 < cell.fragments.size() && cell.fragments.get(fragIndex + 1).codeBlockContext != currentCodeBlock);

                        if (isLastFragment) {
                            // Render the code block background
                            float endX = fragment.x + fragment.getRenderWidth() + 1;
                            if (fragment.text.endsWith(" ")) {
                                endX -= (this.parent.getUnscaledSpaceWidth() * fragment.getScale());
                            }
                            renderCodeBlockBackground(graphics, codeBlockStartX, fragment.y - 2, endX, fragment.y + fragment.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
                            currentCodeBlock = null;
                        }
                    } else {
                        currentCodeBlock = null;
                    }

                    fragmentX += fragment.getTextRenderWidth();
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
        int lineColor = this.parent.tableLineColor.getColorIntWithAlpha(this.parent.textOpacity);
        float lineThickness = this.parent.tableLineThickness;

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
        if (this.tableContext.hasHeader && this.parent.tableShowHeader && !this.tableContext.rows.isEmpty()) {
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

    protected void renderCodeBlock(GuiGraphics graphics) {
        if (this.codeBlockContext == null) return;

        // Check if this fragment is inside a table cell (has tableContext but is not the table itself)
        boolean isInTableCell = this.tableContext != null && !this.isTable();

        // Skip code block background rendering for fragments inside table cells
        // (handled in renderTable method)
        if (isInTableCell) {
            return;
        }

        // Normal code block rendering (outside tables)
        if (this.parentLine != null) {
            MarkdownTextFragment start = this.codeBlockContext.getBlockStart();
            MarkdownTextFragment end = this.codeBlockContext.getBlockEnd();
            if (this.codeBlockContext.singleLine) {
                MarkdownTextLine.SingleLineCodeBlockPart part = this.parentLine.singleLineCodeBlockStartEndPairs.get(this.codeBlockContext);
                if (part == null) return;
                start = part.start;
                end = part.end;
            }
            if ((start == null) || (end == null)) return;
            if (this.codeBlockContext.singleLine) {
                if (start != this) return;
                float xEnd = end.x + end.getRenderWidth();
                if (end.text.endsWith(" ")) {
                    xEnd -= (this.parent.getUnscaledSpaceWidth() * this.getScale());
                }
                renderCodeBlockBackground(graphics, this.x, this.y - 2, xEnd, this.y + this.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (this.drawsCodeBlockBackground()) {
                renderCodeBlockBackground(graphics, this.parent.x + this.parent.border, start.getRenderLineY(), this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, end.getRenderLineY() + end.getRenderHeight() - 1, this.parent.codeBlockMultiLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
        }
    }

    /**
     * The background of a multi-line code block gets drawn once per frame, before the text of the block: by the first fragment of the block,
     * or, when that one is outside the visible range, by the first fragment of the first line of the block that is inside of it.
     */
    protected boolean drawsCodeBlockBackground() {
        if (this.codeBlockContext.getBlockStart() == this) return true;
        if ((this.parentLine == null) || (this.parentLine.fragments.get(0) != this)) return false;
        MarkdownTextLine previousLine = this.parentLine.getPreviousLine();
        if ((previousLine == null) || previousLine.isVisible()) return false;
        return previousLine.fragments.get(previousLine.fragments.size()-1).codeBlockContext == this.codeBlockContext;
    }

    /**
     * Returns the Y position of the render line of the fragment, for where the renderer is right now.<br>
     * A fragment only gets its own position when its line renders, so this is the position to use for fragments that can be outside the visible range.
     */
    protected float getRenderLineY() {
        if (this.parentLine == null) return this.y;
        return this.parent.y + this.parentLine.offsetY;
    }

    protected void renderCodeBlockBackground(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        RenderSystem.enableBlend();
        // Nothing else gets drawn between the three rectangles of the background, so they get drawn as one batch
        RenderingUtils.batchFillF(graphics, minX+1, minY, maxX-1, minY+1, color);
        RenderingUtils.batchFillF(graphics, minX, minY+1, maxX, maxY-1, color);
        RenderingUtils.batchFillF(graphics, minX+1, maxY-1, maxX-1, maxY, color);
        graphics.flush();
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderHeadlineUnderline(GuiGraphics graphics) {
        if (this.startOfRenderLine && ((this.headlineType == HeadlineType.BIGGER) || (this.headlineType == HeadlineType.BIGGEST))) {
            RenderSystem.enableBlend();
            float scale = (this.parent.parentRenderScale != null) ? this.parent.parentRenderScale : (float)Minecraft.getInstance().getWindow().getGuiScale();
            float lineThickness = (scale > 1) ? 0.5f : 1f;
            float lineY = this.y + this.getTextRenderHeight() + 1;
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, lineY, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, lineY + lineThickness, this.parent.headlineUnderlineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    protected void renderQuoteLine(GuiGraphics graphics) {
        if ((this.quoteContext != null) && this.drawsQuoteLine()) {
            MarkdownTextFragment end = Objects.requireNonNull(this.quoteContext.getQuoteEnd());
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).getRenderLineY() - 2;
            float yEnd = end.getRenderLineY() + end.getRenderHeight() + 1;
            RenderSystem.enableBlend();
            if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) {
                float lineX = this.getLeftQuoteLineX();
                RenderingUtils.fillF(graphics, lineX, yStart, lineX + QUOTE_LINE_WIDTH, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                float lineX = this.getRightQuoteLineX();
                RenderingUtils.fillF(graphics, lineX, yStart, lineX + QUOTE_LINE_WIDTH, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    /**
     * Returns the X position of the line next to a left-aligned quote.<br>
     * The line stands at the start of the text area, in the space the quote indent keeps free in front of the text.
     * It must not reach into the border in front of the text area, because containers like scroll areas clip their content there.
     */
    protected float getLeftQuoteLineX() {
        return this.parent.x + this.parent.border;
    }

    /**
     * Returns the X position of the line next to a right-aligned quote, which ends where the text area ends.
     */
    protected float getRightQuoteLineX() {
        return this.parent.x + this.parent.getRealWidth() - this.parent.border - QUOTE_LINE_WIDTH;
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

    protected void renderBulletListDot(GuiGraphics graphics) {
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            RenderSystem.enableBlend();
            final float scale = this.getScale();

            // Calculate dimensions using scale
            final float bulletSize = 3 * scale;

            // Shift bullet dot one level to the right:
            final float bulletX = this.x - (5 * scale) + (this.parent.bulletListIndent * (this.bulletListLevel) * scale);

            // Vertical centering using text baseline
            final float textBaselineY = this.getTextY() + (this.parent.getUnscaledTextHeight() * scale * 0.5f) - (bulletSize * 0.5f);

            RenderingUtils.fillF(graphics, bulletX, textBaselineY, bulletX + bulletSize, textBaselineY + bulletSize, this.parent.bulletListDotColor.getColorIntWithAlpha(this.parent.textOpacity));
        }
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
            this.renderComponentState = state;
            this.renderComponentStyleVersion = this.parent.fragmentStyleVersion;
            this.renderComponentCustomColor = customColor;
        }
        return this.renderComponent;
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
            style = style.withColor(this.parent.quoteColor.getColorInt());
            if (this.parent.quoteItalic) {
                style = style.withItalic(true);
            }
        }
        if (this.textColor != null) {
            style = style.withColor(this.textColor.getColorInt());
        }
        if (this.codeBlockContext != null) {
            style = Style.EMPTY;
        }
        if (this.plainText) {
            style = Style.EMPTY;
        }
        style = this.minecraftFormatting.applyTo(style);
        if (this.hyperlink != null) {
            style = style.withColor(this.parent.hyperlinkColor.getColorInt());
            if (this.hyperlink.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        if (this.clickEvent != null) {
            style = style.withColor(this.parent.getTextClickEventColor().getColorInt());
            if (this.clickEvent.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        if (this.hoverEvent != null) {
            style = style.withColor(this.parent.getTextHoverEventColor().getColorInt());
            if (this.hoverEvent.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        return style;
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
        int trailingSpaceMode = this.getTrailingSpaceMode();
        this.unscaledTextWidth = this.parent.textWidthCache.getWidth(this.buildRenderStyle(true), this.buildRenderText(trailingSpaceMode), trailingSpaceMode == TRAILING_SPACE_SEPARATE);
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

    protected float getTextRenderOffsetX() {
        float offsetX = 0.0F;

        if ((this.quoteContext != null) && this.startOfRenderLine && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
            offsetX += this.parent.quoteIndent;
        }

        if (this.bulletListLevel > 0 && this.startOfRenderLine) {
            // Now apply the full bullet indent for the first fragment.
            float bulletIndent = (this.parent.bulletListIndent * this.bulletListLevel) + BULLET_LIST_SPACE_AFTER_INDENT;
            offsetX += bulletIndent;
        }

        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            offsetX += 10;
        }

        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            offsetX += 1;
        }

        return offsetX;
    }

    public float getTextRenderY() {
        return MarkdownTextRenderMath.toLocalCoordinate(this.getTextY(), this.getScale());
    }

    protected float getTextRenderOffsetY() {
        float offsetY = 0.0F;
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.isInRenderLineOf(this.codeBlockContext.getBlockStart())) {
            offsetY += 10;
        }
        if ((this.bulletListLevel > 0) && (this.parentLine != null) && this.parentLine.bulletListItemStartLine) {
            offsetY += this.parent.bulletListSpacing;
        }
        return offsetY;
    }

    public float getRenderWidth() {

        // Handle table width - return actual table width for proper alignment
        if (this.isTable()) {
            return this.tableContext.totalWidth;
        }

        if (this.imageSupplier != null) {
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            if (t.getWidth() <= (this.parent.getRealWidth() - this.parent.border - this.parent.border)) {
                return t.getWidth();
            }
            return this.parent.getRealWidth() - this.parent.border - this.parent.border;
        }

        float f = this.getTextRenderWidth();
        if ((this.quoteContext != null) && this.startOfRenderLine && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
            f += this.parent.quoteIndent * this.getScale();
        }
        if ((this.quoteContext != null) && (this.naturalLineBreakAfter || this.autoLineBreakAfter) && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT)) {
            f += this.parent.quoteIndent;
        }
        if (this.bulletListLevel > 0 && this.startOfRenderLine) {
            float bulletSpace = (this.parent.bulletListIndent * this.bulletListLevel * this.getScale()) + (BULLET_LIST_SPACE_AFTER_INDENT * this.getScale());
            f += bulletSpace;
        }
        // The paddings of code blocks scale with the text, like the text offset they belong to (see getTextX()). Otherwise the text would grow out of them.
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            f += 10 * this.getScale();
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            f += this.getScale();
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.autoLineBreakAfter || this.naturalLineBreakAfter)) {
            f += 10 * this.getScale();
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() == this)) {
            f += this.getScale();
        }
        return f;

    }

    public float getRenderHeight() {

        // Handle table height
        if (this.isTable()) {
            float totalHeight = this.tableContext.totalRowHeight;
            // Add line thickness for borders
            totalHeight += this.parent.tableLineThickness * (this.tableContext.rows.size() + 1);
            // Add margin above and below tables
            totalHeight += this.parent.tableMargin * 2;
            return totalHeight;
        }

        if (this.imageSupplier != null) {
            ITexture t = this.imageSupplier.get();
            if (t == null) return 10;
            return t.getAspectRatio().getAspectRatioHeight((int)this.getRenderWidth());
        }

        float f = this.getTextRenderHeight();
        if ((this.headlineType == HeadlineType.BIGGER) || (this.headlineType == HeadlineType.BIGGEST)) {
            f += 8;
        }
        if (this.headlineType == HeadlineType.BIG) {
            f += 6;
        }
        // The paddings of code blocks and the spacing of bullet items scale with the text, like the text offset they belong to (see getTextY()). Otherwise the text would grow into the next line.
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.isInRenderLineOf(this.codeBlockContext.getBlockStart())) {
            f += 10 * this.getScale();
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.isInRenderLineOf(this.codeBlockContext.getBlockEnd())) {
            f += 10 * this.getScale();
        }
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            f += this.parent.bulletListSpacing * this.getScale();
        }
        return f;
    }

    public float getTextRenderWidth() {
        return this.unscaledTextWidth * this.getScale();
    }

    public float getTextRenderHeight() {
        return this.unscaledTextHeight * this.getScale();
    }

    public float getTextX() {
        return MarkdownTextRenderMath.resolveOrigin(this.x, this.getTextRenderOffsetX(), this.getScale());
    }

    public float getTextY() {
        return MarkdownTextRenderMath.resolveOrigin(this.y, this.getTextRenderOffsetY(), this.getScale());
    }

    public float getTextWidth() {
        return this.getTextRenderWidth();
    }

    public float getTextHeight() {
        return this.getTextRenderHeight();
    }

    public float getScale() {
        float f = 1.0f;
        if (this.headlineType == HeadlineType.BIG) f = 1.2f;
        if (this.headlineType == HeadlineType.BIGGER) f = 1.6f;
        if (this.headlineType == HeadlineType.BIGGEST) f = 2.0f;
        return MarkdownTextRenderMath.sanitizeScale(f * this.parent.textBaseScale);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isTable()) {
            // For tables, use the actual table rendering area
            float tableX = this.x;
            float tableY = this.y + this.parent.tableMargin;
            float tableWidth = this.tableContext.totalWidth;
            float tableHeight = this.getRenderHeight() - (this.parent.tableMargin * 2); // Subtract the margin
            return RenderingUtils.isXYInArea(mouseX, mouseY, tableX, tableY, tableWidth, tableHeight);
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
        NONE,
        BIG, // ###
        BIGGER, // ##
        BIGGEST // #
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
                        textWidth += fragment.getTextRenderWidth();
                    }
                    cell.textWidth = textWidth;
                    if (i < columnCount) {
                        float cellWidth = textWidth + (renderer.tableCellPadding * 2);
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
            return maxHeight + (row.parent.tableCellPadding * 2);
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
        /** Width of all fragments of the cell together. Measured by {@link TableContext#updateMetrics(MarkdownRenderer)}. */
        public float textWidth = 0;

        public enum TableCellAlignment {
            LEFT,
            CENTER,
            RIGHT
        }

    }

}
