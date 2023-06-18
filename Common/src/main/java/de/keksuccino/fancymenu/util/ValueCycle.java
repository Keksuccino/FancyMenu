package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ValueCycle<T> {

    protected List<T> values = new ArrayList<>();
    protected int currentIndex = 0;

    /**
     * A value toggle.<br>
     * <b>The value list needs at least two entries!</b>
     */
    public static <T> ValueCycle<T> fromList(@NotNull List<T> values) {
        Objects.requireNonNull(values);
        if (values.size() < 2) {
            throw new InvalidParameterException("Failed to create ValueSwitcher! Value list size too small (<2)!");
        }
        ValueCycle<T> valueCycle = new ValueCycle<>();
        valueCycle.values.addAll(values);
        return valueCycle;
    }

    /**
     * A value toggle.<br>
     * <b>The value array needs at least two entries!</b>
     */
    @SafeVarargs
    public static <T> ValueCycle<T> fromArray(@NotNull T... values) {
        Objects.requireNonNull(values);
        return fromList(Arrays.asList(values));
    }

    protected ValueCycle() {
    }

    public List<T> getValues() {
        return new ArrayList<>(this.values);
    }

    /**
     * Returns the current value.
     */
    @NotNull
    public T current() {
        return this.values.get(this.currentIndex);
    }

    /**
     * Sets the next value as current value and returns it.
     */
    @NotNull
    public T next() {
        if (this.currentIndex >= this.values.size()-1) {
            this.currentIndex = 0;
        } else {
            this.currentIndex++;
        }
        return this.current();
    }

    public void setCurrentValue(T value) {
        int i = this.values.indexOf(value);
        if (i != -1) {
            this.currentIndex = i;
        }
    }

    public void setCurrentValueByIndex(int index) {
        if ((index > 0) && (index < this.values.size())) {
            this.currentIndex = index;
        }
    }

}
