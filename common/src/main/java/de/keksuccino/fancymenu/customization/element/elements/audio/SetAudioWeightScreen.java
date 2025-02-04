package de.keksuccino.fancymenu.customization.element.elements.audio;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// In SetAudioWeightScreen.java

public class SetAudioWeightScreen extends CellScreen {
    protected Consumer<Float> callback;
    protected float current;
    protected RangeSlider weightSlider;
    protected LabelCell weightInfo;

    protected SetAudioWeightScreen(float preset, @NotNull Consumer<Float> callback) {
        super(Component.translatable("fancymenu.elements.audio.set_weight"));
        this.current = Math.max(0.0f, preset);
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        // Add info text about weights
        Arrays.asList(LocalizationUtils.splitLocalizedLines("fancymenu.elements.audio.manage_audios.weight_info")).forEach(component -> {
            this.addLabelCell(component.copy().setStyle(Style.EMPTY.withItalic(true)));
        });

        this.addSpacerCell(20);

        this.weightSlider = new RangeSlider(0, 0, 20, 20, Component.empty(), 0.0D, 5.0D, this.current);
        this.weightSlider.setRoundingDecimalPlace(1);
        this.weightSlider.setLabelSupplier(consumes -> Component.translatable("fancymenu.elements.audio.set_weight.weight",
                Component.literal(String.format("%.1f", this.current))));
        this.weightSlider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
            this.current = (float)((RangeSlider)slider1).getRangeValue();
        });
        this.addWidgetCell(this.weightSlider, true);

        this.addSpacerCell(20);

        this.weightInfo = this.addLabelCell(Component.empty());

        this.addStartEndSpacerCell();

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        // Show appropriate info based on current weight value
        Component info;
        if (this.current == 0.0f) {
            info = Component.translatable("fancymenu.elements.audio.manage_audios.weight.zero");
        } else if (this.current < 1.0f) {
            info = Component.translatable("fancymenu.elements.audio.manage_audios.weight.low");
        } else if (this.current == 1.0f) {
            info = Component.translatable("fancymenu.elements.audio.manage_audios.weight.normal");
        } else {
            info = Component.translatable("fancymenu.elements.audio.manage_audios.weight.high");
        }
        info = info.copy().setStyle(Style.EMPTY.withItalic(true));
        this.weightInfo.setText(info);

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
