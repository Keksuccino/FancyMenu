package de.keksuccino.fancymenu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class InternetAvailabilityMonitor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Object lifecycleLock = new Object();
    private final InternetAvailabilityProbe probe;
    private final Supplier<? extends FixedDelayScheduler> schedulerFactory;
    private final Duration refreshDelay;
    private final Consumer<Boolean> availabilityPublisher;

    private Lifecycle lifecycle = Lifecycle.NEW;
    private long generation;
    private @Nullable FixedDelayScheduler scheduler;
    private @Nullable FixedDelayScheduler.ScheduledTask scheduledTask;

    InternetAvailabilityMonitor(@NotNull InternetAvailabilityProbe probe, @NotNull Supplier<? extends FixedDelayScheduler> schedulerFactory, @NotNull Duration refreshDelay, @NotNull Consumer<Boolean> availabilityPublisher) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.schedulerFactory = Objects.requireNonNull(schedulerFactory, "schedulerFactory");
        this.refreshDelay = requirePositive(refreshDelay);
        this.availabilityPublisher = Objects.requireNonNull(availabilityPublisher, "availabilityPublisher");
    }

    void init() {
        long scheduledGeneration;
        synchronized (this.lifecycleLock) {
            if (this.lifecycle != Lifecycle.NEW) return;
            this.lifecycle = Lifecycle.RUNNING;
            scheduledGeneration = ++this.generation;
        }

        @Nullable FixedDelayScheduler createdScheduler = null;
        @Nullable FixedDelayScheduler.ScheduledTask createdTask = null;
        @Nullable RuntimeException initializationFailure = null;
        boolean closeProbeAfterFailure = false;
        try {
            createdScheduler = Objects.requireNonNull(this.schedulerFactory.get(), "schedulerFactory result");
            if (this.isActive(scheduledGeneration)) createdTask = Objects.requireNonNull(createdScheduler.scheduleWithFixedDelay(() -> this.refresh(scheduledGeneration), Duration.ZERO, this.refreshDelay), "scheduled task");
        } catch (RuntimeException ex) {
            initializationFailure = ex;
        }

        boolean ownershipPublished = false;
        synchronized (this.lifecycleLock) {
            if (initializationFailure == null && createdTask != null && this.lifecycle == Lifecycle.RUNNING && this.generation == scheduledGeneration) {
                this.scheduler = createdScheduler;
                this.scheduledTask = createdTask;
                ownershipPublished = true;
            } else if (initializationFailure != null && this.lifecycle == Lifecycle.RUNNING && this.generation == scheduledGeneration) {
                // The production scheduler only rejects creation when the managed executor registry is already
                // shutting down. Treat this as terminal so a later init cannot race teardown or reuse the closed probe.
                this.lifecycle = Lifecycle.STOPPED;
                this.generation++;
                closeProbeAfterFailure = true;
            }
        }

        if (ownershipPublished) return;
        if (createdTask != null) cancelTask(createdTask);
        shutdownScheduler(createdScheduler);
        if (closeProbeAfterFailure) closeProbe();
        if (initializationFailure != null) {
            LOGGER.error("[FANCYMENU] Failed to start the internet availability monitor!", initializationFailure);
        }
    }

    void shutdown() {
        @Nullable FixedDelayScheduler schedulerToShutdown;
        @Nullable FixedDelayScheduler.ScheduledTask taskToCancel;
        synchronized (this.lifecycleLock) {
            if (this.lifecycle == Lifecycle.STOPPED) return;
            this.lifecycle = Lifecycle.STOPPED;
            this.generation++;
            schedulerToShutdown = this.scheduler;
            taskToCancel = this.scheduledTask;
            this.scheduler = null;
            this.scheduledTask = null;
        }

        // Cancellation only interrupts the worker; closing the probe also disconnects active network I/O so the
        // executor cannot remain stuck until a socket timeout during process shutdown.
        if (taskToCancel != null) cancelTask(taskToCancel);
        closeProbe();
        shutdownScheduler(schedulerToShutdown);
    }

    private void refresh(long scheduledGeneration) {
        if (!this.isActive(scheduledGeneration)) return;

        boolean available;
        try {
            available = this.probe.isAvailable();
        } catch (Exception ex) {
            available = false;
            // Exceptions must not escape a periodic ScheduledExecutorService task, because one thrown execution
            // permanently suppresses every later refresh.
            LOGGER.error("[FANCYMENU] Failed to update the cached internet availability value!", ex);
        }

        try {
            synchronized (this.lifecycleLock) {
                if (this.lifecycle == Lifecycle.RUNNING && this.generation == scheduledGeneration) this.availabilityPublisher.accept(available);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to publish the cached internet availability value!", ex);
        }
    }

    private boolean isActive(long scheduledGeneration) {
        synchronized (this.lifecycleLock) {
            return this.lifecycle == Lifecycle.RUNNING && this.generation == scheduledGeneration;
        }
    }

    private void closeProbe() {
        try {
            this.probe.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to close the internet availability probe!", ex);
        }
    }

    private static void cancelTask(@NotNull FixedDelayScheduler.ScheduledTask task) {
        try {
            task.cancel(true);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to cancel the internet availability refresh task!", ex);
        }
    }

    private static void shutdownScheduler(@Nullable FixedDelayScheduler scheduler) {
        if (scheduler == null) return;
        try {
            scheduler.shutdownNow();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to shut down the internet availability scheduler!", ex);
        }
    }

    private static @NotNull Duration requirePositive(@NotNull Duration duration) {
        Duration checked = Objects.requireNonNull(duration, "refreshDelay");
        if (checked.isNegative() || checked.isZero()) throw new IllegalArgumentException("refreshDelay must be positive");
        return checked;
    }

    private enum Lifecycle {
        NEW,
        RUNNING,
        STOPPED
    }
}
