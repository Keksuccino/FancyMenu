package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScrollableMarkdownRenderer implements Renderable, ContainerEventHandler, NarratableEntry {

    @NotNull
    protected ScrollArea scrollArea = new ScrollArea(0,0,0,0);
    @NotNull
    protected MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    @NotNull
    protected List<GuiEventListener> children = new ArrayList<>();
    protected boolean allowScrolling = true;
    @NotNull
    protected String text = "";
    protected boolean dragging = false;
    protected boolean focused = false;

    public ScrollableMarkdownRenderer(float x, float y, float width, float height) {
        this.rebuild(x, y, width, height);
    }

    /**
     * Calling this will rebuild the {@link MarkdownRenderer} and the {@link ScrollArea} and sets the old text again.
     */
    public void rebuild(float x, float y, float width, float height) {

        this.children.clear();

        this.markdownRenderer = new MarkdownRenderer();

        this.scrollArea = new ScrollArea(x, y, width, height) {
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                //Manually update scroll bar area size, so the grabbers are outside the area
                this.verticalScrollBar.scrollAreaEndX = x + width + 12;
                this.horizontalScrollBar.scrollAreaEndY = y + height + 12;
            }
        };
        this.scrollArea.minimumEntryWidthIsAreaWidth = false;
        this.scrollArea.makeEntriesWidthOfArea = false;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.scrollArea.verticalScrollBar.grabberWidth = 10;
        this.scrollArea.verticalScrollBar.grabberHeight = 20;
        this.scrollArea.horizontalScrollBar.grabberWidth = 20;
        this.scrollArea.horizontalScrollBar.grabberHeight = 10;
        this.scrollArea.backgroundColor = () -> DrawableColor.of(0,0,0,0);
        this.scrollArea.borderColor = () -> DrawableColor.of(0,0,0,0);

        this.scrollArea.addEntry(new MarkdownRendererEntry(this.scrollArea, this.markdownRenderer));

        //Don't render markdown lines outside visible area (for performance reasons)
        this.markdownRenderer.addLineRenderValidator(line -> {
            if ((line.parent.getY() + line.offsetY + line.getLineHeight()) < this.scrollArea.getInnerY()) {
                return false;
            }
            if ((line.parent.getY() + line.offsetY) > (this.scrollArea.getInnerY() + this.scrollArea.getInnerHeight())) {
                return false;
            }
            return true;
        });

        this.markdownRenderer.setText(this.text);

        this.children.add(this.markdownRenderer);
        this.children.add(this.scrollArea);

    }

    protected void tick() {

        //Update scroll wheel ONLY FOR vertical bar (horizontal never scrolls via scroll wheel)
        this.scrollArea.verticalScrollBar.setScrollWheelAllowed(this.allowScrolling);

        //Update active state of scroll bars
        this.scrollArea.verticalScrollBar.active = (this.scrollArea.getTotalEntryHeight() > this.scrollArea.getInnerHeight()) && this.allowScrolling;
        this.scrollArea.horizontalScrollBar.active = (this.scrollArea.getTotalEntryWidth() > this.scrollArea.getInnerWidth()) && this.allowScrolling;

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.tick();

        RenderSystem.enableBlend();
        this.scrollArea.render(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

    }

    public ScrollableMarkdownRenderer setText(@NotNull String text) {
        this.text = Objects.requireNonNull(text);
        this.markdownRenderer.setText(text);
        return this;
    }

    public ScrollableMarkdownRenderer setScrollingAllowed(boolean allowed) {
        this.allowScrolling = allowed;
        return this;
    }

    public boolean isScrollingAllowed() {
        return this.allowScrolling;
    }

    @NotNull
    public MarkdownRenderer getMarkdownRenderer() {
        return this.markdownRenderer;
    }

    @NotNull
    public ScrollArea getScrollArea() {
        return this.scrollArea;
    }

    @Override
    public @NotNull List<GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.scrollArea;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener var1) {
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return this.scrollArea.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.markdownRenderer.mouseReleased(mouseX, mouseY, button);
        this.scrollArea.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    protected static class MarkdownRendererEntry extends ScrollAreaEntry {

        protected MarkdownRenderer markdownRenderer;

        public MarkdownRendererEntry(ScrollArea parent, MarkdownRenderer markdownRenderer) {
            super(parent, 20, 20);
            this.markdownRenderer = markdownRenderer;
            this.selectable = false;
            this.playClickSound = false;
            this.backgroundColorNormal = () -> DrawableColor.of(0,0,0,0);
            this.backgroundColorHover = () -> DrawableColor.of(0,0,0,0);
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.markdownRenderer.setOptimalWidth(this.parent.getInnerWidth());
            this.markdownRenderer.setX(this.x);
            this.markdownRenderer.setY(this.y);
            this.setWidth(this.markdownRenderer.getRealWidth());
            this.setHeight(this.markdownRenderer.getRealHeight());
            this.markdownRenderer.render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
