package de.keksuccino.fancymenu.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Tracks the committed frame index for one shader pass whose program or render history can restart independently. */
final class GlslPassFrameState {

    @Nullable
    private String historyIdentity;
    private long committedFrameCount;

    boolean historyIdentityChanged(@NotNull String identity) {
        return !identity.equals(this.historyIdentity);
    }

    void activate(@NotNull String identity, boolean storageRecreated) {
        if (storageRecreated || this.historyIdentityChanged(identity)) {
            this.committedFrameCount = 0L;
        }
        this.historyIdentity = identity;
    }

    void deactivate() {
        this.historyIdentity = null;
        this.committedFrameCount = 0L;
    }

    int currentFrame() {
        return (int) Math.min(Integer.MAX_VALUE, this.committedFrameCount);
    }

    long committedFrameCount() {
        return this.committedFrameCount;
    }

    void commitFrame() {
        if (this.committedFrameCount < Long.MAX_VALUE) {
            this.committedFrameCount++;
        }
    }
}
