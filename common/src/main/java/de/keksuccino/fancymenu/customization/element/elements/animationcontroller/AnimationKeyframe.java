package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;

public class AnimationKeyframe {

    public final long timestamp;
    public final int posOffsetX;
    public final int posOffsetY;
    public final int baseWidth;
    public final int baseHeight;
    public ElementAnchorPoint anchorPoint;
    public final boolean stickyAnchor;

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
                ", anchorPoint=" + anchorPoint +
                ", stickyAnchor=" + stickyAnchor +
                '}';
    }

}
