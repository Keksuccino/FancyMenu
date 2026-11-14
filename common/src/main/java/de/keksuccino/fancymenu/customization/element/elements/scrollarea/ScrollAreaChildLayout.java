package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerLink;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaArrangement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaArranger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Gives the children of a scrollable area the boxes an arranger works out for them.<br>
 * It collects what the arranger needs to know about the area and its children in every frame, which is how it notices that
 * something changed. Only then the arranger runs and the children get new boxes, so an area that stays the same costs next to nothing.
 */
public final class ScrollAreaChildLayout {

    private final ScrollAreaArrangement arrangement = new ScrollAreaArrangement();
    /** The children the arrangement is about, in its order. Children that do not show have no place in an arrangement. */
    private final List<AbstractElement> arranged = new ArrayList<>();
    private int signature = 0;
    private boolean hasArrangement = false;
    /** If children of the area have a box from here, which has to be taken from them when the area stops arranging them. */
    private boolean childrenHaveBoxes = false;

    /**
     * Arranges the children of the area. Has to be called after the geometry of the area got its viewport for this frame.
     */
    public void arrange(@NotNull ScrollAreaElement area, @NotNull ScrollAreaArranger arranger, @NotNull List<AbstractElement> children, @NotNull ScrollAreaGeometry geometry) {

        this.arranged.clear();
        for (int i = 0; i < children.size(); i++) {
            AbstractElement child = children.get(i);
            if (area.takesPartInArrangement(child)) {
                this.arranged.add(child);
            } else if (child.getContainerLink() != null) {
                child.getContainerLink().clearManagedBox();
            }
        }

        ScrollAreaArrangement arrangement = this.arrangement;
        int count = this.arranged.size();
        float scale = geometry.getScale();
        int viewportWidth = geometry.getViewportContentWidth();
        int viewportHeight = geometry.getViewportContentHeight();
        boolean horizontal = area.getScrollDirection() == ScrollDirection.HORIZONTAL;
        int padding = Math.max(0, area.contentPadding.getInteger());
        int spacing = Math.max(0, area.childSpacing.getInteger());
        int gridLines = Math.max(1, area.gridLines.getInteger());
        boolean gridAutoFit = area.gridAutoFit.getBoolean();
        int gridMinCellSize = Math.max(1, area.gridMinCellSize.getInteger());

        // Every value the arrangement depends on goes into the signature, so the same signature means the same boxes
        int signature = System.identityHashCode(arranger);
        signature = (31 * signature) + count;
        signature = (31 * signature) + Float.floatToIntBits(scale);
        signature = (31 * signature) + viewportWidth;
        signature = (31 * signature) + viewportHeight;
        signature = (31 * signature) + (horizontal ? 1 : 0);
        signature = (31 * signature) + padding;
        signature = (31 * signature) + spacing;
        signature = (31 * signature) + area.getChildAlignment().ordinal();
        signature = (31 * signature) + gridLines;
        signature = (31 * signature) + (gridAutoFit ? 1 : 0);
        signature = (31 * signature) + gridMinCellSize;

        // The arrays of the arrangement keep what they held as long as they are big enough, and the same children fill them with the same values
        boolean sameCount = this.hasArrangement && (arrangement.count == count);
        if (!sameCount) arrangement.prepare(count);
        for (int i = 0; i < count; i++) {
            AbstractElement child = this.arranged.get(i);
            int naturalWidth = child.getNaturalWidth();
            int naturalHeight = child.getNaturalHeight();
            arrangement.naturalWidth[i] = naturalWidth;
            arrangement.naturalHeight[i] = naturalHeight;
            arrangement.offsetX[i] = child.posOffsetX;
            arrangement.offsetY[i] = child.posOffsetY;
            signature = (31 * signature) + System.identityHashCode(child);
            signature = (31 * signature) + naturalWidth;
            signature = (31 * signature) + naturalHeight;
            signature = (31 * signature) + child.posOffsetX;
            signature = (31 * signature) + child.posOffsetY;
            // A child that lost its box, like one that just got put into the area, needs a new one
            ElementContainerLink link = child.getContainerLink();
            if ((link == null) || !link.hasManagedBox()) signature++;
        }

        if (this.hasArrangement && (signature == this.signature)) return;

        arrangement.viewportWidth = viewportWidth;
        arrangement.viewportHeight = viewportHeight;
        arrangement.horizontal = horizontal;
        arrangement.padding = padding;
        arrangement.spacing = spacing;
        arrangement.alignment = area.getChildAlignment();
        arrangement.gridLines = gridLines;
        arrangement.gridAutoFit = gridAutoFit;
        arrangement.gridMinCellSize = gridMinCellSize;
        arranger.arrange(arrangement);

        for (int i = 0; i < count; i++) {
            ElementContainerLink link = this.arranged.get(i).getContainerLink();
            if (link == null) continue;
            // Both edges of a box get rounded on their own, so boxes that touch in the content also touch on the screen
            int left = Math.round(arrangement.x[i] * scale);
            int top = Math.round(arrangement.y[i] * scale);
            int right = Math.round((arrangement.x[i] + arrangement.width[i]) * scale);
            int bottom = Math.round((arrangement.y[i] + arrangement.height[i]) * scale);
            link.setManagedBox(left, top, right - left, bottom - top, arrangement.width[i], arrangement.height[i]);
        }

        this.signature = signature;
        this.hasArrangement = true;
        this.childrenHaveBoxes = true;

    }

    /**
     * Takes the boxes away from the children again, for an area that leaves their place to them from now on.
     */
    public void clear(@NotNull List<AbstractElement> children) {
        this.hasArrangement = false;
        if (!this.childrenHaveBoxes) return;
        this.childrenHaveBoxes = false;
        for (int i = 0; i < children.size(); i++) {
            ElementContainerLink link = children.get(i).getContainerLink();
            if (link != null) link.clearManagedBox();
        }
    }

    /**
     * Makes the next {@link #arrange(ScrollAreaElement, ScrollAreaArranger, List, ScrollAreaGeometry)} work everything out again.
     */
    public void invalidate() {
        this.hasArrangement = false;
    }

    /**
     * Returns the width the content needs for the arranged children, or -1 if its size is up to the user.
     */
    public int getContentWidth() {
        return this.hasArrangement ? this.arrangement.contentWidth : -1;
    }

    /**
     * Returns the height the content needs for the arranged children, or -1 if its size is up to the user.
     */
    public int getContentHeight() {
        return this.hasArrangement ? this.arrangement.contentHeight : -1;
    }

}
