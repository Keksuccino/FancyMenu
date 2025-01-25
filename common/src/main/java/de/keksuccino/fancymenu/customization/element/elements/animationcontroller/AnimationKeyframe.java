package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import org.jetbrains.annotations.NotNull;

public class AnimationKeyframe implements Cloneable {

    public long timestamp;
    public int posOffsetX;
    public int posOffsetY;
    public int baseWidth;
    public int baseHeight;
    public ElementAnchorPoint anchorPoint;
    public boolean stickyAnchor;
    @NotNull
    public String uniqueIdentifier = ScreenCustomization.generateUniqueIdentifier();

    public AnimationKeyframe(long timestamp, int posOffsetX, int posOffsetY, int baseWidth, int baseHeight, ElementAnchorPoint anchorPoint, boolean stickyAnchor) {
        this.timestamp = timestamp;
        this.posOffsetX = posOffsetX;
        this.posOffsetY = posOffsetY;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.anchorPoint = anchorPoint;
        this.stickyAnchor = stickyAnchor;
    }

    @Override
    public String toString() {
        return "AnimationKeyframe{" +
                "timestamp=" + timestamp +
                ", posOffsetX=" + posOffsetX +
                ", posOffsetY=" + posOffsetY +
                ", baseWidth=" + baseWidth +
                ", baseHeight=" + baseHeight +
                ", anchorPoint=" + anchorPoint.getName() +
                ", stickyAnchor=" + stickyAnchor +
                ", uniqueIdentifier='" + uniqueIdentifier + '\'' +
                '}';
    }

    @SuppressWarnings("all")
    @NotNull
    @Override
    protected AnimationKeyframe clone() {
        AnimationKeyframe clone = new AnimationKeyframe(this.timestamp, this.posOffsetX, this.posOffsetY, this.baseWidth, this.baseHeight, this.anchorPoint, this.stickyAnchor);
        clone.uniqueIdentifier = this.uniqueIdentifier;
        return clone;
    }

}
