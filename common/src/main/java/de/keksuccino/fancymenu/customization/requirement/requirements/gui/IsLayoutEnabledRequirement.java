package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsLayoutEnabledRequirement extends Requirement {

    public IsLayoutEnabledRequirement() {
        super("fancymenu_visibility_requirement_is_layout_enabled");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            for (Layout s : LayoutHandler.getEnabledLayouts()) {
                File f = s.layoutFile;
                if (f != null) {
                    return Files.getNameWithoutExtension(f.getName()).equals(value);
                }
            }
        }

        return false;

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_layout_enabled");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_layout_enabled.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.is_layout_enabled.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "my_cool_main_menu_layout";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
