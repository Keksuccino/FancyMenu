package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class SetAudioVolumeScreen extends CellScreen {

    protected Consumer<Float> callback;
    protected float current;

    protected SetAudioVolumeScreen(float preset, @NotNull Consumer<Float> callback) {
        super(Component.translatable("fancymenu.elements.audio.set_volume"));
        if (preset > 1.0F) preset = 1.0F;
        if (preset < 0.0F) preset = 0.0F;
        this.current = preset;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        RangeSlider slider = new RangeSlider(0, 0, 20, 20, Component.empty(), 0.0D, 1.0D, this.current);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> Component.translatable("fancymenu.elements.audio.set_volume.track_volume", Component.literal("" + this.getPercentage())));
        slider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> this.current = (float)value);
        this.addWidgetCell(slider, true);

        this.addStartEndSpacerCell();

    }

    protected int getPercentage() {
        return Math.min(100, Math.max(0, (int)(this.current * 100.0F)));
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.current);
    }

}
