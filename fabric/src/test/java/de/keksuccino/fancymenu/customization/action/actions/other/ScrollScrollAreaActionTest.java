package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollScrollAreaActionTest {

    private static final ScrollAreaElementBuilder BUILDER = new ScrollAreaElementBuilder();

    /** An area that shows its content, can be scrolled 400 pixels sideways and 800 pixels downwards, and is scrolled to 100 and 200. */
    private static ScrollAreaElement scrolledArea() {
        ScrollAreaElement area = new ScrollAreaElement(BUILDER) {
            @Override
            public boolean isShowingChildren() {
                return true;
            }
        };
        area.scrollDirection.set(ScrollDirection.BOTH);
        area.smoothScrolling.set(false);
        area.getScroller().setRange(400, 800);
        area.getScroller().x().scrollTo(100, false);
        area.getScroller().y().scrollTo(200, false);
        return area;
    }

    private static String value(String areaIdentifier, ScrollScrollAreaAction.Target target, String positionX, String positionY, String elementIdentifier, boolean smooth) {
        ScrollScrollAreaAction.Config config = new ScrollScrollAreaAction.Config();
        config.areaIdentifier = areaIdentifier;
        config.target = target;
        config.positionX = positionX;
        config.positionY = positionY;
        config.elementIdentifier = elementIdentifier;
        config.smooth = smooth;
        return config.serialize();
    }

    @Test
    void valuesSurviveSavingAndLoading() {
        ScrollScrollAreaAction.Config config = ScrollScrollAreaAction.Config.parse(value("some.area", ScrollScrollAreaAction.Target.POSITION, "12", "{\"placeholder\":\"x\"}", "some.child", false));
        assertNotNull(config);
        assertEquals("some.area", config.areaIdentifier);
        assertEquals(ScrollScrollAreaAction.Target.POSITION, config.target);
        assertEquals("12", config.positionX);
        assertEquals("{\"placeholder\":\"x\"}", config.positionY);
        assertEquals("some.child", config.elementIdentifier);
        assertFalse(config.smooth);
        for (ScrollScrollAreaAction.Target target : ScrollScrollAreaAction.Target.values()) {
            assertEquals(target, ScrollScrollAreaAction.Config.parse(value("a", target, "", "", "", true)).target);
        }
    }

    @Test
    void aValueWithPartsMissingKeepsWhatANewActionStartsWith() {
        ScrollScrollAreaAction.Config config = ScrollScrollAreaAction.Config.parse("some.area");
        assertNotNull(config);
        assertEquals("some.area", config.areaIdentifier);
        assertEquals(ScrollScrollAreaAction.Target.START, config.target);
        assertTrue(config.smooth);
        assertEquals(ScrollScrollAreaAction.Target.START, ScrollScrollAreaAction.Config.parse("some.area|||nonsense").target);
        assertNull(ScrollScrollAreaAction.Config.parse(null));
        assertNotNull(ScrollScrollAreaAction.Config.parse(new ScrollScrollAreaAction(id -> null).getValuePreset()));
    }

    @Test
    void itScrollsToTheStartAndTheEndOnEveryAxisTheAreaScrollsOn() {
        ScrollAreaElement area = scrolledArea();
        ScrollScrollAreaAction action = new ScrollScrollAreaAction(id -> "area".equals(id) ? area : null);

        action.execute(value("area", ScrollScrollAreaAction.Target.END, "", "", "", false));
        assertEquals(800.0, area.getScroller().y().getPosition());
        assertEquals(400.0, area.getScroller().x().getPosition());
        action.execute(value(" area ", ScrollScrollAreaAction.Target.START, "", "", "", false));
        assertEquals(0.0, area.getScroller().y().getPosition());
        assertEquals(0.0, area.getScroller().x().getPosition());

        // An axis the area does not scroll on stays where it is
        area.scrollDirection.set(ScrollDirection.HORIZONTAL);
        area.getScroller().y().scrollTo(200, false);
        action.execute(value("area", ScrollScrollAreaAction.Target.END, "", "", "", false));
        assertEquals(400.0, area.getScroller().x().getPosition());
        assertEquals(200.0, area.getScroller().y().getPosition());
    }

    @Test
    void itScrollsToAPositionOnTheAxesThatGotOne() {
        ScrollAreaElement area = scrolledArea();
        ScrollScrollAreaAction action = new ScrollScrollAreaAction(id -> area);

        action.execute(value("area", ScrollScrollAreaAction.Target.POSITION, "", "350", "", false));
        assertEquals(350.0, area.getScroller().y().getPosition());
        assertEquals(100.0, area.getScroller().x().getPosition());

        action.execute(value("area", ScrollScrollAreaAction.Target.POSITION, " 40.5 ", "", "", false));
        assertEquals(40.5, area.getScroller().x().getPosition());
        assertEquals(350.0, area.getScroller().y().getPosition());

        // A position beyond the content ends at its end, and one that is no number leaves the content where it is
        action.execute(value("area", ScrollScrollAreaAction.Target.POSITION, "abc", "5000", "", false));
        assertEquals(40.5, area.getScroller().x().getPosition());
        assertEquals(800.0, area.getScroller().y().getPosition());
    }

    @Test
    void glidingThereOnlyMovesWhereTheContentIsHeadedAtFirst() {
        ScrollAreaElement area = scrolledArea();
        area.getScroller().setSmooth(true);
        ScrollScrollAreaAction action = new ScrollScrollAreaAction(id -> area);
        action.execute(value("area", ScrollScrollAreaAction.Target.POSITION, "", "600", "", true));
        assertEquals(200.0, area.getScroller().y().getPosition());
        assertEquals(600.0, area.getScroller().y().getTarget());
        action.execute(value("area", ScrollScrollAreaAction.Target.POSITION, "", "700", "", false));
        assertEquals(700.0, area.getScroller().y().getPosition());
    }

    @Test
    void itDoesNothingWithoutAnAreaToScroll() {
        ScrollAreaElement area = scrolledArea();
        new ScrollScrollAreaAction(id -> null).execute(value("missing", ScrollScrollAreaAction.Target.END, "", "", "", false));
        new ScrollScrollAreaAction(id -> area).execute(value("", ScrollScrollAreaAction.Target.END, "", "", "", false));
        new ScrollScrollAreaAction(id -> area).execute(null);
        // An element that is not in the area is nothing the area could scroll to
        new ScrollScrollAreaAction(id -> area).execute(value("area", ScrollScrollAreaAction.Target.ELEMENT, "", "", "area", false));
        new ScrollScrollAreaAction(id -> area).execute(value("area", ScrollScrollAreaAction.Target.ELEMENT, "", "", "", false));
        assertEquals(200.0, area.getScroller().y().getPosition());
        assertEquals(100.0, area.getScroller().x().getPosition());
    }

}
