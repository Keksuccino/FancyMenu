package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerTestElements.Child;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerTestElements.Container;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementContainerChildTest {

    private static Child child(Container container, int width, int height) {
        Child child = new Child("child");
        child.baseWidth = width;
        child.baseHeight = height;
        child.setParentContainer(container);
        return child;
    }

    @Test
    void anchorsReferToTheContentOfTheContainer() {
        Container container = new Container("area");
        container.contentWidth = 300;
        container.contentHeight = 600;
        container.contentScreenX = 50;
        container.contentScreenY = -120;
        Child child = child(container, 40, 20);

        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = 7;
        child.posOffsetY = 9;
        assertEquals(57, child.getAbsoluteX());
        assertEquals(-111, child.getAbsoluteY());

        // The bottom right of the content is 300x600 away from its top left, no matter how much of it shows
        child.anchorPoint = ElementAnchorPoints.BOTTOM_RIGHT;
        child.posOffsetX = -40;
        child.posOffsetY = -20;
        assertEquals(50 + 300 - 40, child.getAbsoluteX());
        assertEquals(-120 + 600 - 20, child.getAbsoluteY());

        child.anchorPoint = ElementAnchorPoints.MID_CENTERED;
        child.posOffsetX = 0;
        child.posOffsetY = 0;
        assertEquals(50 + 150, child.getAbsoluteX());
        assertEquals(-120 + 300, child.getAbsoluteY());
    }

    @Test
    void childrenFollowTheContentWhenItMoves() {
        Container container = new Container("area");
        Child child = child(container, 40, 20);
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = 10;
        child.posOffsetY = 10;
        assertEquals(10, child.getAbsoluteY());
        container.contentScreenY = -75;
        assertEquals(-65, child.getAbsoluteY());
        container.contentScreenX = 33;
        assertEquals(43, child.getAbsoluteX());
    }

    @Test
    void stickyAnchorsPutTheEdgeOfTheElementOnTheEdgeOfTheContent() {
        Container container = new Container("area");
        container.contentWidth = 300;
        container.contentScreenX = 50;
        Child child = child(container, 40, 20);
        child.stickyAnchor = true;
        child.anchorPoint = ElementAnchorPoints.TOP_RIGHT;
        // The right edge of the element sits on the right edge of the content, which starts at 50 on the screen
        assertEquals(50 + 300 - 40, child.getAbsoluteX());
    }

    @Test
    void stretchedChildrenFillTheContent() {
        Container container = new Container("area");
        container.contentWidth = 300;
        container.contentHeight = 600;
        container.contentScreenX = 20;
        container.contentScreenY = -40;
        Child child = child(container, 40, 20);
        child.posOffsetX = 99;
        child.stretchX.set(true);
        child.stretchY.set(true);
        assertEquals(20, child.getAbsoluteX());
        assertEquals(-40, child.getAbsoluteY());
        assertEquals(300, child.getAbsoluteWidth());
        assertEquals(600, child.getAbsoluteHeight());
    }

    @Test
    void childrenThatStayOnScreenStayInTheContent() {
        Container container = new Container("area");
        container.contentWidth = 300;
        container.contentHeight = 600;
        container.contentScreenX = 1000;
        container.contentScreenY = 1000;
        Child child = child(container, 40, 20);
        child.stayOnScreen = true;
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;

        child.posOffsetX = -500;
        child.posOffsetY = -500;
        int edge = child.getAbsoluteX() - 1000;
        assertTrue((edge >= 0) && (edge < 40));
        assertEquals(edge, child.getAbsoluteY() - 1000);

        child.posOffsetX = 5000;
        child.posOffsetY = 5000;
        assertEquals(1000 + 300 - edge - 40, child.getAbsoluteX());
        assertEquals(1000 + 600 - edge - 20, child.getAbsoluteY());
    }

    @Test
    void containersThatArrangeChildrenDecideWhereTheyAre() {
        Container container = new Container("area");
        container.contentScreenX = 30;
        container.contentScreenY = 40;
        Child child = child(container, 40, 20);
        child.anchorPoint = ElementAnchorPoints.BOTTOM_RIGHT;
        child.posOffsetX = 123;
        child.getContainerLink().setManagedBox(8, 16, 120, 50);

        // The box only counts while the container arranges its children
        assertEquals(40, child.getAbsoluteWidth());

        container.arrangesChildren = true;
        assertEquals(38, child.getAbsoluteX());
        assertEquals(56, child.getAbsoluteY());
        assertEquals(120, child.getAbsoluteWidth());
        assertEquals(50, child.getAbsoluteHeight());
        // What the element is on its own is what the container lays it out with
        assertEquals(40, child.getNaturalWidth());
        assertEquals(20, child.getNaturalHeight());

        container.contentScreenY = -100;
        assertEquals(-84, child.getAbsoluteY());

        child.getContainerLink().clearManagedBox();
        assertEquals(40, child.getAbsoluteWidth());
    }

    @Test
    void askingForTheNaturalSizeLeavesTheElementWithItsBox() {
        Container container = new Container("area");
        container.arrangesChildren = true;
        Child child = child(container, 40, 20);
        child.getContainerLink().setManagedBox(0, 0, 120, 50);
        assertEquals(40, child.getNaturalWidth());
        assertEquals(20, child.getNaturalHeight());
        assertFalse(child.getContainerLink().reportsNaturalSize());
        assertEquals(120, child.getAbsoluteWidth());
        assertEquals(50, child.getAbsoluteHeight());

        // An element that reports its natural size already keeps doing so
        child.getContainerLink().setReportsNaturalSize(true);
        assertEquals(40, child.getNaturalWidth());
        assertEquals(40, child.getAbsoluteWidth());
        assertTrue(child.getContainerLink().reportsNaturalSize());
    }

    @Test
    void elementAnchorsWithoutAnElementStartAtTheContent() {
        Container container = new Container("area");
        container.contentScreenX = 100;
        container.contentScreenY = 50;
        Child anchor = new Child("anchor");
        anchor.setParentContainer(container);
        anchor.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        anchor.posOffsetX = 20;
        anchor.posOffsetY = 30;
        Child child = child(container, 40, 20);
        child.anchorPoint = ElementAnchorPoints.ELEMENT;
        child.setAnchorPointElementIdentifier(null);
        child.posOffsetX = 5;
        child.posOffsetY = 6;

        // Without an anchor element, the element anchor is the top left of the content
        assertEquals(105, child.getAbsoluteX());
        assertEquals(56, child.getAbsoluteY());
    }

    @Test
    void childrenAreOnlyThereWhereTheirContainerShowsThem() {
        Container container = new Container("area");
        container.visibleX = 0;
        container.visibleY = 0;
        container.visibleWidth = 100;
        container.visibleHeight = 100;
        Child child = child(container, 80, 80);
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = 60;
        child.posOffsetY = 60;

        assertTrue(child.containsMousePosition(70, 70));
        // The element reaches to (140, 140), but the container cuts it off at (100, 100)
        assertFalse(child.containsMousePosition(120, 70));
        assertFalse(child.containsMousePosition(70, 120));

        container.showingChildren = false;
        assertFalse(child.containsMousePosition(70, 70));
        assertFalse(child.isShownByContainer());
        assertFalse(child.shouldRender());
    }

    @Test
    void elementsOfTheMainLayoutAreNeverCutOff() {
        Child plain = new Child("plain");
        assertTrue(plain.isScreenPointVisibleInContainer(-5000, 9000));
        assertTrue(plain.isShownByContainer());
    }

    @Test
    void theShownAreaOfAChildIsWhatAllItsContainersLeaveOfIt() {
        Container outer = new Container("outer");
        outer.visibleX = 0;
        outer.visibleY = 0;
        outer.visibleWidth = 90;
        outer.visibleHeight = 200;
        Container inner = new Container("inner");
        inner.setParentContainer(outer);
        inner.visibleX = 20;
        inner.visibleY = 30;
        inner.visibleWidth = 100;
        inner.visibleHeight = 100;
        Child child = child(inner, 80, 80);
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = 60;
        child.posOffsetY = 10;

        // The child spans (60, 10) to (140, 90). The inner container shows it from Y 30 on, and the outer one up to X 90.
        int[] shown = new int[4];
        assertTrue(child.getShownScreenArea(shown));
        assertArrayEquals(new int[] {60, 30, 90, 90}, shown);

        child.posOffsetX = 95;
        assertFalse(child.getShownScreenArea(shown));

        child.posOffsetX = 60;
        inner.showingChildren = false;
        assertFalse(child.getShownScreenArea(shown));
    }

    @Test
    void nothingHoldsAChildInTheContentWhileTheUserMovesIt() {
        Container container = new Container("area");
        container.contentWidth = 300;
        container.contentHeight = 600;
        Child child = child(container, 40, 20);
        child.stayOnScreen = true;
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = 500;
        child.posOffsetY = -70;
        assertTrue(child.getAbsoluteX() < 300);
        assertTrue(child.getAbsoluteY() >= 0);

        child.getContainerLink().setMovedByUser(true);
        assertEquals(500, child.getAbsoluteX());
        assertEquals(-70, child.getAbsoluteY());

        child.getContainerLink().setMovedByUser(false);
        assertTrue(child.getAbsoluteX() < 300);
    }

}
