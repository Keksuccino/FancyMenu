package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.element.style.PixelScrollGrabber;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * The two scroll bars of a scrollable area. They sit outside of what the area shows, next to it,
 * and only exist for an axis as long as the content can be scrolled on it.
 */
public final class ScrollAreaBars {

    /** The room between a grabber and the edges of its bar, on both sides of the grabber. */
    private static final int GRABBER_MARGIN = 1;
    /** How long bars that only show while the content scrolls stay after the last time something happened, and how long they take to fade. */
    private static final long IDLE_STAY_MS = 900L;
    private static final float FADE_PER_SECOND = 4.0F;

    @NotNull
    private final ScrollAreaElement area;
    private final ScrollBar vertical;
    private final ScrollBar horizontal;
    /** How visible bars are that only show while the content scrolls, between 0 and 1. */
    private float activityFade = 0.0F;
    private long lastActivityTime = 0L;
    private double lastScrollX = 0.0D;
    private double lastScrollY = 0.0D;
    private float opacity = 1.0F;

    public ScrollAreaBars(@NotNull ScrollAreaElement area) {
        this.area = area;
        ScrollAreaScroller scroller = area.getScroller();
        this.vertical = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, PixelScrollGrabber.THICKNESS, PixelScrollGrabber.LENGTH, 0, 0, 0, 0, PixelScrollGrabber.getTexture(true, false), PixelScrollGrabber.getTexture(true, true));
        this.horizontal = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, PixelScrollGrabber.LENGTH, PixelScrollGrabber.THICKNESS, 0, 0, 0, 0, PixelScrollGrabber.getTexture(false, false), PixelScrollGrabber.getTexture(false, true));
        this.vertical.setGrabberEdgeInset(GRABBER_MARGIN);
        this.horizontal.setGrabberEdgeInset(GRABBER_MARGIN);
        // A dragged grabber is where the mouse is, so the content follows it without an animation
        this.vertical.registerScrollListener(bar -> scroller.y().scrollTo(bar.getScroll() * scroller.y().getMax(), false));
        this.horizontal.registerScrollListener(bar -> scroller.x().scrollTo(bar.getScroll() * scroller.x().getMax(), false));
    }

    /**
     * Returns the space a scroll bar has its grabber in, next to the area.
     */
    public int getBarSpace() {
        return this.getThickness() + (GRABBER_MARGIN * 2);
    }

    private int getThickness() {
        return Math.max(2, this.area.scrollBarThickness.getInteger());
    }

    /**
     * Puts the bars next to what the area shows right now and their grabbers where the content is scrolled to.
     *
     * @param opacity The opacity of the area, between 0 and 1.
     * @param deltaSeconds The time since the last frame.
     */
    public void update(@NotNull ScrollAreaGeometry geometry, float opacity, double deltaSeconds) {

        ScrollAreaScroller scroller = this.area.getScroller();
        ScrollBarVisibility visibility = this.area.scrollBarVisibility.tryGetNonNullElse(ScrollBarVisibility.ALWAYS);
        // Bars are for scrolling by hand, so an area the user can't scroll has none
        boolean shown = (visibility != ScrollBarVisibility.NEVER) && this.area.userScrolling.getBoolean();
        this.vertical.active = shown && scroller.y().canScroll();
        this.horizontal.active = shown && scroller.x().canScroll();

        int thickness = this.getThickness();
        // The grabbers keep the proportions of the pixel art handle they are made for
        int length = thickness * (PixelScrollGrabber.LENGTH / PixelScrollGrabber.THICKNESS);
        this.vertical.grabberWidth = thickness;
        this.vertical.grabberHeight = length;
        this.horizontal.grabberWidth = length;
        this.horizontal.grabberHeight = thickness;

        int space = this.getBarSpace();
        // A background that does not reach behind the bars has them outside of it
        int distance = this.area.background.getScrollBarDistance();
        int left = geometry.getViewportX();
        int top = geometry.getViewportY();
        int right = left + geometry.getViewportWidth();
        int bottom = top + geometry.getViewportHeight();

        // A bar has its grabber at the end of the range it is given, so a bar on the left or the top ends where the area starts
        this.vertical.scrollAreaEndX = this.area.isVerticalScrollBarAtStart() ? (left - distance) : (right + distance + space);
        this.vertical.scrollAreaStartX = this.vertical.scrollAreaEndX - space;
        this.vertical.scrollAreaStartY = top;
        this.vertical.scrollAreaEndY = bottom;
        if (!this.vertical.isGrabberGrabbed()) this.vertical.setScroll((float) scroller.y().getProgress(), false);

        this.horizontal.scrollAreaEndY = this.area.isHorizontalScrollBarAtStart() ? (top - distance) : (bottom + distance + space);
        this.horizontal.scrollAreaStartY = this.horizontal.scrollAreaEndY - space;
        this.horizontal.scrollAreaStartX = left;
        this.horizontal.scrollAreaEndX = right;
        if (!this.horizontal.isGrabberGrabbed()) this.horizontal.setScroll((float) scroller.x().getProgress(), false);

        this.opacity = opacity * this.updateActivityFade(visibility, scroller, deltaSeconds);
        this.area.grabberStyle.apply(this.vertical, this.horizontal, this.opacity);

    }

    /**
     * Returns how visible the bars are for this frame, between 0 and 1. Bars that only show while the content scrolls
     * come back at once when something happens, and fade out a moment after it stopped.
     */
    private float updateActivityFade(@NotNull ScrollBarVisibility visibility, @NotNull ScrollAreaScroller scroller, double deltaSeconds) {
        if (visibility != ScrollBarVisibility.WHILE_SCROLLING) {
            this.activityFade = 1.0F;
            return 1.0F;
        }
        double scrollX = scroller.x().getPosition();
        double scrollY = scroller.y().getPosition();
        long now = System.currentTimeMillis();
        if ((scrollX != this.lastScrollX) || (scrollY != this.lastScrollY) || this.isUsedByMouse()) {
            this.lastActivityTime = now;
        }
        this.lastScrollX = scrollX;
        this.lastScrollY = scrollY;
        if ((now - this.lastActivityTime) <= IDLE_STAY_MS) {
            this.activityFade = 1.0F;
        } else {
            this.activityFade = Math.max(0.0F, this.activityFade - (float) (FADE_PER_SECOND * deltaSeconds));
        }
        return this.activityFade;
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.vertical.active) {
            this.area.scrollBarBackground.render(graphics, this.vertical, true, this.opacity);
            this.vertical.render(graphics, mouseX, mouseY, partial);
        }
        if (this.horizontal.active) {
            this.area.scrollBarBackground.render(graphics, this.horizontal, false, this.opacity);
            this.horizontal.render(graphics, mouseX, mouseY, partial);
        }
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

    /**
     * Returns if the mouse holds one of the grabbers or is over one, where a press of it grabs the grabber.
     */
    public boolean isUsedByMouse() {
        return this.isGrabbed() || this.vertical.isGrabberHovered() || this.horizontal.isGrabberHovered();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.vertical.mouseClicked(mouseX, mouseY, button) || this.horizontal.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.vertical.mouseReleased(mouseX, mouseY, button);
        this.horizontal.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Moves the grabber the user holds to where the mouse is. Has to be called in every frame a grabber is held: on some game versions,
     * screens never tell what takes its clicks before they do that the mouse moved with its button held.
     */
    public void followPointer(int mouseX, int mouseY) {
        this.vertical.mouseDragged(mouseX, mouseY, InputConstants.MOUSE_BUTTON_LEFT, 0.0D, 0.0D);
        this.horizontal.mouseDragged(mouseX, mouseY, InputConstants.MOUSE_BUTTON_LEFT, 0.0D, 0.0D);
    }

}
