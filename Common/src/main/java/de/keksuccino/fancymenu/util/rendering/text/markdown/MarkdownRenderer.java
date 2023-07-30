package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.FocuslessContainerEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownRenderer extends GuiComponent implements Renderable, FocuslessContainerEventHandler, NarratableEntry {

    @NotNull
    protected String text = "";
    protected String renderText;
    protected float x;
    protected float y;
    protected float optimalWidth;
    protected float realWidth;
    protected float realHeight;
    @NotNull
    protected DrawableColor hyperlinkColor = DrawableColor.of(new Color(7, 113, 252));
    @NotNull
    protected DrawableColor textBaseColor = DrawableColor.WHITE;
    protected float lineSpacing = 2;
    protected float border = 2;
    @NotNull
    protected Font font = Minecraft.getInstance().font;
    protected boolean dragging;
    protected final List<MarkdownTextFragment> fragments = new ArrayList<>();

    //TODO - Text Fragmente ordnen sich beim rendern automatisch neu an (auto line break)
    //TODO - Zu lange Worte ohne Leerzeichen werden NICHT geteilt
    //TODO - Line break bei optimalWidth -> wenn nicht möglich, dann realWidth anpassen

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        //Update fragments in case renderText has changed
        String newRenderText = this.buildRenderText();
        if ((this.renderText == null) || !this.renderText.equals(newRenderText)) {
            this.renderText = newRenderText;
            this.updateFragments();
        }

        //TODO render fragments in lines + handle auto line breaks

        float totalWidth = 20;
        float currentLineWidth = 0;
        float currentLineHeight = this.font.lineHeight;
        float totalHeight = this.border;
        for (MarkdownTextFragment f : this.fragments) {
            float fragW = f.getScaledWidth();
            if (currentLineWidth != 0) {
                if ((currentLineWidth + fragW) > this.optimalWidth) {

                }
            }
        }
        totalHeight += this.border;
        this.realWidth = totalWidth;
        this.realHeight = totalHeight;

    }

    public void updateFragments() {
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

    @NotNull
    public DrawableColor getHyperlinkColor() {
        return this.hyperlinkColor;
    }

    public MarkdownRenderer setHyperlinkColor(@NotNull DrawableColor hyperlinkColor) {
        this.hyperlinkColor = Objects.requireNonNull(hyperlinkColor);
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
