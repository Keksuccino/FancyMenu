package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.properties.RuntimePropertyContainer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything that scrolls the content of a scrollable element: the mouse wheel, dragging the content, the keyboard, the element itself
 * (when it scrolls on its own or follows the focus), and whatever else asks it to.<br>
 * All of them only move the target of the {@link ContentScroller}, so none of them ever has to wait for another one to finish.
 */
public final class ElementScrolling {

    /** How many screen pixels one step of the mouse wheel scrolls at a scroll speed of 1. */
    private static final double WHEEL_STEP = 30.0D;
    /** How far the mouse has to move with its button held before it drags the content. Less than this is a click. */
    private static final int DRAG_START_DISTANCE = 4;
    /** How much of what the element shows stays in sight when the keyboard scrolls by a page, so the eye keeps its place. */
    private static final int PAGE_OVERLAP = 24;
    /** The room an element that gets scrolled into sight has to the edge of what shows. */
    private static final int INTO_VIEW_MARGIN = 4;
    /** How far the wheel has to turn for content that snaps to move on to the next place it rests at: one step of a wheel that turns in steps. */
    private static final double SNAP_WHEEL_STEP = 1.0D;
    /** How long the wheel can rest before what it turned so far is forgotten, so half a turn does not get finished by the next one much later. */
    private static final long SNAP_WHEEL_REST_NANOS = 400_000_000L;
    /** How many scrolls an element that does not show its content keeps for when it does. More than anything sensible asks for, and an end for something that keeps asking. */
    private static final int MAX_PENDING_SCROLLS = 8;
    /** The longest time one frame moves the content for. Longer frames, like the first one after the game hung, would make the content jump. */
    private static final double MAX_FRAME_SECONDS = 0.1D;
    private static final String MEMORY_SCROLL_X = "scroll_area_scroll_x";
    private static final String MEMORY_SCROLL_Y = "scroll_area_scroll_y";
    /** What the keys end with for an element of the layout editor. Where the user scrolled to while editing is not where the content is on the real screen. */
    private static final String MEMORY_EDITOR_SUFFIX = "_editor";
    /** How often a mouse button went down since the game started, anywhere and no matter what took the press. */
    private static int mousePresses = 0;

    static {
        // Screens only hand keys to the widget that has their focus, so the keys that scroll get listened to for all screens
        EventHandler.INSTANCE.registerListenersOf(new ElementScrollKeyboard());
        // Every press of a mouse button comes in here, also the ones something in the content takes before its element hears of them
        MouseUtil.addClickListener((button, mouseX, mouseY) -> onMousePressedAnywhere());
    }

    @NotNull
    private final ScrollableElement host;
    private final ContentScroller scroller = new ContentScroller();
    private final AutoScroll autoScroll = new AutoScroll();
    private final ScrollSnapPoints snapPoints = new ScrollSnapPoints();
    /** If the content still has to be put where an element that just showed up has it. */
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
    /** If the content is where it starts, so everything that moves it from now on is scrolling. */
    private boolean settled = false;
    private double notifiedX = 0.0D;
    private double notifiedY = 0.0D;
    private boolean notifiedAtStart = false;
    private boolean notifiedAtEnd = false;
    /** What the wheel turned since content that snaps last moved on, per axis. A wheel that turns in small bits, like a touchpad, needs a few of them for that. */
    private double snapWheelX = 0.0D;
    private double snapWheelY = 0.0D;
    private long snapWheelNanos = 0L;
    /** What {@link #mousePresses} was in the last frame. A mouse button that went down since then is what a focus that changed in that time came from. */
    private int mousePressesSeen = mousePresses;
    /** What got asked of the content while the element did not show it. See {@link #whenShowingContent(Runnable)}. */
    @Nullable
    private List<Runnable> pendingScrolls;
    private long lastFrameNanos = 0L;
    private double frameDeltaSeconds = 0.0D;

    public ElementScrolling(@NotNull ScrollableElement host) {
        this.host = host;
    }

    @NotNull
    public ContentScroller getScroller() {
        return this.scroller;
    }

    /**
     * Returns the axis the element mainly scrolls on: the only one it scrolls on, and the vertical one for an element that scrolls both ways.
     */
    @NotNull
    public ContentScroller.Axis getMainAxis() {
        return this.scroller.axis(!this.host.getScrollDirection().scrollsY());
    }

    /**
     * Returns the time the frame before this one took, capped where a longer frame would make things jump.
     * It is how long everything of the element that moves on its own moves for in this frame.
     */
    public double getFrameDeltaSeconds() {
        return this.frameDeltaSeconds;
    }

    /**
     * Puts the content where the element with the identifier of this one was scrolled to the last time. Elements get rebuilt whenever
     * the screen changes its size, and the content stays where it was through that.<br>
     * What elements remember lasts until the game closes, so this is also what keeps the content in place between two times
     * a screen gets opened, for elements that do not go back to their start position then.
     */
    public void restoreFromMemory() {
        RuntimePropertyContainer memory = this.host.getScrollElement().getMemory();
        Double scrollX = memory.getProperty(this.memoryKey(MEMORY_SCROLL_X), Double.class);
        Double scrollY = memory.getProperty(this.memoryKey(MEMORY_SCROLL_Y), Double.class);
        if ((scrollX == null) && (scrollY == null)) return;
        if (scrollX != null) this.scroller.x().restore(scrollX);
        if (scrollY != null) this.scroller.y().restore(scrollY);
        this.rememberedX = this.scroller.x().getTarget();
        this.rememberedY = this.scroller.y().getTarget();
        this.needsStartPosition = false;
    }

    /**
     * Puts the content back where an element that just showed up has it.
     */
    public void resetToStartPosition() {
        this.scroller.x().restore(0.0D);
        this.scroller.y().restore(0.0D);
        this.needsStartPosition = true;
        this.settled = false;
    }

    /**
     * Moves the content for one frame, and tells the geometry where that takes it.
     * Has to be called after the geometry got the size of the content for this frame.
     *
     * @param mouseOver If the mouse is over what the element shows of its content.
     */
    public void update(@NotNull ScrollGeometry geometry, boolean mouseOver) {

        long now = this.host.getNanoTime();
        double deltaSeconds = (this.lastFrameNanos == 0L) ? 0.0D : Math.min(MAX_FRAME_SECONDS, (now - this.lastFrameNanos) / 1.0E9D);
        this.lastFrameNanos = now;
        this.frameDeltaSeconds = deltaSeconds;

        ScrollSettings settings = this.host.getScrollSettings();
        this.scroller.setSmooth(settings.smoothScrolling.getBoolean());
        this.scroller.setRange(geometry.getMaxScrollX(), geometry.getMaxScrollY());

        // The end of a content is only known once there is something to scroll, which can take a few frames for content that still loads
        if (this.needsStartPosition) {
            if (settings.startPosition.tryGetNonNullElse(ScrollStartPosition.START) == ScrollStartPosition.START) {
                this.needsStartPosition = false;
            } else if (this.getMainAxis().canScroll()) {
                this.getMainAxis().scrollTo(this.getMainAxis().getMax(), false);
                this.needsStartPosition = false;
            }
        }

        this.runPendingScrolls();

        this.updateDrag();

        boolean userHoldsContent = this.dragging || this.host.getScrollBars().isGrabbed();
        if (settings.autoScroll.getBoolean() && !this.host.isInEditor() && !userHoldsContent && !(mouseOver && settings.autoScrollPauseOnHover.getBoolean())) {
            this.autoScroll.update(this.getMainAxis(), deltaSeconds, settings.autoScrollSpeed.getFloat(), settings.autoScrollLoop.getBoolean());
        }

        this.followFocus();

        this.scroller.update(deltaSeconds);
        geometry.setScroll(this.scroller.x().getPosition(), this.scroller.y().getPosition());
        this.remember();
        this.notifyHost();

    }

    /**
     * Tells the element when its content moved since the last frame, and when it got to its start or its end.<br>
     * Where the content is when the element shows up is where it starts and not somewhere it got scrolled to, so that tells nothing.
     */
    private void notifyHost() {
        double scrollX = this.scroller.x().getPosition();
        double scrollY = this.scroller.y().getPosition();
        ContentScroller.Axis main = this.getMainAxis();
        // Content that still glides the last bit of its way has not arrived yet
        boolean arrived = !main.isScrolling();
        boolean atStart = arrived && main.isAtStart();
        boolean atEnd = arrived && main.isAtEnd();
        if (this.settled) {
            if ((scrollX != this.notifiedX) || (scrollY != this.notifiedY)) this.host.onContentScrolled();
            // Content that can't be scrolled is at its start and its end at once, which is nothing it ever gets to. It stays known as being
            // at both, so content that grows under an element that sits at its start does not count as having got there.
            if (main.canScroll()) {
                if (atStart && !this.notifiedAtStart) this.host.onContentReachedStart();
                if (atEnd && !this.notifiedAtEnd) this.host.onContentReachedEnd();
            }
        }
        this.settled = !this.needsStartPosition;
        this.notifiedX = scrollX;
        this.notifiedY = scrollY;
        this.notifiedAtStart = atStart;
        this.notifiedAtEnd = atEnd;
    }

    private void remember() {
        double scrollX = this.scroller.x().getTarget();
        double scrollY = this.scroller.y().getTarget();
        if ((scrollX == this.rememberedX) && (scrollY == this.rememberedY)) return;
        this.rememberedX = scrollX;
        this.rememberedY = scrollY;
        RuntimePropertyContainer memory = this.host.getScrollElement().getMemory();
        memory.putProperty(this.memoryKey(MEMORY_SCROLL_X), scrollX);
        memory.putProperty(this.memoryKey(MEMORY_SCROLL_Y), scrollY);
    }

    @NotNull
    private String memoryKey(@NotNull String key) {
        return this.host.isInEditor() ? (key + MEMORY_EDITOR_SUFFIX) : key;
    }

    /**
     * Scrolls the content with the mouse wheel.<br>
     * An element that only scrolls sideways takes the normal wheel for it. An element that scrolls both ways scrolls up and down
     * with the wheel and sideways with a sideways wheel or with Shift held.
     *
     * @return If the content has somewhere to go in the scrolled direction. If not, the scroll is for whatever is behind the element.
     */
    public boolean scrollByWheel(double deltaX, double deltaY) {
        if (!this.host.getScrollSettings().userScrolling.getBoolean()) return false;
        ScrollDirection direction = this.host.getScrollDirection();
        boolean scrolled;
        if (direction == ScrollDirection.VERTICAL) {
            scrolled = this.scrollAxisByWheel(false, deltaY);
        } else if (direction == ScrollDirection.HORIZONTAL) {
            scrolled = this.scrollAxisByWheel(true, (deltaX != 0.0D) ? deltaX : deltaY);
        } else if (this.host.isShiftDown() && (deltaX == 0.0D)) {
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
        ContentScroller.Axis axis = this.scroller.axis(horizontal);
        if (!this.snaps(horizontal)) return axis.scrollBy(-delta * WHEEL_STEP * Math.max(0.0F, this.host.getScrollSettings().scrollSpeed.getFloat()));
        // A wheel that turns towards the user scrolls the content forwards
        boolean forward = delta < 0.0D;
        this.updateSnapPoints(horizontal);
        double next = this.snapPoints.next(axis.getTarget(), forward);
        boolean hasNext = next != axis.getTarget();
        long now = this.host.getNanoTime();
        double turned = ((now - this.snapWheelNanos) > SNAP_WHEEL_REST_NANOS) ? 0.0D : (horizontal ? this.snapWheelX : this.snapWheelY);
        this.snapWheelNanos = now;
        // A wheel that changed its direction starts over
        if ((turned != 0.0D) && ((turned < 0.0D) != forward)) turned = 0.0D;
        turned += delta;
        // One step of a wheel that turns in steps is as much as the game is set to make of it, which can be less than 1
        boolean movesOn = hasNext && (Math.abs(turned) >= (SNAP_WHEEL_STEP * Math.min(1.0D, this.host.getWheelStepSize())));
        if (movesOn || !hasNext) turned = 0.0D;
        if (horizontal) {
            this.snapWheelX = turned;
        } else {
            this.snapWheelY = turned;
        }
        if (movesOn) axis.scrollTo(next, true);
        // A wheel that has not turned far enough yet is still on its way to the next place, so the scroll is for the element
        return hasNext;
    }

    /**
     * Scrolls the content by about what the element shows of it, on the axis the element mainly scrolls on.
     *
     * @return If the content has somewhere to go in that direction.
     */
    public boolean scrollByPage(boolean forward) {
        if (!this.host.getScrollSettings().userScrolling.getBoolean()) return false;
        boolean horizontal = !this.host.getScrollDirection().scrollsY();
        ContentScroller.Axis axis = this.scroller.axis(horizontal);
        ScrollGeometry geometry = this.host.getScrollGeometry();
        int viewportSize = horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight();
        double distance = Math.max(PAGE_OVERLAP, viewportSize - PAGE_OVERLAP);
        double target = axis.getTarget() + (forward ? distance : -distance);
        if (this.snaps(horizontal)) {
            this.updateSnapPoints(horizontal);
            double snapped = this.snapPoints.nearest(target);
            // A page that is shorter than the way to the next place the content rests at would leave the content where it is, or take it back
            boolean movesOn = forward ? (snapped > axis.getTarget()) : (snapped < axis.getTarget());
            target = movesOn ? snapped : this.snapPoints.next(axis.getTarget(), forward);
        }
        boolean scrolled = axis.scrollTo(target, true);
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    /**
     * Scrolls the content to its start or its end, on the axis the element mainly scrolls on.
     *
     * @return If the content was not there already.
     */
    public boolean scrollToEdge(boolean end, boolean animate) {
        ContentScroller.Axis axis = this.getMainAxis();
        boolean scrolled = axis.scrollTo(end ? axis.getMax() : 0.0D, animate);
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    /**
     * Scrolls the content to where it starts or where it ends, on every axis the element scrolls on: its top left or its bottom right
     * for an element that scrolls both ways.<br>
     * An element that does not show its content yet does it once it does.
     */
    public void scrollToCorner(boolean end, boolean animate) {
        if (!this.host.isShowingScrollContent()) {
            this.whenShowingContent(() -> this.scrollToCorner(end, animate));
            return;
        }
        ScrollDirection direction = this.host.getScrollDirection();
        if (direction.scrollsX()) this.scrollToPosition(true, end ? this.scroller.x().getMax() : 0.0D, animate);
        if (direction.scrollsY()) this.scrollToPosition(false, end ? this.scroller.y().getMax() : 0.0D, animate);
    }

    /**
     * Scrolls the content to a position on one axis, in screen pixels from its start.<br>
     * An element that does not show its content yet does it once it does.
     *
     * @return If the content was not there already, or on its way there.
     */
    public boolean scrollToPosition(boolean horizontal, double position, boolean animate) {
        if (!this.host.isShowingScrollContent()) {
            this.whenShowingContent(() -> this.scrollToPosition(horizontal, position, animate));
            return true;
        }
        boolean scrolled = this.scroller.axis(horizontal).scrollTo(position, animate);
        if (scrolled) this.autoScroll.onUserScrolled();
        return scrolled;
    }

    /**
     * Runs a scroll right away if the element shows its content, and keeps it for when it does otherwise.<br>
     * An element that never showed its content does not know how far it can be scrolled, or where things are in it, so everything
     * that got asked of it would end at its start. That happens to everything that scrolls an element when its screen opens.
     */
    public void whenShowingContent(@NotNull Runnable scroll) {
        if (this.host.isShowingScrollContent()) {
            scroll.run();
            return;
        }
        if (this.pendingScrolls == null) this.pendingScrolls = new ArrayList<>();
        if (this.pendingScrolls.size() >= MAX_PENDING_SCROLLS) this.pendingScrolls.remove(0);
        this.pendingScrolls.add(scroll);
    }

    private void runPendingScrolls() {
        if ((this.pendingScrolls == null) || !this.host.isShowingScrollContent()) return;
        List<Runnable> pending = this.pendingScrolls;
        this.pendingScrolls = null;
        for (int i = 0; i < pending.size(); i++) {
            pending.get(i).run();
        }
    }

    /**
     * Scrolls the content so that the child starts where what the element shows starts, or as close to that as the content can be scrolled.<br>
     * An element that does not show its content yet does it once it does.
     */
    public void scrollToChild(@NotNull AbstractElement child, boolean animate) {
        if (!this.host.isShowingScrollContent()) {
            this.whenShowingContent(() -> this.scrollToChild(child, animate));
            return;
        }
        ScrollGeometry geometry = this.host.getScrollGeometry();
        ScrollDirection direction = this.host.getScrollDirection();
        // How far the child is from the start of the content is how far the content has to be scrolled to have it at the start of what the element shows
        if (direction.scrollsX()) this.scrollToPosition(true, child.getAbsoluteX() - geometry.getContentScreenX() - INTO_VIEW_MARGIN, animate);
        if (direction.scrollsY()) this.scrollToPosition(false, child.getAbsoluteY() - geometry.getContentScreenY() - INTO_VIEW_MARGIN, animate);
    }

    /**
     * Scrolls the content so that all of the child shows, or as much of it as fits, starting at its start.
     * A child that shows completely already leaves the content where it is.
     */
    public void scrollIntoView(@NotNull AbstractElement child, boolean animate) {
        ScrollGeometry geometry = this.host.getScrollGeometry();
        ScrollDirection direction = this.host.getScrollDirection();
        if (direction.scrollsX()) this.scrollIntoView(this.scroller.x(), child.getAbsoluteX() - geometry.getContentScreenX(), child.getAbsoluteWidth(), geometry.getViewportX() - geometry.getContentScreenX(), geometry.getViewportWidth(), animate);
        if (direction.scrollsY()) this.scrollIntoView(this.scroller.y(), child.getAbsoluteY() - geometry.getContentScreenY(), child.getAbsoluteHeight(), geometry.getViewportY() - geometry.getContentScreenY(), geometry.getViewportHeight(), animate);
    }

    /**
     * @param childStart How far the child is from the start of the content, in screen pixels.
     * @param shownStart How far what the element shows is from the start of the content, in screen pixels.
     */
    private void scrollIntoView(@NotNull ContentScroller.Axis axis, int childStart, int childSize, int shownStart, int shownSize, boolean animate) {
        int wantedStart = shownStart + INTO_VIEW_MARGIN;
        int wantedEnd = (shownStart + shownSize) - INTO_VIEW_MARGIN;
        if ((childStart < wantedStart) || (childSize > (wantedEnd - wantedStart))) {
            axis.scrollTo(childStart - INTO_VIEW_MARGIN, animate);
        } else if ((childStart + childSize) > wantedEnd) {
            axis.scrollTo(((childStart + childSize) - shownSize) + INTO_VIEW_MARGIN, animate);
        }
    }

    /**
     * Scrolls the child the keyboard just put its focus on into sight. Only a focus that changed scrolls,
     * so the user can scroll away from a focused child without getting pulled back.
     */
    private void followFocus() {
        boolean byMouse = mousePresses != this.mousePressesSeen;
        this.mousePressesSeen = mousePresses;
        GuiEventListener focused = this.host.getFocusedListener();
        if (focused == this.lastFocused) return;
        this.lastFocused = focused;
        // What the mouse gave the focus to is where the user clicked, and content that moves under the mouse would take what it does next somewhere else
        if ((focused == null) || byMouse) return;
        AbstractElement child = this.host.findChildOwning(focused);
        if (child != null) this.scrollIntoView(child, true);
    }

    /**
     * Returns if the content comes to rest at certain places on the given axis, instead of wherever it got scrolled to.
     */
    private boolean snaps(boolean horizontal) {
        ScrollSnapMode mode = this.host.getScrollSettings().getSnapMode();
        if (mode == ScrollSnapMode.NONE) return false;
        return (mode == ScrollSnapMode.PAGES) || this.host.snapsOnAxis(horizontal);
    }

    /**
     * Works out where the content comes to rest on an axis, for the mode the element snaps in.
     */
    private void updateSnapPoints(boolean horizontal) {
        ScrollGeometry geometry = this.host.getScrollGeometry();
        ContentScroller.Axis axis = this.scroller.axis(horizontal);
        this.snapPoints.clear();
        if (this.host.getScrollSettings().getSnapMode() == ScrollSnapMode.PAGES) {
            this.snapPoints.addPages(horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight(), axis.getMax());
        } else {
            this.host.addSnapPoints(this.snapPoints, horizontal);
        }
        this.snapPoints.finish(axis.getMax());
    }

    /**
     * Lets the content come to rest where the element snaps, after something moved it freely.
     */
    public void snapToNearest() {
        ScrollDirection direction = this.host.getScrollDirection();
        for (int i = 0; i < 2; i++) {
            boolean horizontal = i == 0;
            if ((horizontal ? !direction.scrollsX() : !direction.scrollsY()) || !this.snaps(horizontal)) continue;
            this.updateSnapPoints(horizontal);
            ContentScroller.Axis axis = this.scroller.axis(horizontal);
            axis.scrollTo(this.snapPoints.nearest(axis.getTarget()), true);
        }
    }

    /**
     * Tells all scrollable elements that a mouse button went down, no matter where and what took the press.
     */
    public static void onMousePressedAnywhere() {
        mousePresses++;
    }

    /**
     * Returns if the mouse can drag the content along: the element is set to it, and its content has somewhere to go.
     */
    public boolean canDragContent() {
        ScrollSettings settings = this.host.getScrollSettings();
        // In the layout editor, dragging moves elements around
        if (!settings.dragScrolling.getBoolean() || !settings.userScrolling.getBoolean() || this.host.isInEditor()) return false;
        return this.scroller.x().canScroll() || this.scroller.y().canScroll();
    }

    /**
     * Tells the scrolling that the left mouse button went down on what the element shows of its content.
     * From there on, the mouse drags the content along once it moved far enough to not be a click anymore.
     *
     * @param mouseX Where the mouse is, in the space the content scrolls in.
     * @return If the press is for the element, because the mouse can drag its content.
     */
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!this.canDragContent()) return false;
        this.mousePressed = true;
        this.dragging = false;
        this.pressX = mouseX;
        this.pressY = mouseY;
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
        return true;
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

    private void updateDrag() {
        if (!this.mousePressed) return;
        // Some screens keep the release of the mouse to themselves, like the ones with an inventory, and the content would stick to the mouse forever.
        // So the scrolling does not wait to be told, and looks if the button is still held.
        if (!this.host.isPointerPressed()) {
            this.onMouseReleased();
            return;
        }
        // The content follows the mouse where it really is. What the element gets told while it renders is somewhere else
        // while something keeps the mouse away from it, like a container around it the mouse is not over.
        int pointerX = this.host.getPointerX();
        int pointerY = this.host.getPointerY();
        int mouseX = (int) Math.floor(this.host.toScrollSpaceX(pointerX, pointerY));
        int mouseY = (int) Math.floor(this.host.toScrollSpaceY(pointerX, pointerY));
        if (!this.dragging) {
            if ((Math.abs(mouseX - this.pressX) < DRAG_START_DISTANCE) && (Math.abs(mouseY - this.pressY) < DRAG_START_DISTANCE)) return;
            this.dragging = true;
            this.autoScroll.onUserScrolled();
        }
        ScrollDirection direction = this.host.getScrollDirection();
        // The content sticks to the mouse, so it moves against the scroll
        if (direction.scrollsX()) this.scroller.x().drift(this.lastDragX - mouseX);
        if (direction.scrollsY()) this.scroller.y().drift(this.lastDragY - mouseY);
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
    }

}
