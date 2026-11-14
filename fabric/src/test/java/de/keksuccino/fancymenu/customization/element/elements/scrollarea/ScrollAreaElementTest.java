package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerLink;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaAlignment;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaLayoutMode;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaResizeBehavior;
import de.keksuccino.fancymenu.util.properties.Property;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void aStretchedChildOfAnAreaThatArrangesItsChildrenKeepsItsOwnSize() {
        TestChild child = child("child", 0, 0, 50, 20);
        child.stretchY.set(true);
        TestArea area = area(child);
        area.contentHeight.set(300);
        area.nextFrame();
        // Where the user places the children, a stretched child is as big as the content
        assertTrue(child.isStretchedY());
        assertEquals(300, child.getAbsoluteHeight());

        // Arranged content is as big as its children, so a child that is as big as the content would make it grow with every frame
        area.layoutMode.set(ScrollAreaLayoutMode.STACK);
        area.nextFrame();
        area.nextFrame();
        area.nextFrame();
        assertFalse(child.isStretchedY());
        assertTrue(child.isArrangedByContainer());
        assertEquals(20, child.getAbsoluteHeight());
        assertEquals(100, area.getContentHeight());
    }

    @Test
    void aStackedChildThatIsWiderThanTheAreaEndsWhereTheAreaEnds() {
        TestChild wide = child("wide", 0, 0, 300, 20);
        TestArea area = area(wide);
        area.layoutMode.set(ScrollAreaLayoutMode.STACK);
        area.contentPadding.set(5);
        area.nextFrame();
        assertEquals(190, wide.getAbsoluteWidth());
        assertEquals(105, wide.getAbsoluteX());
    }

    @Test
    void childrenThatFlowHaveNothingToFill() {
        TestArea area = area(child("first", 0, 0, 50, 20));
        area.layoutMode.set(ScrollAreaLayoutMode.STACK);
        area.childAlignment.set(ScrollAreaAlignment.FILL);
        assertEquals(ScrollAreaAlignment.FILL, area.getChildAlignment());
        area.layoutMode.set(ScrollAreaLayoutMode.FLOW);
        assertEquals(ScrollAreaAlignment.START, area.getChildAlignment());
        // Setting the alignment to fill goes on to the next one
        area.childAlignment.set(ScrollAreaAlignment.FILL);
        assertEquals(ScrollAreaAlignment.START, area.childAlignment.get());
        area.childAlignment.set(ScrollAreaAlignment.END);
        assertEquals(ScrollAreaAlignment.END, area.getChildAlignment());
    }

    @Test
    void theMouseOfAScaledChildIsInItWhereItIsOnTheScreen() {
        // Without rounding, the mouse maps by the scale, around where the child starts
        assertEquals(149, ScrollAreaElement.toUnscaledSpace(174, 100, 75, 50, 1.5F));
        assertEquals(150, ScrollAreaElement.toUnscaledSpace(175, 100, 75, 50, 1.5F));
        assertEquals(98, ScrollAreaElement.toUnscaledSpace(97, 100, 75, 50, 1.5F));
        // Three units at a scale of 1.4 are four pixels on the screen. The pixel next to them would map into the child.
        assertEquals(103, ScrollAreaElement.toUnscaledSpace(104, 100, 4, 3, 1.4F));
        // A child that got a pixel more on the screen than its units make would have its last pixel map next to it
        assertEquals(102, ScrollAreaElement.toUnscaledSpace(104, 100, 5, 3, 1.25F));
    }

    @Test
    void whatAScaledChildGetsCutOffOfIsKnownInTheSpaceItRendersIn() {
        TestChild child = child("child", 10, 40, 40, 30);
        TestArea area = area(child);
        area.layoutMode.set(ScrollAreaLayoutMode.PROPORTIONAL);
        area.proportionalReferenceWidth.set(200);
        area.proportionalReferenceHeight.set(100);
        area.contentHeight.set(300);
        area.width = 400;
        area.nextFrame();
        // At twice its size, the child spans (120, 130) to (200, 190) on the screen, and the area shows what is above 150
        assertEquals(130, child.getAbsoluteY());
        assertEquals(60, child.getAbsoluteHeight());
        assertTrue(child.isRenderSpacePointVisibleInContainer(125, 145));
        assertFalse(child.isRenderSpacePointVisibleInContainer(125, 155));

        // While it renders, the child is in the unscaled space, where it spans (120, 130) to (160, 160). A point 15 units into it is 30 pixels into it on the screen.
        ElementContainerLink link = child.getContainerLink();
        assertNotNull(link);
        ElementContainerLink.SizeSpace previous = link.setSizeSpace(ElementContainerLink.SizeSpace.CONTENT);
        assertTrue(child.isRenderSpacePointVisibleInContainer(125, 135));
        assertFalse(child.isRenderSpacePointVisibleInContainer(125, 145));
        link.setSizeSpace(previous);
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
        vertical.smoothScrolling.set(false);
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
        horizontal.smoothScrolling.set(false);
        horizontal.nextFrame();
        // An area that only scrolls sideways takes the normal wheel for it
        assertTrue(horizontal.scrollByWheel(0, -1));
        assertTrue(horizontal.getScroller().x().getPosition() > 0);

        TestArea both = area(child("child", 0, 0, 10, 10));
        both.scrollDirection.set(ScrollDirection.BOTH);
        both.contentWidth.set(1000);
        both.contentHeight.set(1000);
        both.smoothScrolling.set(false);
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

    @Test
    void anAreaThatSnapsToElementsStopsAtTheirStarts() {
        TestArea area = area(child("first", 0, 0, 50, 40), child("second", 0, 60, 50, 40), child("third", 0, 170, 50, 40));
        area.contentHeight.set(600);
        area.snapMode.set(ScrollSnapMode.ELEMENTS);
        area.smoothScrolling.set(false);
        area.nextFrame();

        assertTrue(area.scrollByWheel(0, -1));
        assertEquals(60.0, area.getScroller().y().getPosition());
        area.nextFrame();
        assertTrue(area.scrollByWheel(0, -1));
        assertEquals(170.0, area.getScroller().y().getPosition());
        area.nextFrame();
        // Behind the last element, the next stop is the end of the content
        assertTrue(area.scrollByWheel(0, -1));
        assertEquals(500.0, area.getScroller().y().getPosition());
        area.nextFrame();
        assertFalse(area.scrollByWheel(0, -1));
        assertTrue(area.scrollByWheel(0, 1));
        assertEquals(170.0, area.getScroller().y().getPosition());
    }

    @Test
    void anAreaThatSnapsToPagesScrollsAsFarAsItIsBig() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(350);
        area.snapMode.set(ScrollSnapMode.PAGES);
        area.smoothScrolling.set(false);
        area.nextFrame();
        assertTrue(area.scrollByWheel(0, -1));
        assertEquals(100.0, area.getScroller().y().getPosition());
        assertTrue(area.scrollByWheel(0, -3));
        assertEquals(200.0, area.getScroller().y().getPosition());
        assertTrue(area.scrollByWheel(0, -1));
        // The last page is as long as what is left of the content
        assertEquals(250.0, area.getScroller().y().getPosition());
        assertFalse(area.scrollByWheel(0, -1));
    }

    @Test
    void aWheelThatTurnsInSmallBitsNeedsAFewOfThemToMoveASnappingAreaOn() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(350);
        area.snapMode.set(ScrollSnapMode.PAGES);
        area.smoothScrolling.set(false);
        area.nextFrame();
        // The scroll is for the area all the way, also while the content has not moved yet
        assertTrue(area.scrollByWheel(0, -0.4));
        assertTrue(area.scrollByWheel(0, -0.4));
        assertEquals(0.0, area.getScroller().y().getPosition());
        assertTrue(area.scrollByWheel(0, -0.4));
        assertEquals(100.0, area.getScroller().y().getPosition());

        // A wheel that turns back starts over
        assertTrue(area.scrollByWheel(0, -0.8));
        assertTrue(area.scrollByWheel(0, 0.6));
        assertEquals(100.0, area.getScroller().y().getPosition());
        assertTrue(area.scrollByWheel(0, 0.6));
        assertEquals(0.0, area.getScroller().y().getPosition());

        // Where the content has nowhere to go, the scroll is for what is behind the area
        assertFalse(area.scrollByWheel(0, 0.4));

        // Half a turn that got left alone is forgotten
        assertTrue(area.scrollByWheel(0, -0.6));
        area.passTime(1.0);
        assertTrue(area.scrollByWheel(0, -0.6));
        assertEquals(0.0, area.getScroller().y().getPosition());

        // One step of a wheel is always enough, also where the game makes less than 1 of it
        area.passTime(1.0);
        area.wheelStepSize = 0.5D;
        assertTrue(area.scrollByWheel(0, -0.5));
        assertEquals(100.0, area.getScroller().y().getPosition());
    }

    @Test
    void pagingAnAreaThatSnapsToElementsAlwaysMovesOn() {
        // The elements are further apart than a page is long, so the closest place to a page further is where the content is already
        TestArea area = area(child("first", 0, 0, 50, 40), child("second", 0, 300, 50, 40), child("third", 0, 600, 50, 40));
        area.contentHeight.set(1000);
        area.snapMode.set(ScrollSnapMode.ELEMENTS);
        area.smoothScrolling.set(false);
        area.nextFrame();
        assertTrue(area.getScrolling().scrollByPage(true));
        assertEquals(300.0, area.getScroller().y().getPosition());
        assertTrue(area.getScrolling().scrollByPage(true));
        assertEquals(600.0, area.getScroller().y().getPosition());
        assertTrue(area.getScrolling().scrollByPage(false));
        assertEquals(300.0, area.getScroller().y().getPosition());
    }

    @Test
    void contentThatGotMovedFreelyComesToRestWhereTheAreaSnaps() {
        TestArea area = area(child("first", 0, 0, 50, 40), child("second", 0, 100, 50, 40));
        area.contentHeight.set(600);
        area.snapMode.set(ScrollSnapMode.ELEMENTS);
        area.smoothScrolling.set(false);
        area.nextFrame();
        area.getScroller().y().scrollTo(80, false);
        area.nextFrame();
        area.getScrolling().snapToNearest();
        assertEquals(100.0, area.getScroller().y().getPosition());
    }

    @Test
    void anAreaThatStartsAtItsEndShowsUpThere() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.startPosition.set(ScrollStartPosition.END);
        // Nothing to scroll yet, like content that still loads
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition());
        area.contentHeight.set(400);
        area.nextFrame();
        assertEquals(300.0, area.getScroller().y().getPosition());
        // Only an area that just showed up goes there. Afterwards the content stays where it gets scrolled to.
        area.getScroller().y().scrollTo(50, false);
        area.nextFrame();
        assertEquals(50.0, area.getScroller().y().getPosition());
    }

    @Test
    void openingTheScreenPutsTheContentBackToItsStartPosition() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(400);
        area.nextFrame();
        area.getScroller().y().scrollTo(120, false);
        area.nextFrame();

        area.resetScrollOnOpen.set(false);
        area.onOpenScreen();
        area.nextFrame();
        assertEquals(120.0, area.getScroller().y().getPosition());

        area.resetScrollOnOpen.set(true);
        area.onOpenScreen();
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition());

        area.startPosition.set(ScrollStartPosition.END);
        area.onOpenScreen();
        area.nextFrame();
        assertEquals(300.0, area.getScroller().y().getPosition());
    }

    @Test
    void anAreaThatScrollsOnItsOwnMovesUntilTheMouseIsOverIt() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(5000);
        area.autoScroll.set(true);
        area.autoScrollSpeed.set(100.0F);
        area.nextFrame();
        area.passTime(3.0);
        double moved = area.getScroller().y().getPosition();
        assertTrue(moved > 100, "moved " + moved);

        // The mouse over the area pauses it
        area.setMouse(150, 80);
        area.passTime(2.0);
        assertEquals(moved, area.getScroller().y().getPosition());
        area.autoScrollPauseOnHover.set(false);
        area.passTime(2.0);
        assertTrue(area.getScroller().y().getPosition() > moved);

        // Scrolling by hand makes it rest for a moment
        area.setMouse(ScrollAreaElement.MOUSE_OUTSIDE, ScrollAreaElement.MOUSE_OUTSIDE);
        area.smoothScrolling.set(false);
        area.nextFrame();
        area.scrollByWheel(0, -1);
        double afterWheel = area.getScroller().y().getPosition();
        area.passTime(1.0);
        assertEquals(afterWheel, area.getScroller().y().getPosition());
        area.passTime(3.0);
        assertTrue(area.getScroller().y().getPosition() > afterWheel);

        // In the layout editor, nothing moves on its own
        area.inEditor = true;
        double inEditor = area.getScroller().y().getPosition();
        area.passTime(3.0);
        assertEquals(inEditor, area.getScroller().y().getPosition());
    }

    @Test
    void theMouseDragsTheContentAlongOnceItMovedFarEnough() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(1000);
        area.dragScrolling.set(true);
        area.nextFrame();
        area.getScroller().y().scrollTo(200, false);
        area.nextFrame();

        // The press is for the area, so nothing behind the area gets it
        assertTrue(area.getScrolling().onMousePressed(150, 100));
        area.setMouse(150, 98);
        area.nextFrame();
        // Two pixels are still a click
        assertFalse(area.getScrolling().isDragging());
        assertEquals(200.0, area.getScroller().y().getPosition());

        area.setMouse(150, 70);
        area.nextFrame();
        assertTrue(area.getScrolling().isDragging());
        // The content sticks to the mouse: 30 pixels up from where it was pressed
        assertEquals(230.0, area.getScroller().y().getPosition());
        area.setMouse(150, 120);
        area.nextFrame();
        assertEquals(180.0, area.getScroller().y().getPosition());

        area.getScrolling().onMouseReleased();
        area.setMouse(150, 20);
        area.nextFrame();
        assertFalse(area.getScrolling().isDragging());
        assertEquals(180.0, area.getScroller().y().getPosition());
    }

    @Test
    void aMouseThatGotLetGoOfWithoutTheAreaHearingOfItStopsDraggingTheContent() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(1000);
        area.dragScrolling.set(true);
        area.nextFrame();
        area.getScrolling().onMousePressed(150, 100);
        area.setMouse(150, 70);
        area.nextFrame();
        assertEquals(30.0, area.getScroller().y().getPosition());

        // Some screens never hand the release on, so it gets counted where all of them come in
        ScrollAreaScrolling.onLeftMouseReleasedAnywhere();
        area.setMouse(150, 40);
        area.nextFrame();
        assertFalse(area.getScrolling().isDragging());
        assertEquals(30.0, area.getScroller().y().getPosition());
        area.setMouse(150, 10);
        area.nextFrame();
        assertEquals(30.0, area.getScroller().y().getPosition());
    }

    @Test
    void theMouseDoesNotDragTheContentWhereItIsNotAllowedTo() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(1000);
        area.nextFrame();

        // Not unless the area is set to it, and then the press stays one for what is behind the area
        assertFalse(area.getScrolling().onMousePressed(150, 100));
        area.setMouse(150, 40);
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition() + (area.getScrolling().isDragging() ? 1 : 0));
        area.getScrolling().onMouseReleased();

        // Not in the layout editor, where dragging moves elements around
        area.dragScrolling.set(true);
        area.inEditor = true;
        area.setMouse(150, 100);
        area.nextFrame();
        area.getScrolling().onMousePressed(150, 100);
        area.setMouse(150, 40);
        area.nextFrame();
        assertFalse(area.getScrolling().isDragging());
        area.getScrolling().onMouseReleased();
        area.inEditor = false;

        // Not when players can't scroll at all
        area.userScrolling.set(false);
        area.getScrolling().onMousePressed(150, 100);
        area.setMouse(150, 30);
        area.nextFrame();
        assertFalse(area.getScrolling().isDragging());
        assertFalse(area.scrollByWheel(0, -1));
        assertFalse(area.getScrolling().scrollByPage(true));
    }

    @Test
    void anAreaTakesAPressWhereItStartsDraggingItsContent() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(1000);
        // Nothing has shown yet
        assertFalse(area.takesMousePressAt(150, 100));
        area.nextFrame();
        // An area the mouse does not drag has nothing a press would be for
        assertFalse(area.takesMousePressAt(150, 100));
        area.dragScrolling.set(true);
        assertTrue(area.takesMousePressAt(150, 100));
        assertFalse(area.takesMousePressAt(50, 100));

        // Content that has nowhere to go can't be dragged
        area.contentHeight.set(50);
        area.nextFrame();
        assertFalse(area.takesMousePressAt(150, 100));
    }

    @Test
    void whatGetsAskedOfAnAreaBeforeItShowsItsContentGetsDoneOnceItDoes() {
        TestChild far = child("far", 0, 400, 50, 40);
        TestArea area = area(child("near", 0, 10, 50, 40), far);
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        // The area never worked out how far it can be scrolled, so there is nowhere to go yet
        area.getScrolling().scrollToCorner(true, false);
        assertEquals(0.0, area.getScroller().y().getPosition());
        area.nextFrame();
        assertEquals(900.0, area.getScroller().y().getPosition());

        TestArea other = area(child("first", 0, 10, 50, 40), far);
        other.contentHeight.set(1000);
        other.smoothScrolling.set(false);
        other.getScrolling().scrollToCorner(true, false);
        // What gets asked last counts
        other.getScrolling().scrollToChild(far, false);
        other.nextFrame();
        assertEquals(396.0, other.getScroller().y().getPosition());
        assertTrue(other.getScrolling().scrollToPosition(false, 120, false));
        assertEquals(120.0, other.getScroller().y().getPosition());
    }

    @Test
    void contentThatGrowsUnderAnAreaAtItsStartHasNotGotThere() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(50);
        area.smoothScrolling.set(false);
        area.nextFrame();
        area.nextFrame();
        area.contentHeight.set(400);
        area.nextFrame();
        area.nextFrame();
        assertEquals("0 0 0", area.events());
        // Getting to the end and back are two things that happened
        area.getScrolling().scrollToEdge(true, false);
        area.nextFrame();
        assertEquals("1 0 1", area.events());
        area.getScrolling().scrollToEdge(false, false);
        area.nextFrame();
        assertEquals("2 1 1", area.events());
    }

    @Test
    void aFocusTheMouseSetLeavesTheContentWhereItIs() {
        TestChild far = child("far", 0, 400, 50, 40);
        TestChild further = child("further", 0, 700, 50, 40);
        TestArea area = area(child("near", 0, 10, 50, 40), far, further);
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        area.nextFrame();

        ScrollAreaScrolling.onMousePressedAnywhere();
        area.focused = far.widget;
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition());

        // The next focus comes from the keyboard again
        area.focused = further.widget;
        area.nextFrame();
        assertTrue(area.getScroller().y().getPosition() > 600.0);
    }

    @Test
    void theKeyboardScrollsByPagesAndToTheEnds() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        area.nextFrame();
        assertTrue(area.getScrolling().scrollByPage(true));
        double page = area.getScroller().y().getPosition();
        // A page is a bit less than what the area shows, so the eye keeps its place
        assertTrue((page > 50) && (page < 100), "page " + page);
        assertTrue(area.getScrolling().scrollToEdge(true, false));
        assertEquals(900.0, area.getScroller().y().getPosition());
        assertFalse(area.getScrolling().scrollByPage(true));
        assertTrue(area.getScrolling().scrollByPage(false));
        assertEquals(900.0 - page, area.getScroller().y().getPosition());
        assertTrue(area.getScrolling().scrollToEdge(false, false));
        assertFalse(area.getScrolling().scrollToEdge(false, false));
    }

    @Test
    void aChildThatGetsTheFocusScrollsIntoSight() {
        TestChild far = child("far", 0, 400, 50, 40);
        TestChild near = child("near", 0, 10, 50, 40);
        TestArea area = area(near, far);
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        area.nextFrame();

        area.focused = far.widget;
        area.nextFrame();
        // The child ends a few pixels above the bottom edge of the area
        double position = area.getScroller().y().getPosition();
        assertTrue((position >= 340) && (position <= 350), "position " + position);

        // The same focus does not pull the content back when the user scrolls away
        area.getScroller().y().scrollTo(0, false);
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition());

        // Something that already shows completely leaves the content where it is
        area.focused = near.widget;
        area.nextFrame();
        assertEquals(0.0, area.getScroller().y().getPosition());

        // A focus on something that is not in the area does nothing
        area.getScroller().y().scrollTo(500, false);
        area.focused = new TestChild().widget;
        area.nextFrame();
        assertEquals(500.0, area.getScroller().y().getPosition());
    }

    @Test
    void scrollingAChildIntoSightStartsAtItsStartWhenItDoesNotFit() {
        TestChild huge = child("huge", 0, 300, 50, 400);
        TestArea area = area(huge);
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        area.nextFrame();
        area.getScrolling().scrollIntoView(huge, false);
        double position = area.getScroller().y().getPosition();
        assertTrue((position >= 290) && (position <= 300), "position " + position);
    }

    @Test
    void theAreaGetsToldWhenItsContentMovesAndWhenItGetsToAnEnd() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(400);
        area.smoothScrolling.set(false);
        area.nextFrame();
        area.nextFrame();
        // Where the content is when the area shows up is nothing it got scrolled to
        assertEquals("0 0 0", area.events());

        area.scrollByWheel(0, -1);
        area.nextFrame();
        assertEquals("1 0 0", area.events());
        area.nextFrame();
        assertEquals("1 0 0", area.events());

        area.getScrolling().scrollToEdge(true, false);
        area.nextFrame();
        assertEquals("2 0 1", area.events());
        // Staying at the end is not getting there again
        area.nextFrame();
        area.scrollByWheel(0, -1);
        area.nextFrame();
        assertEquals("2 0 1", area.events());

        area.getScrolling().scrollToEdge(false, false);
        area.nextFrame();
        assertEquals("3 1 1", area.events());
        area.getScrolling().scrollToEdge(true, false);
        area.nextFrame();
        assertEquals("4 1 2", area.events());
    }

    @Test
    void contentThatGlidesTellsTheAreaInEveryFrameItMoves() {
        TestArea area = area(child("first", 0, 0, 50, 40));
        area.contentHeight.set(400);
        area.nextFrame();
        area.nextFrame();
        area.getScrolling().scrollToEdge(true, true);
        area.nextFrame();
        area.nextFrame();
        area.nextFrame();
        assertEquals(3, area.scrolledEvents);
        assertEquals(0, area.reachedEndEvents);
        // The content is all but there long before it stops moving, and only tells the area once it stopped
        while (area.getScroller().y().isScrolling()) {
            assertEquals(0, area.reachedEndEvents);
            area.nextFrame();
        }
        assertEquals(300.0, area.getScroller().y().getPosition());
        assertEquals(1, area.reachedEndEvents);
        int moved = area.scrolledEvents;
        area.passTime(1.0);
        assertEquals(moved, area.scrolledEvents);
    }

    @Test
    void anAreaThatShowsUpSomewhereTellsNothingAboutGettingThere() {
        TestArea atEnd = area(child("first", 0, 0, 50, 40));
        atEnd.startPosition.set(ScrollStartPosition.END);
        atEnd.smoothScrolling.set(false);
        // The content only gets something to scroll after a few frames, like content that still loads
        atEnd.nextFrame();
        atEnd.nextFrame();
        atEnd.contentHeight.set(400);
        atEnd.nextFrame();
        atEnd.nextFrame();
        assertEquals(300.0, atEnd.getScroller().y().getPosition());
        assertEquals("0 0 0", atEnd.events());

        // Opening the screen again puts the content back to where it starts, which tells nothing either
        atEnd.getScrolling().scrollToEdge(false, false);
        atEnd.nextFrame();
        assertEquals("1 1 0", atEnd.events());
        atEnd.resetScrollOnOpen.set(true);
        atEnd.onOpenScreen();
        atEnd.nextFrame();
        atEnd.nextFrame();
        assertEquals(300.0, atEnd.getScroller().y().getPosition());
        assertEquals("1 1 0", atEnd.events());

        // An area with nothing to scroll never gets anywhere
        TestArea still = area(child("first", 0, 0, 50, 40));
        still.nextFrame();
        still.nextFrame();
        still.getScrolling().scrollToEdge(true, false);
        still.nextFrame();
        assertEquals("0 0 0", still.events());
    }

    @Test
    void scrollingToAChildPutsItAtTheStartOfWhatTheAreaShows() {
        TestChild far = child("far", 0, 400, 50, 40);
        TestChild last = child("last", 0, 950, 50, 40);
        TestArea area = area(child("near", 0, 10, 50, 40), far, last);
        area.contentHeight.set(1000);
        area.smoothScrolling.set(false);
        area.nextFrame();

        area.getScrolling().scrollToChild(far, false);
        double position = area.getScroller().y().getPosition();
        assertTrue((position >= 390) && (position <= 400), "position " + position);
        area.nextFrame();
        // A child that is at the start already leaves the content where it is
        assertFalse(area.getScrolling().scrollToPosition(false, position, false));
        area.getScrolling().scrollToChild(far, false);
        assertEquals(position, area.getScroller().y().getPosition());

        // The content can't be scrolled further than to its end
        area.getScrolling().scrollToChild(last, false);
        assertEquals(900.0, area.getScroller().y().getPosition());
    }

    @Test
    void aChildThatMustNotReactToTheMouseGetsToldItIsNextToIt() {
        TestChild child = child("child", 0, 0, 50, 40);
        TestArea area = area(child);
        area.nextFrame();
        // The child spans (100, 50) to (150, 90) on the screen
        assertArrayEquals(new int[]{30, 60}, movedOutOf(child, 30, 60));
        assertArrayEquals(new int[]{150, 60}, movedOutOf(child, 150, 60));

        // In the child, the mouse moves to the closest place outside of it, on one axis
        assertArrayEquals(new int[]{99, 70}, movedOutOf(child, 103, 70));
        assertArrayEquals(new int[]{150, 70}, movedOutOf(child, 147, 70));
        assertArrayEquals(new int[]{125, 49}, movedOutOf(child, 125, 52));
        assertArrayEquals(new int[]{125, 90}, movedOutOf(child, 125, 88));
        for (int x = 100; x < 150; x += 7) {
            for (int y = 50; y < 90; y += 5) {
                int movedX = movedOutOf(child, x, y)[0];
                int movedY = movedOutOf(child, x, y)[1];
                assertFalse((movedX >= 100) && (movedX < 150) && (movedY >= 50) && (movedY < 90), x + "," + y + " stays in the child at " + movedX + "," + movedY);
                assertTrue((movedX == x) || (movedY == y), "moved on both axes from " + x + "," + y);
            }
        }
    }

    @Test
    void anAreaThatGetsSkippedTellsItsChildrenAndStopsShowingThem() {
        TestChild child = child("child", 0, 0, 50, 40);
        TestChild deep = child("deep", 0, 0, 20, 20);
        TestArea inner = area(deep);
        inner.setInstanceIdentifier("inner");
        TestArea area = new TestArea();
        area.setInstanceIdentifier("area");
        child.setParentContainer(area);
        inner.setParentContainer(area);
        area.setContainerChildren(List.of(child, inner));
        area.nextFrame();
        inner.nextFrame();
        assertTrue(area.isShowingChildren());
        assertTrue(deep.isShownByContainer());

        area.onRenderInternalSkipped();
        assertEquals(1, child.skippedRenders);
        assertEquals(1, deep.skippedRenders);
        assertFalse(area.isShowingChildren());
        assertFalse(inner.isShowingChildren());
        assertFalse(child.isShownByContainer());
        assertFalse(deep.shouldRender());
    }

    @Test
    void anAreaInAnAreaOnlyShowsWhatBothShow() {
        TestArea inner = new TestArea();
        inner.setInstanceIdentifier("inner");
        TestArea outer = new TestArea();
        outer.setInstanceIdentifier("outer");
        inner.setParentContainer(outer);
        outer.setContainerChildren(List.of(inner));
        outer.nextFrame();
        // The outer area shows (100, 50) to (300, 150). The inner one is half out of it at the right.
        inner.x = 250;
        inner.y = 60;
        inner.width = 100;
        inner.height = 50;
        inner.nextFrame();

        int[] area = {200, 0, 400, 400};
        assertTrue(inner.clipToShownArea(area));
        assertArrayEquals(new int[] {250, 60, 300, 110}, area);
        assertTrue(inner.isScreenPointVisible(290, 70));
        assertFalse(inner.isScreenPointVisible(320, 70));
        assertFalse(inner.clipToShownArea(new int[] {310, 60, 340, 100}));
    }

    @Test
    void aWidgetBelongsToTheChildThatHasItAlsoThroughItsListener() {
        TestChild child = child("child", 0, 0, 50, 40);
        TestChild deep = child("deep", 0, 0, 20, 20);
        TestArea inner = area(deep);
        inner.setInstanceIdentifier("inner");
        TestArea area = new TestArea();
        area.setInstanceIdentifier("area");
        child.setParentContainer(area);
        inner.setParentContainer(area);
        area.setContainerChildren(List.of(child, inner));

        assertSame(child, area.findChildOwning(child.widget));
        assertSame(child, area.findChildOwning(ElementSpaceInputListener.ofChildWidget(child, child.widget)));
        // Something in an area that is a child belongs to that child
        assertSame(inner, area.findChildOwning(deep.widget));
        assertNull(area.findChildOwning(new TestChild().widget));
    }

    /**
     * An area with a box tests set directly, which never asks the game for anything.
     */
    private static int[] movedOutOf(AbstractElement child, int mouseX, int mouseY) {
        int[] moved = new int[2];
        ScrollAreaElement.moveOutOf(child, mouseX, mouseY, moved);
        return moved;
    }

    private static final class TestArea extends ScrollAreaElement {

        private int x = 100;
        private int y = 50;
        private int width = 200;
        private int height = 100;
        private boolean shiftDown = false;
        private double wheelStepSize = 1.0D;
        private boolean inEditor = false;
        private long nanoTime = 1_000_000_000L;
        private GuiEventListener focused;
        private int scrolledEvents = 0;
        private int reachedStartEvents = 0;
        private int reachedEndEvents = 0;

        private TestArea() {
            super(BUILDER);
        }

        /** Works the geometry out again and counts as having shown the children, like a frame the area renders in. */
        private void nextFrame() {
            this.lastChildrenShownFrame = de.keksuccino.fancymenu.util.rendering.RenderFrameCounter.getFrame();
            // 50 milliseconds per frame
            this.nanoTime += 50_000_000L;
            this.updateFrame();
        }

        /** How often the content moved, got to its start and got to its end. */
        private String events() {
            return this.scrolledEvents + " " + this.reachedStartEvents + " " + this.reachedEndEvents;
        }

        @Override
        protected void onContentScrolled() {
            this.scrolledEvents++;
        }

        @Override
        protected void onContentReachedStart() {
            this.reachedStartEvents++;
        }

        @Override
        protected void onContentReachedEnd() {
            this.reachedEndEvents++;
        }

        private void passTime(double seconds) {
            for (double passed = 0; passed < seconds; passed += 0.05) {
                this.nextFrame();
            }
        }

        private void setMouse(int mouseX, int mouseY) {
            this.frameMouseX = mouseX;
            this.frameMouseY = mouseY;
        }

        @Override
        protected long getNanoTime() {
            return this.nanoTime;
        }

        @Override
        protected boolean isInEditor() {
            return this.inEditor;
        }

        @Override
        protected GuiEventListener getFocusedListener() {
            return this.focused;
        }

        @Override
        public boolean hasInteractiveChildAt(double screenX, double screenY) {
            return false;
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
        protected int getPointerX() {
            return this.frameMouseX;
        }

        @Override
        protected int getPointerY() {
            return this.frameMouseY;
        }

        @Override
        protected double getWheelStepSize() {
            return this.wheelStepSize;
        }

        @Override
        protected boolean isShiftDown() {
            return this.shiftDown;
        }

    }

    private static final class TestWidget implements GuiEventListener, NarratableEntry {

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput output) {
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

    private static final class TestChild extends AbstractElement {

        private final TestWidget widget = new TestWidget();
        private int skippedRenders = 0;

        @Override
        public void onRenderInternalSkipped() {
            this.skippedRenders++;
        }

        private TestChild() {
            super(null);
        }

        @Override
        public List<GuiEventListener> getWidgetsToRegister() {
            return List.of(this.widget);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        /** Auto-sizing asks the game for the size of the screen, which tests don't have. */
        @Override
        public void updateAutoSizing(boolean ignoreLastTickScreenSize) {
        }

    }

}
