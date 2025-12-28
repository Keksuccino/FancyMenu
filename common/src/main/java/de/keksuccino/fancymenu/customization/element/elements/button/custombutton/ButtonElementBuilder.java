package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ButtonElementBuilder extends ElementBuilder<ButtonElement, ButtonEditorElement> {

    public ButtonElementBuilder() {
        super("custom_button");
    }

    @Override
    public @NotNull ButtonElement buildDefaultInstance() {
        ButtonElement element = new ButtonElement(this);
        element.shouldBeAffectedByDecorationOverlays.setDefault(true).set(true);
        element.baseWidth = 100;
        element.baseHeight = 20;
        element.label = "New Button";
        element.setWidget(new ExtendedButton(0, 0, 0, 0, Component.empty(), (press) -> {
            if ((CustomizationOverlay.getCurrentMenuBarInstance() == null) || !CustomizationOverlay.getCurrentMenuBarInstance().isUserNavigatingInMenuBar()) {
                boolean isMousePressed = MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown();
                element.getExecutableBlock().execute();
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    if (isMousePressed) press.setFocused(false);
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }));
        return element;
    }

    @Override
    public @NotNull ButtonElement deserializeElement(@NotNull SerializedElement serialized) {

        ButtonElement element = buildDefaultInstance();

        element.label = serialized.getValue("label");

        String buttonExecutableBlockId = serialized.getValue("button_element_executable_block_identifier");
        if (buttonExecutableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, buttonExecutableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.actionExecutor = g;
            }
        } else {
            //Legacy support for old button action format
            GenericExecutableBlock g = new GenericExecutableBlock();
            g.getExecutables().addAll(ActionInstance.deserializeAll(serialized));
            element.actionExecutor = g;
        }

        element.hoverSound = deserializeAudioResourceSupplier(serialized.getValue("hoversound"));

        element.hoverLabel = serialized.getValue("hoverlabel");

        element.tooltip = serialized.getValue("description");

        element.clickSound = deserializeAudioResourceSupplier(serialized.getValue("clicksound"));

        element.backgroundTextureNormal = deserializeImageResourceSupplier(serialized.getValue("backgroundnormal"));

        element.backgroundTextureHover = deserializeImageResourceSupplier(serialized.getValue("backgroundhovered"));

        element.backgroundTextureInactive = deserializeImageResourceSupplier(serialized.getValue("background_texture_inactive"));

        String restartBackAnimationsOnHover = serialized.getValue("restartbackgroundanimations");
        if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
            element.restartBackgroundAnimationsOnHover = false;
        }

        element.nineSliceCustomBackground = deserializeBoolean(element.nineSliceCustomBackground, serialized.getValue("nine_slice_custom_background"));
        element.nineSliceBorderX = deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_border_x"));
        element.nineSliceBorderY = deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_border_y"));

        element.navigatable = deserializeBoolean(element.navigatable, serialized.getValue("navigatable"));

        String activeStateRequirementContainerIdentifier = serialized.getValue("widget_active_state_requirement_container_identifier");
        if (activeStateRequirementContainerIdentifier != null) {
            LoadingRequirementContainer c = LoadingRequirementContainer.deserializeWithIdentifier(activeStateRequirementContainerIdentifier, serialized);
            if (c != null) {
                element.activeStateSupplier = c;
            }
        }

        element.isTemplate = deserializeBoolean(element.isTemplate, serialized.getValue("is_template"));
        element.templateApplyWidth = deserializeBoolean(element.templateApplyWidth, serialized.getValue("template_apply_width"));
        element.templateApplyHeight = deserializeBoolean(element.templateApplyHeight, serialized.getValue("template_apply_height"));
        element.templateApplyPosX = deserializeBoolean(element.templateApplyPosX, serialized.getValue("template_apply_posx"));
        element.templateApplyPosY = deserializeBoolean(element.templateApplyPosY, serialized.getValue("template_apply_posy"));
        element.templateApplyOpacity = deserializeBoolean(element.templateApplyOpacity, serialized.getValue("template_apply_opacity"));
        element.templateApplyVisibility = deserializeBoolean(element.templateApplyVisibility, serialized.getValue("template_apply_visibility"));
        element.templateApplyLabel = deserializeBoolean(element.templateApplyLabel, serialized.getValue("template_apply_label"));
        element.templateShareWith = Objects.requireNonNullElse(ButtonElement.TemplateSharing.getByName(Objects.requireNonNullElse(serialized.getValue("template_share_with"), ButtonElement.TemplateSharing.BUTTONS.getName())), element.templateShareWith);

        element.sliderBackgroundTextureNormal = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_normal"));
        element.sliderBackgroundTextureHighlighted = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_highlighted"));

        element.nineSliceSliderHandle = deserializeBoolean(element.nineSliceSliderHandle, serialized.getValue("nine_slice_slider_handle"));
        element.nineSliceSliderHandleBorderX = deserializeNumber(Integer.class, element.nineSliceSliderHandleBorderX, serialized.getValue("nine_slice_slider_handle_border_x"));
        element.nineSliceSliderHandleBorderY = deserializeNumber(Integer.class, element.nineSliceSliderHandleBorderY, serialized.getValue("nine_slice_slider_handle_border_y"));

        return element;

    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull ButtonElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("button_element_executable_block_identifier", element.actionExecutor.identifier);
        element.actionExecutor.serializeToExistingPropertyContainer(serializeTo);

        if (element.backgroundTextureNormal != null) {
            serializeTo.putProperty("backgroundnormal", element.backgroundTextureNormal.getSourceWithPrefix());
        }
        if (element.backgroundTextureHover != null) {
            serializeTo.putProperty("backgroundhovered", element.backgroundTextureHover.getSourceWithPrefix());
        }
        if (element.backgroundTextureInactive != null) {
            serializeTo.putProperty("background_texture_inactive", element.backgroundTextureInactive.getSourceWithPrefix());
        }
        serializeTo.putProperty("restartbackgroundanimations", "" + element.restartBackgroundAnimationsOnHover);
        serializeTo.putProperty("nine_slice_custom_background", "" + element.nineSliceCustomBackground);
        serializeTo.putProperty("nine_slice_border_x", "" + element.nineSliceBorderX);
        serializeTo.putProperty("nine_slice_border_y", "" + element.nineSliceBorderY);
        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound.getSourceWithPrefix());
        }
        if (element.hoverLabel != null) {
            serializeTo.putProperty("hoverlabel", element.hoverLabel);
        }
        if (element.clickSound != null) {
            serializeTo.putProperty("clicksound", element.clickSound.getSourceWithPrefix());
        }
        if (element.tooltip != null) {
            serializeTo.putProperty("description", element.tooltip);
        }
        if (element.label != null) {
            serializeTo.putProperty("label", element.label);
        }
        serializeTo.putProperty("navigatable", "" + element.navigatable);

        serializeTo.putProperty("widget_active_state_requirement_container_identifier", element.activeStateSupplier.identifier);
        element.activeStateSupplier.serializeToExistingPropertyContainer(serializeTo);

        serializeTo.putProperty("is_template", "" + element.isTemplate);
        serializeTo.putProperty("template_apply_width", "" + element.templateApplyWidth);
        serializeTo.putProperty("template_apply_height", "" + element.templateApplyHeight);
        serializeTo.putProperty("template_apply_posx", "" + element.templateApplyPosX);
        serializeTo.putProperty("template_apply_posy", "" + element.templateApplyPosY);
        serializeTo.putProperty("template_apply_opacity", "" + element.templateApplyOpacity);
        serializeTo.putProperty("template_apply_visibility", "" + element.templateApplyVisibility);
        serializeTo.putProperty("template_apply_label", "" + element.templateApplyLabel);
        serializeTo.putProperty("template_share_with", element.templateShareWith.getName());

        if (element.sliderBackgroundTextureNormal != null) {
            serializeTo.putProperty("slider_background_texture_normal", element.sliderBackgroundTextureNormal.getSourceWithPrefix());
        }
        if (element.sliderBackgroundTextureHighlighted != null) {
            serializeTo.putProperty("slider_background_texture_highlighted", element.sliderBackgroundTextureHighlighted.getSourceWithPrefix());
        }

        serializeTo.putProperty("nine_slice_slider_handle", "" + element.nineSliceSliderHandle);
        serializeTo.putProperty("nine_slice_slider_handle_border_x", "" + element.nineSliceSliderHandleBorderX);
        serializeTo.putProperty("nine_slice_slider_handle_border_y", "" + element.nineSliceSliderHandleBorderY);

        return serializeTo;

    }

    @Override
    public @NotNull ButtonEditorElement wrapIntoEditorElement(@NotNull ButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new ButtonEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        if ((element instanceof ButtonElement b) && (b.getWidget() != null) && !b.getWidget().getMessage().getString().replace(" ", "").isEmpty()) {
            return b.getWidget().getMessage();
        }
        if (element instanceof VanillaWidgetElement b) {
            if (b.getWidget() instanceof AbstractButton) return Component.translatable("fancymenu.elements.vanilla_widget.button");
            if (b.getWidget() instanceof CustomizableSlider) return Component.translatable("fancymenu.elements.vanilla_widget.slider");
            return Component.translatable("fancymenu.elements.vanilla_widget.generic");
        }
        return Component.translatable("fancymenu.elements.button");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
