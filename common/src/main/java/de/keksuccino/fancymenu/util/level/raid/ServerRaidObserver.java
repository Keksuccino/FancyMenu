package de.keksuccino.fancymenu.util.level.raid;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.raid.ActiveRaidPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connects a vanilla {@link Raid} to its {@link RaidAudienceTracker} and sends the resulting events to FancyMenu clients.
 * Every raid owns exactly one observer, which gets fed by {@code MixinRaid}.
 * <p>
 * Errors never escape into vanilla's raid tick, since an exception there would crash the server.
 */
public class ServerRaidObserver {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger NEXT_RAID_ID = new AtomicInteger();
    // Stable order, so the raider list doesn't shuffle every time a raider joins or dies
    private static final Comparator<Raider> RAIDER_ORDER = Comparator.comparingInt(Raider::getWave).thenComparingInt(Entity::getId);

    private final int raidId = NEXT_RAID_ID.incrementAndGet();
    private final RaidAudienceTracker<ServerPlayer> tracker = new RaidAudienceTracker<>();
    private final RaidAudienceTracker.Sink<ServerPlayer> sink = this::send;
    @Nullable
    private Set<Raider> cachedRaiders;
    @NotNull
    private List<String> cachedRaiderEntries = List.of();
    private boolean errorLogged;

    /**
     * Has to be called for raids that just got started, but not for raids loaded from a save.
     */
    public void markFreshlyCreated() {
        this.tracker.markFreshlyCreated();
    }

    public void onAudienceUpdated(@NotNull ServerBossEvent raidBar) {
        try {
            this.tracker.onAudienceUpdated(getAudience(raidBar));
        } catch (Exception ex) {
            this.logError(ex);
        }
    }

    public void onTick(@NotNull Raid raid, @NotNull ServerBossEvent raidBar, int totalWaves) {
        try {
            this.tracker.onTick(getAudience(raidBar), ServerRaidObserver::canReceive, raid.isOver(), raid.isVictory(), () -> this.buildSnapshot(raid, totalWaves), this.sink);
        } catch (Exception ex) {
            this.logError(ex);
        }
    }

    public void onStopped() {
        try {
            this.tracker.onStopped(this.sink);
        } catch (Exception ex) {
            this.logError(ex);
        }
        this.cachedRaiders = null;
        this.cachedRaiderEntries = List.of();
    }

    @NotNull
    private static Collection<ServerPlayer> getAudience(@NotNull ServerBossEvent raidBar) {
        // Vanilla hides the raid bar while the raid center is not loaded, but keeps its players, so they don't see the raid during that time
        return raidBar.isVisible() ? raidBar.getPlayers() : Set.of();
    }

    private static boolean canReceive(@NotNull ServerPlayer player) {
        return (player.connection != null) && PacketHandler.isFancyMenuClient(player);
    }

    @NotNull
    private RaidSnapshot buildSnapshot(@NotNull Raid raid, int totalWaves) {
        Set<Raider> raiders = raid.getAllRaiders();
        if (!raiders.equals(this.cachedRaiders)) {
            this.cachedRaiders = raiders;
            this.cachedRaiderEntries = buildRaiderEntries(raiders);
        }
        return new RaidSnapshot(totalWaves, raid.getGroupsSpawned(), this.cachedRaiderEntries);
    }

    @NotNull
    private static List<String> buildRaiderEntries(@NotNull Collection<Raider> raiders) {
        List<Raider> sorted = new ArrayList<>(raiders);
        sorted.sort(RAIDER_ORDER);
        List<String> entries = new ArrayList<>(sorted.size());
        for (Raider raider : sorted) {
            entries.add(RaidSnapshot.formatRaiderEntry(EntityType.getKey(raider.getType()).toString(), raider.getUUID()));
        }
        return List.copyOf(entries);
    }

    private void send(@NotNull ServerPlayer player, @NotNull RaidAudienceEvent event, @Nullable RaidSnapshot snapshot, boolean victory) {
        PacketHandler.sendToClient(player, ActiveRaidPacket.create(this.raidId, event, snapshot, victory));
    }

    private void logError(@NotNull Exception ex) {
        if (this.errorLogged) return;
        this.errorLogged = true;
        LOGGER.error("[FANCYMENU] Failed to update the raid state for raid listeners, placeholders and requirements! Further errors for this raid get suppressed.", ex);
    }

}
