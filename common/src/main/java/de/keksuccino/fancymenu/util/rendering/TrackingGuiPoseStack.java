package de.keksuccino.fancymenu.util.rendering;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

/**
 * GUI matrix stack that publishes FancyMenu's additional render transforms on both loaders.
 * NeoForge loads JOML outside its transforming class loader, so this must be installed at
 * GuiGraphicsExtractor's allocation site instead of injecting into Matrix3x2fStack itself.
 */
public final class TrackingGuiPoseStack extends Matrix3x2fStack {

    private Frame[] frames;
    private int depth;

    public TrackingGuiPoseStack(int stackSize) {
        super(stackSize);
        this.frames = new Frame[stackSize];
        for (int i = 0; i < stackSize; i++) this.frames[i] = new Frame();
    }

    @Override
    public Matrix3x2fStack pushMatrix() {
        // Let JOML validate capacity before changing tracking state. Reuse frames to avoid allocations per draw.
        super.pushMatrix();
        this.frames[this.depth + 1].copyFrom(this.frames[this.depth]);
        this.depth++;
        this.publish();
        return this;
    }

    @Override
    public Matrix3x2fStack popMatrix() {
        super.popMatrix();
        this.depth--;
        this.publish();
        return this;
    }

    @Override
    public Matrix3x2fStack clear() {
        super.clear();
        this.depth = 0;
        this.frames[0].reset();
        this.publish();
        return this;
    }

    // JOML's scalar/vector convenience overloads delegate to these destination overloads.
    // Track only in-place operations, exactly once; writing another matrix must not change our active state.
    @Override
    public Matrix3x2f translate(float x, float y, Matrix3x2f dest) {
        Matrix3x2f result = super.translate(x, y, dest);
        if (dest == this) {
            Frame frame = this.frames[this.depth];
            frame.translationX += x * frame.scale;
            frame.translationY += y * frame.scale;
            RenderTranslationUtil.setActiveRenderTranslation_FancyMenu(frame.translationX, frame.translationY, 0.0F);
        }
        return result;
    }

    @Override
    public Matrix3x2f scale(float x, float y, Matrix3x2f dest) {
        Matrix3x2f result = super.scale(x, y, dest);
        if (dest == this) {
            Frame frame = this.frames[this.depth];
            frame.scale *= RenderScaleUtil.getAbsoluteScaleFactor_FancyMenu(x, y, 0.0F);
            RenderScaleUtil.setActiveRenderScale_FancyMenu(frame.scale);
        }
        return result;
    }

    @Override
    public Matrix3x2f rotate(float angle, Matrix3x2f dest) {
        Matrix3x2f result = super.rotate(angle, dest);
        if (dest == this) {
            Frame frame = this.frames[this.depth];
            frame.rotationRadians += angle;
            frame.rotation.setIdentity();
            frame.rotation.z = (float)Math.sin(frame.rotationRadians * 0.5);
            frame.rotation.w = (float)Math.cos(frame.rotationRadians * 0.5);
            RenderRotationUtil.setActiveRenderRotation_FancyMenu(frame.rotation);
        }
        return result;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        TrackingGuiPoseStack copy = (TrackingGuiPoseStack)super.clone();
        // JOML copies its own matrices; copy our frames too so a cloned stack cannot mutate the original's tracking.
        copy.frames = new Frame[this.frames.length];
        for (int i = 0; i < this.frames.length; i++) {
            copy.frames[i] = new Frame();
            copy.frames[i].copyFrom(this.frames[i]);
        }
        return copy;
    }

    private void publish() {
        Frame frame = this.frames[this.depth];
        RenderScaleUtil.setActiveRenderScale_FancyMenu(frame.scale);
        RenderTranslationUtil.setActiveRenderTranslation_FancyMenu(frame.translationX, frame.translationY, 0.0F);
        RenderRotationUtil.setActiveRenderRotation_FancyMenu(frame.rotation);
    }

    private static final class Frame {

        private float scale = 1.0F;
        private float translationX;
        private float translationY;
        private double rotationRadians;
        private final RenderRotationUtil.RotationState rotation = new RenderRotationUtil.RotationState();

        private void copyFrom(Frame other) {
            this.scale = other.scale;
            this.translationX = other.translationX;
            this.translationY = other.translationY;
            this.rotationRadians = other.rotationRadians;
            this.rotation.set(other.rotation);
        }

        private void reset() {
            this.scale = 1.0F;
            this.translationX = 0.0F;
            this.translationY = 0.0F;
            this.rotationRadians = 0.0;
            this.rotation.setIdentity();
        }

    }

}
