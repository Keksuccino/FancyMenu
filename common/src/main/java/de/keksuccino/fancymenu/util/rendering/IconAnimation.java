package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class IconAnimation {

    private final float durationMs;
    @NotNull
    private final OffsetProvider offsetProvider;

    public IconAnimation(float durationMs, @NotNull OffsetProvider offsetProvider) {
        this.durationMs = Math.max(1.0F, durationMs);
        this.offsetProvider = Objects.requireNonNull(offsetProvider);
    }

    public float getDurationMs() {
        return this.durationMs;
    }

    @NotNull
    public Instance createInstance() {
        return new Instance(this);
    }

    @NotNull
    public Offset getOffset(float elapsedMs) {
        if (elapsedMs <= 0.0F || elapsedMs >= this.durationMs) {
            return Offset.ZERO;
        }
        float t = elapsedMs / this.durationMs;
        return this.offsetProvider.getOffset(t);
    }

    public float getOffsetX(float elapsedMs) {
        return this.getOffset(elapsedMs).x();
    }

    public float getOffsetY(float elapsedMs) {
        return this.getOffset(elapsedMs).y();
    }

    public float getRotationDegrees(float elapsedMs) {
        return this.getOffset(elapsedMs).rotationDegrees();
    }

    public float getWidthOffset(float elapsedMs) {
        return this.getOffset(elapsedMs).widthOffset();
    }

    public float getHeightOffset(float elapsedMs) {
        return this.getOffset(elapsedMs).heightOffset();
    }

    @NotNull
    public IconAnimation getAnimation() {
        return this;
    }

    @FunctionalInterface
    public interface OffsetProvider {
        @NotNull
        Offset getOffset(float t);
    }

    public static final class Instance {
        @NotNull
        private final IconAnimation animation;
        private long startMs = -1L;

        private Instance(@NotNull IconAnimation animation) {
            this.animation = animation;
        }

        public void start() {
            this.startMs = Util.getMillis();
        }

        public void reset() {
            this.startMs = -1L;
        }

        public boolean isRunning() {
            if (this.startMs < 0L) {
                return false;
            }
            float elapsedMs = (float) (Util.getMillis() - this.startMs);
            if (elapsedMs >= this.animation.durationMs) {
                this.startMs = -1L;
                return false;
            }
            return true;
        }

        @NotNull
        public Offset getOffset() {
            if (this.startMs < 0L) {
                return Offset.ZERO;
            }
            float elapsedMs = (float) (Util.getMillis() - this.startMs);
            Offset offset = this.animation.getOffset(elapsedMs);
            if (elapsedMs >= this.animation.durationMs) {
                this.startMs = -1L;
            }
            return offset;
        }

        public float getOffsetX() {
            return this.getOffset().x();
        }

        public float getOffsetY() {
            return this.getOffset().y();
        }

        public float getRotationDegrees() {
            return this.getOffset().rotationDegrees();
        }

        public float getWidthOffset() {
            return this.getOffset().widthOffset();
        }

        public float getHeightOffset() {
            return this.getOffset().heightOffset();
        }

        @NotNull
        public IconAnimation getAnimation() {
            return this.animation;
        }
    }

    public static final class Offset {
        public static final Offset ZERO = new Offset(0.0F, 0.0F);

        private final float x;
        private final float y;
        private final float rotationDegrees;
        private final float widthOffset;
        private final float heightOffset;

        public Offset(float x, float y) {
            this(x, y, 0.0F);
        }

        public Offset(float x, float y, float rotationDegrees) {
            this(x, y, rotationDegrees, 0.0F, 0.0F);
        }

        public Offset(float x, float y, float rotationDegrees, float widthOffset, float heightOffset) {
            this.x = x;
            this.y = y;
            this.rotationDegrees = rotationDegrees;
            this.widthOffset = widthOffset;
            this.heightOffset = heightOffset;
        }

        public float x() {
            return this.x;
        }

        public float y() {
            return this.y;
        }

        public float rotationDegrees() {
            return this.rotationDegrees;
        }

        public float widthOffset() {
            return this.widthOffset;
        }

        public float heightOffset() {
            return this.heightOffset;
        }
    }
}
