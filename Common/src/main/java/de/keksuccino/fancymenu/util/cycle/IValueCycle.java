package de.keksuccino.fancymenu.util.cycle;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface IValueCycle<T> {

    List<T> getValues();

    /**
     * Returns the current value.
     */
    @NotNull
    T current();

    /**
     * Sets the next value as current value and returns it.
     */
    @NotNull
    T next();

    IValueCycle<T> setCurrentValue(T value);

    IValueCycle<T> setCurrentValueByIndex(int index);

    IValueCycle<T> addCycleListener(@NotNull Consumer<T> listener);

    IValueCycle<T> clearCycleListeners();

}
