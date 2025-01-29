package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AnimationControllerEditorElement extends AbstractEditorElement {

    public AnimationControllerEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
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
    }

    @Override 
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("manage_keyframes", Component.translatable("fancymenu.elements.animation_controller.manage_keyframes"),
                        (menu, entry) -> {
                            KeyframeManagerScreen managerScreen = new KeyframeManagerScreen(
                                    this.getElement(),
                                    callback -> {
                                        if (callback != null) {
                                            this.editor.history.saveSnapshot();
                                            this.getElement().keyframes = callback.keyframes();
                                            this.getElement().offsetMode = callback.isOffsetMode();
                                        }
                                        Minecraft.getInstance().setScreen(this.editor);
                                    }
                            );
                            Minecraft.getInstance().setScreen(managerScreen);
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_keyframes.desc")))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AnimationControllerEditorElement.class,
                consumes -> consumes.getElement().loop,
                (element, aBoolean) -> element.getElement().loop = aBoolean,
                "fancymenu.elements.animation_controller.loop");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_size", AnimationControllerEditorElement.class,
                        consumes -> consumes.getElement().ignoreSize,
                        (element, aBoolean) -> element.getElement().ignoreSize = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_size")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_size.desc")))
                .setStackable(false);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "ignore_position", AnimationControllerEditorElement.class,
                        consumes -> consumes.getElement().ignorePosition,
                        (element, aBoolean) -> element.getElement().ignorePosition = aBoolean,
                        "fancymenu.elements.animation_controller.ignore_position")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.ignore_position.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_manage");

        this.rightClickMenu.addClickableEntry("manage_targets", Component.translatable("fancymenu.elements.animation_controller.manage_targets"),
                        (menu, entry) -> {
                            TargetElementManagerScreen managerScreen = new TargetElementManagerScreen(
                                    this,
                                    callback -> {
                                        if (callback != null) {
                                            this.editor.history.saveSnapshot();
                                            this.getElement().targetElements = new ArrayList<>(callback);
                                        }
                                        Minecraft.getInstance().setScreen(this.editor);
                                    }
                            );
                            Minecraft.getInstance().setScreen(managerScreen);
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_targets.desc")))
                .setStackable(false);

    }

    protected AnimationControllerElement getElement() {
        return (AnimationControllerElement) this.element;
    }

}
