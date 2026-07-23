package de.keksuccino.fancymenu.networking;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Sends an optional payload only when the exact endpoint reports negotiated support for it.
 * Support and send failures deliberately propagate: only a normal unsupported result is an expected no-op.
 */
final class OptionalPayloadSender {

    private OptionalPayloadSender() {
    }

    static <E, P> boolean sendIfSupported(@NotNull E endpoint, @NotNull P payload, @NotNull BiPredicate<? super E, ? super P> supportPredicate, @NotNull BiConsumer<? super E, ? super P> sender) {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(supportPredicate);
        Objects.requireNonNull(sender);
        if (!supportPredicate.test(endpoint, payload)) return false;
        sender.accept(endpoint, payload);
        return true;
    }
}
