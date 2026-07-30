package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Gates expensive model builds by resource revision and an immutable text snapshot. */
final class ModelBuildAttemptTracker {

    private boolean initialized;
    private long revision;
    @Nullable private List<String> lines;
    private boolean contentAvailable;
    private boolean attempted;

    @NotNull
    synchronized Observation observe(long revision, @Nullable List<String> currentLines) {
        boolean revisionChanged = !this.initialized || this.revision != revision;
        if (revisionChanged) {
            List<String> snapshot = snapshot(currentLines);
            boolean contentChanged = !this.initialized || !Objects.equals(this.lines, snapshot);
            this.initialized = true;
            this.revision = revision;
            this.lines = snapshot;
            this.attempted = false;
            this.contentAvailable = snapshot != null && !snapshot.isEmpty();
            return new Observation(true, contentChanged, this.contentAvailable);
        }
        // Preserve the attempted marker through temporary asynchronous unavailability, otherwise malformed content is
        // rebuilt every time a resource briefly leaves and re-enters its ready state.
        if (currentLines == null) {
            this.contentAvailable = false;
            return new Observation(false, false, false);
        }
        if (Objects.equals(this.lines, currentLines)) {
            this.contentAvailable = !currentLines.isEmpty();
            return new Observation(false, false, this.contentAvailable);
        }
        List<String> snapshot = snapshot(currentLines);
        boolean contentChanged = !Objects.equals(this.lines, snapshot);
        if (contentChanged) {
            this.lines = snapshot;
            this.attempted = false;
        }
        this.contentAvailable = !snapshot.isEmpty();
        return new Observation(contentChanged, contentChanged, this.contentAvailable);
    }

    @Nullable
    synchronized Attempt beginAttempt() {
        if (this.attempted || !this.contentAvailable || this.lines == null || this.lines.isEmpty()) return null;
        this.attempted = true;
        return new Attempt(this.revision, this.lines);
    }

    @Nullable
    synchronized List<String> linesSnapshot() {
        return this.lines;
    }

    @Nullable
    private static List<String> snapshot(@Nullable List<String> lines) {
        return lines != null ? Collections.unmodifiableList(new ArrayList<>(lines)) : null;
    }

    record Observation(boolean changed, boolean contentChanged, boolean hasContent) {}

    record Attempt(long revision, @NotNull List<String> lines) {
        @NotNull String modelJson() {
            return String.join("\n", this.lines);
        }
    }
}
