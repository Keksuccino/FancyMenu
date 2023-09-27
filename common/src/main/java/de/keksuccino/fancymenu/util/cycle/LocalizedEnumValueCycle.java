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
public class LocalizedEnumValueCycle<E extends LocalizedEnum<?>> extends ValueCycle<E> implements ILocalizedValueCycle<E> {

    protected String cycleLocalizationKey;
    protected ConsumingSupplier<E, Style> cycleStyle = consumes -> Style.EMPTY;

    @SafeVarargs
    public static <E extends LocalizedEnum<?>> LocalizedEnumValueCycle<E> ofArray(@NotNull String cycleLocalizationKey, @NotNull E... values) {
        Objects.requireNonNull(values);
        List<E> valueList = Arrays.asList(values);
        if (valueList.size() < 2) {
            throw new InvalidParameterException("Failed to create LocalizedValueCycle! Value list size too small (<2)!");
        }
        LocalizedEnumValueCycle<E> valueCycle = new LocalizedEnumValueCycle<>(cycleLocalizationKey);
        valueCycle.values.addAll(valueList);
        return valueCycle;
    }

    @SuppressWarnings("all")
    public static <E extends LocalizedEnum<?>> LocalizedEnumValueCycle<E> ofList(@NotNull String cycleLocalizationKey, @NotNull List<E> values) {
        return ofArray(cycleLocalizationKey, (E[])values.toArray(new Object[0]));
    }

    protected LocalizedEnumValueCycle(String cycleLocalizationKey) {
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
        return this.current().getValueComponent();
    }

    public LocalizedEnumValueCycle<E> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<E, Style> supplier) {
        this.cycleStyle = supplier;
        return this;
    }

    @Override
    public LocalizedEnumValueCycle<E> addCycleListener(@NotNull Consumer<E> listener) {
        return (LocalizedEnumValueCycle<E>) super.addCycleListener(listener);
    }

}
