package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

/**
 * Converts animation keyframe values from the parent layout editor's logical coordinate space to the keyframe
 * manager's display coordinate space. The manager can use a lower GUI scale to fit its controls, so these spaces
 * must not be treated as interchangeable.
 */
final class AnimationPreviewViewport {

    private int sourceWidth;
    private int sourceHeight;
    private int displayWidth;
    private int displayHeight;

    public void update(int sourceWidth, int sourceHeight, int displayWidth, int displayHeight) {
        this.sourceWidth = Math.max(1, sourceWidth);
        this.sourceHeight = Math.max(1, sourceHeight);
        this.displayWidth = Math.max(1, displayWidth);
        this.displayHeight = Math.max(1, displayHeight);
    }

    public int getDisplayWidth() {
        return this.displayWidth;
    }

    public int getDisplayHeight() {
        return this.displayHeight;
    }

    public int toDisplayX(int sourceValue) {
        return scale(sourceValue, this.displayWidth, this.sourceWidth);
    }

    public int toDisplayY(int sourceValue) {
        return scale(sourceValue, this.displayHeight, this.sourceHeight);
    }

    public int toSourceX(int displayValue) {
        return scale(displayValue, this.sourceWidth, this.displayWidth);
    }

    public int toSourceY(int displayValue) {
        return scale(displayValue, this.sourceHeight, this.displayHeight);
    }

    private static int scale(int value, int targetSize, int sourceSize) {
        return (int)Math.round((double)value * (double)targetSize / (double)sourceSize);
    }

}
