package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollDirection;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScrollAreaValuePlaceholderTest {

    private static final ScrollAreaElementBuilder BUILDER = new ScrollAreaElementBuilder();

    /** An area that can be scrolled 400 pixels sideways and 800 pixels downwards, and is scrolled to 100 and 200. */
    private static ScrollAreaElement scrolledArea() {
        ScrollAreaElement area = new ScrollAreaElement(BUILDER);
        area.getScroller().setRange(400, 800);
        area.getScroller().x().scrollTo(100, false);
        area.getScroller().y().scrollTo(200, false);
        return area;
    }

    private static String replacement(ScrollAreaValuePlaceholder placeholder, String elementIdentifier, String axis) {
        HashMap<String, String> values = new HashMap<>();
        if (elementIdentifier != null) values.put("element_identifier", elementIdentifier);
        if (axis != null) values.put("axis", axis);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString(placeholder.getIdentifier(), values, ""));
    }

    @Test
    void thePositionIsTheOneOfTheAskedAxis() {
        ScrollAreaElement area = scrolledArea();
        ScrollAreaValuePlaceholder placeholder = ScrollAreaValuePlaceholder.scrollPosition(id -> area);
        assertEquals("200", replacement(placeholder, "area", "vertical"));
        assertEquals("100", replacement(placeholder, "area", "horizontal"));
        assertEquals("100", replacement(placeholder, "area", "HORIZONTAL"));
    }

    @Test
    void withoutAnAxisTheAnswerIsForTheDirectionTheAreaMainlyScrollsIn() {
        ScrollAreaElement area = scrolledArea();
        ScrollAreaValuePlaceholder placeholder = ScrollAreaValuePlaceholder.scrollPosition(id -> area);
        assertEquals("200", replacement(placeholder, "area", null));
        assertEquals("200", replacement(placeholder, "area", "something"));
        area.scrollDirection.set(ScrollDirection.BOTH);
        assertEquals("200", replacement(placeholder, "area", null));
        area.scrollDirection.set(ScrollDirection.HORIZONTAL);
        assertEquals("100", replacement(placeholder, "area", null));
        // An axis that got asked for counts, also when it is not the one the area scrolls on
        assertEquals("200", replacement(placeholder, "area", "vertical"));
    }

    @Test
    void thePercentRunsFromTheStartToTheEndOfTheContent() {
        ScrollAreaElement area = scrolledArea();
        ScrollAreaValuePlaceholder placeholder = ScrollAreaValuePlaceholder.scrollPercent(id -> area);
        assertEquals("25", replacement(placeholder, "area", "vertical"));
        area.getScroller().y().scrollTo(800, false);
        assertEquals("100", replacement(placeholder, "area", "vertical"));
        area.getScroller().y().scrollTo(0, false);
        assertEquals("0", replacement(placeholder, "area", "vertical"));
        // Two digits behind the point at most
        area.getScroller().y().scrollTo(100.3, false);
        assertEquals("12.54", replacement(placeholder, "area", "vertical"));
        // Content that can't be scrolled is at its start
        area.getScroller().setRange(0, 0);
        assertEquals("0", replacement(placeholder, "area", "vertical"));
    }

    @Test
    void whatIsNoScrollableAreaAnswersWithZero() {
        Function<String, AbstractElement> nothing = id -> null;
        assertEquals("0", replacement(ScrollAreaValuePlaceholder.scrollPosition(nothing), "missing", "vertical"));
        assertEquals("0", replacement(ScrollAreaValuePlaceholder.scrollPercent(nothing), null, "vertical"));
        assertEquals("0", replacement(ScrollAreaValuePlaceholder.contentSize(nothing), "  ", "vertical"));
        AnimationControllerElement other = new AnimationControllerElement(new AnimationControllerElementBuilder());
        assertEquals("0", replacement(ScrollAreaValuePlaceholder.contentSize(id -> other), "other", "vertical"));
    }

    @Test
    void thePlaceholdersReadWhatElementsLookLikeRightNowSoTheyOnlyRunOnTheMainThread() {
        assertFalse(ScrollAreaValuePlaceholder.scrollPosition(id -> null).canRunAsync());
    }

}
