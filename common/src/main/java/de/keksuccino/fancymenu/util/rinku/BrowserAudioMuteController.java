package de.keksuccino.fancymenu.util.rinku;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

final class BrowserAudioMuteController {

    private final Consumer<Boolean> muteStateApplier;
    private boolean muted;

    BrowserAudioMuteController(@NotNull Consumer<Boolean> muteStateApplier, boolean initiallyMuted) {
        this.muteStateApplier = Objects.requireNonNull(muteStateApplier);
        this.setMuted(initiallyMuted);
    }

    synchronized void setMuted(boolean muted) {
        // Record the state only after CEF accepts it. A failed native call must remain retryable on the next synchronization pass.
        this.muteStateApplier.accept(muted);
        this.muted = muted;
    }

    synchronized void reapply() {
        this.muteStateApplier.accept(this.muted);
    }

    synchronized boolean isMuted() {
        return this.muted;
    }

}
