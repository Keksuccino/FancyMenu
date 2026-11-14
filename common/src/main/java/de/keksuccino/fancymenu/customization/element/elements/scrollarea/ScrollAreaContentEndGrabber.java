package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The handle the layout editor shows at the end of the content of a selected scrollable area, on one axis.
 * Dragging it makes the content longer or shorter, and holding it beyond the area scrolls along, so the content can grow as far as needed.<br><br>
 *
 * The editor asks it for every click before it does anything with it on its own. A click the handle takes
 * never reaches the area behind it, which would start moving the area around otherwise.
 */
public class ScrollAreaContentEndGrabber implements GuiEventListener {

    private static final int GRABBER_LENGTH = 26;
    private static final int GRABBER_THICKNESS = 6;
    /** How far the handle sits inside of the end it marks, which keeps it clear of the resize zone at the edge of the area. */
    private static final int GRABBER_INSET = 5;
    private static final int GRAB_MARGIN = 2;
    /** How close to an edge of the area the mouse has to be for the content to scroll along while the handle is held. */
    private static final int AUTO_SCROLL_ZONE = 10;
    /** How many pixels per second the content scrolls along for every pixel the mouse is into the zone, and the fastest it gets. */
    private static final double AUTO_SCROLL_SPEED = 14.0D;
    private static final double AUTO_SCROLL_MAX_SPEED = 900.0D;

    @NotNull
    protected final ScrollAreaEditorElement editorElement;
    protected final boolean horizontal;
    protected boolean hovered = false;
    protected boolean grabbed = false;
    /** How far the mouse was from the end of the content when it took the handle, so the end does not jump to the mouse. */
    protected int grabOffset = 0;
    /** Where the mouse took the handle, on the axis of the handle. */
    protected int grabMouse = 0;
    protected int sizeOnGrab = 0;
    @Nullable
    protected LayoutEditorHistory.Snapshot snapshotOnGrab;

    /**
     * @param horizontal If the handle is for the width of the content, at its right end. Otherwise it is for the height, at its bottom end.
     */
    public ScrollAreaContentEndGrabber(@NotNull ScrollAreaEditorElement editorElement, boolean horizontal) {
        this.editorElement = editorElement;
        this.horizontal = horizontal;
    }

    /**
     * Renders the line that marks the end of the content and the handle on it, and moves the end while the handle is held.
     */
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (!this.isShown()) {
            this.hovered = false;
            if (this.grabbed) this.release();
            return;
        }

        ScrollAreaElement area = this.editorElement.element;
        if (this.grabbed) this.dragTo(this.horizontal ? mouseX : mouseY);

        ScrollAreaGeometry geometry = area.getGeometry();
        int end = this.getEndOnScreen(geometry);
        int viewportStart = this.horizontal ? geometry.getViewportX() : geometry.getViewportY();
        int viewportEnd = viewportStart + (this.horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight());
        if (this.grabbed) {
            // The content grows a frame before the area scrolls along, and the handle the user holds stays in sight all the time
            end = Math.max(viewportStart, Math.min(viewportEnd, end));
        } else if ((end < viewportStart) || (end > viewportEnd)) {
            // The end of the content only shows while the area is scrolled to it
            this.hovered = false;
            return;
        }

        int crossStart = this.horizontal ? geometry.getViewportY() : geometry.getViewportX();
        int crossLength = this.horizontal ? geometry.getViewportHeight() : geometry.getViewportWidth();
        boolean locked = this.isLocked();
        int color = UIBase.getUITheme().layout_editor_element_border_color_selected.getColorInt();
        int lineColor = RenderingUtils.replaceAlphaInColor(color, locked ? 90 : 200);

        if (this.horizontal) {
            graphics.fill(end - 1, crossStart, end, crossStart + crossLength, lineColor);
        } else {
            graphics.fill(crossStart, end - 1, crossStart + crossLength, end, lineColor);
        }

        // A size that comes from a placeholder can't be dragged, so its line has no handle
        if (locked || (crossLength < (GRABBER_LENGTH + 8))) {
            this.hovered = false;
            RenderingUtils.resetShaderColor(graphics);
            return;
        }

        int grabberX = this.getGrabberX(geometry, end);
        int grabberY = this.getGrabberY(geometry, end);
        int grabberWidth = this.horizontal ? GRABBER_THICKNESS : GRABBER_LENGTH;
        int grabberHeight = this.horizontal ? GRABBER_LENGTH : GRABBER_THICKNESS;
        this.hovered = (mouseX >= (grabberX - GRAB_MARGIN)) && (mouseX < (grabberX + grabberWidth + GRAB_MARGIN)) && (mouseY >= (grabberY - GRAB_MARGIN)) && (mouseY < (grabberY + grabberHeight + GRAB_MARGIN));
        boolean active = this.hovered || this.grabbed;
        float radius = Math.min(UIBase.getInterfaceCornerRoundingRadius(), GRABBER_THICKNESS / 2.0F);
        UIBase.renderRoundedRect(graphics, grabberX, grabberY, grabberWidth, grabberHeight, radius, radius, radius, radius, RenderingUtils.replaceAlphaInColor(color, active ? 255 : 200));
        RenderingUtils.resetShaderColor(graphics);

        if (active) CursorHandler.setClientTickCursor(this.horizontal ? CursorHandler.CURSOR_RESIZE_HORIZONTAL : CursorHandler.CURSOR_RESIZE_VERTICAL);

    }

    /**
     * Puts the end of the content where the mouse is, and scrolls the content along while the mouse is at or beyond an edge of the area.
     */
    protected void dragTo(int mouse) {

        ScrollAreaElement area = this.editorElement.element;
        ScrollAreaGeometry geometry = area.getGeometry();
        ScrollAreaScroller.Axis axis = area.getScroller().axis(this.horizontal);
        int viewportStart = this.horizontal ? geometry.getViewportX() : geometry.getViewportY();
        int viewportEnd = viewportStart + (this.horizontal ? geometry.getViewportWidth() : geometry.getViewportHeight());

        // The handle moves while its area renders, so the frame of the area is its frame
        double deltaSeconds = area.getFrameDeltaSeconds();

        // How far the content scrolls along in this frame: forwards while the mouse is at the end of the area, backwards while it is at its start.
        // The handle rests close to the end of the area when the area is scrolled to the end of its content, so only a mouse that
        // moved on from where it took the handle scrolls along. Holding the handle still changes nothing.
        double scrollStep = 0.0D;
        if ((mouse > (viewportEnd - AUTO_SCROLL_ZONE)) && (mouse > this.grabMouse)) {
            scrollStep = Math.min(AUTO_SCROLL_MAX_SPEED, (mouse - (viewportEnd - AUTO_SCROLL_ZONE)) * AUTO_SCROLL_SPEED) * deltaSeconds;
        } else if ((mouse < (viewportStart + AUTO_SCROLL_ZONE)) && (mouse < this.grabMouse)) {
            scrollStep = -Math.min(AUTO_SCROLL_MAX_SPEED, ((viewportStart + AUTO_SCROLL_ZONE) - mouse) * AUTO_SCROLL_SPEED) * deltaSeconds;
        }

        // The end follows the mouse within the area, and moves on by what the content scrolls along, so it stays under the mouse while the content grows
        int target = Math.max(viewportStart, Math.min(viewportEnd, mouse)) + this.grabOffset;
        double contentStart = viewportStart - axis.getPosition();
        int size = (int) Math.round(((target - contentStart) + scrollStep) / geometry.getScale());
        int minSize = Math.round((viewportEnd - viewportStart) / geometry.getScale());
        this.getSizeProperty().set(Math.max(minSize, size));
        // The range of the scroll follows the new size in the next frame, where what could not be scrolled now gets caught up
        if (scrollStep != 0.0D) axis.drift(scrollStep);

    }

    protected void release() {
        if (!this.grabbed) return;
        this.grabbed = false;
        LayoutEditorHistory history = this.editorElement.editor.history;
        if ((this.snapshotOnGrab != null) && (this.getSizeProperty().getInteger() != this.sizeOnGrab)) {
            history.saveSnapshot(this.snapshotOnGrab);
        } else {
            history.discardSnapshot(this.snapshotOnGrab);
        }
        this.snapshotOnGrab = null;
    }

    /**
     * Returns if the area has an end to show on the axis of this handle: it is the one selected element, it scrolls on the axis,
     * and its content is as big as the user sets it.
     */
    protected boolean isShown() {
        ScrollAreaElement area = this.editorElement.element;
        if (!this.editorElement.isSelected() || this.editorElement.isMultiSelected() || area.layerHiddenInEditor) return false;
        if (!area.hasUserContentSize()) return false;
        return this.horizontal ? area.getScrollDirection().scrollsX() : area.getScrollDirection().scrollsY();
    }

    /**
     * Returns if the size comes from something the user typed that is no plain number, like a placeholder, which dragging would throw away.
     */
    protected boolean isLocked() {
        return ScrollAreaElement.isAdvancedSize(this.getSizeProperty());
    }

    @NotNull
    protected Property.IntegerProperty getSizeProperty() {
        ScrollAreaElement area = this.editorElement.element;
        return this.horizontal ? area.contentWidth : area.contentHeight;
    }

    protected int getEndOnScreen(@NotNull ScrollAreaGeometry geometry) {
        if (this.horizontal) return geometry.getContentScreenX() + Math.round(geometry.getContentWidth() * geometry.getScale());
        return geometry.getContentScreenY() + Math.round(geometry.getContentHeight() * geometry.getScale());
    }

    protected int getGrabberX(@NotNull ScrollAreaGeometry geometry, int end) {
        if (this.horizontal) return end - GRABBER_INSET - GRABBER_THICKNESS;
        return geometry.getViewportX() + ((geometry.getViewportWidth() - GRABBER_LENGTH) / 2);
    }

    protected int getGrabberY(@NotNull ScrollAreaGeometry geometry, int end) {
        if (this.horizontal) return geometry.getViewportY() + ((geometry.getViewportHeight() - GRABBER_LENGTH) / 2);
        return end - GRABBER_INSET - GRABBER_THICKNESS;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        if ((event.button() != InputConstants.MOUSE_BUTTON_LEFT) || !this.hovered || !this.isShown() || this.isLocked()) return false;
        ScrollAreaGeometry geometry = this.editorElement.element.getGeometry();
        this.grabbed = true;
        this.grabMouse = (int) (this.horizontal ? event.x() : event.y());
        this.grabOffset = this.getEndOnScreen(geometry) - this.grabMouse;
        this.sizeOnGrab = this.getSizeProperty().getInteger();
        this.snapshotOnGrab = this.editorElement.editor.history.createSnapshot();
        return true;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) this.release();
        return false;
    }

    /**
     * The handle follows the mouse while it renders, so the content keeps scrolling along when the mouse is held still beyond the area.
     */
    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        return this.grabbed;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

}
