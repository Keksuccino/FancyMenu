package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import org.jetbrains.annotations.NotNull;

/**
 * Serializable state of an animated element at one point on the controller timeline.
 *
 * <p>The fields remain public because controller layouts store keyframes directly through Gson. Runtime and editor
 * code should use {@link #copy()} whenever a keyframe crosses an ownership boundary so history snapshots and running
 * animations cannot accidentally share mutable state.</p>
 */
public class AnimationKeyframe {

    public long timestamp;
    public int posOffsetX;
    public int posOffsetY;
    public int baseWidth;
    public int baseHeight;
    public ElementAnchorPoint anchorPoint;
    public boolean stickyAnchor;
    @NotNull
    public String uniqueIdentifier = ScreenCustomization.generateUniqueIdentifier();

    public AnimationKeyframe() {
    }

    public AnimationKeyframe(long timestamp, int posOffsetX, int posOffsetY, int baseWidth, int baseHeight, ElementAnchorPoint anchorPoint, boolean stickyAnchor) {
        this.timestamp = timestamp;
        this.posOffsetX = posOffsetX;
        this.posOffsetY = posOffsetY;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.anchorPoint = anchorPoint;
        this.stickyAnchor = stickyAnchor;
    }

    @NotNull
    public AnimationKeyframe copy() {
        AnimationKeyframe copy = new AnimationKeyframe(this.timestamp, this.posOffsetX, this.posOffsetY, this.baseWidth, this.baseHeight, this.anchorPoint, this.stickyAnchor);
        copy.uniqueIdentifier = ((this.uniqueIdentifier == null) || this.uniqueIdentifier.isBlank()) ? ScreenCustomization.generateUniqueIdentifier() : this.uniqueIdentifier;
        return copy;
    }

    @Override
    public String toString() {
        return "AnimationKeyframe{" +
                "timestamp=" + timestamp +
                ", posOffsetX=" + posOffsetX +
                ", posOffsetY=" + posOffsetY +
                ", baseWidth=" + baseWidth +
                ", baseHeight=" + baseHeight +
                ", anchorPoint=" + (anchorPoint != null ? anchorPoint.getName() : "null") +
                ", stickyAnchor=" + stickyAnchor +
                ", uniqueIdentifier='" + uniqueIdentifier + '\'' +
                '}';
    }

}
