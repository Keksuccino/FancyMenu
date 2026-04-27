package de.keksuccino.fancymenu.util;

import java.util.Objects;

/**
 * A consumer that accepts three input arguments.
 */
@FunctionalInterface
public interface TripleConsumer<F, S, T> {

    void accept(F first, S second, T third);

    default TripleConsumer<F, S, T> andThen(TripleConsumer<? super F, ? super S, ? super T> after) {
        Objects.requireNonNull(after, "after");
        return (first, second, third) -> {
            accept(first, second, third);
            after.accept(first, second, third);
        };
    }

}
