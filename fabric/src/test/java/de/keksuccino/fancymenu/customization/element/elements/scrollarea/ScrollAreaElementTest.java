package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaAlignment;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaLayoutMode;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaResizeBehavior;
import de.keksuccino.fancymenu.util.properties.Property;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollAreaElementTest {

    private static final ScrollAreaElementBuilder BUILDER = new ScrollAreaElementBuilder();

    /** An area at (100, 50) that shows 200x100 of its content, with the given children. */
    private static TestArea area(TestChild... children) {
        TestArea area = new TestArea();
        area.setInstanceIdentifier("area");
        for (TestChild child : children) {
            child.setParentContainer(area);
        }
        area.setContainerChildren(List.of(children));
        return area;
    }

    private static TestChild child(String identifier, int x, int y, int width, int height) {
        TestChild child = new TestChild();
        child.setInstanceIdentifier(identifier);
        child.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        child.posOffsetX = x;
        child.posOffsetY = y;
        child.baseWidth = width;
        child.baseHeight = height;
        child.stayOnScreen = false;
        return child;
    }

    @Test
    void everySettingHasANameInTheEnglishLanguageFile() throws IOException {
        JsonObject english;
        try (InputStream stream = ScrollAreaElementTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            english = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        ScrollAreaElement area = BUILDER.buildDefaultInstance();
        for (Property<?> property : area.getProperties()) {
            assertTrue(english.has(property.getContextMenuEntryLocalizationKeyBase()), property::getKey);
        }
    }

    @Test
    void settingsSurviveSavingAndLoading() {
        ScrollAreaElement area = BUILDER.buildDefaultInstance();
        area.scrollDirection.set(ScrollDirection.HORIZONTAL);
        area.layoutMode.set(ScrollAreaLayoutMode.GRID);
        area.childAlignment.set(ScrollAreaAlignment.FILL);
        area.resizeBehavior.set(ScrollAreaResizeBehavior.RESIZE);
        area.scrollBarVisibility.set(ScrollBarVisibility.WHILE_SCROLLING);
        area.contentWidth.set(777);
        area.contentHeight.setManualInput("{\"placeholder\":\"some_height\"}");
        area.gridLines.set(5);
        area.childSpacing.set(9);
        area.scrollBarThickness.set(14);
        area.verticalScrollBarLeft.set(true);

        SerializedElement saved = BUILDER.serializeElementInternal(area);
        assertNotNull(saved);
        ScrollAreaElement loaded = BUILDER.deserializeElementInternal(saved);
        assertNotNull(loaded);

        assertEquals(ScrollDirection.HORIZONTAL, loaded.scrollDirection.get());
        assertEquals(ScrollAreaLayoutMode.GRID, loaded.getLayoutMode());
        assertEquals(ScrollAreaAlignment.FILL, loaded.getChildAlignment());
        assertEquals(ScrollAreaResizeBehavior.RESIZE, loaded.resizeBehavior.get());
        assertEquals(ScrollBarVisibility.WHILE_SCROLLING, loaded.scrollBarVisibility.get());
        assertEquals(777, loaded.contentWidth.getInteger());
        assertEquals(area.contentHeight.getManualInput(), loaded.contentHeight.getManualInput());
        assertTrue(ScrollAreaElement.isAdvancedSize(loaded.contentHeight));
        assertFalse(ScrollAreaElement.isAdvancedSize(loaded.contentWidth));
        assertEquals(5, loaded.gridLines.getInteger());
        assertEquals(9, loaded.childSpacing.getInteger());
        assertEquals(14, loaded.scrollBarThickness.getInteger());
        assertTrue(loaded.verticalScrollBarLeft.getBoolean());
        assertTrue(loaded.background.enabled.getBoolean());
    }

    @Test
    void elementsRememberTheirContainerWhenSavedAndLoaded() {
        ScrollAreaElement child = BUILDER.buildDefaultInstance();
        child.setParentContainerIdentifier("outer_area");
        ScrollAreaElement loaded = BUILDER.deserializeElementInternal(BUILDER.serializeElementInternal(child));
        assertNotNull(loaded);
        assertEquals("outer_area", loaded.getParentContainerIdentifier());

        ScrollAreaElement plain = BUILDER.deserializeElementInternal(BUILDER.serializeElementInternal(BUILDER.buildDefaultInstance()));
        assertNotNull(plain);
        assertNull(plain.getParentContainerIdentifier());
    }

    @Test
    void anchoredChildrenSitInTheContentAndFollowTheScroll() {
        TestChild top = child("top", 10, 20, 50, 30);
        TestChild bottom = child("bottom", -60, -40, 50, 30);
        bottom.anchorPoint = ElementAnchorPoints.BOTTOM_RIGHT;
        TestArea area = area(top, bottom);
        area.contentHeight.set(400);
        area.nextFrame();

        assertEquals(200, area.getContentWidth());
        assertEquals(400, area.getContentHeight());
        assertEquals(300, area.getScroller().y().getMax());
        assertEquals(110, top.getAbsoluteX());
        assertEquals(70, top.getAbsoluteY());
        // The bottom right of the content is 400 below its top, no matter how much of it the area shows
        assertEquals(100 + 200 - 60, bottom.getAbsoluteX());
        assertEquals(50 + 400 - 40, bottom.getAbsoluteY());

        area.getScroller().y().scrollTo(120, false);
        area.nextFrame();
        assertEquals(70 - 120, top.getAbsoluteY());
        assertEquals(50 + 400 - 40 - 120, bottom.getAbsoluteY());
    }

    @Test
    void theContentFollowsAnAreaThatChangesItsSizeWhileItIsShown() {
        TestChild bottom = child("bottom", 0, -30, 50, 30);
        bottom.anchorPoint = ElementAnchorPoints.BOTTOM_LEFT;
        TestArea area = area(bottom);
        area.contentHeight.set(400);
        area.nextFrame();
        area.getScroller().y().scrollTo(300, false);
        area.nextFrame();
        assertEquals(50 + 400 - 30 - 300, bottom.getAbsoluteY());

        // The area gets higher: there is less to scroll, and the content stays at its end
        area.height = 250;
        area.nextFrame();
        assertEquals(150, area.getScroller().y().getMax());
        assertEquals(150.0, area.getScroller().y().getPosition());
        assertEquals(50 + 400 - 30 - 150, bottom.getAbsoluteY());

        // The area gets as high as its content: nothing is left to scroll
        area.height = 500;
        area.nextFrame();
        assertEquals(0, area.getScroller().y().getMax());
        assertEquals(500, area.getContentHeight());
        assertEquals(50 + 500 - 30, bottom.getAbsoluteY());

        // The area moves: everything in it moves along
        area.x = 300;
        area.y = 10;
        area.nextFrame();
        assertEquals(300, bottom.getAbsoluteX());
        assertEquals(10 + 500 - 30, bottom.getAbsoluteY());
    }

    @Test
    void aContentSizeThatChangesWhileTheAreaIsShownCounts() {
        TestArea area = area(child("first", 0, 0, 10, 10));
        area.contentHeight.set(300);
        area.nextFrame();
        assertEquals(200, area.getScroller().y().getMax());
        area.contentHeight.set(1000);
        area.nextFrame();
        assertEquals(900, area.getScroller().y().getMax());
        // A content can never be smaller than the area
        area.contentHeight.set(-50);
        area.nextFrame();
        assertEquals(0, area.getScroller().y().getMax());
        assertEquals(100, area.getContentHeight());
    }

    @Test
    void stackedChildrenGetTheirPlacesFromTheArea() {
        TestChild first = child("first", 500, 500, 50, 20);
        TestChild second = child("second", 0, 0, 80, 30);
        TestArea area = area(first, second);
        area.layoutMode.set(ScrollAreaLayoutMode.STACK);
        area.contentPadding.set(5);
        area.childSpacing.set(3);
        area.nextFrame();

        assertTrue(area.managesChildBoxes());
        assertTrue(area.arrangesChildren());
        assertEquals(105, first.getAbsoluteX());
        assertEquals(55, first.getAbsoluteY());
        assertEquals(55 + 20 + 3, second.getAbsoluteY());
        // The children need less than the area shows, and a content is never smaller than its area
        assertEquals(100, area.getContentHeight());
        assertEquals(0, area.getScroller().y().getMax());

        // A child that changes its size while the area is shown moves everything behind it
        first.baseHeight = 60;
        area.nextFrame();
        assertEquals(55 + 60 + 3, second.getAbsoluteY());
        assertEquals(5 + 60 + 3 + 30 + 5, area.getContentHeight());

        // A child that does not show leaves no gap
        first.visible = false;
        area.nextFrame();
        assertEquals(55, second.getAbsoluteY());

        first.visible = true;
        area.childAlignment.set(ScrollAreaAlignment.FILL);
        area.nextFrame();
        assertEquals(190, first.getAbsoluteWidth());
        assertEquals(50, first.getNaturalWidth());
    }

    @Test
    void anAreaThatArrangesItsChildrenOnlyScrollsInOneDirection() {
        TestArea area = area(child("first", 0, 0, 50, 20));
        area.scrollDirection.set(ScrollDirection.BOTH);
        assertEquals(ScrollDirection.BOTH, area.getScrollDirection());
        area.layoutMode.set(ScrollAreaLayoutMode.FLOW);
        assertEquals(ScrollDirection.VERTICAL, area.getScrollDirection());
        // Setting the direction to both goes on to the next one
        area.scrollDirection.set(ScrollDirection.BOTH);
        assertEquals(ScrollDirection.VERTICAL, area.scrollDirection.get());
        area.scrollDirection.set(ScrollDirection.HORIZONTAL);
        assertEquals(ScrollDirection.HORIZONTAL, area.getScrollDirection());
    }

    @Test
    void aProportionalContentScalesWithTheArea() {
        TestChild child = child("child", 10, 20, 40, 30);
        child.anchorPoint = ElementAnchorPoints.BOTTOM_RIGHT;
        TestArea area = area(child);
        area.layoutMode.set(ScrollAreaLayoutMode.PROPORTIONAL);
        area.proportionalReferenceWidth.set(200);
        area.proportionalReferenceHeight.set(100);
        area.contentHeight.set(150);
        area.nextFrame();

        // At the size it was made for, the content shows unscaled, and anchors do not count
        assertEquals(1.0F, area.getContentScale());
        assertEquals(110, child.getAbsoluteX());
        assertEquals(70, child.getAbsoluteY());
        assertEquals(40, child.getAbsoluteWidth());
        assertEquals(50, area.getScroller().y().getMax());
        assertTrue(area.scalesChildVisuals());

        // Twice as wide: everything is twice as big and twice as far from the top left of the content
        area.width = 400;
        area.nextFrame();
        assertEquals(2.0F, area.getContentScale());
        assertEquals(120, child.getAbsoluteX());
        assertEquals(90, child.getAbsoluteY());
        assertEquals(80, child.getAbsoluteWidth());
        assertEquals(60, child.getAbsoluteHeight());
        assertEquals(40, child.getContentSpaceWidth());
        assertEquals(200, area.getContentWidth());
        assertEquals(150, area.getContentHeight());
        // 150 units are 300 pixels now, 200 more than the area shows
        assertEquals(200, area.getScroller().y().getMax());

        area.resizeBehavior.set(ScrollAreaResizeBehavior.RESIZE);
        assertFalse(area.scalesChildVisuals());
        assertEquals(80, child.getAbsoluteWidth());
    }

    @Test
    void theWheelScrollsInTheDirectionsTheAreaScrollsIn() {
        TestArea vertical = area(child("child", 0, 0, 10, 10));
        vertical.contentHeight.set(1000);
        vertical.contentWidth.set(1000);
        vertical.getScroller().setSmooth(false);
        vertical.nextFrame();
        assertTrue(vertical.scrollByWheel(0, -1));
        assertTrue(vertical.getScroller().y().getPosition() > 0);
        assertEquals(0, vertical.getScroller().x().getPosition());
        // Nothing to scroll sideways, so a sideways wheel is for whatever is behind the area
        assertFalse(vertical.scrollByWheel(-1, 0));
        // At the start, scrolling further up is for whatever is behind the area too
        vertical.getScroller().y().scrollTo(0, false);
        assertFalse(vertical.scrollByWheel(0, 1));

        TestArea horizontal = area(child("child", 0, 0, 10, 10));
        horizontal.scrollDirection.set(ScrollDirection.HORIZONTAL);
        horizontal.contentWidth.set(1000);
        horizontal.getScroller().setSmooth(false);
        horizontal.nextFrame();
        // An area that only scrolls sideways takes the normal wheel for it
        assertTrue(horizontal.scrollByWheel(0, -1));
        assertTrue(horizontal.getScroller().x().getPosition() > 0);

        TestArea both = area(child("child", 0, 0, 10, 10));
        both.scrollDirection.set(ScrollDirection.BOTH);
        both.contentWidth.set(1000);
        both.contentHeight.set(1000);
        both.getScroller().setSmooth(false);
        both.nextFrame();
        assertTrue(both.scrollByWheel(0, -1));
        assertTrue(both.getScroller().y().getPosition() > 0);
        assertEquals(0, both.getScroller().x().getPosition());
        both.shiftDown = true;
        assertTrue(both.scrollByWheel(0, -1));
        assertTrue(both.getScroller().x().getPosition() > 0);
        both.shiftDown = false;
        double x = both.getScroller().x().getPosition();
        assertTrue(both.scrollByWheel(-1, 0));
        assertTrue(both.getScroller().x().getPosition() > x);
    }

    @Test
    void onlyWhatTheAreaShowsOfItsContentIsThere() {
        TestChild child = child("child", 0, 80, 50, 60);
        TestArea area = area(child);
        area.contentHeight.set(400);
        area.nextFrame();
        // The child reaches from 130 to 190 on the screen, and the area ends at 150
        assertTrue(area.isScreenPointVisible(120, 140));
        assertFalse(area.isScreenPointVisible(120, 160));
        assertFalse(area.isScreenPointVisible(99, 140));
    }

    @Test
    void fittingTheContentOnlyCountsChildrenThatArePlacedFromItsStart() {
        TestChild top = child("top", 10, 20, 50, 130);
        TestChild bottom = child("bottom", 0, -30, 50, 30);
        bottom.anchorPoint = ElementAnchorPoints.BOTTOM_LEFT;
        TestArea area = area(top, bottom);
        area.contentHeight.set(900);
        area.nextFrame();
        area.fitContentToChildren();
        assertEquals(150, area.contentHeight.getInteger());

        // A size that comes from a placeholder stays as it is
        area.contentHeight.setManualInput("{\"placeholder\":\"some_height\"}");
        area.fitContentToChildren();
        assertTrue(ScrollAreaElement.isAdvancedSize(area.contentHeight));
    }

    /**
     * An area with a box tests set directly, which never asks the game for anything.
     */
    private static final class TestArea extends ScrollAreaElement {

        private int x = 100;
        private int y = 50;
        private int width = 200;
        private int height = 100;
        private boolean shiftDown = false;

        private TestArea() {
            super(BUILDER);
        }

        /** Works the geometry out again and counts as having shown the children, like a frame the area renders in. */
        private void nextFrame() {
            this.lastChildrenShownFrame = de.keksuccino.fancymenu.util.rendering.RenderFrameCounter.getFrame();
            this.updateFrame();
        }

        @Override
        public int getAbsoluteX() {
            return this.x;
        }

        @Override
        public int getAbsoluteY() {
            return this.y;
        }

        @Override
        public int getAbsoluteWidth() {
            return this.width;
        }

        @Override
        public int getAbsoluteHeight() {
            return this.height;
        }

        @Override
        protected double getGuiScale() {
            return 1.0D;
        }

        @Override
        protected boolean isShiftDown() {
            return this.shiftDown;
        }

    }

    private static final class TestChild extends AbstractElement {

        private TestChild() {
            super(null);
        }

        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        /** Auto-sizing asks the game for the size of the screen, which tests don't have. */
        @Override
        public void updateAutoSizing(boolean ignoreLastTickScreenSize) {
        }

    }

}
