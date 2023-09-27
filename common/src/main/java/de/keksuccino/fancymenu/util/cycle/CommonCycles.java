package de.keksuccino.fancymenu.util.cycle;

import de.keksuccino.fancymenu.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class CommonCycles {

    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey) {
        return LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF);
    }

    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey, @NotNull CycleOnOff selectedValue) {
        return (LocalizedEnumValueCycle<CycleOnOff>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF)
                .setCurrentValue(selectedValue);
    }

    @NotNull
    public static LocalizedEnumValueCycle<CycleOnOff> cycleOnOff(@NotNull String cycleLocalizationKey, boolean selectedValue) {
        return (LocalizedEnumValueCycle<CycleOnOff>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleOnOff.ON, CycleOnOff.OFF)
                .setCurrentValue(CycleOnOff.getByBoolean(selectedValue));
    }

    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey) {
        return LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED);
    }

    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey, @NotNull CycleEnabledDisabled selectedValue) {
        return (LocalizedEnumValueCycle<CycleEnabledDisabled>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED)
                .setCurrentValue(selectedValue);
    }

    @NotNull
    public static LocalizedEnumValueCycle<CycleEnabledDisabled> cycleEnabledDisabled(@NotNull String cycleLocalizationKey, boolean selectedValue) {
        return (LocalizedEnumValueCycle<CycleEnabledDisabled>) LocalizedEnumValueCycle.ofArray(cycleLocalizationKey, CycleEnabledDisabled.ENABLED, CycleEnabledDisabled.DISABLED)
                .setCurrentValue(CycleEnabledDisabled.getByBoolean(selectedValue));
    }

    @SuppressWarnings("all")
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycleOrangeValue(@NotNull String cycleLocalizationKey, @NotNull List<T> values) {
        return (LocalizedGenericValueCycle<T>) LocalizedGenericValueCycle.of(cycleLocalizationKey, values.toArray())
                .setValueComponentStyleSupplier(consumes -> LocalizedEnum.WARNING_TEXT_STYLE.get());
    }

    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycleOrangeValue(@NotNull String cycleLocalizationKey, @NotNull List<T> values, @NotNull T selectedValue) {
        return (LocalizedGenericValueCycle<T>) cycleOrangeValue(cycleLocalizationKey, values)
                .setCurrentValue(selectedValue);
    }

    @SuppressWarnings("all")
    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycle(@NotNull String cycleLocalizationKey, @NotNull List<T> values) {
        return (LocalizedGenericValueCycle<T>) LocalizedGenericValueCycle.of(cycleLocalizationKey, values.toArray());
    }

    @NotNull
    public static <T> LocalizedGenericValueCycle<T> cycle(@NotNull String cycleLocalizationKey, @NotNull List<T> values, @NotNull T selectedValue) {
        return (LocalizedGenericValueCycle<T>) cycle(cycleLocalizationKey, values)
                .setCurrentValue(selectedValue);
    }

    public enum CycleOnOff implements LocalizedEnum<CycleOnOff> {

        ON("on", true, LocalizedEnum.SUCCESS_TEXT_STYLE),
        OFF("off", false, LocalizedEnum.ERROR_TEXT_STYLE);

        final String name;
        final Supplier<Style> style;
        final boolean valueBoolean;

        CycleOnOff(String name, boolean valueBoolean, Supplier<Style> style) {
            this.name = name;
            this.style = style;
            this.valueBoolean = valueBoolean;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.general.cycle.on_off";
        }

        public boolean getAsBoolean() {
            return this.valueBoolean;
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull CycleOnOff[] getValues() {
            return CycleOnOff.values();
        }

        @Override
        public @Nullable CycleOnOff getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.style.get();
        }

        public static CycleOnOff getByBoolean(boolean b) {
            if (b) return ON;
            return OFF;
        }

        @Nullable
        public static CycleOnOff getByName(@NotNull String name) {
            for (CycleOnOff e : CycleOnOff.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

    }

    public enum CycleEnabledDisabled implements LocalizedEnum<CycleEnabledDisabled> {

        ENABLED("enabled", true, LocalizedEnum.SUCCESS_TEXT_STYLE),
        DISABLED("disabled", false, LocalizedEnum.ERROR_TEXT_STYLE);

        final String name;
        final Supplier<Style> style;
        final boolean valueBoolean;

        CycleEnabledDisabled(String name, boolean valueBoolean, Supplier<Style> style) {
            this.name = name;
            this.style = style;
            this.valueBoolean = valueBoolean;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.general.cycle.enabled_disabled";
        }

        public boolean getAsBoolean() {
            return this.valueBoolean;
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull CycleEnabledDisabled[] getValues() {
            return CycleEnabledDisabled.values();
        }

        @Override
        public @Nullable CycleEnabledDisabled getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.style.get();
        }

        public static CycleEnabledDisabled getByBoolean(boolean b) {
            if (b) return ENABLED;
            return DISABLED;
        }

        @Nullable
        public static CycleEnabledDisabled getByName(@NotNull String name) {
            for (CycleEnabledDisabled e : CycleEnabledDisabled.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

    }

}
