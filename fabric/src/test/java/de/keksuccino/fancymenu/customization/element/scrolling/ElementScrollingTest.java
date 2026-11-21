package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.style.ScrollBarTrackBackground;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scrolling of an element that is no Scrollable Area: content of its own that only has places to rest at on one axis, like a text with its lines.
 */
class ElementScrollingTest {

    /** A scrollable that shows 200x100 of a content of 600x1000, and scrolls both ways. */
    private static TestScrollable scrollable() {
        TestScrollable scrollable = new TestScrollable();
        scrollable.setInstanceIdentifier("scrollable");
        scrollable.settings.smoothScrolling.set(false);
        scrollable.nextFrame();
        return scrollable;
    }

    @Test
    void theGeometryGetsToldWhereTheContentIsEveryFrame() {
        TestScrollable scrollable = scrollable();
        assertEquals(900, scrollable.getScroller().y().getMax());
        assertEquals(400, scrollable.getScroller().x().getMax());
        scrollable.getScrolling().scrollToPosition(false, 120, false);
        scrollable.getScrolling().scrollToPosition(true, 30, false);
        scrollable.nextFrame();
        assertEquals(50 - 120, scrollable.geometry.getContentScreenY());
        assertEquals(100 - 30, scrollable.geometry.getContentScreenX());
    }

    @Test
    void contentThatOnlyRestsOnOneAxisScrollsFreelyOnTheOther() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.snapMode.set(ScrollSnapMode.LINES);
        scrollable.snapPoints.addAll(List.of(0.0, 40.0, 95.0, 300.0));

        // Up and down, the wheel moves on to the next place the content rests at
        assertTrue(scrollable.getScrolling().scrollByWheel(0, -1));
        assertEquals(40.0, scrollable.getScroller().y().getPosition());
        assertTrue(scrollable.getScrolling().scrollByWheel(0, -1));
        assertEquals(95.0, scrollable.getScroller().y().getPosition());

        // Sideways there is nothing to rest at, so the wheel scrolls as far as the scroll speed says
        scrollable.settings.scrollSpeed.set(1.0F);
        assertTrue(scrollable.getScrolling().scrollByWheel(-1, 0));
        double oneStep = scrollable.getScroller().x().getPosition();
        assertTrue((oneStep > 0.0) && (oneStep < 400.0));
        scrollable.settings.scrollSpeed.set(2.0F);
        assertTrue(scrollable.getScrolling().scrollByWheel(-1, 0));
        assertEquals(oneStep * 3.0, scrollable.getScroller().x().getPosition());
        assertEquals(95.0, scrollable.getScroller().y().getPosition());
    }

    @Test
    void contentThatGotMovedFreelyOnlyComesToRestOnTheAxisItRestsOn() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.snapMode.set(ScrollSnapMode.HEADLINES);
        scrollable.snapPoints.addAll(List.of(0.0, 100.0, 500.0));
        scrollable.getScroller().y().scrollTo(130, false);
        scrollable.getScroller().x().scrollTo(77, false);
        scrollable.getScrolling().snapToNearest();
        assertEquals(100.0, scrollable.getScroller().y().getPosition());
        assertEquals(77.0, scrollable.getScroller().x().getPosition());
    }

    @Test
    void pagesAreTheSameForEveryContent() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.snapMode.set(ScrollSnapMode.PAGES);
        // Pages never ask the element for the places in its content, and both axes rest at them
        assertTrue(scrollable.getScrolling().scrollByWheel(0, -1));
        assertEquals(100.0, scrollable.getScroller().y().getPosition());
        scrollable.shiftDown = true;
        assertTrue(scrollable.getScrolling().scrollByWheel(0, -1));
        assertEquals(200.0, scrollable.getScroller().x().getPosition());
        assertEquals(0, scrollable.snapPointRequests);
    }

    @Test
    void theKeyboardPagesToThePlacesTheContentRestsAt() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.snapMode.set(ScrollSnapMode.LINES);
        scrollable.snapPoints.addAll(List.of(0.0, 20.0, 60.0, 90.0, 400.0));
        assertTrue(scrollable.getScrolling().scrollByPage(true));
        // A page is a bit less than what shows, and the content rests at the place closest to that
        assertEquals(90.0, scrollable.getScroller().y().getPosition());
        assertTrue(scrollable.getScrolling().scrollByPage(false));
        assertEquals(20.0, scrollable.getScroller().y().getPosition());
    }

    @Test
    void whatGetsAskedBeforeTheContentShowsHappensOnceItDoes() {
        TestScrollable scrollable = new TestScrollable();
        scrollable.setInstanceIdentifier("hidden");
        scrollable.settings.smoothScrolling.set(false);
        List<String> ran = new ArrayList<>();

        scrollable.getScrolling().whenShowingContent(() -> ran.add("first"));
        scrollable.getScrolling().whenShowingContent(() -> scrollable.getScrolling().scrollToPosition(false, 250, false));
        assertTrue(ran.isEmpty());
        assertEquals(0.0, scrollable.getScroller().y().getPosition());

        scrollable.nextFrame();
        assertEquals(List.of("first"), ran);
        assertEquals(250.0, scrollable.getScroller().y().getPosition());

        // An element that shows its content does it right away
        scrollable.getScrolling().whenShowingContent(() -> ran.add("second"));
        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    void theElementHearsWhenItsContentMovesAndWhenItGetsToAnEnd() {
        TestScrollable scrollable = scrollable();
        scrollable.nextFrame();
        assertEquals("0/0/0", scrollable.events());
        scrollable.getScrolling().scrollByWheel(0, -1);
        scrollable.nextFrame();
        assertEquals("1/0/0", scrollable.events());
        scrollable.getScrolling().scrollToEdge(true, false);
        scrollable.nextFrame();
        assertEquals("2/0/1", scrollable.events());
        scrollable.getScrolling().scrollToEdge(false, false);
        scrollable.nextFrame();
        assertEquals("3/1/1", scrollable.events());
    }

    @Test
    void draggingMovesTheContentAlongWithTheMouseInTheSpaceItScrollsIn() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.dragScrolling.set(true);
        // The space the content scrolls in is half as big as the screen, like in a container that scales the element
        scrollable.spaceScale = 0.5;
        scrollable.pointerX = 200;
        scrollable.pointerY = 200;
        assertTrue(scrollable.getScrolling().onMousePressed(100, 100));
        scrollable.pointerY = 100;
        scrollable.nextFrame();
        assertTrue(scrollable.getScrolling().isDragging());
        // The mouse moved up by 100 on the screen, which is 50 for the content
        assertEquals(50.0, scrollable.getScroller().y().getPosition());
        scrollable.pointerPressed = false;
        scrollable.nextFrame();
        assertFalse(scrollable.getScrolling().isDragging());
    }

    @Test
    void theContentDoesNotGetDraggedInTheEditorOrWhenPlayersCanNotScroll() {
        TestScrollable scrollable = scrollable();
        assertFalse(scrollable.getScrolling().canDragContent());
        scrollable.settings.dragScrolling.set(true);
        assertTrue(scrollable.getScrolling().canDragContent());
        scrollable.inEditor = true;
        assertFalse(scrollable.getScrolling().canDragContent());
        scrollable.inEditor = false;
        scrollable.settings.userScrolling.set(false);
        assertFalse(scrollable.getScrolling().canDragContent());
        assertFalse(scrollable.getScrolling().scrollByWheel(0, -1));
        assertFalse(scrollable.getScrolling().scrollByPage(true));
    }

    @Test
    void theContentStartsAtItsEndOnceThereIsSomethingToScroll() {
        TestScrollable scrollable = new TestScrollable();
        scrollable.setInstanceIdentifier("log");
        scrollable.settings.startPosition.set(ScrollStartPosition.END);
        // Content that still loads is as big as what shows of it
        scrollable.contentHeight = 100;
        scrollable.nextFrame();
        assertEquals(0.0, scrollable.getScroller().y().getPosition());
        scrollable.contentHeight = 1000;
        scrollable.nextFrame();
        assertEquals(900.0, scrollable.getScroller().y().getPosition());
        // Getting there is no scrolling, so nothing hears of it
        assertEquals("0/0/0", scrollable.events());
    }

    @Test
    void theContentScrollsOnItsOwnAndRestsAfterTheUserScrolled() {
        TestScrollable scrollable = scrollable();
        scrollable.settings.autoScroll.set(true);
        scrollable.settings.autoScrollSpeed.set(100.0F);
        for (int i = 0; i < 40; i++) scrollable.nextFrame();
        double moved = scrollable.getScroller().y().getPosition();
        assertTrue(moved > 0.0);

        // In the editor, nothing moves on its own
        scrollable.inEditor = true;
        for (int i = 0; i < 40; i++) scrollable.nextFrame();
        assertEquals(moved, scrollable.getScroller().y().getPosition());
        scrollable.inEditor = false;

        scrollable.getScrolling().scrollToPosition(false, 10, false);
        scrollable.nextFrame();
        assertEquals(10.0, scrollable.getScroller().y().getPosition());
    }

    /**
     * An element that scrolls content of its own, with places to rest at up and down only.
     */
    private static final class TestScrollable extends AbstractElement implements ScrollableElement {

        private final ScrollSettings settings = new ScrollSettings(this, "fancymenu.elements.text", ScrollDirection.BOTH, EnumSet.of(ScrollSnapMode.NONE, ScrollSnapMode.LINES, ScrollSnapMode.HEADLINES, ScrollSnapMode.PAGES));
        private final ElementScrolling scrolling = new ElementScrolling(this);
        private final ScrollGeometry geometry = new ScrollGeometry();
        private final ElementScrollBars bars = new ElementScrollBars(this, this.scrolling.getScroller());
        private final ScrollBarTrackBackground barBackground = new ScrollBarTrackBackground(this);
        private final List<Double> snapPoints = new ArrayList<>();
        private int snapPointRequests = 0;
        private int contentWidth = 600;
        private int contentHeight = 1000;
        private boolean showing = false;
        private boolean inEditor = false;
        private boolean shiftDown = false;
        private boolean pointerPressed = true;
        private int pointerX = 0;
        private int pointerY = 0;
        /** How many units of the space the content scrolls in one pixel of the screen is. */
        private double spaceScale = 1.0;
        private long nanoTime = 1_000_000_000L;
        private int scrolledEvents = 0;
        private int reachedStartEvents = 0;
        private int reachedEndEvents = 0;

        private TestScrollable() {
            super(null);
        }

        /** Works the geometry out again and counts as having shown the content, like a frame the element renders in. */
        private void nextFrame() {
            this.showing = true;
            // 50 milliseconds per frame
            this.nanoTime += 50_000_000L;
            ScrollDirection direction = this.getScrollDirection();
            this.geometry.setViewport(100, 50, 200, 100);
            this.geometry.setContentSize(this.contentWidth, this.contentHeight, direction.scrollsX(), direction.scrollsY());
            this.scrolling.update(this.geometry, false);
        }

        /** How often the content moved, got to its start and got to its end. */
        private String events() {
            return this.scrolledEvents + "/" + this.reachedStartEvents + "/" + this.reachedEndEvents;
        }

        @Override
        public @NotNull AbstractElement getScrollElement() {
            return this;
        }

        @Override
        public @NotNull ScrollSettings getScrollSettings() {
            return this.settings;
        }

        @Override
        public @NotNull ElementScrolling getScrolling() {
            return this.scrolling;
        }

        @Override
        public @NotNull ElementScrollBars getScrollBars() {
            return this.bars;
        }

        @Override
        public @NotNull ScrollGeometry getScrollGeometry() {
            return this.geometry;
        }

        @Override
        public @NotNull ScrollDirection getScrollDirection() {
            return this.settings.scrollDirection.tryGetNonNullElse(ScrollDirection.BOTH);
        }

        @Override
        public boolean isShowingScrollContent() {
            return this.showing;
        }

        @Override
        public int getScrollBarDistance() {
            return 0;
        }

        @Override
        public @NotNull ScrollBarTrackBackground getScrollBarBackground() {
            return this.barBackground;
        }

        @Override
        public void applyScrollGrabberLooks(@NotNull ScrollBar vertical, @NotNull ScrollBar horizontal, float opacity) {
        }

        @Override
        public void addSnapPoints(@NotNull ScrollSnapPoints points, boolean horizontal) {
            this.snapPointRequests++;
            if (horizontal) return;
            for (double point : this.snapPoints) points.add(point);
        }

        @Override
        public boolean snapsOnAxis(boolean horizontal) {
            return !horizontal;
        }

        @Override
        public boolean takesContentPressAt(double screenX, double screenY) {
            return false;
        }

        @Override
        public double toScrollSpaceX(double screenX, double screenY) {
            return screenX * this.spaceScale;
        }

        @Override
        public double toScrollSpaceY(double screenX, double screenY) {
            return screenY * this.spaceScale;
        }

        @Override
        public int getPointerX() {
            return this.pointerX;
        }

        @Override
        public int getPointerY() {
            return this.pointerY;
        }

        @Override
        public boolean isPointerPressed() {
            return this.pointerPressed;
        }

        @Override
        public double getWheelStepSize() {
            return 1.0D;
        }

        @Override
        public boolean isShiftDown() {
            return this.shiftDown;
        }

        @Override
        public long getNanoTime() {
            return this.nanoTime;
        }

        @Override
        public boolean isInEditor() {
            return this.inEditor;
        }

        @Override
        public GuiEventListener getFocusedListener() {
            return null;
        }

        @Override
        public void onContentScrolled() {
            this.scrolledEvents++;
        }

        @Override
        public void onContentReachedStart() {
            this.reachedStartEvents++;
        }

        @Override
        public void onContentReachedEnd() {
            this.reachedEndEvents++;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

    }

}
