package de.keksuccino.fancymenu.util.level.raid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable state of a raid, as a player in its area sees it.
 */
public final class RaidSnapshot {

    public static final String RAIDER_LIST_SEPARATOR = ",";
    public static final String RAIDER_ENTRY_UUID_SEPARATOR = "#";

    private final int totalWaves;
    private final int currentWave;
    @NotNull
    private final List<String> raiders;
    @Nullable
    private volatile String joinedRaiders;

    /**
     * @param totalWaves All waves of the raid, including its bonus wave if it has one.
     * @param currentWave The number of waves that spawned so far.
     * @param raiders The alive raiders of the raid, built via {@link RaidSnapshot#formatRaiderEntry(String, UUID)}.
     */
    public RaidSnapshot(int totalWaves, int currentWave, @NotNull List<String> raiders) {
        this.totalWaves = Math.max(0, totalWaves);
        this.currentWave = Math.max(0, currentWave);
        // Returns the same instance for lists that are already immutable, so cached raider lists don't get copied every tick
        this.raiders = List.copyOf(raiders);
    }

    @NotNull
    public static String formatRaiderEntry(@NotNull String entityTypeKey, @NotNull UUID uuid) {
        return entityTypeKey + RAIDER_ENTRY_UUID_SEPARATOR + uuid;
    }

    public int totalWaves() {
        return this.totalWaves;
    }

    public int currentWave() {
        return this.currentWave;
    }

    public int remainingWaves() {
        return Math.max(0, this.totalWaves - this.currentWave);
    }

    @NotNull
    public List<String> raiders() {
        return this.raiders;
    }

    public int aliveRaidersCount() {
        return this.raiders.size();
    }

    /**
     * @return All raider entries separated by {@link RaidSnapshot#RAIDER_LIST_SEPARATOR} without spaces.
     */
    @NotNull
    public String joinRaiders() {
        String joined = this.joinedRaiders;
        if (joined == null) {
            joined = String.join(RAIDER_LIST_SEPARATOR, this.raiders);
            this.joinedRaiders = joined;
        }
        return joined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RaidSnapshot other)) return false;
        return (this.totalWaves == other.totalWaves) && (this.currentWave == other.currentWave) && this.raiders.equals(other.raiders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.totalWaves, this.currentWave, this.raiders);
    }

    @Override
    public String toString() {
        return "RaidSnapshot{totalWaves=" + this.totalWaves + ", currentWave=" + this.currentWave + ", raiders=" + this.raiders + "}";
    }

}
