package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorPointOverlayContainerTest {

    /** A container that shows (100, 50) to (300, 150) of a content of 200x600, scrolled down by 200. */
    private static TestContainer scrolledContainer() {
        TestContainer container = new TestContainer();
        container.viewportX = 100;
        container.viewportY = 50;
        container.viewportWidth = 200;
        container.viewportHeight = 100;
        container.contentScreenX = 100;
        container.contentScreenY = -150;
        container.contentWidth = 200;
        container.contentHeight = 600;
        return container;
    }

    private static AnchorPointOverlay overlayFor(@NotNull TestContainer container) throws ReflectiveOperationException {
        // The editor screen needs a running game to get built, and the areas of a container never ask it for anything
        AnchorPointOverlay overlay = new AnchorPointOverlay((LayoutEditorScreen) loadUnsafe().allocateInstance(LayoutEditorScreen.class));
        overlay.anchorSpace = container;
        overlay.anchorSpaceEditorElement = container;
        overlay.updateAreaBounds();
        return overlay;
    }

    private static int[] bounds(@NotNull AnchorPointOverlay.AnchorPointArea area) {
        return new int[] { area.getX(), area.getY(), area.getWidth(), area.getHeight() };
    }

    @Test
    void theAreasOfAContainerSitAtTheEdgesAndInTheMiddleOfItsWholeContent() throws ReflectiveOperationException {
        AnchorPointOverlay overlay = overlayFor(scrolledContainer());
        // What the container shows is 100 high, which makes the areas 20 big
        assertArrayEquals(new int[] { 100, -150, 20, 20 }, bounds(overlay.topLeftArea));
        assertArrayEquals(new int[] { 190, -150, 20, 20 }, bounds(overlay.topCenteredArea));
        assertArrayEquals(new int[] { 280, -150, 20, 20 }, bounds(overlay.topRightArea));
        // The middle and the bottom of the content are 300 and 600 below its top, which is 200 above what the container shows
        assertArrayEquals(new int[] { 100, 140, 20, 20 }, bounds(overlay.midLeftArea));
        assertArrayEquals(new int[] { 190, 140, 20, 20 }, bounds(overlay.midCenteredArea));
        assertArrayEquals(new int[] { 280, 430, 20, 20 }, bounds(overlay.bottomRightArea));
        assertArrayEquals(new int[] { 100, 430, 20, 20 }, bounds(overlay.bottomLeftArea));
    }

    @Test
    void onlyThePartOfTheAreasTheContainerShowsCanBeHovered() throws ReflectiveOperationException {
        AnchorPointOverlay overlay = overlayFor(scrolledContainer());
        assertTrue(overlay.anchorSpaceShown);
        assertArrayEquals(new int[] { 100, 50, 300, 150 }, overlay.shownAnchorSpace);
        // The middle areas are in sight, the bottom ones are scrolled out of it
        assertTrue(overlay.midLeftArea.isMouseOver(105, 145));
        assertTrue(overlay.isShownInAnchorSpace(105, 145));
        assertTrue(overlay.bottomLeftArea.isMouseOver(105, 435));
        assertFalse(overlay.isShownInAnchorSpace(105, 435));
    }

    @Test
    void theAreasFollowTheContentWhenItScrolls() throws ReflectiveOperationException {
        TestContainer container = scrolledContainer();
        // Scrolled to the end, the bottom of the content is the bottom of what the container shows
        container.contentScreenY = 50 - 500;
        AnchorPointOverlay overlay = overlayFor(container);
        assertEquals(150 - 20, overlay.bottomLeftArea.getY());
        assertTrue(overlay.isShownInAnchorSpace(105, 135));
    }

    @Test
    void noAreaShowsForAContainerThatDoesNotShowItsContent() throws ReflectiveOperationException {
        TestContainer container = scrolledContainer();
        container.showingChildren = false;
        AnchorPointOverlay overlay = overlayFor(container);
        assertFalse(overlay.anchorSpaceShown);
    }

    @Test
    void theAreasOfASmallContainerKeepASizeTheyCanBeHitWith() throws ReflectiveOperationException {
        TestContainer container = scrolledContainer();
        container.viewportWidth = 20;
        container.viewportHeight = 20;
        AnchorPointOverlay overlay = overlayFor(container);
        assertEquals(6, overlay.topLeftArea.getWidth());
    }

    private static Unsafe loadUnsafe() throws ReflectiveOperationException {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    /** A container with a geometry tests set directly, which is its own editor element. */
    private static class TestContainer implements ElementContainer, ContainerEditorElement {

        int viewportX;
        int viewportY;
        int viewportWidth;
        int viewportHeight;
        int contentScreenX;
        int contentScreenY;
        int contentWidth;
        int contentHeight;
        boolean showingChildren = true;

        @Override
        public @NotNull AbstractElement getContainerElement() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContainerChildren(@NotNull List<? extends AbstractElement> children) {
        }

        @Override
        public int getContentWidth() {
            return this.contentWidth;
        }

        @Override
        public int getContentHeight() {
            return this.contentHeight;
        }

        @Override
        public int getContentScreenX() {
            return this.contentScreenX;
        }

        @Override
        public int getContentScreenY() {
            return this.contentScreenY;
        }

        @Override
        public boolean arrangesChildren() {
            return false;
        }

        @Override
        public void refreshChildBoxes() {
        }

        @Override
        public boolean isScreenPointVisible(double screenX, double screenY) {
            return (screenX >= this.viewportX) && (screenX < (this.viewportX + this.viewportWidth)) && (screenY >= this.viewportY) && (screenY < (this.viewportY + this.viewportHeight));
        }

        @Override
        public boolean clipToShownArea(int @NotNull [] area) {
            area[0] = Math.max(area[0], this.viewportX);
            area[1] = Math.max(area[1], this.viewportY);
            area[2] = Math.min(area[2], this.viewportX + this.viewportWidth);
            area[3] = Math.min(area[3], this.viewportY + this.viewportHeight);
            return (area[0] < area[2]) && (area[1] < area[3]);
        }

        @Override
        public boolean isShowingChildren() {
            return this.showingChildren;
        }

        @Override
        public float getChildOpacity() {
            return 1.0F;
        }

        @Override
        public boolean isCollapsedInLayerList() {
            return false;
        }

        @Override
        public void setCollapsedInLayerList(boolean collapsed) {
        }

        @Override
        public void setChildEditorElements(@NotNull List<AbstractEditorElement<?, ?>> children) {
        }

        @Override
        public boolean canHoldChild(@NotNull AbstractEditorElement<?, ?> element) {
            return true;
        }

        @Override
        public int getDropIndex(int mouseX, int mouseY, @NotNull List<AbstractEditorElement<?, ?>> dragged) {
            return -1;
        }

        @Override
        public boolean getDropMarker(int dropIndex, @NotNull List<AbstractEditorElement<?, ?>> dragged, int @NotNull [] marker) {
            return false;
        }

        @Override
        public @NotNull List<AbstractEditorElement<?, ?>> getChildEditorElements() {
            return List.of();
        }

        @Override
        public int getDropAreaX() {
            return this.viewportX;
        }

        @Override
        public int getDropAreaY() {
            return this.viewportY;
        }

        @Override
        public int getDropAreaWidth() {
            return this.viewportWidth;
        }

        @Override
        public int getDropAreaHeight() {
            return this.viewportHeight;
        }

    }

}
