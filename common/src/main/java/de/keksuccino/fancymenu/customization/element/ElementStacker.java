package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface ElementStacker<E extends AbstractElement> {

    @SuppressWarnings("all")
    public abstract void stackElements(@NotNull E element, @NotNull E stack);

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @SuppressWarnings("all")
    public default void stackElementsSingleInternal(AbstractElement e, AbstractElement stack) {

        //AbstractElement stuff
        if ((e.anchorPoint != null) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.anchorPoint = e.anchorPoint;
        }
        if (e.anchorPointElementIdentifier != null) {
            stack.anchorPointElementIdentifier = e.anchorPointElementIdentifier;
        }
        if ((e.posOffsetX != 0) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.posOffsetX = e.posOffsetX;
        }
        if ((e.posOffsetY != 0) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.posOffsetY = e.posOffsetY;
        }
        if ((e.baseWidth != 0) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.baseWidth = e.baseWidth;
        }
        if ((e.baseHeight != 0) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.baseHeight = e.baseHeight;
        }
        if (!e.advancedX.isDefault() && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedX.copyValueFrom(e.advancedX);
        }
        if (!e.advancedY.isDefault() && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedY.copyValueFrom(e.advancedY);
        }
        if (!e.advancedWidth.isDefault() && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedWidth.copyValueFrom(e.advancedWidth);
        }
        if (!e.advancedHeight.isDefault() && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedHeight.copyValueFrom(e.advancedHeight);
        }
        if (!e.stretchX.isDefault()) {
            stack.stretchX.copyValueFrom(e.stretchX);
        }
        if (!e.stretchY.isDefault()) {
            stack.stretchY.copyValueFrom(e.stretchY);
        }
        if (e.appearanceDelay != AbstractElement.AppearanceDelay.NO_DELAY) {
            stack.appearanceDelay = e.appearanceDelay;
        }
        if (!e.appearanceDelaySeconds.isDefault()) {
            stack.appearanceDelaySeconds.copyValueFrom(e.appearanceDelaySeconds);
        }
        if (e.disappearanceDelay != AbstractElement.DisappearanceDelay.NO_DELAY) {
            stack.disappearanceDelay = e.disappearanceDelay;
        }
        if (!e.disappearanceDelaySeconds.isDefault()) {
            stack.disappearanceDelaySeconds.copyValueFrom(e.disappearanceDelaySeconds);
        }
        if (e.fadeIn != AbstractElement.Fading.NO_FADING) {
            stack.fadeIn = e.fadeIn;
        }
        if (!e.fadeInSpeed.isDefault()) {
            stack.fadeInSpeed.copyValueFrom(e.fadeInSpeed);
        }
        if (e.fadeOut != AbstractElement.Fading.NO_FADING) {
            stack.fadeOut = e.fadeOut;
        }
        if (!e.fadeOutSpeed.isDefault()) {
            stack.fadeOutSpeed.copyValueFrom(e.fadeOutSpeed);
        }
        if (!e.baseOpacity.isDefault()) {
            stack.baseOpacity.copyValueFrom(e.baseOpacity);
        }
        if (e.autoSizing) {
            stack.autoSizing = true;
        }
        if (e.autoSizingBaseScreenWidth != 0) {
            stack.autoSizingBaseScreenWidth = e.autoSizingBaseScreenWidth;
        }
        if (e.autoSizingBaseScreenHeight != 0) {
            stack.autoSizingBaseScreenHeight = e.autoSizingBaseScreenHeight;
        }
        if (e.stickyAnchor) {
            stack.stickyAnchor = true;
        }
        //---------------------

        this.stackElements((E) e, (E) stack);

    }

    /**
     * Only for internal use. Don't touch this if you don't know what you're doing!
     */
    @Nullable
    @SuppressWarnings("all")
    public default E stackElementsInternal(AbstractElement stack, AbstractElement... elements) {
        try {
            List<RequirementContainer> containers = new ArrayList<>();
            for (AbstractElement e : elements) {
                this.stackElementsSingleInternal(e, stack);
                containers.add(e.requirementContainer);
            }
            stack.requirementContainer = RequirementContainer.stackContainers(containers.toArray(new RequirementContainer[0]));
            return (E) stack;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
