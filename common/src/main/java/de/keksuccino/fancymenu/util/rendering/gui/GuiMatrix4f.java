package de.keksuccino.fancymenu.util.rendering.gui;

public class GuiMatrix4f extends com.mojang.math.Matrix4f {

    /**
     * Set this matrix to be a simple scale matrix.
     * <p>
     * The resulting matrix can be multiplied against another transformation
     * matrix to obtain an additional scaling.
     * <p>
     * In order to post-multiply a scaling transformation directly to a
     * matrix, use {@link #scale(float, float, float) scale()} instead.
     *
     * @param x the scale in x
     * @param y the scale in y
     * @param z the scale in z
     * @return this
     */
    public GuiMatrix4f scaling(float x, float y, float z) {
        // Reset to identity first
        setIdentity();
        // Set the diagonal elements to the scaling factors
        this.m00 = x;
        this.m11 = y;
        this.m22 = z;
        return this;
    }

}
