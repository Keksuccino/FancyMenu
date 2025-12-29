package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
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
                    ActionScriptEditorScreen s = new ActionScriptEditorScreen(this.element.getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.element.actionExecutor = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Component.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(this.element.activeStateSupplier.copy(false), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.element.activeStateSupplier = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_variable");

        this.element.variableMode.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.variable_mode.desc")));

        this.element.linkedVariable.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.editor.set_variable.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .addIsActiveSupplier((menu, entry) -> this.element.variableMode.tryGetNonNull());

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_1");

        this.addTextureSettings();

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_3").setStackable(true);

        this.element.hoverSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.element.clickSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_5").setStackable(true);

        this.element.tooltip.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.tooltip.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.element.navigatable.buildContextMenuEntryAndAddTo(this.rightClickMenu, this.selfClass(), this)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));
        
    }

    protected void addTextureSettings() {
        
        ContextMenu texturesMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("checkbox_textures", Component.translatable("fancymenu.elements.checkbox.textures"), texturesMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(true);

        this.element.backgroundTextureNormal.buildContextMenuEntryAndAddTo(texturesMenu, this.selfClass(), this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_normal.desc")));

        this.element.backgroundTextureHover.buildContextMenuEntryAndAddTo(texturesMenu, this.selfClass(), this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_hover.desc")));

        this.element.backgroundTextureInactive.buildContextMenuEntryAndAddTo(texturesMenu, this.selfClass(), this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_inactive.desc")));

        texturesMenu.addSeparatorEntry("separator_after_background_textures");

        this.element.checkmarkTexture.buildContextMenuEntryAndAddTo(texturesMenu, this.selfClass(), this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.checkmark_texture.desc")));
        
    }


}
