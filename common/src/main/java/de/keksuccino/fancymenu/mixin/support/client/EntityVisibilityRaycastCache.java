package de.keksuccino.fancymenu.mixin.support.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Reuses entity-visibility block raycasts for one exact render context within one game tick.
 *
 * <p>A one-tick lifetime turns repeated render-frame raycasts into at most one raycast per stable entity per tick,
 * while limiting an unkeyed intervening block change to the tick in which it happened (normally at most 50 ms).
 * Camera and target movement are compared at their exact ray endpoints, so interpolated movement immediately misses
 * instead of reusing geometry from another partial tick.</p>
 *
 * <p>The cache is render-thread confined. World, observer, and target identities are weak so a discarded level or
 * entity can never be retained by this controller. Entries are updated in place across ticks and camera-state changes
 * to avoid rebuilding the map when every ray needs to be refreshed. New targets are not admitted after the
 * tick-and-context-local capacity is reached: evicting current entries during the renderer's stable sequential entity
 * scan would evict future hits before they are visited on the next frame.</p>
 */
public final class EntityVisibilityRaycastCache {

    public static final int DEFAULT_MAXIMUM_ENTRIES = 1024;

    private final int maximumEntries;
    private final Map<UUID, Entry> entries = new HashMap<>();
    @Nullable private WeakReference<Object> worldReference;
    @Nullable private WeakReference<Object> observerReference;
    @Nullable private ObserverState observerState;
    private long gameTick;
    private long contextGeneration;
    private int currentContextEntryCount;
    private boolean prepared;

    public EntityVisibilityRaycastCache() {
        this(DEFAULT_MAXIMUM_ENTRIES);
    }

    EntityVisibilityRaycastCache(int maximumEntries) {
        if (maximumEntries <= 0) throw new IllegalArgumentException("maximumEntries must be positive");
        this.maximumEntries = maximumEntries;
    }

    public void beginFrame(@NotNull Object world, long gameTick, @NotNull Object observer, @NotNull ObserverState observerState) {
        Objects.requireNonNull(observerState, "observerState");
        this.beginFrame(world, gameTick, observer, observerState.rayX, observerState.rayY, observerState.rayZ, observerState.entityBottomY, observerState.descending, observerState.fallDistance, observerState.mainHandItem, observerState.feetItem);
    }

    public void beginFrame(@NotNull Object world, long gameTick, @NotNull Object observer, double rayX, double rayY, double rayZ, double entityBottomY, boolean descending, double fallDistance, @NotNull Object mainHandItem, @NotNull Object feetItem) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(mainHandItem, "mainHandItem");
        Objects.requireNonNull(feetItem, "feetItem");

        Object previousWorld = this.worldReference != null ? this.worldReference.get() : null;
        Object previousObserver = this.observerReference != null ? this.observerReference.get() : null;
        boolean identityChanged = previousWorld != world || previousObserver != observer;
        boolean tickRolledBack = this.prepared && gameTick < this.gameTick;
        boolean tickChanged = this.prepared && gameTick != this.gameTick;
        boolean observerStateChanged = this.observerState == null || !this.observerState.matches(rayX, rayY, rayZ, entityBottomY, descending, fallDistance, mainHandItem, feetItem);
        if (!this.prepared || identityChanged || tickRolledBack) {
            this.entries.clear();
            this.contextGeneration = 0L;
            this.currentContextEntryCount = 0;
        } else if (observerStateChanged) {
            this.advanceContextGeneration();
        } else if (tickChanged) {
            this.currentContextEntryCount = 0;
        }

        if (previousWorld != world) this.worldReference = new WeakReference<>(world);
        if (previousObserver != observer) this.observerReference = new WeakReference<>(observer);
        if (observerStateChanged) this.observerState = new ObserverState(rayX, rayY, rayZ, entityBottomY, descending, fallDistance, mainHandItem, feetItem);
        this.gameTick = gameTick;
        this.prepared = true;
    }

    public @Nullable Boolean getCachedVisibility(@NotNull Object target, @NotNull UUID targetUuid, int targetId, double targetX, double targetY, double targetZ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetUuid, "targetUuid");
        if (!this.prepared) return null;

        Entry cached = this.entries.get(targetUuid);
        return cached != null && cached.matches(this.gameTick, this.contextGeneration, target, targetId, targetX, targetY, targetZ) ? cached.visible : null;
    }

    public void store(@NotNull Object target, @NotNull UUID targetUuid, int targetId, double targetX, double targetY, double targetZ, boolean visible) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetUuid, "targetUuid");
        if (!this.prepared) return;

        Entry cached = this.entries.get(targetUuid);
        if (cached != null) {
            boolean wasCurrent = cached.isCurrent(this.gameTick, this.contextGeneration);
            cached.update(this.gameTick, this.contextGeneration, target, targetId, targetX, targetY, targetZ, visible);
            if (!wasCurrent) this.currentContextEntryCount++;
            return;
        }

        if (this.currentContextEntryCount >= this.maximumEntries) return;
        Entry reusable = null;
        if (this.entries.size() >= this.maximumEntries) reusable = this.removeReusableEntry();
        if (reusable == null && this.entries.size() < this.maximumEntries) reusable = new Entry();
        if (reusable != null) {
            reusable.update(this.gameTick, this.contextGeneration, target, targetId, targetX, targetY, targetZ, visible);
            this.entries.put(targetUuid, reusable);
            this.currentContextEntryCount++;
        }
    }

    public void reset() {
        this.entries.clear();
        this.worldReference = null;
        this.observerReference = null;
        this.observerState = null;
        this.gameTick = 0L;
        this.contextGeneration = 0L;
        this.currentContextEntryCount = 0;
        this.prepared = false;
    }

    int entryCount() {
        return this.entries.size();
    }

    int currentContextEntryCount() {
        return this.currentContextEntryCount;
    }

    private void advanceContextGeneration() {
        if (this.contextGeneration == Long.MAX_VALUE) {
            this.entries.clear();
            this.contextGeneration = 0L;
        } else {
            this.contextGeneration++;
        }
        this.currentContextEntryCount = 0;
    }

    private @Nullable Entry removeReusableEntry() {
        Iterator<Map.Entry<UUID, Entry>> iterator = this.entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            boolean current = entry.isCurrent(this.gameTick, this.contextGeneration);
            if (!current || entry.targetReference == null || entry.targetReference.get() == null) {
                iterator.remove();
                if (current) this.currentContextEntryCount--;
                return entry;
            }
        }
        return null;
    }

    public record ObserverState(double rayX, double rayY, double rayZ, double entityBottomY, boolean descending, double fallDistance, @NotNull Object mainHandItem, @NotNull Object feetItem) {

        public ObserverState {
            Objects.requireNonNull(mainHandItem, "mainHandItem");
            Objects.requireNonNull(feetItem, "feetItem");
        }

        private boolean matches(double rayX, double rayY, double rayZ, double entityBottomY, boolean descending, double fallDistance, @NotNull Object mainHandItem, @NotNull Object feetItem) {
            return Double.compare(this.rayX, rayX) == 0 && Double.compare(this.rayY, rayY) == 0 && Double.compare(this.rayZ, rayZ) == 0 && Double.compare(this.entityBottomY, entityBottomY) == 0 && this.descending == descending && Double.compare(this.fallDistance, fallDistance) == 0 && this.mainHandItem == mainHandItem && this.feetItem == feetItem;
        }

    }

    private static final class Entry {

        @Nullable private WeakReference<Object> targetReference;
        private long computedTick;
        private long contextGeneration;
        private int targetId;
        private double targetX;
        private double targetY;
        private double targetZ;
        private boolean visible;

        private void update(long computedTick, long contextGeneration, @NotNull Object target, int targetId, double targetX, double targetY, double targetZ, boolean visible) {
            if (this.targetReference == null || this.targetReference.get() != target) this.targetReference = new WeakReference<>(target);
            this.computedTick = computedTick;
            this.contextGeneration = contextGeneration;
            this.targetId = targetId;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.visible = visible;
        }

        private boolean matches(long computedTick, long contextGeneration, @NotNull Object target, int targetId, double targetX, double targetY, double targetZ) {
            return this.computedTick == computedTick && this.contextGeneration == contextGeneration && this.targetReference != null && this.targetReference.get() == target && this.targetId == targetId && Double.compare(this.targetX, targetX) == 0 && Double.compare(this.targetY, targetY) == 0 && Double.compare(this.targetZ, targetZ) == 0;
        }

        private boolean isCurrent(long computedTick, long contextGeneration) {
            return this.computedTick == computedTick && this.contextGeneration == contextGeneration;
        }

    }

}
