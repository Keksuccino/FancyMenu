package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarTrackBackground;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.OnScrollAreaEventListener;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An element that shows content in an area of its own and scrolls it: a Scrollable Area with the elements in it, or a Text with its text.<br>
 * The element owns everything that scrolls its content ({@link ElementScrolling}, {@link ElementScrollBars}, {@link ElementScrollInput})
 * and tells them here what they can't know: what the content is, where it shows and what is in it.<br><br>
 *
 * The content scrolls in the space the element renders it in. For an element that can be rotated or tilted,
 * that is not the screen, so everything that comes from the screen goes through {@link #toScrollSpaceX(double, double)} first.
 */
public interface ScrollableElement {

    /**
     * Returns the element this is.
     */
    @NotNull
    AbstractElement getScrollElement();

    @NotNull
    ScrollSettings getScrollSettings();

    @NotNull
    ElementScrolling getScrolling();

    /**
     * Returns where the content is, where it is scrolling to and how it gets there.
     */
    @NotNull
    default ContentScroller getScroller() {
        return this.getScrolling().getScroller();
    }

    @NotNull
    ElementScrollBars getScrollBars();

    /**
     * Returns the geometry the content has in this frame. An element that did not render yet works it out for the answer.
     */
    @NotNull
    ScrollGeometry getScrollGeometry();

    /**
     * Returns the direction the content scrolls in, which can be another one than the element is set to where it has no use for that one.
     */
    @NotNull
    ScrollDirection getScrollDirection();

    /**
     * Returns if the element shows its content right now. An element that never showed it does not know how big it is,
     * so everything that scrolls to a place in the content waits until it does.
     */
    boolean isShowingScrollContent();

    /**
     * Returns how far the scroll bars stand away from what the element shows of its content, like behind the edge of a background around it.
     */
    int getScrollBarDistance();

    @NotNull
    ScrollBarTrackBackground getScrollBarBackground();

    /**
     * Gives the scroll bars the look of their grabbers for this frame.
     *
     * @param opacity The opacity the bars have, between 0 and 1.
     */
    void applyScrollGrabberLooks(@NotNull ScrollBar vertical, @NotNull ScrollBar horizontal, float opacity);

    /**
     * Adds the places the content comes to rest at on one axis, in screen pixels from its start, for the mode the element snaps in.
     * Only gets asked for modes that snap to something in the content. Pages are the same for every content.
     */
    void addSnapPoints(@NotNull ScrollSnapPoints points, boolean horizontal);

    /**
     * Returns if the content comes to rest at certain places on the given axis, for the mode the element snaps in.
     * Content that has nothing to snap to on an axis scrolls freely on it.
     */
    default boolean snapsOnAxis(boolean horizontal) {
        return true;
    }

    /**
     * Returns if something in the content takes a press of the mouse at the given position on the screen, like a button or a link.
     * A press there is a click for that, and does not start dragging the content.
     */
    boolean takesContentPressAt(double screenX, double screenY);

    /**
     * Offers a scroll of the mouse wheel to what is in the content under the mouse and scrolls on its own.
     *
     * @return If something in the content took the scroll.
     */
    default boolean scrollContentByWheel(double screenX, double screenY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Returns the element in the content the given widget belongs to, or NULL if it belongs to nothing in the content.
     * The content follows the focus of the keyboard to the elements in it.
     */
    @Nullable
    default AbstractElement findChildOwning(@NotNull GuiEventListener widget) {
        return null;
    }

    /**
     * Returns if the keys that scroll the element under the mouse are for this element. An element that takes no input at all takes none of them.
     */
    default boolean takesScrollKeys() {
        return true;
    }

    /**
     * Scrolls to an element in the content. Does nothing for content that holds no elements, and for an element that is not in it.
     */
    default void scrollToElement(@NotNull AbstractElement element, boolean animate) {
    }

    /**
     * Scrolls to a line of the text in the content. Does nothing for content that is no text.
     *
     * @param lineNumber The number of the line in the text, starting at 1.
     */
    default void scrollToLine(int lineNumber, boolean animate) {
    }

    /**
     * Scrolls to the first line of the text in the content that holds the given text. Does nothing for content that is no text.
     */
    default void scrollToText(@NotNull String text, boolean animate) {
    }

    /**
     * Maps a point on the screen to the X position it has in the space the content scrolls in.
     */
    default double toScrollSpaceX(double screenX, double screenY) {
        return this.getScrollElement().untransformMouseX(screenX, screenY);
    }

    /** The Y counterpart of {@link #toScrollSpaceX(double, double)}. */
    default double toScrollSpaceY(double screenX, double screenY) {
        return this.getScrollElement().untransformMouseY(screenX, screenY);
    }

    /**
     * Returns if the point on the screen is in what the element shows of its content, where nothing cuts the element off.
     */
    default boolean isScrollPointVisible(double screenX, double screenY) {
        return this.getScrollGeometry().isInViewport(this.toScrollSpaceX(screenX, screenY), this.toScrollSpaceY(screenX, screenY)) && this.getScrollElement().isScreenPointVisibleInContainer(screenX, screenY);
    }

    /**
     * Returns if the mouse is over what the element shows of its content right now.
     */
    default boolean isPointerOverContent() {
        return this.isScrollPointVisible(this.getPointerX(), this.getPointerY());
    }

    /**
     * Returns where the mouse really is on the screen. What an element gets told while it renders is somewhere else
     * while something keeps the mouse away from it, like a container around it the mouse is not over.
     */
    default int getPointerX() {
        Minecraft minecraft = Minecraft.getInstance();
        return (int) Math.floor(minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
    }

    /** The Y counterpart of {@link #getPointerX()}. */
    default int getPointerY() {
        Minecraft minecraft = Minecraft.getInstance();
        return (int) Math.floor(minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
    }

    /**
     * Returns if the left mouse button is held right now, no matter what took its press.
     */
    default boolean isPointerPressed() {
        return MouseUtil.isLeftMouseDown();
    }

    /**
     * Returns how far one step of a mouse wheel that turns in steps counts as turned, which is what the game is set to make of it.
     */
    default double getWheelStepSize() {
        return Minecraft.getInstance().options.mouseWheelSensitivity().get();
    }

    default boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    /**
     * Returns the time everything of the element that moves on its own measures its frames with.
     */
    default long getNanoTime() {
        return System.nanoTime();
    }

    /**
     * Returns if the element is one of the layout editor. Nothing there happens on a real screen: the content does not scroll on its own,
     * nothing listens to it, and dragging moves elements around.
     */
    boolean isInEditor();

    /**
     * Returns what has the focus of the screen right now, which the keyboard moves from widget to widget.
     */
    @Nullable
    default GuiEventListener getFocusedListener() {
        Screen screen = ScreenUtils.getScreen();
        return (screen != null) ? screen.getFocused() : null;
    }

    /**
     * Gets called in every frame the content is somewhere else than in the frame before.
     */
    default void onContentScrolled() {
        this.notifyScrollListener(Listeners.ON_SCROLL_AREA_SCROLLED);
    }

    /**
     * Gets called when the content got to its start, in the direction the element mainly scrolls in.
     */
    default void onContentReachedStart() {
        this.notifyScrollListener(Listeners.ON_SCROLL_AREA_REACHED_START);
    }

    /**
     * Gets called when the content got to its end, in the direction the element mainly scrolls in.
     */
    default void onContentReachedEnd() {
        this.notifyScrollListener(Listeners.ON_SCROLL_AREA_REACHED_END);
    }

    private void notifyScrollListener(@NotNull OnScrollAreaEventListener listener) {
        // Where the user scrolls to while editing is nothing that happens on the real screen
        if (this.isInEditor() || !listener.hasInstancesListening()) return;
        ElementScrolling scrolling = this.getScrolling();
        ContentScroller scroller = scrolling.getScroller();
        listener.onScrollAreaEvent(this.getScrollElement().getInstanceIdentifier(), (int) Math.round(scroller.x().getPosition()), (int) Math.round(scroller.y().getPosition()), (float) scrolling.getMainAxis().getPercent());
    }

}
