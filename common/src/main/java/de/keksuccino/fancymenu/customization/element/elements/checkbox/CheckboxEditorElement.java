package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class CheckboxEditorElement extends AbstractEditorElement<CheckboxEditorElement, CheckboxElement> {

    public CheckboxEditorElement(@NotNull CheckboxElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.actionExecutor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        this.element.activeStateSupplier.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")));

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_variable");

        this.element.variableMode.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.variable_mode.desc")));

        this.element.linkedVariable.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.editor.set_variable.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .addIsActiveSupplier((menu, entry) -> this.element.variableMode.tryGetNonNull());

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_1");

        this.addTextureSettings();

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_3").setStackable(true);

        this.element.hoverSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.element.unhoverAudio.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.element.clickSound.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("checkbox_separator_5").setStackable(true);

        this.element.tooltip.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.tooltip.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.element.navigatable.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));

    }

    protected void addTextureSettings() {

        ContextMenu texturesMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("checkbox_textures", Component.translatable("fancymenu.elements.checkbox.textures"), texturesMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(true);

        this.element.backgroundTextureNormal.buildContextMenuEntryAndAddTo(texturesMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_normal.desc")));

        this.element.backgroundTextureHover.buildContextMenuEntryAndAddTo(texturesMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_hover.desc")));

        this.element.backgroundTextureInactive.buildContextMenuEntryAndAddTo(texturesMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.background_texture_inactive.desc")));

        texturesMenu.addSeparatorEntry("separator_after_background_textures");

        this.element.checkmarkTexture.buildContextMenuEntryAndAddTo(texturesMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.checkmark_texture.desc")));

    }

}
