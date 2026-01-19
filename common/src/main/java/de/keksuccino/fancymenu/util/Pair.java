package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

public class Pair<F, S> {

    protected F key;
    protected S value;

    @NotNull
    public static <L, R> Pair<L, R> of(L key, R value) {
        return new Pair<>(key, value);
    }

    protected Pair(F key, S value) {
        this.key = key;
        this.value = value;
    }

    public F getFirst() {
        return this.key;
    }

    public void setFirst(F key) {
        this.key = key;
    }

    public S getSecond() {
        return this.value;
    }

    public void setSecond(S value) {
        this.value = value;
    }

}
