package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

/**
 * Converts serialized keyframe values from the parent editor coordinate space to the manager's display space.
 * The manager may temporarily use a lower GUI scale to fit its controls, so these spaces are not interchangeable.
 */
final class AnimationPreviewViewport {

    private int sourceWidth = 1;
    private int sourceHeight = 1;
    private int displayWidth = 1;
    private int displayHeight = 1;

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
