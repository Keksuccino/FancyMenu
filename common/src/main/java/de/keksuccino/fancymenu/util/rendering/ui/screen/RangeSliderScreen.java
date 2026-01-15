package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class RangeSliderScreen extends PiPScreen {

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
    protected RangeSlider slider;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public RangeSliderScreen(double minValue, double maxValue, double valuePreset, @NotNull ConsumingSupplier<Double, Component> labelSupplier, @NotNull Consumer<Double> onValueUpdate, @NotNull Consumer<Double> onDone, @NotNull Consumer<Double> onCancel) {
        super(Component.empty());
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
    protected void init() {

        int sliderWidth = Math.max(160, this.width - 80);
        int sliderX = (this.width - sliderWidth) / 2;
        int sliderY = (this.height / 2) - 20;

        this.slider = new RangeSlider(sliderX, sliderY, sliderWidth, 20, Component.empty(), this.minValue, this.maxValue, this.currentValue);
        this.slider.setRoundingDecimalPlace(2);
        this.slider.setLabelSupplier(consumes -> this.labelSupplier.get(((RangeSlider)consumes).getRangeValue()));
        this.slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
            this.currentValue = ((RangeSlider)slider1).getRangeValue();
            this.onValueUpdate.accept(this.currentValue);
        });
        UIBase.applyDefaultWidgetSkinTo(this.slider, UIBase.shouldBlur());
        this.addRenderableWidget(this.slider);

        this.cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, this.height - 40, 100, 20, Component.translatable("fancymenu.common_components.cancel"), button -> {
            this.onCancel.accept(this.presetValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, this.height - 40, 100, 20, Component.translatable("fancymenu.common_components.done"), button -> {
            this.onDone.accept(this.currentValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());

    }

    @Override
    public void onWindowClosedExternally() {
        this.onCancel.accept(this.presetValue);
    }

}
