package de.keksuccino.fancymenu.util.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <E> The enum type.
 */
public interface NamedEnum<E> {

    @NotNull
    String getName();

    @NotNull
    E[] getValues();

    @Nullable
    E getByNameInternal(@NotNull String name);

}
