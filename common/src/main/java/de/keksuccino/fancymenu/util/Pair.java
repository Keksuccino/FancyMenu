package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

public class Pair<L, R> {

    protected L key;
    protected R value;

    @NotNull
    public static <L, R> Pair<L, R> of(L key, R value) {
        return new Pair<>(key, value);
    }

    protected Pair(L key, R value) {
        this.key = key;
        this.value = value;
    }

    public L getKey() {
        return this.key;
    }

    public void setKey(L key) {
        this.key = key;
    }

    public R getValue() {
        return this.value;
    }

    public void setValue(R value) {
        this.value = value;
    }

}
