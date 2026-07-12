package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/** Timeline geometry and duration calculations used by rendering and input handling. */
final class KeyframeTimeline {

    public static final int HEIGHT = 50;
    public static final int Y_PADDING = 20;
    public static final int MIN_DURATION = 1000;
    public static final int EXTENSION_STEP = 2000;
    public static final long PADDING_DURATION = 2000L;

    private int x;
    private int y;
    private int width;
    private long duration = MIN_DURATION;

    public KeyframeTimeline(@NotNull List<AnimationKeyframe> keyframes) {
        this.updateDuration(AnimationKeyframeSequence.getMaxTimestamp(keyframes));
    }

    public void updateBounds(int screenWidth, int screenHeight) {
        this.x = 50;
        this.width = Math.max(1, screenWidth - 100);
        this.y = screenHeight - HEIGHT - Y_PADDING;
    }

    public void updateDurationToKeyframes(@NotNull List<AnimationKeyframe> keyframes, boolean recording) {
        if (!recording) this.updateDuration(AnimationKeyframeSequence.getMaxTimestamp(keyframes));
    }

    public void updateDuration(long durationWithoutPadding) {
        long paddedDuration = durationWithoutPadding > Long.MAX_VALUE - PADDING_DURATION ? Long.MAX_VALUE : durationWithoutPadding + PADDING_DURATION;
        this.duration = Math.max(MIN_DURATION, paddedDuration);
    }

    public void extend() {
        long endTime = this.getEndTime();
        this.updateDuration(endTime > Long.MAX_VALUE - EXTENSION_STEP ? Long.MAX_VALUE : endTime + EXTENSION_STEP);
    }

    public int timestampToX(long timestamp) {
        return this.x + (int)((double)this.width * (double)timestamp / (double)this.duration);
    }

    public long xToTimestamp(double mouseX) {
        double progress = Math.max(0.0D, Math.min(1.0D, (mouseX - this.x) / this.width));
        return (long)(this.duration * progress);
    }

    public boolean contains(double mouseX, double mouseY) {
        return (mouseX >= this.x) && (mouseX <= this.x + this.width) && (mouseY >= this.y) && (mouseY <= this.y + HEIGHT);
    }

    public int findKeyframeIndex(@NotNull List<AnimationKeyframe> keyframes, double mouseX, double mouseY, int hitRadius) {
        if ((mouseY < this.y) || (mouseY > this.y + HEIGHT)) return -1;
        for (int index = 0; index < keyframes.size(); index++) {
            int lineX = this.timestampToX(keyframes.get(index).timestamp);
            if ((mouseX >= lineX - hitRadius) && (mouseX <= lineX + hitRadius)) return index;
        }
        return -1;
    }

    public boolean isOverTimestamp(double mouseX, double mouseY, long timestamp, int hitRadius) {
        int timestampX = this.timestampToX(timestamp);
        return (mouseY >= this.y) && (mouseY <= this.y + HEIGHT) && (mouseX >= timestampX - hitRadius) && (mouseX <= timestampX + hitRadius);
    }

    @NotNull
    public String formatTime(long milliseconds) {
        if (this.duration < 2000L) return milliseconds + "ms";
        return String.format(Locale.ROOT, "%.1fs", milliseconds / 1000.0F);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public long getDuration() {
        return this.duration;
    }

    public long getEndTime() {
        return Math.max(0L, this.duration - PADDING_DURATION);
    }

}
