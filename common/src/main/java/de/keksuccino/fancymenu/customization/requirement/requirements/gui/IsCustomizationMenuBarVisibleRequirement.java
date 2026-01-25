package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class IsCustomizationMenuBarVisibleRequirement extends Requirement {

    public IsCustomizationMenuBarVisibleRequirement() {
        super("is_customization_menu_bar_visible");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return false;
        }
        if (ScreenCustomization.isScreenBlacklisted(screen.getClass().getName())) {
            return false;
        }
        return CustomizationOverlay.isOverlayVisible(screen);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_customization_menu_bar_visible");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_customization_menu_bar_visible.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
