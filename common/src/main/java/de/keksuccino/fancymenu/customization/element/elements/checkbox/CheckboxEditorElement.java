package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class CheckboxEditorElement extends AbstractEditorElement<CheckboxEditorElement, CheckboxElement> {

    public CheckboxEditorElement(@NotNull CheckboxElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {
        
        super.init();
        
        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.actions.screens.manage_screen.manage"), (menu, entry) -> {
                    ActionScriptEditorScreen s = new ActionScriptEditorScreen(this.getElement().getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().actionExecutor = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.manage_actions.desc")))
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

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_variable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "variable_mode", CheckboxEditorElement.class,
                        consumes -> consumes.getElement().variableMode,
                        (checkboxEditorElement, aBoolean) -> checkboxEditorElement.getElement().variableMode = aBoolean,
                        "fancymenu.elements.checkbox.variable_mode")
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.variable_mode.desc")));

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable",
                        consumes -> (consumes instanceof CheckboxEditorElement),
                        consumes -> ((CheckboxElement)consumes.element).linkedVariable,
                        (element1, s) -> ((CheckboxElement)element1.element).linkedVariable = s,
                        null, false, false, Component.translatable("fancymenu.elements.checkbox.editor.set_variable"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.editor.set_variable.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .addIsActiveSupplier((menu, entry) -> this.getElement().variableMode);

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_1");

        this.addTextureSettings();

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_3").setStackable(true);

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().hoverSound,
                        (checkboxEditorElement, supplier) -> checkboxEditorElement.getElement().hoverSound = supplier,
                        Component.translatable("fancymenu.elements.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (checkboxEditorElement, supplier) -> checkboxEditorElement.getElement().clickSound = supplier,
                        Component.translatable("fancymenu.elements.button.clicksound"), true, null, true, true, true)
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
                        null, true, true, Component.translatable("fancymenu.elements.button.tooltip"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.tooltip.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", CheckboxEditorElement.class,
                        consumes -> consumes.getElement().navigatable,
                        (checkboxEditorElement, aBoolean) -> checkboxEditorElement.getElement().navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));
        
    }

    protected void addTextureSettings() {
        
        ContextMenu texturesMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("checkbox_textures", Component.translatable("fancymenu.elements.checkbox.textures"), texturesMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(texturesMenu, "background_texture_normal",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().backgroundTextureNormal,
                        (checkboxEditorElement, iTextureResourceSupplier) -> checkboxEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.checkbox.background_texture_normal"), true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_normal.desc")));

        this.addImageResourceChooserContextMenuEntryTo(texturesMenu, "background_texture_hover",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().backgroundTextureHover,
                        (checkboxEditorElement, iTextureResourceSupplier) -> checkboxEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.checkbox.background_texture_hover"), true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_hover.desc")));

        this.addImageResourceChooserContextMenuEntryTo(texturesMenu, "background_texture_inactive",
                        CheckboxEditorElement.class,
                        null,
                        consumes -> consumes.getElement().backgroundTextureInactive,
                        (checkboxEditorElement, iTextureResourceSupplier) -> checkboxEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.checkbox.background_texture_inactive"), true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_inactive.desc")));

        texturesMenu.addSeparatorEntry("separator_after_background_textures");

        this.addImageResourceChooserContextMenuEntryTo(texturesMenu, "checkmark_texture",
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
