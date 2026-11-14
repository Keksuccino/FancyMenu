package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsScrollAreaScrolledToEndRequirementTest {

    private static final ScrollAreaElementBuilder BUILDER = new ScrollAreaElementBuilder();

    @Test
    void itIsMetWhileTheContentIsAtTheEndOfTheDirectionTheAreaScrollsIn() {
        ScrollAreaElement area = new ScrollAreaElement(BUILDER);
        area.getScroller().setRange(400, 800);
        IsScrollAreaScrolledToEndRequirement requirement = new IsScrollAreaScrolledToEndRequirement(id -> "area".equals(id) ? area : null);

        assertFalse(requirement.isRequirementMet("area"));
        area.getScroller().y().scrollTo(799, false);
        assertFalse(requirement.isRequirementMet("area"));
        area.getScroller().y().scrollTo(800, false);
        assertTrue(requirement.isRequirementMet("area"));
        assertTrue(requirement.isRequirementMet("  area "));

        // An area that scrolls sideways is at its end when its content is at its right end
        area.scrollDirection.set(ScrollDirection.HORIZONTAL);
        assertFalse(requirement.isRequirementMet("area"));
        area.getScroller().x().scrollTo(400, false);
        assertTrue(requirement.isRequirementMet("area"));
    }

    @Test
    void anAreaWithNothingToScrollShowsItsEnd() {
        ScrollAreaElement area = new ScrollAreaElement(BUILDER);
        assertTrue(new IsScrollAreaScrolledToEndRequirement(id -> area).isRequirementMet("area"));
    }

    @Test
    void itIsNotMetForWhatIsNoScrollableArea() {
        AnimationControllerElement other = new AnimationControllerElement(new AnimationControllerElementBuilder());
        assertFalse(new IsScrollAreaScrolledToEndRequirement(id -> other).isRequirementMet("other"));
        assertFalse(new IsScrollAreaScrolledToEndRequirement(id -> null).isRequirementMet("missing"));
        assertFalse(new IsScrollAreaScrolledToEndRequirement(id -> null).isRequirementMet(null));
        assertFalse(new IsScrollAreaScrolledToEndRequirement(id -> null).isRequirementMet(" "));
    }

    @Test
    void itReadsWhatAnElementLooksLikeRightNowSoItOnlyRunsOnTheMainThread() {
        assertFalse(new IsScrollAreaScrolledToEndRequirement(id -> null).canRunAsync());
    }

}
