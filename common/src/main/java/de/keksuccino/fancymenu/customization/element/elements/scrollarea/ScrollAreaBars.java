package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.elements.text.v2.ScrollGrabberDesign;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

/**
 * The two scroll bars of a scrollable area. They sit outside of what the area shows, next to its right and bottom edge,
 * and only exist for an axis as long as the content can be scrolled on it.
 */
public final class ScrollAreaBars {

    /** The space a scroll bar has its grabber in, next to the area. */
    public static final int BAR_SPACE = 12;
    private static final int GRABBER_THICKNESS = 10;
    private static final int GRABBER_LENGTH = 20;

    private final ScrollBar vertical;
    private final ScrollBar horizontal;

    public ScrollAreaBars(@NotNull ScrollAreaScroller scroller) {
        this.vertical = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, GRABBER_THICKNESS, GRABBER_LENGTH, 0, 0, 0, 0, ScrollGrabberDesign.PIXEL_HANDLE.getTexture(true, false), ScrollGrabberDesign.PIXEL_HANDLE.getTexture(true, true));
        this.horizontal = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, GRABBER_LENGTH, GRABBER_THICKNESS, 0, 0, 0, 0, ScrollGrabberDesign.PIXEL_HANDLE.getTexture(false, false), ScrollGrabberDesign.PIXEL_HANDLE.getTexture(false, true));
        // A dragged grabber is where the mouse is, so the content follows it without an animation
        this.vertical.registerScrollListener(bar -> scroller.y().scrollTo(bar.getScroll() * scroller.y().getMax(), false));
        this.horizontal.registerScrollListener(bar -> scroller.x().scrollTo(bar.getScroll() * scroller.x().getMax(), false));
    }

    /**
     * Puts the bars next to what the area shows right now and their grabbers where the content is scrolled to.
     *
     * @param opacity The opacity of the area, between 0 and 1.
     */
    public void update(@NotNull ScrollAreaGeometry geometry, @NotNull ScrollAreaScroller scroller, float opacity) {

        int right = geometry.getViewportX() + geometry.getViewportWidth();
        int bottom = geometry.getViewportY() + geometry.getViewportHeight();

        this.vertical.active = scroller.y().canScroll();
        this.vertical.grabberOpacity = opacity;
        this.vertical.scrollAreaStartX = right;
        this.vertical.scrollAreaEndX = right + BAR_SPACE;
        this.vertical.scrollAreaStartY = geometry.getViewportY();
        this.vertical.scrollAreaEndY = bottom;
        if (!this.vertical.isGrabberGrabbed()) this.vertical.setScroll((float) scroller.y().getProgress(), false);

        this.horizontal.active = scroller.x().canScroll();
        this.horizontal.grabberOpacity = opacity;
        this.horizontal.scrollAreaStartX = geometry.getViewportX();
        this.horizontal.scrollAreaEndX = right;
        this.horizontal.scrollAreaStartY = bottom;
        this.horizontal.scrollAreaEndY = bottom + BAR_SPACE;
        if (!this.horizontal.isGrabberGrabbed()) this.horizontal.setScroll((float) scroller.x().getProgress(), false);

    }

    public void render(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.vertical.active) this.vertical.extractRenderState(graphics, mouseX, mouseY, partial);
        if (this.horizontal.active) this.horizontal.extractRenderState(graphics, mouseX, mouseY, partial);
    }

    public boolean isVerticalShown() {
        return this.vertical.active;
    }

    public boolean isHorizontalShown() {
        return this.horizontal.active;
    }

    /**
     * Returns if the user holds one of the grabbers right now.
     */
    public boolean isGrabbed() {
        return this.vertical.isGrabberGrabbed() || this.horizontal.isGrabberGrabbed();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.vertical.mouseClicked(mouseX, mouseY, button) || this.horizontal.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.vertical.mouseReleased(mouseX, mouseY, button);
        this.horizontal.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.vertical.mouseDragged(mouseX, mouseY, button, dragX, dragY) || this.horizontal.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

}
