package de.keksuccino.fancymenu.misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValueToggle<T> {

    protected List<T> values = new ArrayList<>();
    protected int currentIndex;

    /**
     * A value toggle.<br>
     * <b>The value array needs at least two entries!</b>
     */
    @SafeVarargs
    public ValueToggle(int currentIndex, @NotNull T... values) {
        this.values.addAll(Arrays.asList(values));
        this.currentIndex = currentIndex;
        if (this.currentIndex > this.values.size()-1) {
            this.currentIndex = Math.max(0, this.values.size()-1);
        } else if (this.currentIndex < 0) {
            this.currentIndex = 0;
        }
    }

    /**
     * A value toggle.<br>
     * <b>The value array needs at least two entries!</b>
     */
    @SafeVarargs
    public ValueToggle(@NotNull T currentValue, @NotNull T...value) {
        this(0, value);
        this.currentIndex = Math.max(0, this.values.indexOf(currentValue));
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

}
