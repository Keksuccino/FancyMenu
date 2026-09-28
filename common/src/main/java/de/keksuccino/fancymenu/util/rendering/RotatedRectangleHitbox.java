package de.keksuccino.fancymenu.util.rendering;

/** Shared hit testing and pointer coordinates for element requirements and their widgets. */
public final class RotatedRectangleHitbox {

    private float rotationDegrees;
    private float verticalTiltDegrees;
    private float horizontalTiltDegrees;
    private boolean transformed;
    private double inverse00;
    private double inverse01;
    private double inverse10;
    private double inverse11;

    public void setRotation(float rotationDegrees, float verticalTiltDegrees, float horizontalTiltDegrees) {
        if ((this.rotationDegrees == rotationDegrees) && (this.verticalTiltDegrees == verticalTiltDegrees) && (this.horizontalTiltDegrees == horizontalTiltDegrees)) return;
        this.rotationDegrees = rotationDegrees;
        this.verticalTiltDegrees = verticalTiltDegrees;
        this.horizontalTiltDegrees = horizontalTiltDegrees;
        this.transformed = false;
        if ((rotationDegrees == 0.0F) && (verticalTiltDegrees == 0.0F) && (horizontalTiltDegrees == 0.0F)) return;

        double rotation = Math.toRadians(rotationDegrees);
        double verticalTilt = Math.toRadians(verticalTiltDegrees);
        double horizontalTilt = Math.toRadians(horizontalTiltDegrees);
        double sinX = Math.sin(verticalTilt);
        double cosX = Math.cos(verticalTilt);
        double sinY = Math.sin(horizontalTilt);
        double cosY = Math.cos(horizontalTilt);
        double sinZ = Math.sin(rotation);
        double cosZ = Math.cos(rotation);

        // Match rendering's Rx(vertical tilt) * Ry(horizontal tilt) * Rz(rotation) order.
        // Invert its projected XY matrix, not the 3D rotation: tilting compresses the screen footprint.
        double m00 = cosY * cosZ;
        double m01 = -cosY * sinZ;
        double m10 = sinX * sinY * cosZ + cosX * sinZ;
        double m11 = cosX * cosZ - sinX * sinY * sinZ;
        double determinant = m00 * m11 - m01 * m10;
        // Preserve the widget's untransformed fallback for invalid or edge-on projections.
        if (!Double.isFinite(determinant) || (Math.abs(determinant) < 1.0E-6)) return;
        this.inverse00 = m11 / determinant;
        this.inverse01 = -m01 / determinant;
        this.inverse10 = -m10 / determinant;
        this.inverse11 = m00 / determinant;
        this.transformed = true;
    }

    public boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        if ((width <= 0) || (height <= 0)) return false;
        if (!this.transformed) return (mouseX >= x) && (mouseY >= y) && (mouseX < (double)x + width) && (mouseY < (double)y + height);
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double dx = mouseX - (x + halfWidth);
        double dy = mouseY - (y + halfHeight);
        double localX = this.inverseTransformX(dx, dy);
        double localY = this.inverse10 * dx + this.inverse11 * dy;
        return (localX >= -halfWidth) && (localX < halfWidth) && (localY >= -halfHeight) && (localY < halfHeight);
    }

    /**
     * Maps a screen point to the widget's untransformed GUI X coordinate, using the same center and
     * inverse as hit testing. Do not clamp to the hitbox: captured slider drags can leave its bounds.
     * Bounds are supplied on every call so moving or resizing a widget cannot leave a stale center.
     */
    public double untransformX(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (!this.transformed) return mouseX;
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        return centerX + this.inverseTransformX(mouseX - centerX, mouseY - centerY);
    }

    public boolean isTransformed() {
        return this.transformed;
    }

    public float getRotationDegrees() {
        return this.rotationDegrees;
    }

    public float getVerticalTiltDegrees() {
        return this.verticalTiltDegrees;
    }

    public float getHorizontalTiltDegrees() {
        return this.horizontalTiltDegrees;
    }

    private double inverseTransformX(double offsetX, double offsetY) {
        return this.inverse00 * offsetX + this.inverse01 * offsetY;
    }

}
