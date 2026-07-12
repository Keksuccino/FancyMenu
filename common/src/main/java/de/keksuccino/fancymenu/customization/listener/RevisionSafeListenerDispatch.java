package de.keksuccino.fancymenu.customization.listener;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Captures a listener provider's active revision when an event is produced and suppresses delivery if its instance set changes before dispatch.
 */
public final class RevisionSafeListenerDispatch {

    private RevisionSafeListenerDispatch() {
    }

    public static boolean scheduleIfActive(@NotNull AbstractListener listener, @NotNull Consumer<Runnable> scheduler, @NotNull Runnable dispatch) {
        long listenerRevision = listener.getActiveInstanceRevision();
        if (listenerRevision < 0L) {
            return false;
        }
        scheduler.accept(() -> { if (listener.isActiveAtRevision(listenerRevision)) dispatch.run(); });
        return true;
    }
}
