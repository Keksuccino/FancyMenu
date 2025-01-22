package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class AnimationControllerEditorElement extends AbstractEditorElement {

    public static final int KEY_ADD_KEYFRAME = InputConstants.KEY_K;
    public static final int KEY_START_STOP_RECORDING = InputConstants.KEY_R;
    public static final int KEY_PAUSE_RESUME_RECORDING = InputConstants.KEY_P;

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
    }

    @Override 
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("start_recording",
            Component.translatable("fancymenu.elements.animation_controller.start_recording"), 
            (menu, entry) -> {
                if (!this.getElement().isRecording()) {
                    this.getElement().startRecording();
                }
            })
            .setTooltipSupplier((menu, entry) -> Tooltip.of(
                Component.translatable("fancymenu.elements.animation_controller.start_recording.desc")))
            .setIcon(ContextMenu.IconFactory.getIcon("record"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("stop_recording",
            Component.translatable("fancymenu.elements.animation_controller.stop_recording"),
            (menu, entry) -> {
                if (this.getElement().isRecording()) {
                    this.getElement().stopRecording(); 
                }
            })
            .setTooltipSupplier((menu, entry) -> Tooltip.of(
                Component.translatable("fancymenu.elements.animation_controller.stop_recording.desc")))
            .setIcon(ContextMenu.IconFactory.getIcon("stop"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("pause_recording",
                        Component.translatable("fancymenu.elements.animation_controller.pause_recording"),
                        (menu, entry) -> {
                            if (this.getElement().isRecording() && !this.getElement().isPaused()) {
                                this.getElement().pauseRecording();
                            }
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.elements.animation_controller.pause_recording.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("pause"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("resume_recording",
                        Component.translatable("fancymenu.elements.animation_controller.resume_recording"),
                        (menu, entry) -> {
                            if (this.getElement().isRecording() && this.getElement().isPaused()) {
                                this.getElement().resumeRecording();
                            }
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.elements.animation_controller.resume_recording.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("play"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_stop_recording");

        this.rightClickMenu.addClickableEntry("manage_keyframes",
                        Component.translatable("fancymenu.elements.animation_controller.manage_keyframes"),
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
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.elements.animation_controller.manage_keyframes.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("list"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_manage");

        // Add target element selection
        this.addStringInputContextMenuEntryTo(
            this.rightClickMenu,
            "set_target_element",
            AnimationControllerEditorElement.class,
            element -> element.getElement().getTargetElementId(),
            (element, value) -> element.getElement().setTargetElementId(value),
            null,
            false,
            false,
            Component.translatable("fancymenu.elements.animation_controller.target_element"),
            true,
            null,
            null,
            null
        ).setTooltipSupplier((menu, entry) -> Tooltip.of(
            Component.translatable("fancymenu.elements.animation_controller.target_element.desc")))
                .setStackable(false);

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        AnimationControllerElement element = this.getElement();
        if (element.isRecording()) {
            if (keyCode == KEY_ADD_KEYFRAME) {
                if (!element.isPaused()) {
                    element.addCurrentKeyframe();
                }
                return true;
            } else if (keyCode == KEY_START_STOP_RECORDING) {
                if (element.isPaused()) {
                    element.resumeRecording();
                } else {
                    element.stopRecording();
                }
                return true;
            } else if (keyCode == KEY_PAUSE_RESUME_RECORDING) {
                if (element.isPaused()) {
                    element.resumeRecording();
                } else {
                    element.pauseRecording();
                }
                return true;
            }
        } else if (this.isSelected() && !this.isMultiSelected()) {
            if (keyCode == KEY_START_STOP_RECORDING) {
                element.startRecording();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected AnimationControllerElement getElement() {
        return (AnimationControllerElement) this.element;
    }

}
