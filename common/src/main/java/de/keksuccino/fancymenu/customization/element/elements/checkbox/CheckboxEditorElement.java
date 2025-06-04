package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class CheckboxEditorElement extends AbstractEditorElement {

    public CheckboxEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {
        
        super.init();
        
        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
                    ManageActionsScreen s = new ManageActionsScreen(this.getElement().getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().actionExecutor = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.checkbox.manage_actions.desc")))
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

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_1");

        this.addColorSettings();

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_3").setStackable(true);

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().hoverSound,
                        (checkboxEditorElement, supplier) -> checkboxEditorElement.getElement().hoverSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (checkboxEditorElement, supplier) -> checkboxEditorElement.getElement().clickSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.clicksound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_5").setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        consumes -> (consumes instanceof CheckboxEditorElement),
                        consumes -> {
                            String t = ((CheckboxElement)consumes.element).tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            ((CheckboxElement)element1.element).tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.editor.items.button.btndescription"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", CheckboxEditorElement.class,
                        consumes -> consumes.getElement().navigatable,
                        (checkboxEditorElement, aBoolean) -> checkboxEditorElement.getElement().navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));
        
    }

    protected void addColorSettings() {
        
        ContextMenu colorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("checkbox_colors", Component.translatable("fancymenu.elements.checkbox.colors"), colorMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(colorMenu, "border_color",
                        CheckboxEditorElement.class,
                        consumes -> (consumes.getElement().borderColor != null) ? consumes.getElement().borderColor.getHex() : null,
                        (element1, s) -> {
                            if (s != null) {
                                element1.getElement().borderColor = DrawableColor.of(s);
                            } else {
                                element1.getElement().borderColor = null;
                            }
                        },
                        null, false, false, Component.translatable("fancymenu.elements.checkbox.border_color"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.border_color.desc")));

        this.addStringInputContextMenuEntryTo(colorMenu, "background_color",
                        CheckboxEditorElement.class,
                        consumes -> (consumes.getElement().backgroundColor != null) ? consumes.getElement().backgroundColor.getHex() : null,
                        (element1, s) -> {
                            if (s != null) {
                                element1.getElement().backgroundColor = DrawableColor.of(s);
                            } else {
                                element1.getElement().backgroundColor = null;
                            }
                        },
                        null, false, false, Component.translatable("fancymenu.elements.checkbox.background_color"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_color.desc")));

        this.addImageResourceChooserContextMenuEntryTo(colorMenu, "checkmark_texture",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().checkmarkTexture,
                        (checkboxEditorElement, iTextureResourceSupplier) -> checkboxEditorElement.getElement().checkmarkTexture = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.checkbox.checkmark_texture"), true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.checkmark_texture.desc")));
        
    }

    protected CheckboxElement getElement() {
        return (CheckboxElement) this.element;
    }

}
