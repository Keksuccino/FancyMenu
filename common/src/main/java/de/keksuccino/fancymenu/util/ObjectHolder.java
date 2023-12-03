package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObjectHolder<T> {

    private T object;

    @NotNull
    public static <T> ObjectHolder<T> of(@Nullable T object) {
        return new ObjectHolder<>(object);
    }

    protected ObjectHolder(@Nullable T object) {
        this.object = object;
    }

    public T get() {
        return this.object;
    }

    public void set(@Nullable T object) {
        this.object = object;
    }

}
