package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gates expensive model builds by both resource revision and an immutable text snapshot. The snapshot comparison is
 * intentional: {@code IText} implementations may publish new content without replacing the resource instance.
 */
final class ModelBuildAttemptTracker {

    private boolean initialized;
    private long revision;
    @Nullable
    private List<String> lines;
    private boolean attempted;

    @NotNull
    Observation observe(long revision, @Nullable List<String> currentLines) {
        boolean contentChanged = !this.initialized || !Objects.equals(this.lines, currentLines);
        boolean changed = !this.initialized || this.revision != revision || contentChanged;
        if (changed) {
            List<String> snapshot = contentChanged ? (currentLines != null ? Collections.unmodifiableList(new ArrayList<>(currentLines)) : null) : this.lines;
            this.initialized = true;
            this.revision = revision;
            this.lines = snapshot;
            this.attempted = false;
        }
        return new Observation(changed, contentChanged, this.lines != null && !this.lines.isEmpty());
    }

    boolean beginAttempt() {
        if (this.attempted || this.lines == null || this.lines.isEmpty()) return false;
        this.attempted = true;
        return true;
    }

    @NotNull
    String modelJson() {
        if (!this.attempted || this.lines == null || this.lines.isEmpty()) throw new IllegalStateException("No model build attempt is active");
        return String.join("\n", this.lines);
    }

    @Nullable
    List<String> linesSnapshot() {
        return this.lines;
    }

    record Observation(boolean changed, boolean contentChanged, boolean hasContent) {
    }

}
