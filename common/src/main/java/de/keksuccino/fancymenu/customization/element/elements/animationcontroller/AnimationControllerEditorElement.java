package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class AnimationControllerEditorElement extends AbstractEditorElement<AnimationControllerEditorElement, AnimationControllerElement> {

    public AnimationControllerEditorElement(@NotNull AnimationControllerElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setFadeable(false);
        this.settings.setAdvancedSizingSupported(false);
        this.settings.setAdvancedPositioningSupported(false);
        this.settings.setOpacityChangeable(false);
        this.settings.setDelayable(false);
        this.settings.setElementAnchorPointAllowed(false);
        this.settings.setStretchable(false);
        this.settings.setVanillaAnchorPointAllowed(false);
        this.settings.setParallaxAllowed(false);
        this.settings.setAutoSizingAllowed(false);
        this.settings.setStayOnScreenAllowed(false);
        this.settings.setStickyAnchorAllowed(false);
        this.settings.setInEditorColorSupported(true);
    }

    @Override 
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("manage_keyframes", Component.translatable("fancymenu.elements.animation_controller.manage_keyframes"),
                        (menu, entry) -> {
                            KeyframeManagerScreen managerScreen = new KeyframeManagerScreen(
                                    this.element,
                                    callback -> {
                                        if (callback != null) {
                                            this.editor.history.saveSnapshot();
                                            this.element.keyframes = callback.keyframes();
                                            this.element.offsetMode = callback.isOffsetMode();
                                        }
                                        this.openContextMenuScreen(this.editor);
                                    }
                            );
                            this.openContextMenuScreen(managerScreen);
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_keyframes.desc")))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AnimationControllerEditorElement.class,
                consumes -> consumes.element.loop,
                (element, aBoolean) -> element.element.loop = aBoolean,
                "fancymenu.elements.animation_controller.loop");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_size", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.ignoreSize,
                        (element, aBoolean) -> element.element.ignoreSize = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_size")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_size.desc")))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_position", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.ignorePosition,
                        (element, aBoolean) -> element.element.ignorePosition = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_position")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_position.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_before_random_timing_offsets");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "random_timing_offsets", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.randomTimingOffsetMode,
                        (element, aBoolean) -> element.element.randomTimingOffsetMode = aBoolean,
                        "fancymenu.elements.animation_controller.random_timing_offsets")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.random_timing_offsets.desc")))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("random_timing_offsets_range", Component.translatable("fancymenu.elements.animation_controller.random_timing_offsets.range"),
                        (menu, entry) -> {
                            DualTextInputScreen s = DualTextInputScreen.build(
                                    Component.translatable("fancymenu.elements.animation_controller.random_timing_offsets.range"),
                                    Component.translatable("fancymenu.elements.animation_controller.random_timing_offsets.range.min"),
                                    Component.translatable("fancymenu.elements.animation_controller.random_timing_offsets.range.max"),
                                    CharacterFilter.buildIntegerFilter(),
                                    callback -> {
                                        if (callback != null) {
                                            int min = parseOffsetValue(callback.getKey(), this.element.randomTimingOffsetMinMs);
                                            int max = parseOffsetValue(callback.getValue(), this.element.randomTimingOffsetMaxMs);
                                            if (min > max) {
                                                int temp = min;
                                                min = max;
                                                max = temp;
                                            }
                                            this.editor.history.saveSnapshot();
                                            this.element.randomTimingOffsetMinMs = min;
                                            this.element.randomTimingOffsetMaxMs = max;
                                        }
                                        this.openContextMenuScreen(this.editor);
                                    });
                            s.setFirstText("" + this.element.randomTimingOffsetMinMs);
                            s.setSecondText("" + this.element.randomTimingOffsetMaxMs);
                            s.setAllowPlaceholders(false);
                            this.openContextMenuScreen(s);
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.random_timing_offsets.range.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_manage");

        this.rightClickMenu.addClickableEntry("manage_targets", Component.translatable("fancymenu.elements.animation_controller.manage_targets"),
                        (menu, entry) -> {
                            TargetElementManagerScreen managerScreen = new TargetElementManagerScreen(
                                    this,
                                    callback -> {
                                        if (callback != null) {
                                            this.editor.history.saveSnapshot();
                                            this.element.targetElements = new ArrayList<>(callback);
                                        }
                                        this.openContextMenuScreen(this.editor);
                                    }
                            );
                            this.openContextMenuScreen(managerScreen);
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_targets.desc")))
                .setStackable(false);

    }

    protected int parseOffsetValue(@NotNull String value, int fallback) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

}
