package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorScreen;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class ButtonEditorElement<E extends ButtonEditorElement<?, ?>, N extends ButtonElement> extends AbstractEditorElement<E, N> {

    public boolean showTemplateOptions = true;

    public ButtonEditorElement(@NotNull N element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        boolean isButton = (this.element.getWidget() instanceof AbstractButton);
        boolean isSlider = (this.element.getWidget() instanceof CustomizableSlider);

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.actions.screens.manage_screen.manage"), (menu, entry) -> {
                    ActionScriptEditorScreen s = new ActionScriptEditorScreen(this.element.getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.element.actionExecutor = call;
                        }
                        this.openContextMenuScreen(this.editor);
                    });
                    this.openContextMenuScreen(s);
                }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Component.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(this.element.activeStateSupplier.copy(false), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.element.activeStateSupplier = call;
                        }
                        this.openContextMenuScreen(this.editor);
                    });
                    this.openContextMenuScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("button_separator_1");

        this.addTextureOptions(isButton, isSlider);

        this.addSliderTextureOptionsForTemplateMode();

        this.rightClickMenu.addSeparatorEntry("button_separator_2").setStackable(true);

        if (!isSlider) {

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                            this.selfClass(),
                            consumes -> consumes.element.label,
                            (element1, s) -> element1.element.label = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.elements.button.editlabel" : "fancymenu.elements.button.label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                            this.selfClass(),
                            consumes -> consumes.element.hoverLabel,
                            (element1, s) -> element1.element.hoverLabel = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.elements.button.hoverlabel" : "fancymenu.elements.button.hover_label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.rightClickMenu.addSeparatorEntry("button_separator_3").setStackable(true);

        }

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        this.selfClass(),
                        null,
                        consumes -> consumes.element.hoverSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.element.hoverSound = supplier,
                        Component.translatable("fancymenu.elements.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.element.unhoverAudio.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        this.selfClass(),
                        null,
                        consumes -> consumes.element.clickSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.element.clickSound = supplier,
                        Component.translatable("fancymenu.elements.button.clicksound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("button_separator_4").setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> {
                            String t = consumes.element.tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            element1.element.tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.elements.button.tooltip"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.tooltip.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        if (!(this instanceof VanillaWidgetEditorElement)) {

            this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", this.selfClass(),
                            consumes -> consumes.element.navigatable,
                            (buttonEditorElement, aBoolean) -> buttonEditorElement.element.navigatable = aBoolean,
                            "fancymenu.elements.widgets.generic.navigatable")
                    .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));

        }

        this.addTemplateOptions();

    }

    protected void addTemplateOptions() {

        ContextMenu templateSettingsMenu = new ContextMenu();

        this.rightClickMenu.addSeparatorEntry("separator_before_template_settings")
                .addIsVisibleSupplier((menu, entry) -> this.showTemplateOptions);

        this.rightClickMenu.addSubMenuEntry("template_settings", Component.translatable("fancymenu.elements.button.template_settings"), templateSettingsMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.template_settings.desc")))
                .addIsVisibleSupplier((menu, entry) -> this.showTemplateOptions);

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "is_template", this.selfClass(),
                        consumes -> consumes.element.isTemplate,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.isTemplate = aBoolean,
                        "fancymenu.elements.button.is_template")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.template_settings.desc")));

        this.addCycleContextMenuEntryTo(templateSettingsMenu, "share_with", Arrays.asList(ButtonElement.TemplateSharing.values()), this.selfClass(),
                consumes -> consumes.element.templateShareWith,
                (buttonEditorElement, templateSharing) -> buttonEditorElement.element.templateShareWith = templateSharing,
                (menu, entry, switcherValue) -> switcherValue.getCycleComponent());

        templateSettingsMenu.addSeparatorEntry("separator_after_is_template");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_width", this.selfClass(),
                consumes -> consumes.element.templateApplyWidth,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyWidth = aBoolean,
                "fancymenu.elements.button.template_apply_width");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_height", this.selfClass(),
                consumes -> consumes.element.templateApplyHeight,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyHeight = aBoolean,
                "fancymenu.elements.button.template_apply_height");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_posx", this.selfClass(),
                consumes -> consumes.element.templateApplyPosX,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyPosX = aBoolean,
                "fancymenu.elements.button.template_apply_posx");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_posy", this.selfClass(),
                consumes -> consumes.element.templateApplyPosY,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyPosY = aBoolean,
                "fancymenu.elements.button.template_apply_posy");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_opacity", this.selfClass(),
                consumes -> consumes.element.templateApplyOpacity,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyOpacity = aBoolean,
                "fancymenu.elements.button.template_apply_opacity");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_visibility", this.selfClass(),
                consumes -> consumes.element.templateApplyVisibility,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyVisibility = aBoolean,
                "fancymenu.elements.button.template_apply_visibility");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_label", this.selfClass(),
                consumes -> consumes.element.templateApplyLabel,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.templateApplyLabel = aBoolean,
                "fancymenu.elements.button.template_apply_label");

    }

    protected void addTextureOptions(boolean isButton, boolean isSlider) {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        if (isSlider || isButton) {
            this.rightClickMenu.addSubMenuEntry("button_background", isButton ? Component.translatable("fancymenu.elements.buttons.buttonbackground") : Component.translatable("fancymenu.elements.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                    .setIcon(ContextMenu.IconFactory.getIcon("image"))
                    .setStackable(true)
                    .addIsVisibleSupplier((menu, entry) -> !this.element.isTemplate || (this.element.templateShareWith == ButtonElement.TemplateSharing.BUTTONS));
        }

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.elements.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureNormal = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.elements.buttons.buttonbackground.normal") : Component.translatable("fancymenu.elements.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureHover = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.elements.buttons.buttonbackground.hover") : Component.translatable("fancymenu.elements.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureInactive = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.elements.buttons.buttonbackground.inactive") : Component.translatable("fancymenu.elements.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_background").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                        this.selfClass(),
                        consumes -> consumes.element.restartBackgroundAnimationsOnHover,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.restartBackgroundAnimationsOnHover = aBoolean,
                        "fancymenu.elements.buttons.textures.restart_animated_on_hover")
                .setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", this.selfClass(),
                consumes -> consumes.element.nineSliceCustomBackground,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.nineSliceCustomBackground = aBoolean,
                "fancymenu.elements.buttons.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", this.selfClass(),
                consumes -> consumes.element.nineSliceBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderX = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", this.selfClass(),
                consumes -> consumes.element.nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderY = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_y"), true, 5, null, null);

    }

    protected void addSliderTextureOptionsForTemplateMode() {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("slider_background", Component.translatable("fancymenu.elements.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(false)
                .addIsVisibleSupplier((menu, entry) -> this.element.isTemplate && (this.element.templateShareWith == ButtonElement.TemplateSharing.SLIDERS));

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.elements.buttons.buttonbackground.set"), setBackMenu);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureHover = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.backgroundTextureInactive = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true);

        setBackMenu.addSeparatorEntry("separator_before_slider_background_entries");

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_slider_background_texture",
                this.selfClass(),
                null,
                consumes -> consumes.element.sliderBackgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.sliderBackgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.normal"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "highlighted_slider_background_texture",
                        this.selfClass(),
                        null,
                        consumes -> consumes.element.sliderBackgroundTextureHighlighted,
                        (buttonEditorElement, iTextureResourceSupplier) -> {
                            buttonEditorElement.element.sliderBackgroundTextureHighlighted = iTextureResourceSupplier;
                        }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.highlighted"), true, null, true, true, true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.buttons.buttonbackground.slider.highlighted.desc")));

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_texture").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                this.selfClass(),
                consumes -> consumes.element.restartBackgroundAnimationsOnHover,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.restartBackgroundAnimationsOnHover = aBoolean,
                "fancymenu.elements.buttons.textures.restart_animated_on_hover");

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", this.selfClass(),
                consumes -> consumes.element.nineSliceCustomBackground,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.nineSliceCustomBackground = aBoolean,
                "fancymenu.elements.buttons.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", this.selfClass(),
                consumes -> consumes.element.nineSliceBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderX = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", this.selfClass(),
                consumes -> consumes.element.nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderY = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_y"), true, 5, null, null);

        buttonBackgroundMenu.addSeparatorEntry("separator_before_nine_slider_slider_handle_settings");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle", this.selfClass(),
                consumes -> consumes.element.nineSliceSliderHandle,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.nineSliceSliderHandle = aBoolean,
                "fancymenu.elements.slider.v2.handle.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_x", this.selfClass(),
                consumes -> consumes.element.nineSliceSliderHandleBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceSliderHandleBorderX = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_y", this.selfClass(),
                consumes -> consumes.element.nineSliceSliderHandleBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceSliderHandleBorderY = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"), true, 5, null, null);

    }

}
