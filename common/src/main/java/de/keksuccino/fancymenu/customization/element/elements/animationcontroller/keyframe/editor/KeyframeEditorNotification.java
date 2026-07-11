package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** One fading notification displayed by the keyframe manager. */
final class KeyframeEditorNotification {

    private static final long FADE_DURATION_MS = 500L;

    private final Component message;
    private final long startTime;
    private final long duration;

    public KeyframeEditorNotification(@NotNull Component message, long duration) {
        this.message = message;
        this.startTime = System.currentTimeMillis();
        this.duration = Math.max(0L, duration);
    }

    @NotNull
    public Component getMessage() {
        return this.message;
    }

    public boolean isExpired(long currentTime) {
        return currentTime - this.startTime > this.duration;
    }

    public float getOpacity(long currentTime) {
        long elapsedTime = currentTime - this.startTime;
        long fadeStartTime = Math.max(0L, this.duration - FADE_DURATION_MS);
        if (elapsedTime <= fadeStartTime) return 1.0F;
        float opacity = 1.0F - ((elapsedTime - fadeStartTime) / (float)FADE_DURATION_MS);
        return Math.max(0.05F, Math.min(1.0F, opacity));
    }

    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 2;
    }

}
