package de.keksuccino.fancymenu.menu.fancy.helper.ui.slider;

import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public abstract class AdvancedSliderButton extends AbstractHandledSlider {

    protected String messagePrefix = null;
    protected String messageSuffix = null;
    protected Consumer<AdvancedSliderButton> applyValueCallback;

    /**
     * @param handleClick UNUSED IN 1.12, BUT WILL KEEP PARAM FOR EASIER PORTING
     **/
    public AdvancedSliderButton(int x, int y, int width, int height, boolean handleClick, double value, Consumer<AdvancedSliderButton> applyValueCallback) {
        super(x, y, width, height, "", value);
        this.applyValueCallback = applyValueCallback;
    }

    @Override
    protected void applyValue() {
        if (this.applyValueCallback != null) {
            this.applyValueCallback.accept(this);
        }
    }

    @Override
    public void updateMessage() {
        String s = "";
        if (this.messagePrefix != null) {
            s += this.messagePrefix;
        }
        s += this.getSliderMessageWithoutPrefixSuffix();
        if (this.messageSuffix != null) {
            s += this.messageSuffix;
        }
        this.displayString = s;
    }

    public abstract String getSliderMessageWithoutPrefixSuffix();

    public void setLabelPrefix(String prefix) {
        this.messagePrefix = prefix;
        this.updateMessage();
    }

    public void setLabelSuffix(String suffix) {
        this.messageSuffix = suffix;
        this.updateMessage();
    }

    public void setValue(double value) {
        double d0 = this.value;
        this.value = MathHelper.clamp(value, 0.0D, 1.0D);
        if (d0 != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    public double getValue() {
        return this.value;
    }

}
