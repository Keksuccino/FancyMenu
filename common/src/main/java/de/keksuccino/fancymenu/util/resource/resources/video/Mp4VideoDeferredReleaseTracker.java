package de.keksuccino.fancymenu.util.resource.resources.video;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Owns a player detached by {@link Mp4Video#close()} until exactly one cleanup path claims it. Sound reload cleanup
 * can preempt the deferred path, invalidating every already-queued task before the old OpenAL context is destroyed.
 */
final class Mp4VideoDeferredReleaseTracker {

    record SoundReloadClaim(@Nullable Object activePlayer, @Nullable Object closeDetachedPlayer) {
    }

    enum ClaimResult {
        LOST,
        DEFER,
        CLAIMED
    }

    @Nullable
    private Object player;
    private long requestVersion = 0L;

    synchronized long schedule(@Nullable Object player) {
        this.requestVersion++;
        this.player = player;
        return this.requestVersion;
    }

    synchronized boolean runIfOwned(@NotNull Object player, long requestVersion, @NotNull Runnable action) {
        if (!this.isOwned(player, requestVersion)) return false;
        action.run();
        return true;
    }

    synchronized ClaimResult claimAndReleaseWhenReady(@NotNull Object player, long requestVersion, @NotNull BooleanSupplier readyToRelease, @NotNull Runnable releaseAction) {
        if (!this.isOwned(player, requestVersion)) return ClaimResult.LOST;
        if (!readyToRelease.getAsBoolean()) return ClaimResult.DEFER;
        this.player = null;
        this.requestVersion++;
        // Keep the monitor until release returns so reload HEAD cannot destroy the context at the claim/release boundary.
        releaseAction.run();
        return ClaimResult.CLAIMED;
    }

    @NotNull
    SoundReloadClaim claimForSoundEngineReload(@NotNull Object playerLifecycleLock, @NotNull Supplier<Object> activePlayerClaim) {
        synchronized (playerLifecycleLock) {
            Object closeDetachedPlayer = this.claimCloseDetachedPlayerForSoundEngineReload();
            Object activePlayer = activePlayerClaim.get();
            return new SoundReloadClaim(activePlayer, closeDetachedPlayer);
        }
    }

    @Nullable
    private synchronized Object claimCloseDetachedPlayerForSoundEngineReload() {
        Object claimedPlayer = this.player;
        if (claimedPlayer == null) return null;
        this.player = null;
        this.requestVersion++;
        return claimedPlayer;
    }

    synchronized boolean hasPendingPlayer() {
        return this.player != null;
    }

    synchronized boolean isOwned(@NotNull Object player, long requestVersion) {
        return this.requestVersion == requestVersion && this.player == player;
    }
}
