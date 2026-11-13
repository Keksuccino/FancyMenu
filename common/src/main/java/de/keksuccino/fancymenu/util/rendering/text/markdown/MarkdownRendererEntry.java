package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a {@link MarkdownRenderer} as the content of a {@link ScrollArea}.<br>
 * The entry makes the text wrap at the width of the area, takes over the real size of the text, so the area knows how far it scrolls,
 * and tells the renderer which part of it the area shows, so lines outside the area don't get rendered.
 */
public class MarkdownRendererEntry extends ScrollAreaEntry {

    protected MarkdownRenderer markdownRenderer;

    public MarkdownRendererEntry(@NotNull ScrollArea parent, @NotNull MarkdownRenderer markdownRenderer) {
        super(parent, 20, 20);
        this.markdownRenderer = markdownRenderer;
        this.selectable = false;
        this.playClickSound = false;
        this.backgroundColorNormal = () -> DrawableColor.FULLY_TRANSPARENT;
        this.backgroundColorHover = () -> DrawableColor.FULLY_TRANSPARENT;
    }

    @Override
    public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.markdownRenderer.setOptimalWidth(this.parent.getInnerWidth());
        this.markdownRenderer.setX(this.x);
        this.markdownRenderer.setY(this.y);
        this.markdownRenderer.setVisibleVerticalRange(this.parent.getInnerY(), this.parent.getInnerY() + this.parent.getInnerHeight());
        this.setWidth(this.markdownRenderer.getRealWidth());
        this.setHeight(this.markdownRenderer.getRealHeight());
        this.markdownRenderer.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
    }

}
