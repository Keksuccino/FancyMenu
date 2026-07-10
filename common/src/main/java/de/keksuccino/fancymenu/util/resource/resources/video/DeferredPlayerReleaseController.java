package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Owns a player scheduled for deferred close cleanup. Claiming transfers ownership exactly once and invalidates every
 * already-queued task, which is critical when a sound-engine reload must perform the release early.
 */
final class DeferredPlayerReleaseController<T> {

    @Nullable
    private T pending;
    private long token = 0L;

    synchronized long schedule(@NotNull T pending) {
        this.token++;
        this.pending = pending;
        return this.token;
    }

    synchronized boolean isScheduled(long token) {
        return (this.pending != null) && (this.token == token);
    }

    synchronized boolean claimScheduled(@NotNull T pending, long token) {
        if ((this.pending != pending) || (this.token != token)) return false;
        this.pending = null;
        this.token++;
        return true;
    }

    @Nullable
    synchronized T claimForSoundEngineReload() {
        T claimed = this.pending;
        if (claimed == null) return null;
        this.pending = null;
        this.token++;
        return claimed;
    }
}
