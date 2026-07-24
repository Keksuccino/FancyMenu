package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Immutable, same-generation view of one user variable. */
public record UserVariableSnapshot(@NotNull String name, @NotNull String value, boolean resetOnLaunch) {

    public UserVariableSnapshot {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
    }

}
