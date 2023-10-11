package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2;

import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class RangeSlider extends AbstractExtendedSlider {

    protected double minRangeValue;
    protected double maxRangeValue;
    protected boolean showAsInteger = false;
    protected int roundingDecimalPlace = 2;

    public RangeSlider(int x, int y, int width, int height, Component label, double minRangeValue, double maxRangeValue, double preSelectedRangeValue) {
        super(x, y, width, height, label, 0);
        this.minRangeValue = minRangeValue;
        this.maxRangeValue = maxRangeValue;
        this.setRangeValue(preSelectedRangeValue);
    }

    @Override
    public @NotNull String getValueDisplayText() {
        if (this.showAsInteger()) return "" + this.getIntegerRangeValue();
        return "" + this.getRangeValue();
    }

    public int getIntegerRangeValue() {
        return (int) this.getRangeValue();
    }

    public double getRangeValue() {
        double d = Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), this.minRangeValue, this.maxRangeValue);
        if (this.roundingDecimalPlace < 0) return d;
        return MathUtils.round(d, this.roundingDecimalPlace);
    }

    public RangeSlider setRangeValue(double rangeValue) {
        rangeValue = Math.min(this.maxRangeValue, Math.max(this.minRangeValue, rangeValue));
        if (rangeValue == this.maxRangeValue) {
            this.setValue(1.0D);
        } else if (rangeValue == this.minRangeValue) {
            this.setValue(0.0D);
        } else {
            this.setValue(((Mth.clamp(rangeValue, this.minRangeValue, this.maxRangeValue) - this.minRangeValue) / (this.maxRangeValue - this.minRangeValue)));
        }
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

    public boolean showAsInteger() {
        return this.showAsInteger;
    }

    public RangeSlider setShowAsInteger(boolean showAsInteger) {
        this.showAsInteger = showAsInteger;
        return this;
    }

    public int getRoundingDecimalPlace() {
        return this.roundingDecimalPlace;
    }

    public RangeSlider setRoundingDecimalPlace(int decimalPlace) {
        this.roundingDecimalPlace = decimalPlace;
        return this;
    }

}
