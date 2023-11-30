package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class RangeSliderScreen extends CellScreen {

    @NotNull
    protected Consumer<Double> callback;
    protected double minValue;
    protected double maxValue;
    protected double currentValue;
    @NotNull
    protected ConsumingSupplier<Double, Component> labelSupplier;

    public RangeSliderScreen(@NotNull Component title, double minValue, double maxValue, double valuePreset, @NotNull ConsumingSupplier<Double, Component> labelSupplier, @NotNull Consumer<Double> callback) {
        super(title);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = valuePreset;
        this.labelSupplier = labelSupplier;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        RangeSlider slider = new RangeSlider(0, 0, 20, 20, Component.empty(), this.minValue, this.maxValue, this.currentValue);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> this.labelSupplier.get(((RangeSlider)consumes).getRangeValue()));
        slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> this.currentValue = ((RangeSlider)slider1).getRangeValue());
        this.addWidgetCell(slider, true);

        this.addStartEndSpacerCell();

    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.currentValue);
    }

}
