package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import org.jetbrains.annotations.NotNull;

public class ButtonEditorElement extends AbstractEditorElement {

    public ButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        boolean isButton = (this.getElement().getWidget() instanceof AbstractButton);
        boolean isSlider = (this.getElement().getWidget() instanceof CustomizableSlider);

        this.rightClickMenu.addClickableEntry("manage_actions", Components.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
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

        //TODO übernehmen
        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Components.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
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

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        if (isSlider || isButton) {
            this.rightClickMenu.addSubMenuEntry("button_background", isButton ? Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground") : Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                    .setIcon(ContextMenu.IconFactory.getIcon("image"))
                    .setStackable(true);
        }

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationNormal = null;
                }, isButton ? Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal") : Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationHover = null;
                }, isButton ? Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover") : Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationInactive = null;
                }, isButton ? Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive") : Components.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true);

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
                Components.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", ButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.getElement().nineSliceBorderY = integer,
                Components.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_y"), true, 5, null, null);

        this.rightClickMenu.addSeparatorEntry("button_separator_2").setStackable(true);

        if (!isSlider) {

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).label,
                            (element1, s) -> ((ButtonElement)element1.element).label = s,
                            null, false, true, Components.translatable(isButton ? "fancymenu.editor.items.button.editlabel" : "fancymenu.editor.items.button.label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).hoverLabel,
                            (element1, s) -> ((ButtonElement)element1.element).hoverLabel = s,
                            null, false, true, Components.translatable(isButton ? "fancymenu.editor.items.button.hoverlabel" : "fancymenu.editor.items.button.hover_label.generic"),
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
                        Components.translatable("fancymenu.editor.items.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.getElement().clickSound = supplier,
                        Components.translatable("fancymenu.editor.items.button.clicksound"), true, null, true, true, true)
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
                        null, true, true, Components.translatable("fancymenu.editor.items.button.btndescription"),
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

    }

    protected ButtonElement getElement() {
        return (ButtonElement) this.element;
    }

}
