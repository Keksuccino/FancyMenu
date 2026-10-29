package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.util.resource.PlayableResource;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Decides when the game intro finished playing.<br>
 * Once reached, the end stays reached, so the intro can't come back when its shared resource gets restarted later, like by the target screen using the same resource.
 */
public final class GameIntroEndTracker {

    /** Resources can need a moment to actually start playing, so a resource that is not playing only counts as finished after this delay. **/
    static final long START_GRACE_PERIOD_MS = 2000L;

    @NotNull
    private final LongSupplier clock;
    private long startTimeMs = -1L;
    private boolean endReached = false;

    public GameIntroEndTracker() {
        this(System::currentTimeMillis);
    }

    GameIntroEndTracker(@NotNull LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean isStarted() {
        return this.startTimeMs != -1L;
    }

    public void markStarted() {
        this.startTimeMs = this.clock.getAsLong();
    }

    /**
     * @param ignoreLoops if the intro should end after its first full play cycle, even if the resource would loop it
     */
    public boolean checkEndReached(@NotNull PlayableResource intro, boolean ignoreLoops) {
        if (this.endReached) return true;
        if (!this.isStarted()) return false;
        boolean stoppedPlaying = ((this.startTimeMs + START_GRACE_PERIOD_MS) < this.clock.getAsLong()) && !intro.isPlaying();
        this.endReached = stoppedPlaying || (ignoreLoops && (intro.getCompletedPlayCycles() > 0));
        return this.endReached;
    }

}
