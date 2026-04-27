package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AfmaEncodeProgress(
        @NotNull Phase phase,
        @NotNull String task,
        @Nullable String detail,
        double progress
) {

    public AfmaEncodeProgress {
        progress = Math.max(0.0D, Math.min(1.0D, progress));
    }

    public static @NotNull AfmaEncodeProgress idle() {
        return new AfmaEncodeProgress(Phase.IDLE, "Idle", null, 0.0D);
    }

    public enum Phase {
        IDLE,
        VALIDATING_SOURCES,
        ANALYZING_FRAMES,
        PACKING_ARCHIVE,
        COMPLETE,
        FAILED,
        CANCELLED
    }

}
