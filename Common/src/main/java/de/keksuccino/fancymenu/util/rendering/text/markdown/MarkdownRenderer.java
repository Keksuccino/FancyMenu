package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FocuslessContainerEventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownRenderer extends GuiComponent implements Renderable, FocuslessContainerEventHandler, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected String text = "";
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
    protected DrawableColor quoteColor = DrawableColor.of(new Color(129, 129, 129));
    protected float quoteIndent = 8;
    @NotNull
    protected DrawableColor textBaseColor = DrawableColor.WHITE;
    protected float lineSpacing = 2;
    protected float border = 2;
    @Nullable
    protected Float parentRenderScale = null;
    @NotNull
    protected Font font = Minecraft.getInstance().font;
    protected boolean dragging;
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();
    protected float lastOptimalWidth = -1000;

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.onRender(pose, mouseX, mouseY, partial, true);
    }

    protected void onRender(PoseStack pose, int mouseX, int mouseY, float partial, boolean shouldRender) {

        this.updateRenderText();

        boolean queueNewLine = true;
        float totalWidth = 20;
        float totalHeight = this.border;
        float currentLineWidth = this.border;
        float currentLineHeight = 0;
        MarkdownTextFragment lastFragment = null;

        int i = -1;
        for (MarkdownTextFragment f : this.fragments) {

            i++;

            boolean isStartOfLine = queueNewLine;
            queueNewLine = false;

            //Handle Auto Line Break
            if (!isStartOfLine) {
//                if (lastFragment.endOfWord && ((currentLineWidth + f.getRenderWidth() + this.border) > this.optimalWidth)) {
                if (lastFragment.autoLineBreakAfter) {
                    if (totalWidth < currentLineWidth) {
                        totalWidth = currentLineWidth;
                    }
                    currentLineWidth = this.border;
                    totalHeight += currentLineHeight + this.lineSpacing;
                    currentLineHeight = 0;
                    isStartOfLine = true;
                }
            }

            //Handle Quote Indent
            if ((f.quoteContext != null) && isStartOfLine) {
                currentLineWidth += this.quoteIndent;
            }

            //Handle Fragment Positioning + Rendering
            f.x = this.x + currentLineWidth;
            f.y = this.y + totalHeight;
            f.startOfRenderLine = isStartOfLine;
            f.autoLineBreakAfter = !f.naturalLineBreakAfter && (i+1 < this.fragments.size()) && f.endOfWord && ((currentLineWidth + f.getRenderWidth() + this.fragments.get(i+1).getRenderWidth() + this.border) > this.optimalWidth);
            float fw = f.getRenderWidth();
            float fh = f.getRenderHeight();
            if (shouldRender) {
                f.render(pose, mouseX, mouseY, partial);
            }
            currentLineWidth += fw;
            if (currentLineHeight < fh) {
                currentLineHeight = fh;
            }

            //Handle Natural Line Break
            if (f.naturalLineBreakAfter) {
                if (totalWidth < currentLineWidth) {
                    totalWidth = currentLineWidth;
                }
                currentLineWidth = this.border;
                totalHeight += currentLineHeight + this.lineSpacing;
                currentLineHeight = 0;
                queueNewLine = true;
            }

            lastFragment = f;

        }

        totalWidth += this.border;
        totalHeight += this.border;
        this.realWidth = totalWidth;
        this.realHeight = totalHeight;

        //TODO remove debug
        if (shouldRender) {
            UIBase.renderBorder(pose, this.x, this.y, this.x + this.optimalWidth, this.y + this.getRealHeight(), 1, DrawableColor.BLACK.getColorInt(), true, true, true, true);
        }

    }

    public void updateRenderText() {
        String newRenderText = this.buildRenderText();
        if ((this.renderText == null) || !this.renderText.equals(newRenderText)) {
            this.renderText = newRenderText;
            this.rebuildFragments();
            //This is to update the total width/height before first actual render
            this.onRender(null, 0, 0, 0, false);
        }
        if ((this.lastOptimalWidth != -1000) && (this.lastOptimalWidth != this.optimalWidth)) {
            this.onRender(null, 0, 0, 0, false);
        }
        this.lastOptimalWidth = this.optimalWidth;
    }

    public void rebuildFragments() {
        this.fragments.clear();
        this.fragments.addAll(MarkdownParser.parse(this, this.renderText));
    }

    @NotNull
    protected String buildRenderText() {
        return PlaceholderParser.replacePlaceholders(this.text);
    }

    @NotNull
    public String getText() {
        return this.text;
    }

    public MarkdownRenderer setText(@NotNull String text) {
        this.text = Objects.requireNonNull(text);
        return this;
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
        this.optimalWidth = width;
        return this;
    }

    public float getOptimalWidth() {
        return this.optimalWidth;
    }

    public float getRealWidth() {
        return this.realWidth;
    }

    public float getRealHeight() {
        return this.realHeight;
    }

    @Nullable
    public Float getParentRenderScale() {
        return this.parentRenderScale;
    }

    public MarkdownRenderer setParentRenderScale(@Nullable Float parentRenderScale) {
        this.parentRenderScale = parentRenderScale;
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

    public MarkdownRenderer setHeadlineUnderlineColor(@NotNull DrawableColor headlineUnderlineColor) {
        this.headlineUnderlineColor = headlineUnderlineColor;
        return this;
    }

    @NotNull
    public DrawableColor getHyperlinkColor() {
        return this.hyperlinkColor;
    }

    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.hyperlinkColor = Objects.requireNonNull(hyperlinkColor);
        return this;
    }

    @NotNull
    public DrawableColor getQuoteColor() {
        return this.quoteColor;
    }

    public MarkdownRenderer setQuoteColor(@NotNull DrawableColor quoteColor) {
        this.quoteColor = Objects.requireNonNull(quoteColor);
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

    public float getQuoteIndent() {
        return this.quoteIndent;
    }

    public MarkdownRenderer setQuoteIndent(float quoteIndent) {
        this.quoteIndent = quoteIndent;
        return this;
    }

    public float getLineSpacing() {
        return this.lineSpacing;
    }

    public MarkdownRenderer setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    public float getBorder() {
        return this.border;
    }

    public MarkdownRenderer setBorder(float border) {
        this.border = border;
        return this;
    }

    @Override
    @NotNull
    public List<MarkdownTextFragment> children() {
        return this.fragments;
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

}
