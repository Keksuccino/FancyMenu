package de.keksuccino.fancymenu.util.cycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ValueCycle<T> implements IValueCycle<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    protected List<T> values = new ArrayList<>();
    protected int currentIndex = 0;
    protected List<Consumer<T>> cycleListeners = new ArrayList<>();

    /**
     * A value toggle.<br>
     * <b>The value list needs at least one entry!</b>
     */
    public static <T> ValueCycle<T> fromList(@NotNull List<T> values) {
        Objects.requireNonNull(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Failed to create ValueCycle! Value list size too small (empty)!");
        }
        ValueCycle<T> valueCycle = new ValueCycle<>();
        valueCycle.values.addAll(values);
        return valueCycle;
    }

    /**
     * A value toggle.<br>
     * <b>The value array needs at least one entry!</b>
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

    public ValueCycle<T> removeValue(@NotNull T value) {
        if (this.values.size() <= 1) {
            LOGGER.error("Unable to remove value! At least 1 value needed!");
            return this;
        }
        this.values.remove(value);
        this.setCurrentValueByIndex(0, false);
        return this;
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
        this.notifyListeners();
        return this.current();
    }

    public ValueCycle<T> setCurrentValue(T value, boolean notifyListeners) {
        int i = this.values.indexOf(value);
        if (i != -1) {
            this.currentIndex = i;
            if (notifyListeners) this.notifyListeners();
        }
        return this;
    }

    public ValueCycle<T> setCurrentValue(T value) {
        return this.setCurrentValue(value, true);
    }

    public ValueCycle<T> setCurrentValueByIndex(int index, boolean notifyListeners) {
        if ((index >= 0) && (index < this.values.size())) {
            this.currentIndex = index;
            if (notifyListeners) this.notifyListeners();
        }
        return this;
    }

    public ValueCycle<T> setCurrentValueByIndex(int index) {
        return this.setCurrentValueByIndex(index, true);
    }

    public ValueCycle<T> addCycleListener(@NotNull Consumer<T> listener) {
        this.cycleListeners.add(listener);
        return this;
    }

    public ValueCycle<T> clearCycleListeners() {
        this.cycleListeners.clear();
        return this;
    }

    protected void notifyListeners() {
        for (Consumer<T> listener : this.cycleListeners) {
            listener.accept(this.current());
        }
    }

}
