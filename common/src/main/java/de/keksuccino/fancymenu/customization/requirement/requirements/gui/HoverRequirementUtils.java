package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import org.jetbrains.annotations.NotNull;

final class HoverRequirementUtils {

    private HoverRequirementUtils() {
    }

    static boolean isElementHovered(@NotNull AbstractElement element, int mouseX, int mouseY) {
        if (element instanceof HideableElement hideable && hideable.isHidden()) {
            return false;
        }
        if (!element.shouldRender()) {
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

}
