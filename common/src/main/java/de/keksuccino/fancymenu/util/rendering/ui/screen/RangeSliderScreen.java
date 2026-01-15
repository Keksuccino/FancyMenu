package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class RangeSliderScreen extends PiPCellScreen {

    public static final int PIP_WINDOW_WIDTH = 420;
    public static final int PIP_WINDOW_HEIGHT = 220;

    @NotNull
    protected final Consumer<Double> onValueUpdate;
    @NotNull
    protected final Consumer<Double> onDone;
    @NotNull
    protected final Consumer<Double> onCancel;
    protected final double minValue;
    protected final double maxValue;
    protected final double presetValue;
    protected double currentValue;
    @NotNull
    protected final ConsumingSupplier<Double, Component> labelSupplier;

    public RangeSliderScreen(@NotNull Component title, double minValue, double maxValue, double valuePreset, @NotNull ConsumingSupplier<Double, Component> labelSupplier, @NotNull Consumer<Double> onValueUpdate, @NotNull Consumer<Double> onDone, @NotNull Consumer<Double> onCancel) {
        super(title);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.presetValue = valuePreset;
        this.currentValue = valuePreset;
        this.labelSupplier = labelSupplier;
        this.onValueUpdate = onValueUpdate;
        this.onDone = onDone;
        this.onCancel = onCancel;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        RangeSlider slider = new RangeSlider(0, 0, 20, 20, Component.empty(), this.minValue, this.maxValue, this.currentValue);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> this.labelSupplier.get(((RangeSlider)consumes).getRangeValue()));
        slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
            this.currentValue = ((RangeSlider)slider1).getRangeValue();
            this.onValueUpdate.accept(this.currentValue);
        });
        this.addWidgetCell(slider, true);

        this.addStartEndSpacerCell();

    }

    @Override
    protected void onCancel() {
        this.onCancel.accept(this.presetValue);
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        this.onDone.accept(this.currentValue);
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.onCancel.accept(this.presetValue);
    }

}
