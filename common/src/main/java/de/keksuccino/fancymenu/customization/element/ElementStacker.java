package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
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
        if ((e.advancedX != null) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedX = e.advancedX;
        }
        if ((e.advancedY != null) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedY = e.advancedY;
        }
        if ((e.advancedWidth != null) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedWidth = e.advancedWidth;
        }
        if ((e.advancedHeight != null) && (e.anchorPoint != ElementAnchorPoints.VANILLA)) {
            stack.advancedHeight = e.advancedHeight;
        }
        if (e.stretchX) {
            stack.stretchX = true;
        }
        if (e.stretchY) {
            stack.stretchY = true;
        }
        if (e.appearanceDelay != AbstractElement.AppearanceDelay.NO_DELAY) {
            stack.appearanceDelay = e.appearanceDelay;
        }
        if (e.appearanceDelayInSeconds != 1.0F) {
            stack.appearanceDelayInSeconds = e.appearanceDelayInSeconds;
        }
        //TODO übernehmen
        if (e.fadeIn != AbstractElement.Fading.NO_FADING) {
            stack.fadeIn = e.fadeIn;
        }
        if (e.fadeInSpeed != 1.0F) {
            stack.fadeInSpeed = e.fadeInSpeed;
        }
        if (e.fadeOut != AbstractElement.Fading.NO_FADING) {
            stack.fadeOut = e.fadeOut;
        }
        if (e.fadeOutSpeed != 1.0F) {
            stack.fadeOutSpeed = e.fadeOutSpeed;
        }
        if (e.baseOpacity != 1.0F) {
            stack.baseOpacity = e.baseOpacity;
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
            List<LoadingRequirementContainer> containers = new ArrayList<>();
            for (AbstractElement e : elements) {
                this.stackElementsSingleInternal(e, stack);
                containers.add(e.loadingRequirementContainer);
            }
            stack.loadingRequirementContainer = LoadingRequirementContainer.stackContainers(containers.toArray(new LoadingRequirementContainer[0]));
            return (E) stack;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
