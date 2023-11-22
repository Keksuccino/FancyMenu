package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

public class Trio<F, S, T> {

    protected F first;
    protected S second;
    protected T third;

    @NotNull
    public static <F, S, T> Trio<F, S, T> of(F first, S second, T third) {
        return new Trio<>(first, second, third);
    }

    protected Trio(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F getFirst() {
        return this.first;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return this.second;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public T getThird() {
        return this.third;
    }

    public void setThird(T third) {
        this.third = third;
    }

}
