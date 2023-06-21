package de.keksuccino.fancymenu.util.cycle;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class LocalizedValueCycle<T extends LocalizedEnum> extends ValueCycle<T> implements ILocalizedValueCycle<T> {

    protected String cycleLocalizationKey;
    protected ConsumingSupplier<T, Style> cycleStyle = consumes -> Style.EMPTY;

    @SafeVarargs
    public static <T extends LocalizedEnum> LocalizedValueCycle<T> of(@NotNull String cycleLocalizationKey, @NotNull T... values) {
        Objects.requireNonNull(values);
        List<T> valueList = Arrays.asList(values);
        if (valueList.size() < 2) {
            throw new InvalidParameterException("Failed to create LocalizedValueCycle! Value list size too small (<2)!");
        }
        LocalizedValueCycle<T> valueCycle = new LocalizedValueCycle<>(cycleLocalizationKey);
        valueCycle.values.addAll(valueList);
        return valueCycle;
    }

    protected LocalizedValueCycle(String cycleLocalizationKey) {
        this.cycleLocalizationKey = cycleLocalizationKey;
    }

    @NotNull
    public String getCycleLocalizationKey() {
        return this.cycleLocalizationKey;
    }

    public MutableComponent getCycleComponent() {
        return Component.translatable(this.getCycleLocalizationKey(), this.getCurrentValueComponent()).withStyle(this.cycleStyle.get(this.current()));
    }

    public MutableComponent getCurrentValueComponent() {
        return this.current().getEntryComponent();
    }

    public LocalizedValueCycle<T> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<T, Style> supplier) {
        this.cycleStyle = supplier;
        return this;
    }

    @Override
    public LocalizedValueCycle<T> addCycleListener(@NotNull Consumer<T> listener) {
        return (LocalizedValueCycle<T>) super.addCycleListener(listener);
    }

}
