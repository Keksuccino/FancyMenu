package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

final class HoverRequirementUtils {

    private static final ThreadLocal<Set<String>> ACTIVE_HOVER_CHECKS = ThreadLocal.withInitial(HashSet::new);

    private HoverRequirementUtils() {
    }

    static boolean isElementHovered(@NotNull AbstractElement element, int mouseX, int mouseY, @Nullable RequirementInstance currentRequirement) {
        if (element instanceof HideableElement hideable && hideable.isHidden()) {
            return false;
        }
        if (!shouldRenderForHoverCheck(element, currentRequirement)) {
            return false;
        }

        int elementWidth = element.getAbsoluteWidth();
        int elementHeight = element.getAbsoluteHeight();
        if ((elementWidth <= 0) || (elementHeight <= 0)) {
            return false;
        }

        int elementX = element.getAbsoluteX();
        int elementY = element.getAbsoluteY();
        return (mouseX >= elementX) && (mouseX <= (elementX + elementWidth)) && (mouseY >= elementY) && (mouseY <= (elementY + elementHeight));
    }

    private static boolean shouldRenderForHoverCheck(@NotNull AbstractElement element, @Nullable RequirementInstance currentRequirement) {
        String instanceIdentifier = element.getInstanceIdentifier();
        Set<String> activeHoverChecks = ACTIVE_HOVER_CHECKS.get();
        boolean probingCurrentRequirementOwner = (currentRequirement != null) && (currentRequirement.parent == element.requirementContainer);
        if (!activeHoverChecks.add(instanceIdentifier)) {
            // An enclosing probe already checks the owner's other render gates. Only its exact current requirement
            // may use that result; returning true for a cross-element cycle would make the cycle self-sustaining.
            return probingCurrentRequirementOwner;
        }
        try {
            if (probingCurrentRequirementOwner) return currentRequirement.testWithThisRequirementAssumedMet(element::shouldRender);
            return element.shouldRender();
        } finally {
            activeHoverChecks.remove(instanceIdentifier);
            if (activeHoverChecks.isEmpty()) {
                ACTIVE_HOVER_CHECKS.remove();
            }
        }
    }

}
