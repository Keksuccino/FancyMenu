package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class RangeSlider extends AbstractExtendedSlider {

    protected double minRangeValue;
    protected double maxRangeValue;
    protected boolean showRounded = true;

    public RangeSlider(int x, int y, int width, int height, Component label, double minRangeValue, double maxRangeValue, double preSelectedRangeValue) {
        super(x, y, width, height, label, 0);
        this.minRangeValue = minRangeValue;
        this.maxRangeValue = maxRangeValue;
        this.setRangeValue(preSelectedRangeValue);
    }

    @Override
    public @NotNull String getValueDisplayText() {
        return "" + (showRounded ? this.getRoundedRangeValue() : this.getRangeValue());
    }

    public int getRoundedRangeValue() {
        return (int) this.getRangeValue();
    }

    public double getRangeValue() {
        return Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), this.minRangeValue, this.maxRangeValue);
    }

    public RangeSlider setRangeValue(double rangeValue) {
        rangeValue = Math.min(this.maxRangeValue, Math.max(this.minRangeValue, rangeValue));
        if (rangeValue == this.maxRangeValue) this.setValue(1.0D);
        if (rangeValue == this.minRangeValue) this.setValue(0.0D);
        this.setValue(((Mth.clamp(rangeValue, this.minRangeValue, this.maxRangeValue) - this.minRangeValue) / (this.maxRangeValue - this.minRangeValue)));
        return this;
    }

    public double getMinRangeValue() {
        return this.minRangeValue;
    }

    public RangeSlider setMinRangeValue(double minRangeValue) {
        this.minRangeValue = minRangeValue;
        return this;
    }

    public double getMaxRangeValue() {
        return this.maxRangeValue;
    }

    public RangeSlider setMaxRangeValue(double maxRangeValue) {
        this.maxRangeValue = maxRangeValue;
        return this;
    }

    public boolean showRounded() {
        return this.showRounded;
    }

    public RangeSlider setShowRounded(boolean showRounded) {
        this.showRounded = showRounded;
        return this;
    }

}
