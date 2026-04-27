package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

final class HoverRequirementUtils {

    private static final ThreadLocal<Set<String>> ACTIVE_HOVER_CHECKS = ThreadLocal.withInitial(HashSet::new);

    private HoverRequirementUtils() {
    }

    static boolean isElementHovered(@NotNull AbstractElement element, int mouseX, int mouseY) {
        if (element instanceof HideableElement hideable && hideable.isHidden()) {
            return false;
        }
        if (!shouldRenderForHoverCheck(element)) {
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

    private static boolean shouldRenderForHoverCheck(@NotNull AbstractElement element) {
        String instanceIdentifier = element.getInstanceIdentifier();
        Set<String> activeHoverChecks = ACTIVE_HOVER_CHECKS.get();
        if (!activeHoverChecks.add(instanceIdentifier)) {
            // Prevent self-referential hover requirements from recursing through shouldRender().
            return false;
        }
        try {
            return element.shouldRender();
        } finally {
            activeHoverChecks.remove(instanceIdentifier);
            if (activeHoverChecks.isEmpty()) {
                ACTIVE_HOVER_CHECKS.remove();
            }
        }
    }

}
