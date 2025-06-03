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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
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

    protected static final int BULLET_LIST_SPACE_AFTER_INDENT = 5;

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
    public boolean bulletListItemStart = false;
    public int bulletListLevel = 0;
    @NotNull
    public MarkdownRenderer.MarkdownLineAlignment alignment = MarkdownRenderer.MarkdownLineAlignment.LEFT;
    public Hyperlink hyperlink = null;
    @NotNull
    public HeadlineType headlineType = HeadlineType.NONE;
    public QuoteContext quoteContext = null;
    public CodeBlockContext codeBlockContext = null;
    public boolean plainText = false;
    public ResourceLocation font = null;
    public boolean hovered = false;
    public TableContext tableContext = null;

    public MarkdownTextFragment(@NotNull MarkdownRenderer parent, @NotNull String text) {
        this.parent = parent;
        this.text = text;
        this.unscaledTextHeight = this.parent.font.lineHeight;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.hovered = this.isMouseOver(mouseX, mouseY);

        if ((this.hyperlink != null) && this.hovered) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }

        // Handle table rendering
        if ((this.tableContext != null) && this.text.equals("[TABLE]")) {
            renderTable(graphics);
            return;
        }

        if (this.imageSupplier != null) {
            this.imageSupplier.forRenderable((iTexture, location) -> {
                RenderSystem.enableBlend();
                graphics.setColor(1.0F, 1.0F, 1.0F, this.parent.textOpacity);
                RenderingUtils.blitF(graphics, location, this.x, this.y, 0.0F, 0.0F, this.getRenderWidth(), this.getRenderHeight(), this.getRenderWidth(), this.getRenderHeight());
                RenderingUtils.resetShaderColor(graphics);
            });
        } else if (this.separationLine) {

            RenderSystem.enableBlend();
            RenderingUtils.fillF(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border, this.y + this.getRenderHeight(), this.parent.separationLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            RenderingUtils.resetShaderColor(graphics);

        } else {

            this.renderCodeBlock(graphics);

            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().scale(this.getScale(), this.getScale(), this.getScale());
            graphics.drawString(this.parent.font, this.buildRenderComponent(false), (int) this.getTextRenderX(), (int) this.getTextRenderY(), this.parent.textBaseColor.getColorIntWithAlpha(this.parent.textOpacity), this.parent.textShadow && (this.codeBlockContext == null));
            graphics.pose().popPose();
            RenderingUtils.resetShaderColor(graphics);

            this.renderQuoteLine(graphics);

            this.renderBulletListDot(graphics);

            this.renderHeadlineUnderline(graphics);

        }

    }

    protected void renderTable(@NotNull GuiGraphics graphics) {
        if (this.tableContext == null) return;
        
        // Calculate column widths
        this.tableContext.calculateColumnWidths(this.parent);
        
        float tableX = this.x;
        float tableY = this.y;
        float currentY = tableY;
        
        RenderSystem.enableBlend();
        
        // Render rows
        for (int rowIndex = 0; rowIndex < this.tableContext.rows.size(); rowIndex++) {
            TableRow row = this.tableContext.rows.get(rowIndex);
            float rowHeight = this.tableContext.getRowHeight(row);
            float currentX = tableX;
            
            // Draw row background
            if (row.isHeader && this.parent.tableShowHeader) {
                // Header background
                RenderingUtils.fillF(graphics, tableX, currentY, tableX + this.tableContext.totalWidth, 
                    currentY + rowHeight, this.parent.tableHeaderBackgroundColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else if (!row.isHeader && this.parent.tableAlternateRowColors && rowIndex % 2 == 0) {
                // Alternate row background
                RenderingUtils.fillF(graphics, tableX, currentY, tableX + this.tableContext.totalWidth, 
                    currentY + rowHeight, this.parent.tableAlternateRowColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
            
            // Render cells
            for (int cellIndex = 0; cellIndex < row.cells.size(); cellIndex++) {
                if (cellIndex >= this.tableContext.columnWidths.size()) break;
                
                TableCell cell = row.cells.get(cellIndex);
                float cellWidth = this.tableContext.columnWidths.get(cellIndex);
                
                // Calculate text position based on alignment
                float textX = currentX + this.parent.tableCellPadding;
                float textY = currentY + this.parent.tableCellPadding;
                
                // Render cell content
                for (MarkdownTextFragment fragment : cell.fragments) {
                    // Calculate alignment offset
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
                    
                    fragment.x = textX + alignmentOffset;
                    fragment.y = textY;
                    fragment.render(graphics, 0, 0, 0); // Don't pass mouse coords to avoid hover effects
                    textX += fragment.getTextRenderWidth();
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
            RenderingUtils.fillF(graphics, tableX, y, tableX + this.tableContext.totalWidth, y + lineThickness, lineColor);
            if (i < this.tableContext.rows.size()) {
                y += this.tableContext.getRowHeight(this.tableContext.rows.get(i));
            }
        }
        
        // Draw thicker line under header
        if (this.tableContext.hasHeader && this.parent.tableShowHeader && !this.tableContext.rows.isEmpty()) {
            float headerY = tableY + this.tableContext.getRowHeight(this.tableContext.rows.get(0));
            RenderingUtils.fillF(graphics, tableX, headerY, tableX + this.tableContext.totalWidth, 
                headerY + lineThickness * 2, lineColor);
        }
        
        // Vertical lines
        float x = tableX;
        for (int i = 0; i <= this.tableContext.columnWidths.size(); i++) {
            RenderingUtils.fillF(graphics, x, tableY, x + lineThickness, currentY, lineColor);
            if (i < this.tableContext.columnWidths.size()) {
                x += this.tableContext.columnWidths.get(i);
            }
        }
        
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderCodeBlock(GuiGraphics graphics) {
        if ((this.codeBlockContext != null) && (this.parentLine != null)) {
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
                    xEnd -= (this.parent.font.width(" ") * this.getScale());
                }
                renderCodeBlockBackground(graphics, this.x, this.y - 2, xEnd, this.y + this.getTextRenderHeight(), this.parent.codeBlockSingleLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            } else {
                renderCodeBlockBackground(graphics, this.parent.x + this.parent.border, this.y, this.parent.x + this.parent.getRealWidth() - this.parent.border - 1, end.y + end.getRenderHeight() - 1, this.parent.codeBlockMultiLineColor.getColorIntWithAlpha(this.parent.textOpacity));
            }
        }
    }

    protected void renderCodeBlockBackground(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        RenderSystem.enableBlend();
        RenderingUtils.fillF(graphics, minX+1, minY, maxX-1, minY+1, color);
        RenderingUtils.fillF(graphics, minX, minY+1, maxX, maxY-1, color);
        RenderingUtils.fillF(graphics, minX+1, maxY-1, maxX-1, maxY, color);
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
        if ((this.quoteContext != null) && (this.quoteContext.getQuoteEnd() != null) && (this.quoteContext.getQuoteEnd() == this)) {
            float yStart = Objects.requireNonNull(this.quoteContext.getQuoteStart()).y - 2;
            float yEnd = this.y + this.getRenderHeight() + 1;
            RenderSystem.enableBlend();
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
            RenderSystem.enableBlend();
            final float scale = this.getScale();

            // Calculate dimensions using scale
            final float bulletSize = 3 * scale;

            // Shift bullet dot one level to the right:
            final float bulletX = this.x - (5 * scale) + (this.parent.bulletListIndent * (this.bulletListLevel) * scale);

            // Vertical centering using text baseline
            final float textBaselineY = this.getTextY() + (Minecraft.getInstance().font.lineHeight * scale * 0.5f) - (bulletSize * 0.5f);

            RenderingUtils.fillF(graphics, bulletX, textBaselineY, bulletX + bulletSize, textBaselineY + bulletSize,
                    this.parent.bulletListDotColor.getColorIntWithAlpha(this.parent.textOpacity)
            );
        }
    }

    @NotNull
    protected Component buildRenderComponent(boolean forWidthCalculation) {
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
        if (this.hyperlink != null) {
            style = style.withColor(this.parent.hyperlinkColor.getColorInt());
            if (this.hyperlink.isHovered()) {
                style = style.withUnderlined(true);
            }
        }
        boolean addSpaceComponentAtEnd = false;
        String t = this.text;
        if ((this.hyperlink != null) && (this.naturalLineBreakAfter || this.autoLineBreakAfter) && t.endsWith(" ")) {
            //Remove spaces at line end that would look ugly when underlined
            t = t.substring(0, t.length()-1);
        } else if ((this.hyperlink != null) && (ListUtils.getLast(this.hyperlink.hyperlinkFragments) == this) && t.endsWith(" ")) {
            //Make space at the end not underlined without removing it completely
            t = t.substring(0, t.length()-1);
            addSpaceComponentAtEnd = true;
        }
        if (this.codeBlockContext != null) {
            style = Style.EMPTY;
        }
        if (this.plainText) {
            style = Style.EMPTY;
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_UPPER) {
            t = t.toUpperCase();
        }
        if (this.parent.textCase == MarkdownRenderer.TextCase.ALL_LOWER) {
            t = t.toLowerCase();
        }
        MutableComponent comp = Component.literal(t).setStyle(style);
        if (addSpaceComponentAtEnd) {
            comp.append(Component.literal(" ").setStyle(Style.EMPTY.withUnderlined(false)));
        }
        return comp;
    }

    protected void updateWidth() {
        this.unscaledTextWidth = this.parent.font.width(this.buildRenderComponent(true));
    }

    public float getTextRenderX() {
        float baseX = this.x / this.getScale();

        if ((this.quoteContext != null) && this.startOfRenderLine && (this.alignment == MarkdownRenderer.MarkdownLineAlignment.LEFT)) {
            baseX += this.parent.quoteIndent;
        }

        if (this.bulletListLevel > 0 && this.startOfRenderLine) {
            // Now apply the full bullet indent for the first fragment.
            float bulletIndent = (this.parent.bulletListIndent * this.bulletListLevel) + BULLET_LIST_SPACE_AFTER_INDENT;
            baseX += bulletIndent;
        }

        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && this.startOfRenderLine) {
            baseX += 10;
        }

        if ((this.codeBlockContext != null) && this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() == this)) {
            baseX += 1;
        }

        return (int)baseX;
    }

    public float getTextRenderY() {
        float f = this.y / this.getScale();
        if ((this.codeBlockContext != null) && !this.codeBlockContext.singleLine && (this.codeBlockContext.getBlockStart() != null) && (this.codeBlockContext.getBlockStart().y == this.y)) {
            f += 10;
        }
        if ((this.bulletListLevel > 0) && (this.parentLine != null) && this.parentLine.bulletListItemStartLine) {
            f += this.parent.bulletListSpacing;
        }
        return (int)f;
    }

    public float getRenderWidth() {

        // Handle table width
        if ((this.tableContext != null) && this.text.equals("[TABLE]")) {
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
        if ((this.tableContext != null) && this.text.equals("[TABLE]")) {
            float totalHeight = 0;
            for (TableRow row : this.tableContext.rows) {
                totalHeight += this.tableContext.getRowHeight(row);
            }
            // Add line thickness for borders
            totalHeight += this.parent.tableLineThickness * (this.tableContext.rows.size() + 1);
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
        float f = this.getTextRenderX();
        f -= (this.x / this.getScale());
        f += this.x;
        return f;
    }

    public float getTextY() {
        float f = this.getTextRenderY();
        f -= (this.y / this.getScale());
        f += this.y;
        return f;
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
        return f * this.parent.textBaseScale;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if ((this.tableContext != null) && this.text.equals("[TABLE]")) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.x, this.y, this.getRenderWidth(), this.getRenderHeight());
        }
        if ((this.imageSupplier != null) && (this.imageSupplier.get() != null)) {
            return RenderingUtils.isXYInArea(mouseX, mouseY, this.x, this.y, this.getRenderWidth(), this.getRenderHeight());
        }
        return RenderingUtils.isXYInArea(mouseX, mouseY, this.getTextX(), this.getTextY(), this.getTextWidth(), this.getTextHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((this.hyperlink != null) && this.hovered) {
            WebUtils.openWebLink(this.hyperlink.link);
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

    public static class Hyperlink {

        public String link = null;
        public final List<MarkdownTextFragment> hyperlinkFragments = new ArrayList<>();

        public boolean isHovered() {
            for (MarkdownTextFragment f : this.hyperlinkFragments) {
                if (f.hovered) return true;
            }
            return false;
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
        public float x = 0;
        public float y = 0;
        
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
