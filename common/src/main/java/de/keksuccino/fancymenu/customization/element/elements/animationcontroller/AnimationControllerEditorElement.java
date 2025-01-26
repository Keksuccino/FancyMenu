package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "loop", AnimationControllerEditorElement.class,
                consumes -> consumes.getElement().loop,
                (element, aBoolean) -> element.getElement().loop = aBoolean,
                "fancymenu.elements.animation_controller.loop");

        this.rightClickMenu.addClickableEntry("manage_keyframes", Component.translatable("fancymenu.elements.animation_controller.manage_keyframes"),
                        (menu, entry) -> {
                            KeyframeManagerScreen managerScreen = new KeyframeManagerScreen(
                                    this.getElement(),
                                    keyframes -> {
                                        if (keyframes != null) {
                                            this.editor.history.saveSnapshot();
                                            this.getElement().keyframes = keyframes;
                                        }
                                        Minecraft.getInstance().setScreen(this.editor);
                                    }
                            );
                            Minecraft.getInstance().setScreen(managerScreen);
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_keyframes.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("list"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_manage");

        // Add target element selection
        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_target_element", AnimationControllerEditorElement.class,
                        element -> element.getElement().getTargetElementId(),
                        (element, value) -> element.getElement().setTargetElementId(value),
                        null, false, false, Component.translatable("fancymenu.elements.animation_controller.target_element"),
                        true, null, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.target_element.desc")))
                .setStackable(false);

    }

    protected AnimationControllerElement getElement() {
        return (AnimationControllerElement) this.element;
    }

}
