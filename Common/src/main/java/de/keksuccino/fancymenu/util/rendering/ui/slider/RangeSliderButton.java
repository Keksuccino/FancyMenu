package de.keksuccino.fancymenu.util.rendering.ui.slider;

import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class RangeSliderButton extends AdvancedSliderButton {

    private static final Logger LOGGER = LogManager.getLogger();

    public double minValue;
    public double maxValue;

    public RangeSliderButton(int x, int y, int width, int height, boolean handleClick, double minRangeValue, double maxRangeValue, double selectedRangeValue, Consumer<AdvancedSliderButton> applyValueCallback) {
        super(x, y, width, height, handleClick, 0, applyValueCallback);
        this.minValue = minRangeValue;
        this.maxValue = maxRangeValue;
        this.setSelectedRangeValue(selectedRangeValue);
        this.updateMessage();
    }

    @Override
    public String getSliderMessageWithoutPrefixSuffix() {
        return "" + getSelectedRangeValue();
    }

    public int getSelectedRangeValue() {
        return (int) Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), minValue, maxValue);
    }

    public double getSelectedRangeDoubleValue() {
        return Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), minValue, maxValue);
    }

    public void setSelectedRangeValue(double rangeValue) {
        this.setValue(((Mth.clamp(rangeValue, this.minValue, this.maxValue) - this.minValue) / (this.maxValue - this.minValue)));
    }

}
