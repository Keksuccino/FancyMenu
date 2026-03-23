package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.enums.LocalizedEnum;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum AfmaOptimizationPreset implements LocalizedEnum<AfmaOptimizationPreset> {

    BALANCED("balanced", 30, true, true, 512, 5, false, LocalizedEnum.SUCCESS_TEXT_STYLE),
    SMALLEST_FILE("smallest_file", 90, true, true, 2048, 12, false, LocalizedEnum.WARNING_TEXT_STYLE),
    FASTEST_DECODE("fastest_decode", 18, false, true, 96, 2, false, () -> Style.EMPTY);

    private final @NotNull String name;
    private final int keyframeInterval;
    private final boolean rectCopyEnabled;
    private final boolean duplicateFrameElision;
    private final int maxCopySearchDistance;
    private final int maxCandidateAxisOffsets;
    private final boolean thumbnailEnabledByDefault;
    private final @NotNull Supplier<Style> style;

    AfmaOptimizationPreset(@NotNull String name, int keyframeInterval, boolean rectCopyEnabled,
                           boolean duplicateFrameElision, int maxCopySearchDistance, int maxCandidateAxisOffsets,
                           boolean thumbnailEnabledByDefault, @NotNull Supplier<Style> style) {
        this.name = name;
        this.keyframeInterval = keyframeInterval;
        this.rectCopyEnabled = rectCopyEnabled;
        this.duplicateFrameElision = duplicateFrameElision;
        this.maxCopySearchDistance = maxCopySearchDistance;
        this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
        this.thumbnailEnabledByDefault = thumbnailEnabledByDefault;
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

    public int getMaxCopySearchDistance() {
        return this.maxCopySearchDistance;
    }

    public int getMaxCandidateAxisOffsets() {
        return this.maxCandidateAxisOffsets;
    }

    public boolean isThumbnailEnabledByDefault() {
        return this.thumbnailEnabledByDefault;
    }

}
