package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;

public class AnimationKeyframe {

    public long timestamp;
    public int posOffsetX;
    public int posOffsetY;
    public int baseWidth;
    public int baseHeight;
    public ElementAnchorPoint anchorPoint;
    public boolean stickyAnchor;

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
