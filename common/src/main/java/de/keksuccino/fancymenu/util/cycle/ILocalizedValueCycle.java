package de.keksuccino.fancymenu.util.cycle;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface ILocalizedValueCycle<T> extends IValueCycle<T> {

    String getCycleLocalizationKey();

    MutableComponent getCycleComponent();

    MutableComponent getCurrentValueComponent();

    ILocalizedValueCycle<T> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<T, Style> supplier);

}
