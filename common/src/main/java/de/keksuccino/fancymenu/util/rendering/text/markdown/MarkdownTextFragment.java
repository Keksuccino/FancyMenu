package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownTextFragment implements Renderable, GuiEventListener {

    protected static final int BULLET_LIST_SPACE_AFTER_INDENT = 5;
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
    public boolean separationLine;
    public DrawableColor textColor = null;
    public boolean bold;
    public boolean italic;
    public boolean strikethrough;
    @NotNull
    MinecraftFormattingState minecraftFormatting = MinecraftFormattingState.EMPTY;
    public boolean bulletListItemStart = false;
    public int bulletListLevel = 0;
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    public Hyperlink hyperlink = null;
    public TextClickEvent clickEvent = null;
    public TextHoverEvent hoverEvent = null;
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    public QuoteContext quoteContext = null;
    public CodeBlockContext codeBlockContext = null;
    public boolean plainText = false;
    public Identifier font = null;
    /** Only gets changed through {@link #setHovered(boolean)}, because the fragment's hyperlink and text events count their hovered fragments. */
    public boolean hovered = false;
    public TableContext tableContext = null;
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
                com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
                float width = this.getRenderWidth();
                float height = this.getRenderHeight();
                RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.parent.textOpacity);
                RenderingUtils.blitF(graphics, location, this.x, this.y, 0.0F, 0.0F, width, height, width, height);
                RenderingUtils.resetShaderColor(graphics);
            });
        } else if (this.separationLine) {

            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border, this.y + this.getRenderHeight(), this.parent.separationLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);

        } else {

            float scale = this.getScale();
            if (!Float.isFinite(scale) || scale <= 0.0F) return;

            this.renderCodeBlock(graphics);

            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
            graphics.pose().pushMatrix();
            try {
                // Vanilla text extraction ultimately accepts integer draw coordinates. Translating to the exact origin before local scaling avoids dividing and truncating the absolute position, which could move glyphs across the parent's clipping boundary.
                graphics.pose().translate(this.getTextX(), this.getTextY());
                graphics.pose().scale(scale, scale);
                this.parent.renderText(graphics, this.getRenderComponent(), 0.0F, 0.0F, this.parent.textBaseColor.getColorIntWithAlpha(this.parent.textOpacity), this.parent.textShadow && (this.codeBlockContext == null));
            } finally {
                graphics.pose().popMatrix();
            }
            RenderingUtils.resetShaderColor(graphics);

            this.renderQuoteLine(graphics);

            this.renderBulletListDot(graphics);

            this.renderHeadlineUnderline(graphics);

        }

    }

    protected void renderTable(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.tableContext == null) return;
        
        // Calculate column widths
        this.tableContext.calculateColumnWidths(this.parent);
        
        // Use the fragment's x position which already includes alignment offset
        float tableX = this.x;
        float tableY = this.y + this.parent.tableMargin; // Add margin before table
        float currentY = tableY;
        
        // Use the actual table width for rendering (not the full area width)
        float actualTableWidth = this.tableContext.totalWidth;
        
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        
        // Render rows
        int dataRowIndex = 0; // Track data rows separately for alternating colors
        for (int rowIndex = 0; rowIndex < this.tableContext.rows.size(); rowIndex++) {
            TableRow row = this.tableContext.rows.get(rowIndex);
            float rowHeight = this.tableContext.getRowHeight(row);
            float currentX = tableX;
            
            // Draw row background
            if (row.isHeader && this.parent.tableShowHeader) {
                // Header background
                RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, 
                    currentY + rowHeight, this.parent.tableHeaderBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (!row.isHeader) {
                // Regular row background - alternate between two colors
                if (this.parent.tableAlternateRowColors && dataRowIndex % 2 == 1) {
                    // Alternate rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, 
                        currentY + rowHeight, this.parent.tableAlternateRowColor.getColorIntWithAlpha(this.parent.textOpacity));
                } else {
                    // Base rows
                    RenderingUtils.fillF(graphics, tableX, currentY, tableX + actualTableWidth, 
                        currentY + rowHeight, this.parent.tableRowBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
                }
                dataRowIndex++;
            }
            
            // Render cells
            for (int cellIndex = 0; cellIndex < row.cells.size(); cellIndex++) {
                if (cellIndex >= this.tableContext.columnWidths.size()) break;
                
                TableCell cell = row.cells.get(cellIndex);
                float cellWidth = this.tableContext.columnWidths.get(cellIndex);
                
                // Calculate text position based on alignment
                float textX = currentX + this.parent.tableCellPadding;
                float textY = currentY + this.parent.tableCellPadding;
                
                // Calculate alignment offset once for all fragments
                float totalTextWidth = 0;
                for (MarkdownTextFragment f : cell.fragments) {
                    totalTextWidth += f.getTextRenderWidth();
                }
                
                float alignmentOffset = 0;
                if (cell.alignment == TableCell.TableCellAlignment.CENTER) {
                    alignmentOffset = (cellWidth - this.parent.tableCellPadding * 2 - totalTextWidth) / 2;
                } else if (cell.alignment == TableCell.TableCellAlignment.RIGHT) {
                    alignmentOffset = cellWidth - this.parent.tableCellPadding * 2 - totalTextWidth;
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
                        boolean isLastFragment = (fragIndex == cell.fragments.size() - 1) || 
                                                (fragIndex + 1 < cell.fragments.size() && 
                                                 cell.fragments.get(fragIndex + 1).codeBlockContext != currentCodeBlock);
                        
                        if (isLastFragment) {
                            // Render the code block background
                            float endX = fragment.x + fragment.getRenderWidth() + 1;
                            if (fragment.text.endsWith(" ")) {
                                endX -= (this.parent.getUnscaledTextWidth(" ") * fragment.getScale());
                            }
                            renderCodeBlockBackground(graphics, codeBlockStartX, fragment.y - 2, endX, 
                                fragment.y + fragment.getTextRenderHeight(), 
                                this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
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
        
        // Horizontal lines
        float y = tableY;
        for (int i = 0; i <= this.tableContext.rows.size(); i++) {
            RenderingUtils.fillF(graphics, tableX, y, tableX + actualTableWidth, y + lineThickness, lineColor);
            if (i < this.tableContext.rows.size()) {
                y += this.tableContext.getRowHeight(this.tableContext.rows.get(i));
            }
        }
        
        // Draw thicker line under header
        if (this.tableContext.hasHeader && this.parent.tableShowHeader && !this.tableContext.rows.isEmpty()) {
            float headerY = tableY + this.tableContext.getRowHeight(this.tableContext.rows.get(0));
            RenderingUtils.fillF(graphics, tableX, headerY, tableX + actualTableWidth, 
                headerY + lineThickness * 2, lineColor);
        }
        
        // Vertical lines
        float x = tableX;
        for (int i = 0; i <= this.tableContext.columnWidths.size(); i++) {
            RenderingUtils.fillF(graphics, x, tableY, x + lineThickness, currentY + lineThickness, lineColor);
            if (i < this.tableContext.columnWidths.size()) {
                x += this.tableContext.columnWidths.get(i);
            }
        }
        
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
            if (start != this) return;
            if (end == null) return;
            if (this.codeBlockContext.singleLine) {
                float xEnd = end.x + end.getRenderWidth();
                if (end.text.endsWith(" ")) {
                    xEnd -= (this.parent.getUnscaledTextWidth(" ") * this.getScale());
                }
                renderCodeBlockBackground(graphics, this.x, this.y - 2, xEnd, this.y + this.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else {
                renderCodeBlockBackground(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, end.y + end.getRenderHeight() - 1, this.parent.codeBlockMultiLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
        }
    }

    protected void renderCodeBlockBackground(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        RenderingUtils.fillF(graphics, minX+1, minY, maxX-1, minY+1, color);
        RenderingUtils.fillF(graphics, minX, minY+1, maxX, maxY-1, color);
        RenderingUtils.fillF(graphics, minX+1, maxY-1, maxX-1, maxY, color);
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderHeadlineUnderline(GuiGraphics graphics) {
        if (this.startOfRenderLine && ((this.headlineType == HeadlineType.BIGGER) || (this.headlineType == HeadlineType.BIGGEST))) {
            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
            float scale = (this.parent.parentRenderScale != null) ? this.parent.parentRenderScale : (float)WindowHandler.getGuiScale();
            float lineThickness = (scale > 1) ? 0.5f : 1f;
            float lineY = this.y + this.getTextRenderHeight() + 1;
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, lineY, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, lineY + lineThickness, this.parent.headlineUnderlineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    protected void renderQuoteLine(GuiGraphics graphics) {
        if ((this.quoteContext != null) && (this.quoteContext.getQuoteEnd() != null) && (this.quoteContext.getQuoteEnd() == this)) {
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).y - 2;
            float yEnd = this.y + this.getRenderHeight() + 1;
            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
            if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT) {
                RenderingUtils.fillF(graphics, this.parent.x, yStart, this.parent.x + 2, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (this.alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) {
                RenderingUtils.fillF(graphics, this.parent.x + this.parent.getRealWidth() - this.parent.border - 2, yStart, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, yEnd, this.parent.quoteColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    protected void renderBulletListDot(GuiGraphics graphics) {
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
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
            style = style.withFont(new FontDescription.Resource(this.font));
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

    public float getTextRenderX() {
        return MarkdownTextPositioning.calculateRenderCoordinate(this.getTextX(), this.getScale());
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
        return MarkdownTextPositioning.calculateRenderCoordinate(this.getTextY(), this.getScale());
    }

    protected float getTextRenderOffsetY() {
        float offsetY = 0.0F;
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() != null) && (this.codeBlockContext.getBlockStart().y == this.y)) {
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
            this.tableContext.calculateColumnWidths(this.parent);
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
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            f += 1;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.autoLineBreakAfter || this.naturalLineBreakAfter)) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() == this)) {
            f += 1;
        }
        return f;

    }

    public float getRenderHeight() {

        // Handle table height
        if (this.isTable()) {
            float totalHeight = 0;
            for (TableRow row : this.tableContext.rows) {
                totalHeight += this.tableContext.getRowHeight(row);
            }
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
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() != null) && (this.codeBlockContext.getBlockStart().y == this.y)) {
            f += 10;
        }
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockEnd() != null) && (this.codeBlockContext.getBlockEnd().y == this.y)) {
            f += 10;
        }
        if ((this.bulletListLevel > 0) && this.bulletListItemStart) {
            f += this.parent.bulletListSpacing;
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
        return MarkdownTextPositioning.calculateOrigin(this.x, this.getTextRenderOffsetX(), this.getScale());
    }

    public float getTextY() {
        return MarkdownTextPositioning.calculateOrigin(this.y, this.getTextRenderOffsetY(), this.getScale());
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
        return MarkdownTextPositioning.normalizeScale(f * this.parent.textBaseScale);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isTable()) {
            // For tables, use the actual table rendering area
            this.tableContext.calculateColumnWidths(this.parent);
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
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }
    
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

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }
    
    public boolean isTable() {
        return (this.tableContext != null) && this.text.equals("[TABLE]");
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

    public static class TableContext {
        public final List<TableRow> rows = new ArrayList<>();
        public final List<Float> columnWidths = new ArrayList<>();
        public boolean hasHeader = false;
        public float totalWidth = 0;
        
        public void calculateColumnWidths(MarkdownRenderer renderer) {
            columnWidths.clear();
            if (rows.isEmpty()) return;
            
            // Initialize column widths
            int columnCount = rows.get(0).cells.size();
            for (int i = 0; i < columnCount; i++) {
                columnWidths.add(0f);
            }
            
            // Find maximum width for each column
            for (TableRow row : rows) {
                for (int i = 0; i < Math.min(row.cells.size(), columnCount); i++) {
                    TableCell cell = row.cells.get(i);
                    float cellWidth = 0;
                    for (MarkdownTextFragment fragment : cell.fragments) {
                        cellWidth += fragment.getTextRenderWidth();
                    }
                    cellWidth += renderer.tableCellPadding * 2;
                    if (cellWidth > columnWidths.get(i)) {
                        columnWidths.set(i, cellWidth);
                    }
                }
            }
            
            // Calculate total width
            totalWidth = 0;
            for (Float width : columnWidths) {
                totalWidth += width;
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
        
        public TableRow(MarkdownRenderer parent) {
            this.parent = parent;
        }
    }
    
    public static class TableCell {
        public final List<MarkdownTextFragment> fragments = new ArrayList<>();
        public TableCellAlignment alignment = TableCellAlignment.LEFT;
        
        public enum TableCellAlignment {
            LEFT,
            CENTER,
            RIGHT
        }
    }

}
