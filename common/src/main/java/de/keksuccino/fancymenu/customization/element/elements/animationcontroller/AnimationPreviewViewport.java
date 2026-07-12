package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

/**
 * Converts animation values between the parent layout's logical coordinate space and the keyframe manager's visible
 * preview coordinate space. Every dimension is kept positive so conversions remain defined during early screen init.
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
