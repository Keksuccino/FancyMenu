package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Everything that scrolls the content of a scrollable area: the mouse wheel, dragging the content, the keyboard, the area itself
 * (when it scrolls on its own or follows the focus), and whatever else asks it to.<br>
 * All of them only move the target of the {@link ScrollAreaScroller}, so none of them ever has to wait for another one to finish.
 */
public final class ScrollAreaScrolling {

    /** How many screen pixels one step of the mouse wheel scrolls at a scroll speed of 1. */
    private static final double WHEEL_STEP = 30.0D;
    /** How far the mouse has to move with its button held before it drags the content. Less than this is a click. */
    private static final int DRAG_START_DISTANCE = 4;
    /** How much of what the area shows stays in sight when the keyboard scrolls by a page, so the eye keeps its place. */
    private static final int PAGE_OVERLAP = 24;
    /** The room an element that gets scrolled into sight has to the edge of the area. */
    private static final int INTO_VIEW_MARGIN = 4;
    private static final String MEMORY_SCROLL_X = "scroll_area_scroll_x";
    private static final String MEMORY_SCROLL_Y = "scroll_area_scroll_y";

    @NotNull
    private final ScrollAreaElement area;
    private final ScrollAreaScroller scroller = new ScrollAreaScroller();
    private final ScrollAreaAutoScroll autoScroll = new ScrollAreaAutoScroll();
    private final ScrollAreaSnapPoints snapPoints = new ScrollAreaSnapPoints();
    /** If the content still has to be put where an area that just showed up has it. */
    private boolean needsStartPosition = true;
    private double rememberedX = 0.0D;
    private double rememberedY = 0.0D;
    @Nullable
    private GuiEventListener lastFocused;
    private boolean mousePressed = false;
    private boolean dragging = false;
    private int pressX = 0;
    private int pressY = 0;
    private int lastDragX = 0;
    private int lastDragY = 0;

    public ScrollAreaScrolling(@NotNull ScrollAreaElement area) {
        this.area = area;
    }

    @NotNull
    public ScrollAreaScroller getScroller() {
        return this.scroller;
    }

    /**
     * Returns the axis the area mainly scrolls on: the one its children run along, and the vertical one for an area that scrolls both ways.
     */
    @NotNull
    public ScrollAreaScroller.Axis getMainAxis() {
        return this.scroller.axis(!this.area.getScrollDirection().scrollsY());
    }

    /**
     * Puts the content where it was when the element of the area got built the last time. Elements get rebuilt whenever
     * the screen changes its size, and the content stays where it was scrolled to.
     */
    public void restoreFromMemory() {
        RuntimePropertyContainer memory = this.area.getMemory();
        Double scrollX = memory.getProperty(MEMORY_SCROLL_X, Double.class);
        Double scrollY = memory.getProperty(MEMORY_SCROLL_Y, Double.class);
        if ((scrollX == null) && (scrollY == null)) return;
        if (scrollX != null) this.scroller.x().restore(scrollX);
        if (scrollY != null) this.scroller.y().restore(scrollY);
        this.rememberedX = this.scroller.x().getTarget();
        this.rememberedY = this.scroller.y().getTarget();
        this.needsStartPosition = false;
    }

    /**
     * Puts the content back where an area that just showed up has it.
     */
    public void resetToStartPosition() {
        this.scroller.x().restore(0.0D);
        this.scroller.y().restore(0.0D);
        this.needsStartPosition = true;
    }

    /**
     * Moves the content for one frame. Has to be called after the geometry of the area got the size of its content for this frame.
     *
     * @param deltaSeconds The time since the last frame.
     * @param mouseX Where the mouse is on the screen, or {@link ScrollAreaElement#MOUSE_OUTSIDE} if it is not known.
     * @param mouseOver If the mouse is over what the area shows.
     */
    public void update(@NotNull ScrollAreaGeometry geometry, double deltaSeconds, int mouseX, int mouseY, boolean mouseOver) {

        this.scroller.setSmooth(this.area.smoothScrolling.getBoolean());
        this.scroller.setRange(geometry.getMaxScrollX(), geometry.getMaxScrollY());

        // The end of a content is only known once there is something to scroll, which can take a few frames for content that still loads
        if (this.needsStartPosition) {
            if (this.area.startPosition.tryGetNonNullElse(ScrollStartPosition.START) == ScrollStartPosition.START) {
                this.needsStartPosition = false;
            } else if (this.getMainAxis().canScroll()) {
                this.getMainAxis().scrollTo(this.getMainAxis().getMax(), false);
                this.needsStartPosition = false;
            }
        }

        this.updateDrag(mouseX, mouseY);

        boolean userHoldsContent = this.dragging || this.area.getBars().isGrabbed();
        if (this.area.autoScroll.getBoolean() && !this.area.isInEditor() && !userHoldsContent && !(mouseOver && this.area.autoScrollPauseOnHover.getBoolean())) {
            this.autoScroll.update(this.getMainAxis(), deltaSeconds, this.area.autoScrollSpeed.getFloat(), this.area.autoScrollLoop.getBoolean());
        }

        this.followFocus();

        this.scroller.update(deltaSeconds);
        this.remember();

    }

    private void remember() {
        double scrollX = this.scroller.x().getTarget();
        double scrollY = this.scroller.y().getTarget();
        if ((scrollX == this.rememberedX) && (scrollY == this.rememberedY)) return;
        this.rememberedX = scrollX;
        this.rememberedY = scrollY;
        RuntimePropertyContainer memory = this.area.getMemory();
        memory.putProperty(MEMORY_SCROLL_X, scrollX);
        memory.putProperty(MEMORY_SCROLL_Y, scrollY);
    }

    /**
     * Scrolls the content with the mouse wheel.<br>
     * An area that only scrolls sideways takes the normal wheel for it. An area that scrolls both ways scrolls up and down
     * with the wheel and sideways with a sideways wheel or with Shift held.
     *
     * @return If the content has somewhere to go in the scrolled direction. If not, the scroll is for whatever is behind the area.
     */
    public boolean scrollByWheel(double deltaX, double deltaY) {
        if (!this.area.userScrolling.getBoolean()) return false;
        ScrollDirection direction = this.area.getScrollDirection();
        boolean scrolled;
        if (direction == ScrollDirection.VERTICAL) {
            scrolled = this.scrollAxisByWheel(false, deltaY);
        } else if (direction == ScrollDirection.HORIZONTAL) {
            scrolled = this.scrollAxisByWheel(true, (deltaX != 0.0D) ? deltaX : deltaY);
        } else if (this.area.isShiftDown() && (deltaX == 0.0D)) {
            scrolled = this.scrollAxisByWheel(true, deltaY);
        } else {
            // Both axes get asked, so a wheel that scrolls both ways at once moves the content on both
            boolean scrolledX = this.scrollAxisByWheel(true, deltaX);
            boolean scrolledY = this.scrollAxisByWheel(false, deltaY);
            scrolled = scrolledX || scrolledY;
        }
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    private boolean scrollAxisByWheel(boolean horizontal, double delta) {
        if (delta == 0.0D) return false;
        ScrollAreaScroller.Axis axis = this.scroller.axis(horizontal);
        // A wheel that turns towards the user scrolls the content forwards
        boolean forward = delta < 0.0D;
        if (this.area.getSnapMode() != ScrollSnapMode.NONE) {
            this.updateSnapPoints(horizontal);
            return axis.scrollTo(this.snapPoints.next(axis.getTarget(), forward), true);
        }
        return axis.scrollBy(-delta * WHEEL_STEP * Math.max(0.0F, this.area.scrollSpeed.getFloat()));
    }

    /**
     * Scrolls the content by about what the area shows of it, on the axis the area mainly scrolls on.
     *
     * @return If the content has somewhere to go in that direction.
     */
    public boolean scrollByPage(boolean forward) {
        if (!this.area.userScrolling.getBoolean()) return false;
        boolean horizontal = !this.area.getScrollDirection().scrollsY();
        ScrollAreaScroller.Axis axis = this.scroller.axis(horizontal);
        ScrollAreaGeometry geometry = this.area.getGeometry();
        int viewportSize = horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight();
        double distance = Math.max(PAGE_OVERLAP, viewportSize - PAGE_OVERLAP);
        double target = axis.getTarget() + (forward ? distance : -distance);
        if (this.area.getSnapMode() != ScrollSnapMode.NONE) {
            this.updateSnapPoints(horizontal);
            target = this.snapPoints.nearest(target);
        }
        boolean scrolled = axis.scrollTo(target, true);
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    /**
     * Scrolls the content to its start or its end, on the axis the area mainly scrolls on.
     *
     * @return If the content was not there already.
     */
    public boolean scrollToEdge(boolean end, boolean animate) {
        ScrollAreaScroller.Axis axis = this.getMainAxis();
        boolean scrolled = axis.scrollTo(end ? axis.getMax() : 0.0D, animate);
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    /**
     * Scrolls the content so that all of the child shows, or as much of it as fits, starting at its start.
     * A child that shows completely already leaves the content where it is.
     */
    public void scrollIntoView(@NotNull AbstractElement child, boolean animate) {
        ScrollAreaGeometry geometry = this.area.getGeometry();
        ScrollDirection direction = this.area.getScrollDirection();
        if (direction.scrollsX()) this.scrollIntoView(this.scroller.x(), child.getAbsoluteX(), child.getAbsoluteWidth(), geometry.getViewportX(), geometry.getViewportWidth(), animate);
        if (direction.scrollsY()) this.scrollIntoView(this.scroller.y(), child.getAbsoluteY(), child.getAbsoluteHeight(), geometry.getViewportY(), geometry.getViewportHeight(), animate);
    }

    private void scrollIntoView(@NotNull ScrollAreaScroller.Axis axis, int childStart, int childSize, int viewportStart, int viewportSize, boolean animate) {
        int wantedStart = viewportStart + INTO_VIEW_MARGIN;
        int wantedEnd = (viewportStart + viewportSize) - INTO_VIEW_MARGIN;
        double distance = 0.0D;
        if ((childStart < wantedStart) || (childSize > (wantedEnd - wantedStart))) {
            distance = childStart - wantedStart;
        } else if ((childStart + childSize) > wantedEnd) {
            distance = (childStart + childSize) - wantedEnd;
        }
        // The child is where the content is right now, not where the content is on its way to
        if (distance != 0.0D) axis.scrollTo(axis.getPosition() + distance, animate);
    }

    /**
     * Scrolls the child the keyboard just put its focus on into sight. Only a focus that changed scrolls,
     * so the user can scroll away from a focused child without getting pulled back.
     */
    private void followFocus() {
        GuiEventListener focused = this.area.getFocusedListener();
        if (focused == this.lastFocused) return;
        this.lastFocused = focused;
        if (focused == null) return;
        AbstractElement child = this.area.findChildOwning(focused);
        if (child != null) this.scrollIntoView(child, true);
    }

    /**
     * Works out where the content comes to rest on an axis, for the mode the area snaps in.
     */
    private void updateSnapPoints(boolean horizontal) {
        ScrollAreaGeometry geometry = this.area.getGeometry();
        ScrollAreaScroller.Axis axis = this.scroller.axis(horizontal);
        this.snapPoints.clear();
        if (this.area.getSnapMode() == ScrollSnapMode.PAGES) {
            this.snapPoints.addPages(horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight(), axis.getMax());
        } else {
            // Arranged children have a padding in front of the first one, which stays in sight in front of all of them
            int padding = this.area.arrangesChildren() ? Math.round(this.area.contentPadding.getInteger() * geometry.getScale()) : 0;
            int contentStart = horizontal ? geometry.getContentScreenX() : geometry.getContentScreenY();
            List<AbstractElement> children = this.area.getChildren();
            for (int i = 0; i < children.size(); i++) {
                AbstractElement child = children.get(i);
                if (!this.area.takesPartInArrangement(child)) continue;
                this.snapPoints.add(((horizontal ? child.getAbsoluteX() : child.getAbsoluteY()) - contentStart) - padding);
            }
        }
        this.snapPoints.finish(axis.getMax());
    }

    /**
     * Lets the content come to rest where the area snaps, after something moved it freely.
     */
    public void snapToNearest() {
        if (this.area.getSnapMode() == ScrollSnapMode.NONE) return;
        ScrollDirection direction = this.area.getScrollDirection();
        for (int i = 0; i < 2; i++) {
            boolean horizontal = i == 0;
            if (horizontal ? !direction.scrollsX() : !direction.scrollsY()) continue;
            this.updateSnapPoints(horizontal);
            ScrollAreaScroller.Axis axis = this.scroller.axis(horizontal);
            axis.scrollTo(this.snapPoints.nearest(axis.getTarget()), true);
        }
    }

    /**
     * Tells the area that the left mouse button went down on what it shows of its content.
     * From there on, the mouse drags the content along once it moved far enough to not be a click anymore.
     */
    public void onMousePressed(int mouseX, int mouseY) {
        // In the layout editor, dragging moves elements around
        if (!this.area.dragScrolling.getBoolean() || !this.area.userScrolling.getBoolean() || this.area.isInEditor()) return;
        this.mousePressed = true;
        this.dragging = false;
        this.pressX = mouseX;
        this.pressY = mouseY;
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
    }

    public void onMouseReleased() {
        boolean wasDragging = this.dragging;
        this.mousePressed = false;
        this.dragging = false;
        if (wasDragging) this.snapToNearest();
    }

    /**
     * Returns if the mouse drags the content right now. Nothing in the content should react to the mouse while it does.
     */
    public boolean isDragging() {
        return this.dragging;
    }

    private void updateDrag(int mouseX, int mouseY) {
        if (!this.mousePressed || (mouseX == ScrollAreaElement.MOUSE_OUTSIDE) || (mouseY == ScrollAreaElement.MOUSE_OUTSIDE)) return;
        if (!this.dragging) {
            if ((Math.abs(mouseX - this.pressX) < DRAG_START_DISTANCE) && (Math.abs(mouseY - this.pressY) < DRAG_START_DISTANCE)) return;
            this.dragging = true;
            this.autoScroll.onUserScrolled();
        }
        ScrollDirection direction = this.area.getScrollDirection();
        // The content sticks to the mouse, so it moves against the scroll
        if (direction.scrollsX()) this.scroller.x().drift(this.lastDragX - mouseX);
        if (direction.scrollsY()) this.scroller.y().drift(this.lastDragY - mouseY);
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
    }

}
