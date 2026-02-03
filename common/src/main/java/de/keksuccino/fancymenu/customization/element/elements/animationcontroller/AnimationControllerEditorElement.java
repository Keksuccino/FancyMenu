package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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
                .setStackable(false)
                .setIcon(MaterialIcons.ANIMATION);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AnimationControllerEditorElement.class,
                consumes -> consumes.element.loop,
                (element, aBoolean) -> element.element.loop = aBoolean,
                "fancymenu.elements.animation_controller.loop")
                .setIcon(MaterialIcons.REPEAT);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_size", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.ignoreSize,
                        (element, aBoolean) -> element.element.ignoreSize = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_size")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_size.desc")))
                .setStackable(false)
                .setIcon(MaterialIcons.STRAIGHTEN);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_position", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.ignorePosition,
                        (element, aBoolean) -> element.element.ignorePosition = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_position")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_position.desc")))
                .setStackable(false)
                .setIcon(MaterialIcons.MOVE);

        this.rightClickMenu.addSeparatorEntry("separator_before_random_timing_offsets");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "random_timing_offsets", AnimationControllerEditorElement.class,
                        consumes -> consumes.element.randomTimingOffsetMode,
                        (element, aBoolean) -> element.element.randomTimingOffsetMode = aBoolean,
                        "fancymenu.elements.animation_controller.random_timing_offsets")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.random_timing_offsets.desc")))
                .setStackable(false)
                .setIcon(MaterialIcons.SHUFFLE);

        this.element.randomTimingOffsetMinMs.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.random_timing_offsets.range.desc")))
                .setStackable(false)
                .setIcon(MaterialIcons.TIMER);

        this.element.randomTimingOffsetMaxMs.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.random_timing_offsets.range.desc")))
                .setStackable(false)
                .setIcon(MaterialIcons.TIMER);

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
                .setStackable(false)
                .setIcon(MaterialIcons.LINK);

    }

}
