package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum AfmaOptimizationPreset implements LocalizedEnum<AfmaOptimizationPreset> {

    BALANCED("balanced", 30, true, true, LocalizedEnum.SUCCESS_TEXT_STYLE),
    SMALLEST_FILE("smallest_file", 60, true, true, LocalizedEnum.WARNING_TEXT_STYLE),
    FASTEST_DECODE("fastest_decode", 18, false, true, () -> Style.EMPTY);

    private final @NotNull String name;
    private final int keyframeInterval;
    private final boolean rectCopyEnabled;
    private final boolean duplicateFrameElision;
    private final @NotNull Supplier<Style> style;

    AfmaOptimizationPreset(@NotNull String name, int keyframeInterval, boolean rectCopyEnabled,
                           boolean duplicateFrameElision, @NotNull Supplier<Style> style) {
        this.name = name;
        this.keyframeInterval = keyframeInterval;
        this.rectCopyEnabled = rectCopyEnabled;
        this.duplicateFrameElision = duplicateFrameElision;
        this.style = style;
    }

    @Override
    public @NotNull String getLocalizationKeyBase() {
        return "fancymenu.afma.creator.optimization_preset";
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public @NotNull AfmaOptimizationPreset[] getValues() {
        return values();
    }

    @Override
    public @Nullable AfmaOptimizationPreset getByNameInternal(@NotNull String name) {
        for (AfmaOptimizationPreset preset : values()) {
            if (preset.getName().equals(name)) return preset;
        }
        return null;
    }

    @Override
    public @NotNull Style getValueComponentStyle() {
        return this.style.get();
    }

    public int getKeyframeInterval() {
        return this.keyframeInterval;
    }

    public boolean isRectCopyEnabled() {
        return this.rectCopyEnabled;
    }

    public boolean isDuplicateFrameElision() {
        return this.duplicateFrameElision;
    }

}
