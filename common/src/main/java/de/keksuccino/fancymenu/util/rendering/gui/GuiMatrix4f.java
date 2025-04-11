package de.keksuccino.fancymenu.util.rendering.gui;

import com.mojang.math.Matrix4f;
import org.jetbrains.annotations.NotNull;

public class GuiMatrix4f extends com.mojang.math.Matrix4f {

    public GuiMatrix4f(@NotNull Matrix4f matrix4f) {
        super(matrix4f);
    }

    public GuiMatrix4f() {
        super();
    }

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

    public float getM00() {
        return m00;
    }

    public float getM01() {
        return m01;
    }

    public float getM02() {
        return m02;
    }

    public float getM03() {
        return m03;
    }

    public float getM10() {
        return m10;
    }

    public float getM11() {
        return m11;
    }

    public float getM12() {
        return m12;
    }

    public float getM13() {
        return m13;
    }

    public float getM20() {
        return m20;
    }

    public float getM21() {
        return m21;
    }

    public float getM22() {
        return m22;
    }

    public float getM23() {
        return m23;
    }

    public float getM30() {
        return m30;
    }

    public float getM31() {
        return m31;
    }

    public float getM32() {
        return m32;
    }

    public float getM33() {
        return m33;
    }

}
