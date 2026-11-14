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

    private static ShownArea shownArea() {
        return new ShownArea();
    }

    @Test
    void itIsMetWhileTheContentIsAtTheEndOfTheDirectionTheAreaScrollsIn() {
        ScrollAreaElement area = shownArea();
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
        ScrollAreaElement area = shownArea();
        assertTrue(new IsScrollAreaScrolledToEndRequirement(id -> area).isRequirementMet("area"));
    }

    @Test
    void anAreaThatDoesNotShowItsContentIsNotScrolledToAnything() {
        // Before its first frame, an area has no idea how big its content is, and would count as being at its end
        ShownArea area = shownArea();
        area.shown = false;
        IsScrollAreaScrolledToEndRequirement requirement = new IsScrollAreaScrolledToEndRequirement(id -> area);
        assertFalse(requirement.isRequirementMet("area"));
        area.shown = true;
        assertTrue(requirement.isRequirementMet("area"));
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

    private static final class ShownArea extends ScrollAreaElement {

        private boolean shown = true;

        private ShownArea() {
            super(BUILDER);
        }

        @Override
        public boolean isShowingChildren() {
            return this.shown;
        }

    }

}
