package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class ButtonEditorElement extends AbstractEditorElement {

    public boolean showTemplateOptions = true;

    public ButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        boolean isButton = (this.getElement().getWidget() instanceof AbstractButton);
        boolean isSlider = (this.getElement().getWidget() instanceof CustomizableSlider);

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
                    ManageActionsScreen s = new ManageActionsScreen(this.getElement().getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().actionExecutor = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.button.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Component.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(this.getElement().activeStateSupplier.copy(false), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().activeStateSupplier = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("button_separator_1");

        this.addTextureOptions(isButton, isSlider);

        this.addSliderTextureOptionsForTemplateMode();

        this.rightClickMenu.addSeparatorEntry("button_separator_2").setStackable(true);

        if (!isSlider) {

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).label,
                            (element1, s) -> ((ButtonElement)element1.element).label = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.editor.items.button.editlabel" : "fancymenu.editor.items.button.label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).hoverLabel,
                            (element1, s) -> ((ButtonElement)element1.element).hoverLabel = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.editor.items.button.hoverlabel" : "fancymenu.editor.items.button.hover_label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.rightClickMenu.addSeparatorEntry("button_separator_3").setStackable(true);

        }

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().hoverSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.getElement().hoverSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.getElement().clickSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.clicksound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("button_separator_4").setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> {
                            String t = ((ButtonElement)consumes.element).tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            ((ButtonElement)element1.element).tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.editor.items.button.btndescription"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        if (!(this instanceof VanillaWidgetEditorElement)) {

            this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", ButtonEditorElement.class,
                            consumes -> consumes.getElement().navigatable,
                            (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().navigatable = aBoolean,
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

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "is_template", ButtonEditorElement.class,
                        consumes -> consumes.getElement().isTemplate,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().isTemplate = aBoolean,
                        "fancymenu.elements.button.is_template")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.template_settings.desc")));

        this.addCycleContextMenuEntryTo(templateSettingsMenu, "share_with", List.of(ButtonElement.TemplateSharing.values()), ButtonEditorElement.class,
                consumes -> consumes.getElement().templateShareWith,
                (buttonEditorElement, templateSharing) -> buttonEditorElement.getElement().templateShareWith = templateSharing,
                (menu, entry, switcherValue) -> switcherValue.getCycleComponent());

        templateSettingsMenu.addSeparatorEntry("separator_after_is_template");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_width", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyWidth,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyWidth = aBoolean,
                "fancymenu.elements.button.template_apply_width");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_height", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyHeight,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyHeight = aBoolean,
                "fancymenu.elements.button.template_apply_height");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_posx", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyPosX,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyPosX = aBoolean,
                "fancymenu.elements.button.template_apply_posx");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_posy", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyPosY,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyPosY = aBoolean,
                "fancymenu.elements.button.template_apply_posy");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_opacity", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyOpacity,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyOpacity = aBoolean,
                "fancymenu.elements.button.template_apply_opacity");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_visibility", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyVisibility,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyVisibility = aBoolean,
                "fancymenu.elements.button.template_apply_visibility");

        this.addToggleContextMenuEntryTo(templateSettingsMenu, "template_apply_label", ButtonEditorElement.class,
                consumes -> consumes.getElement().templateApplyLabel,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().templateApplyLabel = aBoolean,
                "fancymenu.elements.button.template_apply_label");

    }

    protected void addTextureOptions(boolean isButton, boolean isSlider) {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        if (isSlider || isButton) {
            this.rightClickMenu.addSubMenuEntry("button_background", isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                    .setIcon(ContextMenu.IconFactory.getIcon("image"))
                    .setStackable(true)
                    .addIsVisibleSupplier((menu, entry) -> !this.getElement().isTemplate || (this.getElement().templateShareWith == ButtonElement.TemplateSharing.BUTTONS));
        }

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier;
                }, isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_background").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                        ButtonEditorElement.class,
                        consumes -> consumes.getElement().restartBackgroundAnimationsOnHover,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().restartBackgroundAnimationsOnHover = aBoolean,
                        "fancymenu.helper.editor.items.buttons.textures.restart_animated_on_hover")
                .setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceCustomBackground,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().nineSliceCustomBackground = aBoolean,
                "fancymenu.helper.editor.items.buttons.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceBorderX = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceBorderY = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_y"), true, 5, null, null);

    }

    protected void addSliderTextureOptionsForTemplateMode() {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("slider_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(false)
                .addIsVisibleSupplier((menu, entry) -> this.getElement().isTemplate && (this.getElement().templateShareWith == ButtonElement.TemplateSharing.SLIDERS));

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true);

        setBackMenu.addSeparatorEntry("separator_before_slider_background_entries");

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_slider_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().sliderBackgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().sliderBackgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.normal"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "highlighted_slider_background_texture",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().sliderBackgroundTextureHighlighted,
                        (buttonEditorElement, iTextureResourceSupplier) -> {
                            buttonEditorElement.getElement().sliderBackgroundTextureHighlighted = iTextureResourceSupplier;
                        }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted"), true, null, true, true, true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted.desc")));

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_texture").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                        ButtonEditorElement.class,
                        consumes -> consumes.getElement().restartBackgroundAnimationsOnHover,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().restartBackgroundAnimationsOnHover = aBoolean,
                        "fancymenu.helper.editor.items.buttons.textures.restart_animated_on_hover");

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceCustomBackground,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().nineSliceCustomBackground = aBoolean,
                "fancymenu.helper.editor.items.buttons.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceBorderX = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceBorderY = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_y"), true, 5, null, null);

        buttonBackgroundMenu.addSeparatorEntry("separator_before_nine_slider_slider_handle_settings");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceSliderHandle,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.getElement().nineSliceSliderHandle = aBoolean,
                "fancymenu.elements.slider.v2.handle.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_x", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceSliderHandleBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceSliderHandleBorderX = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_y", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceSliderHandleBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceSliderHandleBorderY = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"), true, 5, null, null);

    }

    protected ButtonElement getElement() {
        return (ButtonElement) this.element;
    }

}
